/*
 * Copyright (C) 2016 Bonitasoft S.A.
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
 */
package org.bonitasoft.migration.version.to7_4_0

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Laurent Leseigneur
 */
class MigrateTo7_4_0Test extends Specification {

    @Unroll
    def "should migration to 7.4.0 include #stepName step"(def stepName) {
        given:
        def migrateTo740 = new MigrateTo7_4_0()

        expect:
        def steps = migrateTo740.migrationSteps
        steps.collect {
            it.class.getSimpleName()
        }.contains(stepName) == true

        where:
        stepName << [
                "MigrateProcessDefinitionXmlWithXSD"
                , "RemoveEventHandlingJob"
                , "WarnAboutCSRF"
                , "IncreaseVersionField"
                , "FixProcessPermissionRuleScript"
                , "UpdateCompoundPermissionMapping"
                , "FixProcessSupervisorPermissionRuleScript"
                , "AddAvatarPermission"
                , "AddCSRFCookieSecure"
                , "FixJarJarDependencyName"
        ]


    }
}
