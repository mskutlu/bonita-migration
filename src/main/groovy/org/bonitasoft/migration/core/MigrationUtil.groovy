/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.bonitasoft.migration.core.exception.NotFoundException

import java.sql.ResultSet

/**
 *
 * Util classes that contains common methods for migration
 *
 * @author Baptiste Mesta
 * @author Celine Souchet
 *
 */
class MigrationUtil {

    public final static String AUTO_ACCEPT = "auto.accept"

    public final static String REQUEST_SEPARATOR = "@@"

    public static read = System.in.newReader().&readLine

    static boolean isAutoAccept() {
        return System.getProperty(AUTO_ACCEPT) == "true"
    }

    /**
     * Get a single property and print it
     * First it try to get it from system (in order to override properties)
     * then from the given property object
     */
    static String getAndPrintProperty(Properties properties, String propertyName, boolean isMandatory) {
        if (properties == null || propertyName == null || "".equals(propertyName)) {
            throw new IllegalArgumentException("Can't execute getAndPrintProperty method with arguments : properties = " + properties + ", propertyName = " + propertyName)
        }
        def String property = System.getProperty(propertyName)
        if (property != null) {
            println "" + propertyName + " = " + property
            return property
        }
        property = properties.getProperty(propertyName)
        if (property != null) {
            property = property.trim()
            println "" + propertyName + " = " + property
            return property
        }
        if (isMandatory) {
            throw new NotFoundException("The property " + propertyName + " doesn't exist !!")
        }
    }

    /**
     * Execute a feature migration script
     *
     * @param gse
     * @param file
     * @param scriptName
     * @param binding
     * @param startMigrationDate
     * @return
     */
    static executeMigration(GroovyScriptEngine gse, File file, String scriptName, Binding binding, Date startMigrationDate) {
        def startDate = new Date()
        DisplayUtil.executeWrappedWithTabs {
            gse.run(new File(file, scriptName).getPath(), binding)
        }
        printSuccessMigration(startDate, startMigrationDate)
    }

    static printSuccessMigration(Date startFeatureDate, Date startMigrationDate) {
        if (startFeatureDate == null || startMigrationDate == null) {
            throw new IllegalArgumentException("Can't execute printSuccessMigration method with arguments : startFeatureDate = " + startFeatureDate + ", startMigrationDate = " + startMigrationDate)
        }

        def endFeatureDate = new Date()
        def logger = new Logger()
        logger.info("| --> Migration step success in " + TimeCategory.minus(endFeatureDate, startFeatureDate) + ". Migration started " + TimeCategory.minus(endFeatureDate, startMigrationDate) + " ago. --")
    }

    static Sql getSqlConnection(String dburl, String user, String pwd, String driverClass) {
        return Sql.newInstance(dburl, user, pwd, driverClass)
    }

    static executeDefaultSqlFile(File file, String dbVendor, Sql sql) {
        executeSqlFile(file, dbVendor, null, null, sql, true)
    }

    /**
     * execute a sql file
     * @param feature
     *      the folder where the feature to migrate is
     * @param dbVendor
     *      the current database type
     * @param suffix
     *      the suffix of the file to execute, format is: <dbVendor>-<suffixe>.sql
     * @param parameters
     *      parameters to replace inside the file, if parameter 'tenant' is given, all word 'tenant' will be replace by the value
     * @param sql
     *      the sql connection to the database
     * @param toUpdate
     *      if this will update elements
     */

    static executeSqlFile(File feature, String dbVendor, String suffix, Map<String, String> parameters, Sql sql, boolean toUpdate) {
        def sqlFile = getSqlFile(feature, dbVendor, suffix)
        if (sqlFile.exists()) {
            def List<String> contents = getSqlContent(sqlFile.text, parameters)

            for (content in contents) {
                if (!content.trim().empty) {
                    if (toUpdate) {
                        def count = sql.executeUpdate(content)
                        if (count > 0) {
                            println count + " row(s) updated"
                        }
                    } else {
                        sql.execute(content)
                    }
                }
            }
        } else {
            println "nothing to execute"
        }
    }

    static List<String> getSqlContent(String sqlFileContent, Map<String, String> parameters) {
        def sqlFileContentWithParameters = replaceParameters(sqlFileContent, parameters).replaceAll("\r", "")
        return Arrays.asList(sqlFileContentWithParameters.split(REQUEST_SEPARATOR))
    }

    static File getSqlFile(File folder, String dbVendor, String suffix) {
        if (folder == null || dbVendor == null || "".equals(dbVendor)) {
            throw new IllegalArgumentException("Can't execute getSqlFile method with arguments : folder = " + folder + ", dbVendor = " + dbVendor)
        }
        return new File(folder, dbVendor + (suffix == null || suffix.isEmpty() ? "" : "-" + suffix) + ".sql")
    }

