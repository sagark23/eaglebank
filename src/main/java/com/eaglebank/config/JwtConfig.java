package com.eaglebank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "jwt")
@Configuration
public class JwtConfig {
  private Long expiration;
  private String secret;
}




