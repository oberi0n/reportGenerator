package com.example.reportgenerator.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
  @Bean MinioClient minioClient(MinioProperties p) {
    return MinioClient.builder().endpoint(p.endpoint()).credentials(p.accessKey(), p.secretKey()).build();
  }
}
