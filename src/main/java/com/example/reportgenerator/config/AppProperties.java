package com.example.reportgenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String incomingDir, String processedDir, String errorDir, long pollIntervalSeconds,
                            String templateDir, String templateMain, String imageDir, String fontDir) {}
