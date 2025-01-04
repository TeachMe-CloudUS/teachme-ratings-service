package com.mongodb.starter.configuration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfiguration {

  @Value("${base-url}")
  private String BASE_URL;

  @Bean
  OpenAPI customOpenAPI() {
    return new OpenAPI()
        .servers(List.of(new Server().url(BASE_URL)))
        .info(new Info().title("Teachme-rating-service APIs").version("1.0"));
  }

}
