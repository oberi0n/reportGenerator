package com.example.reportgenerator.search;

public record ReportSearchResult(
        String referenceNumber,
        String patientInternalId,
        String ssn,
        String patientLastName,
        String patientFirstName,
        String birthdate,
        String validationDate,
        String examinationStatus,
        String objectXmlPath,
        String objectPdfPath,
        String metadataPath,
        String pdfUrl
) {}
