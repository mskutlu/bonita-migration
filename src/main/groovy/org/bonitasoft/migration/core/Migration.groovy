/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

package org.bonitasoft.migration.core

import com.github.zafarkhaja.semver.Version

/**
 *
 * Get all versions and steps to execute and launch the migration runner with it
 *
 * @author Baptiste Mesta
 */
class Migration {

    public static final Version FIRST_VERSION_WITHOUT_BONITA_HOME = Version.valueOf("7.3.0")

    // This list contains the steps in which we cannot stop. Migration will execute steps but slide until next version:
    public static List<Version> TRANSITION_VERSIONS = ["7.7.0"].collect {
        Version.valueOf(it)
    }
    Logger logger = new Logger()
    MigrationContext context
    DisplayUtil displayUtil = new DisplayUtil()

    Migration() {
        this.context = new MigrationContext(logger: logger)
    }

    Migration(MigrationContext context) {
        this.context = context
    }

    public static void main(String[] args) {
        new Migration().run(false)
    }

    public void run(boolean isSp) {
        setupOutputs()
        printMigrationBannerAndGlobalWarnings(isSp)
        context.loadProperties()

        context.openSqlConnection()
        def versionMigrations = getMigrationVersionsToRun()

        def runner = getRunner(versionMigrations)
        runner.run(isSp)
        context.closeSqlConnection()
    }

    protected MigrationRunner getRunner(List<VersionMigration> versionMigrations) {
        new MigrationRunner(versionMigrations: versionMigrations, context: context, logger: logger, displayUtil: displayUtil)
    }

    /**
     * get a version as string and return the class of the migration step
     */
    def Closure toVersionMigrationInstance = { Version it ->
        def versionUnderscored = it.toString().replace(".", "_")
        def versionMigrationClass
        def className = "com.bonitasoft.migration.version.to${versionUnderscored}.MigrateTo$versionUnderscored"
        try {
            logger.debug("Trying to find " + className)
            versionMigrationClass = Thread.currentThread().contextClassLoader.loadClass(className)
        } catch (ClassNotFoundException ignored) {
            logger.debug("Unable to find " + className)
            versionMigrationClass = Thread.currentThread().contextClassLoader.loadClass("org.bonitasoft.migration.version.to${versionUnderscored}.MigrateTo$versionUnderscored")
        }
        logger.debug("Using " + versionMigrationClass)
        return versionMigrationClass.newInstance(version: it, logger: logger)
    }


    def List<VersionMigration> getMigrationVersionsToRun() {
        def version = Version.valueOf(getPlatformVersion().normalVersion)
        verifyPlatformIsValid(version)
        logger.info("Detected version in database: " + version)
        if (version < FIRST_VERSION_WITHOUT_BONITA_HOME) {
            verifyPlatformIsTheSameInBonitaHome(version)
        }
        context.sourceVersion = version
        def versions = getVersionsAfter(version)
        def visibleVersions = filterOutInvisibleVersions(versions)
        if (context.targetVersion == null) {
            println "Enter the target version"
            context.targetVersion = Version.valueOf(MigrationUtil.askForOptions(visibleVersions.collect {
                it.toString()
            }))
        }
        verifyTargetVersionIsValid(visibleVersions)
        return getVersionsToExecute(versions)
    }

    static List<Version> filterOutInvisibleVersions(List<Version> versions) {
        versions.findAll { !TRANSITION_VERSIONS.contains(it) }
    }

    def verifyPlatformIsTheSameInBonitaHome(Version version) {
        if (!version.equals(getBonitaHomeVersion())) {
            //invalid case: given source (if any) not the same as version in db and as version in bonita home
            logger.error("The versions are not consistent:")
            logger.error("The version of the database is ${version}")
            logger.error("The version of the bonita home is ${getBonitaHomeVersion()}")
            logger.error("Check that you configuration is correct and restart the migration")
            throw new IllegalStateException("Versions are not consistent, see logs")
        }
    }

    def verifyPlatformIsValid(Version platformVersionInDatabase) {
        if (platformVersionInDatabase.majorVersion != 7) {
            throw new IllegalStateException("Sorry, but this tool can't manage version under 7.0.0")
        }
    }

