package com.example.reportgenerator.model;

public record LabResult(String title, String subTitle, String codeTestInternal, String descriptionInternal, String testClass,
                        String resultValue, String resultStatus, String unit, String unitType, String referenceValue,
                        String precedingValue, String precedingDate, String alarm, String prozent, String resultComments, String validator) {
  public boolean sectionHeader() { return notBlank(title) && !notBlank(codeTestInternal); }
  public boolean high() { return "+".equals(alarm); }
  public boolean low() { return "-".equals(alarm); }
  public boolean abnormal() { return high() || low(); }
  public String displayLabel() { return firstNonBlank(descriptionInternal, codeTestInternal, subTitle, title); }
  public String alarmSymbol() { return high() ? "↑" : low() ? "↓" : ""; }
  public String sectionTitle() { return value(title); }

  public boolean isSectionHeader() { return sectionHeader(); }
  public String getDisplayLabel() { return displayLabel(); }
  public String getResultValue() { return value(resultValue); }
  public String getUnit() { return value(unit); }
  public String getReferenceValue() { return value(referenceValue); }
  public String getPrecedingValue() { return value(precedingValue); }
  public String getResultComments() { return value(resultComments); }
  public String getAlarmSymbol() { return alarmSymbol(); }
  public String getSectionTitle() { return sectionTitle(); }

  private static String firstNonBlank(String... values) { for (String v : values) if (notBlank(v)) return value(v); return ""; }
  private static String value(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }
  private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
