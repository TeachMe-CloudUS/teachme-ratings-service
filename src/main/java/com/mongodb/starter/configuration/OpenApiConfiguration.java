package com.mongodb.starter.configuration;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@Configuration
@OpenAPIDefinition(
  info =@Info(
    title = "teachme-rating-service APIs",
    version = "v1.0",
    license = @License(
      name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0"
    )
  )
)

public class OpenApiConfiguration {
    
}
