package com.example.reportgenerator.parser;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class LabReportXmlParserTest { @Test void parsesSample() throws Exception{ var r=new LabReportXmlParser().parse(getClass().getResourceAsStream("/samples/ExportMedLogin2605066005.xml")); assertThat(r.referenceNumber()).isEqualTo("2605066005"); assertThat(r.patient().internalId()).isEqualTo("P123"); assertThat(r.resultCount()).isEqualTo(2); assertThat(r.abnormalResultCount()).isEqualTo(1); assertThat(r.sections()).contains("Hématologie","Biochimie"); }}
