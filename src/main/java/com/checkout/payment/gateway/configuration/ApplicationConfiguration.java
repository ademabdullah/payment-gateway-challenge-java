package com.checkout.payment.gateway.configuration;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public RestTemplate restTemplate(
      RestTemplateBuilder builder,
      @Value("${bank.connect-timeout:2s}") Duration connectTimeout,
      @Value("${bank.read-timeout:5s}") Duration readTimeout) {
    return builder.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
