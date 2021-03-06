/**
 * Copyright (C) 2015 Bonitasoft S.A.
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
package org.bonitasoft.migration.version.to7_3_0

import org.bonitasoft.migration.core.MigrationContext
import org.bonitasoft.migration.core.MigrationStep

import javax.activation.MimetypesFileTypeMap
import java.nio.file.Files

/**
 *
 * @author Laurent Leseigneur
 */
class MigrateAvatar extends MigrationStep {


    private static final MimetypesFileTypeMap MIMETYPES_FILE_TYPE_MAP = new MimetypesFileTypeMap();
    public static final int ICON_SEQUENCE_ID = 27

    @Override
    def execute(MigrationContext context) {
        MIMETYPES_FILE_TYPE_MAP.addMimeTypes("image/png png PNG");
        MIMETYPES_FILE_TYPE_MAP.addMimeTypes("image/gif gif GIF");
        MIMETYPES_FILE_TYPE_MAP.addMimeTypes("image/jpeg jpeg jpg jpe JPG");
        def helper = context.databaseHelper
        helper.executeScript("icon", "icon")
        def Map<Long, Long> ids = [:]
        context.sql.rows("select t.id from tenant t").each {
            ids.put(it.id as Long, 1)
        }
        migrateIconForTable(ids, context, "user_")
        migrateIconForTable(ids, context, "group_")
        migrateIconForTable(ids, context, "role")
        context.databaseHelper.insertSequences(ids, context, ICON_SEQUENCE_ID)
        helper.executeScript("icon", "drop")
    }


    @Override
    String getDescription() {
        return "Migrate user, group and role icons in database"
    }

    def migrateIconForTable(Map ids, MigrationContext context, String table) {
        context.databaseHelper.sql.eachRow("SELECT tenantid, iconpath, id FROM " + table + " WHERE iconpath IS NOT null") { row ->

            def tenantId = String.valueOf(row.tenantid)
            def iconPath = row.iconpath as String
            if (iconPath == null || iconPath.isEmpty()) {
                return
            }
            def icon = context.bonitaHome.toPath().resolve("client").resolve("tenants").resolve(tenantId).resolve("work").resolve("icons").resolve(iconPath.startsWith("/") ? iconPath.substring(1) : iconPath)
            if (!Files.isReadable(icon)) {
                context.logger.info "user icon ${icon} does not exists in file system. Skip icon migration"
                return
            }
            context.logger.info "store user icon ${icon} in database"
            def iconId = getNextId(ids, Long.valueOf(tenantId))
            context.sql.executeInsert("INSERT INTO icon (tenantid, id, mimetype, content) VALUES (${row.tenantid}, ${iconId}, ${MIMETYPES_FILE_TYPE_MAP.getContentType(icon.toFile()) ?: "image/png"}, ${icon.bytes})")
            context.sql.executeUpdate("UPDATE " + table + " SET iconid = ? WHERE " + table + ".tenantid = ? AND " + table + ".id = ?", iconId, row.tenantid, row.id)
        }
    }

    def getNextId(Map map, Long tenantId) {
        if (!map.containsKey(tenantId)) {
            map.put(tenantId, 1)
        }
        map.put(tenantId, map.get(tenantId) + 1)
    }
}