    static String replaceParameters(String sqlFileContent, Map<String, String> parameters) {
        if (sqlFileContent == null) {
            throw new IllegalArgumentException("Can't execute replaceParameters method with arguments : sqlFileContent = " + sqlFileContent)
        }

        String newSqlFileContent = sqlFileContent
        if (parameters != null) {
            for (parameter in parameters) {
                newSqlFileContent = newSqlFileContent.replaceAll(parameter.key, String.valueOf(parameter.value))
            }
        }
        return newSqlFileContent
    }

    static Version getPlatformVersion(Sql sql) {
        return Version.valueOf(sql.firstRow("SELECT version FROM platform")[0] as String)
    }

    static Object getId(File feature, String dbVendor, String fileExtension, Object it, Sql sql) {
        if (it == null || sql == null) {
            throw new IllegalArgumentException("Can't execute getId method with arguments : it = " + it + ", sql = " + sql)
        }

        def sqlFile = getSqlFile(feature, dbVendor, fileExtension)
        def parameters = Collections.singletonMap(":tenantId", String.valueOf(it))
        def id = null
        sql.eachRow(getSqlContent(sqlFile.text, parameters).get(0)) { row ->
            id = row[0]
        }
        return id
    }

    /**
     * Return a list of ids. 
     * The SQL file to execute need to begin by "SELECT id FROM...".
     */

    static List<Long> getIds(File feature, String dbVendor, String fileExtension, Map<String, String> parameters, Sql sql) {
        if (sql == null) {
            throw new IllegalArgumentException("Can't execute getId method with arguments : sql = " + sql)
        }
        def sqlFile = getSqlFile(feature, dbVendor, fileExtension)
        def ids = []
        sql.query(getSqlContent(sqlFile.text, parameters).get(0)) { ResultSet rs ->
            while (rs.next())
                ids.add(rs.getLong(1))
        }
        return ids
    }

    static List<Object> getTenantsId(String dbVendor, groovy.sql.Sql sql) {
        if (sql == null) {
            throw new IllegalArgumentException("Can't execute getTenantsId method with arguments : sql = " + sql)
        }
        def tenants = []

        sql.query("SELECT id FROM tenant ORDER BY id ASC") { ResultSet rs ->
            while (rs.next()) tenants.add(rs.getLong(1))
        }
        return tenants
    }

    static migrateDirectory(String fromDir, String toDir, boolean deleteOldDirectory) {
        if (fromDir == null || toDir == null) {
            throw new IllegalArgumentException("Can't execute migrateDirectory method with arguments : fromDir = " + fromDir + ", toDir = " + toDir)
        }
        def fileFromDir = new File(fromDir)
        def fileToDir = new File(toDir)

        if (!fileFromDir.exists() || !fileFromDir.isDirectory()) {
            throw new IllegalStateException("Migration failed. Source folder does not exist : " + fromDir)
        }

        if (!deleteOldDirectory) {
            println "Adding/overwriting content in $toDir..."
        } else {
            IOUtil.deleteDirectory(fileToDir)
        }
        IOUtil.copyDirectory(fileFromDir, fileToDir)
    }

    static void askIfWeContinue() {
        if (!isAutoAccept()) {
            println "Continue migration? (yes/no): "
            def String input = read()
            if (input != "yes") {
                println "Migration cancelled"
                System.exit(0)
            }
        }
    }

    static String askForOptions(List<String> options) {
        def input = null
        while (true) {
            options.eachWithIndex { it, idx ->
                println "${idx + 1} -- $it "
            }
            println "choice: "
            input = read()
            try {
                def choiceNumber = Integer.valueOf(input) - 1 //index in the list is -1
                if (choiceNumber <= options.size()) {
                    return options.get(choiceNumber)
                }
            } catch (Exception e) {
            }
            println "Invalid choice, please enter a value between 1 and ${options.size()}"
        }
    }

    static getNexIdsForTable(Sql sql, long sequenceId) {
        def idsByTenants = [:]
        sql.eachRow("SELECT tenantid,nextId from sequence WHERE id = $sequenceId") { row ->
            idsByTenants.put(row[0], row[1])
        }
        println "next id by tenants for sequence id $sequenceId: $idsByTenants"
        return idsByTenants
    }

    static updateNextIdsForTable(Sql sql, long sequenceId, Map nexIdsByTenants) {
        nexIdsByTenants.each {
            println "update next id to $it.value for sequence id $sequenceId on tenant $it.key"
            println sql.executeUpdate("UPDATE sequence SET nextId = $it.value WHERE tenantId = $it.key and id = $sequenceId") + " row(s) updated"
        }
    }
}

