package com.example.reportgenerator.parser;

import com.example.reportgenerator.model.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.InputStream;
import java.util.*;

@Component
public class LabReportXmlParser {
  private final XPath xp = XPathFactory.newInstance().newXPath();
  public LabReport parse(InputStream in) {
    try {
      var f = DocumentBuilderFactory.newInstance(); f.setNamespaceAware(false); f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document d = f.newDocumentBuilder().parse(in); d.getDocumentElement().normalize();
      Patient p = new Patient(text(d,"//Patient/Person/InternalID"), text(d,"//SSNPatient"), text(d,"//Patient/Person/LastName"), text(d,"//Patient/Person/FirstName"), text(d,"//Patient/Person/Birthdate"), text(d,"//Patient/Person/Sex"), text(d,"//Patient/Person/KorrID"));
      Physician ph = new Physician(text(d,"//CodePhysicianUCM"), text(d,"//CodePhysicianInternal"), text(d,"//Destination/Organisation/OrganisationName"), text(d,"//Destination/Organisation/OrganisationID"), text(d,"//Destination/Organisation/ContactPerson/MedTitle"), text(d,"//Destination/Organisation/ContactPerson/LastName"), text(d,"//Destination/Organisation/ContactPerson/FirstName"));
      List<LabResult> results = new ArrayList<>(); NodeList nodes = (NodeList) xp.evaluate("//Result", d, XPathConstants.NODESET);
      for (int i=0;i<nodes.getLength();i++){ Node n=nodes.item(i); results.add(new LabResult(t(n,"Title"),t(n,"SubTitle"),t(n,"CodeTestInternal"),t(n,"DescriptionInternal"),t(n,"TestClass"),t(n,"ResultValue"),t(n,"ResultStatus"),t(n,"Unit"),t(n,"UnitType"),t(n,"ReferenceValue"),t(n,"PrecedingValue"),t(n,"PrecedingDate"),t(n,"Alarm"),t(n,"Prozent"),t(n,"ResultComments"),t(n,"Validator"))); }
      Map<String,String> footer = new LinkedHashMap<>(); NodeList fn = (NodeList) xp.evaluate("//Footer//*[not(*)]", d, XPathConstants.NODESET); for(int i=0;i<fn.getLength();i++) footer.put(fn.item(i).getNodeName(), fn.item(i).getTextContent().trim());
      return new LabReport(text(d,"//ReferenceNumber"),text(d,"//ExternalNumber"),text(d,"//UrsprGlimsRefnr"),text(d,"//XML_TargetLang"),text(d,"//LaboFileFormatVersion"),text(d,"//Producer"),text(d,"//CodeLabUCM"),text(d,"//CodeLabInternal"),text(d,"//SSNPatient"),text(d,"//ACCREDITION"),text(d,"//CreationDate"),text(d,"//CompletionDate"),text(d,"//ValidationDate"),text(d,"//PrelevementDate"),text(d,"//SpecimenDate"),text(d,"//EditDate"),text(d,"//ReeditDate"),text(d,"//PrescriptionDate"),text(d,"//ExaminationStatus"),text(d,"//RecordComments"),p,ph,results,new ReportFooter(footer));
    } catch (Exception e) { throw new IllegalArgumentException("Invalid laboratory XML", e); }
  }
  private String text(Object item, String expr) throws XPathExpressionException { return ((String) xp.evaluate(expr, item, XPathConstants.STRING)).trim(); }
  private String t(Node n, String child) throws XPathExpressionException { return text(n, child); }
}
