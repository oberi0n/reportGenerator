package com.example.reportgenerator.model;
import java.util.*;
public record ReportMetadata(String referenceNumber,String externalNumber,String targetLanguage,String sourceFilename,String sourceChecksumSha256,String pdfChecksumSha256,String uploadTimestamp,String objectXmlPath,String objectPdfPath,Patient patient,Physician physician,Map<String,String> dates,String examinationStatus,String accreditation,int resultCount,int abnormalResultCount,List<String> sections,List<String> validators,ReportFooter footer,Map<String,String> processing) {}
