package com.example.reportgenerator.parser;

import com.example.reportgenerator.model.LabReport;
import com.example.reportgenerator.model.LabResult;
import com.example.reportgenerator.model.Patient;
import com.example.reportgenerator.model.Physician;
import com.example.reportgenerator.model.ReportFooter;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LabReportXmlParser {
    private static final Pattern XML_DECLARATION = Pattern.compile("<\\?xml[^?]*\\?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_PROCESSING_INSTRUCTION = Pattern.compile("<\\?(?!xml\\b)[^?]*\\?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCTYPE = Pattern.compile("<!DOCTYPE[^>]*(?:\\[[\\s\\S]*?])?\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_ENCODING = Pattern.compile("encoding\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    private final XPath xp = XPathFactory.newInstance().newXPath();

    public LabReport parse(InputStream in) {
        try {
            Document document = parseLeniently(in);
            Patient patient = new Patient(
                    text(document, "//Patient/Person/InternalID"),
                    text(document, "//SSNPatient"),
                    text(document, "//Patient/Person/LastName"),
                    text(document, "//Patient/Person/FirstName"),
                    text(document, "//Patient/Person/Birthdate"),
                    text(document, "//Patient/Person/Sex"),
                    text(document, "//Patient/Person/KorrID")
            );
            Physician physician = new Physician(
                    text(document, "//CodePhysicianUCM"),
                    text(document, "//CodePhysicianInternal"),
                    text(document, "//Destination/Organisation/OrganisationName"),
                    text(document, "//Destination/Organisation/OrganisationID"),
                    text(document, "//Destination/Organisation/ContactPerson/MedTitle"),
                    text(document, "//Destination/Organisation/ContactPerson/LastName"),
                    text(document, "//Destination/Organisation/ContactPerson/FirstName")
            );

            List<LabResult> results = new ArrayList<>();
            NodeList nodes = (NodeList) xp.evaluate("//Result", document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                results.add(new LabResult(
                        t(node, "Title"), t(node, "SubTitle"), t(node, "CodeTestInternal"),
                        t(node, "DescriptionInternal"), t(node, "TestClass"), t(node, "ResultValue"),
                        t(node, "ResultStatus"), t(node, "Unit"), t(node, "UnitType"),
                        t(node, "ReferenceValue"), t(node, "PrecedingValue"), t(node, "PrecedingDate"),
                        t(node, "Alarm"), t(node, "Prozent"), t(node, "ResultComments"), t(node, "Validator")
                ));
            }

            Map<String, String> footer = new LinkedHashMap<>();
            NodeList footerNodes = (NodeList) xp.evaluate("//Footer//*[not(*)]", document, XPathConstants.NODESET);
            for (int i = 0; i < footerNodes.getLength(); i++) {
                footer.put(footerNodes.item(i).getNodeName(), footerNodes.item(i).getTextContent().trim());
            }

            return new LabReport(
                    text(document, "//ReferenceNumber"), text(document, "//ExternalNumber"),
                    text(document, "//UrsprGlimsRefnr"), text(document, "//XML_TargetLang"),
                    text(document, "//LaboFileFormatVersion"), text(document, "//Producer"),
                    text(document, "//CodeLabUCM"), text(document, "//CodeLabInternal"),
                    text(document, "//SSNPatient"), text(document, "//ACCREDITION"),
                    text(document, "//CreationDate"), text(document, "//CompletionDate"),
                    text(document, "//ValidationDate"), text(document, "//PrelevementDate"),
                    text(document, "//SpecimenDate"), text(document, "//EditDate"),
                    text(document, "//ReeditDate"), text(document, "//PrescriptionDate"),
                    text(document, "//ExaminationStatus"), text(document, "//RecordComments"),
                    patient, physician, results, new ReportFooter(footer)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid laboratory XML", e);
        }
    }

    private Document parseLeniently(InputStream in) throws Exception {
        byte[] bytes = in.readAllBytes();
        String xml = decode(bytes);
        String sanitized = DOCTYPE.matcher(xml).replaceAll("");
        sanitized = XML_DECLARATION.matcher(sanitized).replaceAll("");
        sanitized = XML_PROCESSING_INSTRUCTION.matcher(sanitized).replaceAll("");
        sanitized = "<LaboratoryEnvelope>" + sanitized + "</LaboratoryEnvelope>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(sanitized.getBytes(StandardCharsets.UTF_8)));
        document.getDocumentElement().normalize();
        return document;
    }

    private String decode(byte[] bytes) throws IOException {
        String prefix = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1);
        var matcher = XML_ENCODING.matcher(prefix);
        if (matcher.find()) {
            return new String(bytes, Charset.forName(matcher.group(1)));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String text(Object item, String expr) throws XPathExpressionException {
        return ((String) xp.evaluate(expr, item, XPathConstants.STRING)).trim();
    }

    private String t(Node node, String child) throws XPathExpressionException {
        return text(node, child);
    }
}
