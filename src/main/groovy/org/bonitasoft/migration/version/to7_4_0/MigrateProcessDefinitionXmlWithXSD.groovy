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

package org.bonitasoft.migration.version.to7_4_0

import org.bonitasoft.migration.core.Logger
import org.bonitasoft.migration.core.MigrationContext
import org.bonitasoft.migration.core.MigrationStep
import org.bonitasoft.migration.version.to7_2_0.MigrateProcessDefXml
import org.codehaus.groovy.runtime.InvokerHelper

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import static javax.xml.transform.TransformerFactory.newInstance

/**
 * @author Emmanuel Duchastenier
 */
class MigrateProcessDefinitionXmlWithXSD extends MigrationStep {

    public static final String PROCESS_DEFINITION_7_4 = "/version/to_7_4_0/ProcessDefinition.xsd"
    public static final String PROCESS_DEFINITION_XSL = "/version/to_7_4_0/ProcessDefinition.xsl"
    public static final String GET_ALL_PROCESS_CONTENT = "SELECT * FROM process_content"

    Logger logger
    private List<Exception> errors = new ArrayList<>()

    @Override
    def execute(MigrationContext context) {
        this.logger = context.logger
        def rows = context.sql.rows(GET_ALL_PROCESS_CONTENT)
        rows.each { processContent ->
            def originalContent = context.databaseHelper.getClobContent(processContent.content)
            migrateProcessContent(context, originalContent, processContent.tenantid, processContent.id)
        }
        if (errors) {
            throw new IllegalStateException("some processes could not be migrated. see error log for more information")
        }

    }

    def migrateProcessContent(MigrationContext context, String originalContent, tenantId, processId) {
        def migratedXML
        try {
            context.logger.debug("Process Definition before migration:\n$originalContent")
            migratedXML = migrateProcessDefinitionXML(originalContent)
            context.logger.debug("Process Definition migrated to 7.4:\n$migratedXML")
            if (migratedXmlIsValidAgainstXSD(migratedXML)) {
                context.sql.executeUpdate("UPDATE process_content SET content = $migratedXML WHERE tenantid=${tenantId} AND id=${processId}")
            }
        }
        catch (Exception e) {
            registerErrorOnProcessMigration(context, e, originalContent, migratedXML)
        }


    }

    private void registerErrorOnProcessMigration(MigrationContext context, Exception e, String originalContent, migratedXML) {
        context.logger.error """ 
            failed to migrate process definition due to ${e.getMessage()}  
            original content:
            -----------------
            ${originalContent}
            migrated xml :
            --------------
            ${migratedXML}"""
        this.errors.add(e)
    }


    boolean migratedXmlIsValidAgainstXSD(String xmlContent) {
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new StreamSource(this.getClass().getResourceAsStream(PROCESS_DEFINITION_7_4)))
                .newValidator()
                .validate(new StreamSource(new StringReader(xmlContent)))
        true
    }

    String migrateProcessDefinitionXML(String processDefinitionXMLAsText) {
        def processDefNode = new XmlParser().parseText(processDefinitionXMLAsText)
        // remove potential extra <flowNodes> tags (already fixed in 7.2.0, but not for customer who have migrated
        // before step 7.2.0 was fixed):
        new MigrateProcessDefXml().removeFlowNodes(processDefNode)
        applyChangesOnXml(processDefNode)
    }

    protected String applyChangesOnXml(Node processDefinitionXml) {
        def transformer = newInstance().newTransformer(new StreamSource(this.getClass().getResourceAsStream(PROCESS_DEFINITION_XSL)))
        StringWriter stringWriter = new StringWriter()
        transformer.transform(new StreamSource(new StringReader(getContent(processDefinitionXml))), new StreamResult(stringWriter))
        stringWriter.toString()
    }

    String getContent(Node processDefinitionXml) {
        def writer = new StringWriter()
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")

        def printer = new XmlNodePrinter(new PrintWriter(writer)) {
            protected void printSimpleItem(Object value) {
                if (!preserveWhitespace) printLineBegin()
                out.print(InvokerHelper.toString(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'))
                if (!preserveWhitespace) printLineEnd()
            }
        }
        printer.setPreserveWhitespace(true)
        printer.setExpandEmptyElements(false)
        printer.print(processDefinitionXml)

        writer.toString()
    }

    @Override
    String getDescription() {
        return "Update process definition xml to the new 7.4 format"
    }

}
