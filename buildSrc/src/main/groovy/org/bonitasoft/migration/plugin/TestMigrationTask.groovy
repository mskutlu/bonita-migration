package org.bonitasoft.migration.plugin

import com.github.zafarkhaja.semver.Version
import org.bonitasoft.migration.plugin.db.DatabaseResourcesConfigurator
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

import static org.bonitasoft.migration.plugin.VersionUtils.dotted
import static org.bonitasoft.migration.plugin.VersionUtils.underscored

/**
 * @author Baptiste Mesta.
 */
class TestMigrationTask extends Test {

    private String bonitaVersion
    private boolean isSP

    @Override
    void executeTests() {
        def testValues = DatabaseResourcesConfigurator.getDatabaseConnectionSystemProperties(project)
        if (isSP) {
            // From 7.3.0, 'bonita.client.home' is the default key used by EngineStarterSP to retrieve licenses:
            testValues.put("bonita.client.home", String.valueOf(project.buildDir) + "/licenses")
        }
        if (Version.valueOf(bonitaVersion) <= Version.valueOf("7.3.0")) {
            testValues.put("bonita.home", String.valueOf(project.buildDir.absolutePath + File.separator +
                    "bonita-home-" + dotted(bonitaVersion) + File.separator + "bonita-home-to-migrate"))
        }
        setSystemProperties testValues

        def property = project.property('org.gradle.jvmargs')
        if (property) {
            println "Using extra property 'org.gradle.jvmargs=$property'"
            jvmArgs property.toString().split(" ")
        }
        def sysProperty = System.getProperty("org.gradle.jvmargs")
        if (sysProperty) {
            println "Using extra property 'org.gradle.jvmargs=$property'"
            jvmArgs sysProperty.split(" ")
        }

        super.executeTests()
    }


    def configureBonita(Project project, String bonitaVersion, boolean isSP) {
        this.isSP = isSP
        this.bonitaVersion = bonitaVersion
        testClassesDirs = project.sourceSets.enginetest.output.classesDirs
        classpath = project.files(project.sourceSets.enginetest.runtimeClasspath,
                project.getConfigurations().getByName(underscored(bonitaVersion)),
                project.getConfigurations().getByName("drivers"))
        //add as input the database configuration, tests must  be relaunched when database configuration change
        inputs.property("dbvendor", project.extensions.database.dbvendor)
        inputs.property("dburl", project.extensions.database.dburl)

        if (Version.valueOf(bonitaVersion) < Version.valueOf("7.2.0")) {
            include "**/*Before7_2_0DefaultTest*"
        } else {
            include "**/*After7_2_0DefaultTest*"
        }
        include "**/*To" + underscored(bonitaVersion) + (isSP ? "SP" : "") + "*"
    }

}
