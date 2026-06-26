package com.example.reportgenerator.search;

import com.example.reportgenerator.config.MinioProperties;
import com.example.reportgenerator.model.ReportMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class ReportSearchService {
    private static final int MAX_RESULTS = 100;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ObjectMapper objectMapper;
    private final MinioClient presignedUrlClient;

    public ReportSearchService(MinioClient minioClient, MinioProperties minioProperties, ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.objectMapper = objectMapper;
        this.presignedUrlClient = MinioClient.builder()
                .endpoint(minioProperties.presignedEndpoint())
                .region(minioProperties.effectiveRegion())
                .credentials(minioProperties.accessKey(), minioProperties.secretKey())
                .build();
    }

    public List<ReportSearchResult> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.bucket()).build())) {
                return List.of();
            }

            List<ReportSearchResult> matches = new ArrayList<>();
            Iterable<Result<Item>> objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioProperties.bucket())
                    .prefix("patients/")
                    .recursive(true)
                    .build());

            for (Result<Item> object : objects) {
                Item item = object.get();
                if (!item.objectName().endsWith("/metadata.json")) {
                    continue;
                }
                ReportMetadata metadata = readMetadata(item.objectName());
                if (matches(metadata, normalizedQuery)) {
                    matches.add(toResult(metadata, item.objectName()));
                    if (matches.size() >= MAX_RESULTS) {
                        break;
                    }
                }
            }

            matches.sort(Comparator
                    .comparing(ReportSearchResult::patientLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(ReportSearchResult::patientFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(ReportSearchResult::validationDate, Comparator.nullsLast(String::compareTo)).reversed());
            return matches;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to search reports in MinIO", e);
        }
    }

    private ReportMetadata readMetadata(String objectName) throws Exception {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.bucket())
                .object(objectName)
                .build())) {
            return objectMapper.readValue(stream, ReportMetadata.class);
        }
    }

    private boolean matches(ReportMetadata metadata, String query) {
        if (metadata == null || metadata.patient() == null) {
            return false;
        }
        String searchable = String.join(" ",
                value(metadata.patient().lastName()),
                value(metadata.patient().firstName()),
                value(metadata.patient().internalId()),
                value(metadata.patient().ssn()),
                value(metadata.referenceNumber())
        );
        return normalize(searchable).contains(query);
    }

    private ReportSearchResult toResult(ReportMetadata metadata, String metadataPath) throws Exception {
        String pdfUrl = metadata.objectPdfPath() == null || metadata.objectPdfPath().isBlank()
                ? null
                : presignedUrlClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioProperties.bucket())
                .object(metadata.objectPdfPath())
                .expiry(15, TimeUnit.MINUTES)
                .build());
        return new ReportSearchResult(
                metadata.referenceNumber(),
                metadata.patient().internalId(),
                metadata.patient().ssn(),
                metadata.patient().lastName(),
                metadata.patient().firstName(),
                metadata.patient().birthdate(),
                metadata.dates() == null ? null : metadata.dates().get("validationDate"),
                metadata.examinationStatus(),
                metadata.objectXmlPath(),
                metadata.objectPdfPath(),
                metadataPath,
                pdfUrl
        );
    }

    private String normalize(String value) {
        String lowerCase = value(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lowerCase, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
