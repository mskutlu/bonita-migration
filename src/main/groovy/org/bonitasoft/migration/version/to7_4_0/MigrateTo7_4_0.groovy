/**
 * Copyright (C) 2016 Bonitasoft S.A.
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
package org.bonitasoft.migration.version.to7_4_0

import org.bonitasoft.migration.core.MigrationStep
import org.bonitasoft.migration.core.VersionMigration
import org.bonitasoft.migration.version.to7_3_1.AddAvatarPermission
import org.bonitasoft.migration.version.to7_3_1.FixProcessPermissionRuleScript
import org.bonitasoft.migration.version.to7_3_1.UpdateCompoundPermissionMapping
import org.bonitasoft.migration.version.to7_3_3.FixProcessSupervisorPermissionRuleScript

/**
 */
class MigrateTo7_4_0 extends VersionMigration {

    @Override
    def List<MigrationStep> getMigrationSteps() {
        //keep one line per step to avoid false-positive merge conflict
        [
                new MigrateProcessDefinitionXmlWithXSD(),
                new RemoveEventHandlingJob(),
                new WarnAboutCSRF(),
                new IncreaseVersionField(),
                new UpdatePermissionMappingProperties(),
                new FixProcessPermissionRuleScript(),
                new UpdateCompoundPermissionMapping(),
                new FixProcessSupervisorPermissionRuleScript(),
                new AddAvatarPermission()
                , new AddCSRFCookieSecure()
        ]
    }
}