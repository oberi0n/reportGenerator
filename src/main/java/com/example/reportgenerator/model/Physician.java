package com.example.reportgenerator.model;
public record Physician(String codeUcm, String codeInternal, String organisationName, String organisationId, String title, String lastName, String firstName) {
  public String fullName() { return String.join(" ", java.util.stream.Stream.of(title, firstName, lastName).filter(s -> s != null && !s.isBlank()).toList()); }
}
