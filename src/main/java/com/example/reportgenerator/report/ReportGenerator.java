package com.example.reportgenerator.report;

import com.example.reportgenerator.config.AppProperties;
import com.example.reportgenerator.model.LabReport;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import java.io.*;import java.nio.file.*;import java.util.*;

@Service
public class ReportGenerator {
  private final AppProperties props; public ReportGenerator(AppProperties props){this.props=props;}
  public byte[] generatePdf(LabReport report) {
    try (InputStream jrxml = open(props.templateDir(), props.templateMain())) {
      JasperReport compiled = JasperCompileManager.compileReport(jrxml);
      Map<String,Object> p = new HashMap<>();
      p.put("report", report); p.put("patient", report.patient()); p.put("physician", report.physician()); p.put("resultsDataSource", new JRBeanCollectionDataSource(report.results()));
      p.put("imageDir", props.imageDir()); p.put("fontDir", props.fontDir());
      JasperPrint print = JasperFillManager.fillReport(compiled, p, new JREmptyDataSource(1));
      return JasperExportManager.exportReportToPdf(print);
    } catch(Exception e){ throw new IllegalStateException("PDF generation failed", e); }
  }
  private InputStream open(String dir, String name) throws IOException { if(dir.startsWith("classpath:")) return new DefaultResourceLoader().getResource(dir + "/" + name).getInputStream(); return Files.newInputStream(Path.of(dir, name)); }
}
