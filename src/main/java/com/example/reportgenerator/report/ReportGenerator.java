package com.example.reportgenerator.report;

import com.example.reportgenerator.config.AppProperties;
import com.example.reportgenerator.model.LabReport;
import com.example.reportgenerator.model.LabResult;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportGenerator {
    private final AppProperties props;

    public ReportGenerator(AppProperties props) {
        this.props = props;
    }

    public byte[] generatePdf(LabReport report) {
        try (InputStream jrxml = open(props.templateDir(), props.templateMain())) {
            JasperReport compiled = JasperCompileManager.compileReport(jrxml);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("report", report);
            parameters.put("patient", report.patient());
            parameters.put("physician", report.physician());
            parameters.put("resultsDataSource", new JRBeanCollectionDataSource(report.results()));
            parameters.put("resultsTable", buildResultsTable(report.results()));
            parameters.put("footerText", buildFooterText(report));
            parameters.put("imageDir", props.imageDir());
            parameters.put("logoImage", resolveImage("laboreunis-logo.png"));
            parameters.put("fontDir", props.fontDir());
            JasperPrint print = JasperFillManager.fillReport(compiled, parameters, new JRBeanCollectionDataSource(report.results()));
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            throw new IllegalStateException("PDF generation failed", e);
        }
    }

    private InputStream open(String dir, String name) throws IOException {
        if (dir.startsWith("classpath:")) {
            return new DefaultResourceLoader().getResource(dir + "/" + name).getInputStream();
        }
        return Files.newInputStream(Path.of(dir, name));
    }

    private Object resolveImage(String imageName) throws IOException {
        if (props.imageDir().startsWith("classpath:")) {
            var resource = new DefaultResourceLoader().getResource(props.imageDir() + "/" + imageName);
            return resource.exists() ? resource.getInputStream() : null;
        }
        Path imagePath = Path.of(props.imageDir(), imageName);
        return Files.exists(imagePath) ? imagePath.toString() : null;
    }

    private String buildResultsTable(List<LabResult> results) {
        if (results == null || results.isEmpty()) {
            return "Aucun résultat disponible.";
        }
        StringBuilder table = new StringBuilder();
        String currentSection = "";
        for (LabResult result : results) {
            if (result.sectionHeader()) {
                currentSection = value(result.title());
                table.append("\n").append(currentSection).append("\n");
                table.append("-".repeat(Math.min(96, Math.max(20, currentSection.length())))).append("\n");
                continue;
            }
            String label = firstNonBlank(result.descriptionInternal(), result.codeTestInternal(), result.subTitle(), "Analyse");
            String alarm = result.high() ? "↑" : result.low() ? "↓" : " ";
            table.append(String.format(
                    "%-34s %1s %-14s %-10s réf. %-18s ant. %-12s %s%n",
                    truncate(label, 34),
                    alarm,
                    truncate(value(result.resultValue()), 14),
                    truncate(value(result.unit()), 10),
                    truncate(value(result.referenceValue()), 18),
                    truncate(value(result.precedingValue()), 12),
                    value(result.resultComments())
            ));
        }
        return table.toString().stripLeading();
    }

    private String buildFooterText(LabReport report) {
        String validators = report.validators().isEmpty() ? "" : "Validateurs: " + String.join(", ", report.validators());
        String footer = report.footer() == null || report.footer().fields() == null
                ? ""
                : report.footer().fields().entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        return String.join("\n", validators, footer).strip();
    }

    private String firstNonBlank(String... values) {
        for (String candidate : values) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    private String value(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private String truncate(String text, int maxLength) {
        String value = value(text);
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
