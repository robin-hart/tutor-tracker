package com.tutortimetracker.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata for Scalar and other API documentation consumers. */
@Configuration
public class OpenApiConfig {

  /**
   * Creates OpenAPI metadata used by Scalar UI.
   *
   * @return configured OpenAPI object
   */
  @Bean
  public OpenAPI tutorTimeTrackerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TutorTimeTracker API")
                .version("v1")
                .description(
                    "REST API for managing tutoring projects, students, timeslots, and "
                        + "monthly reports.")
                .contact(new Contact().name("TutorTimeTracker Team"))
                .license(new License().name("Internal Use")))
        .servers(List.of(new Server().url("/").description("Current environment")))
        .components(new Components());
  }
}
