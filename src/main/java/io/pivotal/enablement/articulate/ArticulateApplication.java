package io.pivotal.enablement.articulate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author mborges
 */
@SpringBootApplication
@EnableCircuitBreaker
public class ArticulateApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArticulateApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new Jackson2HalModule());

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/json"));
    converter.setObjectMapper(mapper);
    return new RestTemplate(Collections.singletonList(converter));
  }

}