    private Version getPlatformVersion() {
        return MigrationUtil.getPlatformVersion(context.sql)
    }

    private Version getBonitaHomeVersion() {
        if (context.bonitaHome == null) {
            throw new IllegalStateException("The property bonita.home is neither set in system property nor in the configuration file")
        }
        def File versionFile = context.bonitaHome.toPath().resolve("engine-server/work/platform/VERSION").toFile()
        if (!versionFile.exists()) {
            throw new IllegalStateException("The bonita home does not exists or is not consistent, the file $versionFile.path does not exists")
        }
        return Version.valueOf(versionFile.text)
    }

    def verifyTargetVersionIsValid(List<Version> possibleTarget) {
        if (context.targetVersion < context.sourceVersion) {
            throw new IllegalStateException("The target version $context.targetVersion can not be before source version $context.sourceVersion")
        }
        if (context.targetVersion == context.sourceVersion) {
            throw new IllegalStateException("The version is already in $context.sourceVersion")
        }
        if (!possibleTarget?.contains(context.targetVersion)) {
            if (System.getProperty("ignore.invalid.target.version") != null && TRANSITION_VERSIONS.contains(context.targetVersion)) {
                // only accept this hidden sysprop "ignore.invalid.target.version" if the targetVersion is in the list of invisible transition versions:
                logger.info("Ignoring normally-forbidden target version $context.targetVersion (for tests only)")
            } else {
                throw new IllegalStateException("$context.targetVersion is not yet handled by this version of the migration tool")
            }
        }
    }

    private List<VersionMigration> getVersionsToExecute(List<Version> versions) {
        return versions.subList(versions.indexOf(context.sourceVersion) + 1, versions.indexOf(context.targetVersion) + 1).collect(toVersionMigrationInstance) as List<VersionMigration>
    }

    private List<Version> getVersionsAfter(Version sourceVersion) {
        Properties migrationProperties = getMigrationProperties()
        def versionsAsString = migrationProperties.getProperty("versions")
        def allVersions = parseVersionsFromMigrationProperties(versionsAsString)
        def indexOfSourceVersion = allVersions.indexOf(sourceVersion)
        if (indexOfSourceVersion == -1) {
            throw new IllegalStateException("Sorry the version $sourceVersion can not be migrated using this migration tool")
        }
        return allVersions.subList(indexOfSourceVersion + 1, allVersions.size())
    }

    private static List<Version> parseVersionsFromMigrationProperties(String versionsAsString) {
        return versionsAsString.substring(1, versionsAsString.length() - 1).split(",").collect { it.trim() }.collect {
            Version.valueOf(it)
        }
    }

    private Properties getMigrationProperties() {
        return loadFromClasspath("/bonita-versions.properties")
    }

    private Properties getProjectProperties() {
        return loadFromClasspath("/bonita-migration-info.properties")
    }

    private Properties loadFromClasspath(String name) {
        def properties = new Properties()
        this.class.getResourceAsStream(name).withStream {
            properties.load(it)
        }
        return properties
    }

    static def setupOutputs() {
        def logInFile = new FileOutputStream(new File("migration-" + new Date().format("yyyy-MM-dd-HHmmss") + ".log"))
        System.setOut(new PrintStream(new SplitPrintStream(System.out, logInFile)))
        System.setErr(new PrintStream(new SplitPrintStream(System.err, logInFile)))
    }


    def printMigrationBannerAndGlobalWarnings(boolean isSp) {
        def migrationToolVersion = getProjectProperties().getProperty("migration.tool.version", "DEV")
        displayUtil.printInRectangle("", "Bonita migration tool ${migrationToolVersion} ${Edition.from(isSp).displayName} edition", "",
                "This tool will migrate your installation of Bonita.",
                "Both database and bonita home will be modified.",
                "Please refer to the documentation for further steps to completely migrate your production environment.",
                "",
                "Warning:",
                "Back up the database AND the bonita home before migrating",
                "If you have a custom Look & Feel, test and update it, if it's necessary when the migration is finished.",
                "If you have customized the configuration of your bonita home, reapply the customizations when the migration is finished.", "")
    }
}
