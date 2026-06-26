package com.example.reportgenerator.model;
import java.util.*;
public record LabReport(String referenceNumber, String externalNumber, String ursprGlimsRefnr, String targetLanguage,
 String laboFileFormatVersion, String producer, String codeLabUcm, String codeLabInternal, String ssnPatient,
 String accreditation, String creationDate, String completionDate, String validationDate, String prelevementDate,
 String specimenDate, String editDate, String reeditDate, String prescriptionDate, String examinationStatus,
 String recordComments, Patient patient, Physician physician, List<LabResult> results, ReportFooter footer) {
 public int resultCount() { return results == null ? 0 : (int) results.stream().filter(r -> !r.sectionHeader()).count(); }
 public int abnormalResultCount() { return results == null ? 0 : (int) results.stream().filter(LabResult::abnormal).count(); }
 public List<String> sections() { return distinct(results.stream().map(LabResult::title)); }
 public List<String> validators() { return distinct(results.stream().map(LabResult::validator)); }
 private static List<String> distinct(java.util.stream.Stream<String> s){ return s.filter(v -> v != null && !v.isBlank()).distinct().toList(); }
 public String patientStorageId(){ if(patient!=null && patient.internalId()!=null && !patient.internalId().isBlank()) return patient.internalId(); if(ssnPatient!=null && !ssnPatient.isBlank()) return ssnPatient; return "UNKNOWN_PATIENT"; }
}
