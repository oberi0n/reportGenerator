package com.example.reportgenerator.model;
public record LabResult(String title, String subTitle, String codeTestInternal, String descriptionInternal, String testClass,
                        String resultValue, String resultStatus, String unit, String unitType, String referenceValue,
                        String precedingValue, String precedingDate, String alarm, String prozent, String resultComments, String validator) {
  public boolean sectionHeader() { return notBlank(title) && !notBlank(codeTestInternal); }
  public boolean high() { return "+".equals(alarm); }
  public boolean low() { return "-".equals(alarm); }
  public boolean abnormal() { return high() || low(); }
  private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
