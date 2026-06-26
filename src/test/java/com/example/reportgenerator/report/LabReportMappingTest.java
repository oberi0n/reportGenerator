package com.example.reportgenerator.report;
import com.example.reportgenerator.parser.LabReportXmlParser;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class LabReportMappingTest { @Test void derivesStorageAndValidators(){ var r=new LabReportXmlParser().parse(getClass().getResourceAsStream("/samples/ExportMedLogin2605066005.xml")); assertThat(r.patientStorageId()).isEqualTo("P123"); assertThat(r.validators()).containsExactly("VAL1","VAL2"); }}
