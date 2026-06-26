package com.example.reportgenerator.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LabReportXmlParserLegacyExportTest {
    @Test
    void parsesLegacyExportWithDoctypeStylesheetAndFooterOutsideFileRoot() {
        String xml = """
                <?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>
                <!DOCTYPE File SYSTEM \"laboratory.dtd\">
                <?xml-stylesheet type=\"text/xsl\" href=\"laboratory-english.xsl\"?>
                <File><Record><Header>
                  <ReferenceNumber>2605066005</ReferenceNumber>
                  <XML_TargetLang>Deutsch</XML_TargetLang>
                  <CodePhysicianUCM>?</CodePhysicianUCM>
                  <SSNPatient>19950319XXXXX</SSNPatient>
                  <ACCREDITION>1</ACCREDITION>
                </Header><Body><Destination><Organisation><OrganisationName>TESTD Testarzt</OrganisationName><OrganisationID>TESTD</OrganisationID><ContactPerson><MedTitle>Dr. med.</MedTitle><LastName>TESTD</LastName><FirstName>Testarzt</FirstName></ContactPerson></Organisation></Destination>
                <Patient><Person><LastName>-TESTER</LastName><FirstName>CHEMIE 22</FirstName><Sex>1</Sex><Birthdate>1995-03-19</Birthdate><InternalID>19950319TESC07</InternalID><KorrID>1161314</KorrID></Person></Patient>
                <ValidationDate>2026-06-18 13:01:00</ValidationDate><SpecimenDate>2026-05-06 08:53:00</SpecimenDate><ExaminationStatus>Complet</ExaminationStatus></Body>
                <Result><Title>Hématologie (EDTA)</Title><CodeTestInternal></CodeTestInternal></Result>
                <Result><CodeTestInternal>HCT</CodeTestInternal><DescriptionInternal>Hématokrit</DescriptionInternal><ResultValue>34</ResultValue><Alarm>-</Alarm><Validator>Christoph Kleifges</Validator></Result>
                <Result><CodeTestInternal>EOA</CodeTestInternal><ResultValue>2000</ResultValue><Alarm>+</Alarm><Validator>Christoph Kleifges</Validator></Result>
                </Record></File>
                <Footer><Befund_Validator>Dieser Befund wurde medizinisch validiert.</Befund_Validator></Footer>
                """;

        var report = new LabReportXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));

        assertThat(report.referenceNumber()).isEqualTo("2605066005");
        assertThat(report.patient().internalId()).isEqualTo("19950319TESC07");
        assertThat(report.resultCount()).isEqualTo(2);
        assertThat(report.abnormalResultCount()).isEqualTo(2);
        assertThat(report.sections()).containsExactly("Hématologie (EDTA)");
        assertThat(report.footer().fields()).containsEntry("Befund_Validator", "Dieser Befund wurde medizinisch validiert.");
    }
}
