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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ReportSearchService {
    private static final Logger log = LoggerFactory.getLogger(ReportSearchService.class);
    private static final int MAX_RESULTS = 100;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ObjectMapper objectMapper;
    private final MinioClient presignedUrlClient;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private volatile List<IndexedReport> index = List.of();
    private volatile Instant lastRefresh = Instant.EPOCH;

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

        ensureIndexLoaded();
        return index.stream()
                .filter(report -> report.searchableText().contains(normalizedQuery))
                .map(IndexedReport::result)
                .sorted(Comparator
                        .comparing(ReportSearchResult::validationDate, Comparator.nullsLast(String::compareTo)).reversed()
                        .thenComparing(ReportSearchResult::patientLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ReportSearchResult::patientFirstName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(MAX_RESULTS)
                .toList();
    }

    @Scheduled(fixedDelayString = "#{${app.search-refresh-seconds:60} * 1000}")
    public void refreshIndex() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            List<IndexedReport> refreshed = loadIndexFromMinio();
            index = List.copyOf(refreshed);
            lastRefresh = Instant.now();
            log.info("Report search index refreshed: {} report(s)", index.size());
        } catch (Exception e) {
            log.warn("Unable to refresh report search index; keeping {} cached report(s)", index.size(), e);
        } finally {
            refreshInProgress.set(false);
        }
    }

    public Instant lastRefresh() {
        return lastRefresh;
    }

    private void ensureIndexLoaded() {
        if (index.isEmpty() && refreshInProgress.compareAndSet(false, true)) {
            try {
                index = List.copyOf(loadIndexFromMinio());
                lastRefresh = Instant.now();
            } catch (Exception e) {
                log.warn("Unable to initialize report search index", e);
            } finally {
                refreshInProgress.set(false);
            }
        }
    }

    private List<IndexedReport> loadIndexFromMinio() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.bucket()).build())) {
            return List.of();
        }

        List<IndexedReport> reports = new ArrayList<>();
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
            reports.add(index(metadata, item.objectName()));
        }
        return reports;
    }

    private ReportMetadata readMetadata(String objectName) throws Exception {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.bucket())
                .object(objectName)
                .build())) {
            return objectMapper.readValue(stream, ReportMetadata.class);
        }
    }

    private IndexedReport index(ReportMetadata metadata, String metadataPath) throws Exception {
        String searchableText = normalize(objectMapper.writeValueAsString(metadata));
        return new IndexedReport(toResult(metadata, metadataPath), searchableText);
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
                metadata.patient() == null ? null : metadata.patient().internalId(),
                metadata.patient() == null ? null : metadata.patient().ssn(),
                metadata.patient() == null ? null : metadata.patient().lastName(),
                metadata.patient() == null ? null : metadata.patient().firstName(),
                metadata.patient() == null ? null : metadata.patient().birthdate(),
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

    private record IndexedReport(ReportSearchResult result, String searchableText) {}
}
