package com.tutortimetracker.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures CORS for API endpoints.
 *
 * <p>Uses origin patterns so LAN-hosted frontends (for example on Raspberry Pi) can call the
 * backend through dockerized reverse proxies.
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

  private final String[] allowedOriginPatterns;

  public CorsConfiguration(
      @Value(
              "${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*,http://172.16.*:*,http://172.17.*:*,http://172.18.*:*,http://172.19.*:*}")
          String[] allowedOriginPatterns) {
    this.allowedOriginPatterns = allowedOriginPatterns;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOriginPatterns(allowedOriginPatterns)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
