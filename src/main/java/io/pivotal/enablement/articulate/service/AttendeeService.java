package io.pivotal.enablement.articulate.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.pivotal.enablement.articulate.cloud.service.WebServiceInfo;
import io.pivotal.enablement.articulate.model.Attendee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class AttendeeService {

  private static final Logger logger = LoggerFactory.getLogger(AttendeeService.class);
  private static final String DEFAULT_ATTENDEE_SERVICE_URI = "http://localhost:8181";

  @Value("${articulate.attendee-service.uri:" + DEFAULT_ATTENDEE_SERVICE_URI + "}")
  private String baseUri;
  private String endpoint;

  private final RestTemplate restTemplate;

  public AttendeeService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostConstruct
  public void init() {
    try {
      CloudFactory cloudFactory = new CloudFactory();
      Cloud cloud = cloudFactory.getCloud();
      List<ServiceInfo> serviceInfos = cloud.getServiceInfos();
      for (ServiceInfo serviceInfo : serviceInfos) {
        if (serviceInfo instanceof WebServiceInfo) {
          WebServiceInfo webServiceInfo = (WebServiceInfo) serviceInfo;
          this.baseUri = webServiceInfo.getUri();
          deriveEndpointFromBaseUri();
        }
      }
    } catch (CloudException e) {
      logger.debug("Failed to read cloud environment.  Ignore if running locally.");
    }
    logger.info("attendee-service uri is: {}", this.baseUri);
  }

  private void deriveEndpointFromBaseUri() {
    if (this.baseUri.endsWith("/attendees")) {
      this.endpoint = this.baseUri;
    } else if (this.baseUri.endsWith("/")) {
      this.endpoint = String.format("%sattendees", baseUri);
    } else {
      this.endpoint = String.format("%s/attendees", baseUri);
    }
  }


  @HystrixCommand
  public void add(Attendee attendee) {
    ResponseEntity<Attendee> responseEntity = restTemplate.postForEntity(endpoint, attendee, Attendee.class);
    logger.debug("ResponseEntity<Attendee>: {}", responseEntity);
  }

  @HystrixCommand(fallbackMethod = "defaultList")
  public List<Attendee> getAttendees() {
    try {
      ResponseEntity<PagedResources<Attendee>> responseEntity = restTemplate.exchange(
          endpoint, HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<PagedResources<Attendee>>() {
          }
      );

      PagedResources<Attendee> pagedResources = responseEntity.getBody();
      logger.debug("PagedResources<Attendee>: {}", pagedResources);

      List<Attendee> attendeeList = new ArrayList<>();
      for (Attendee attendee : pagedResources) {
        attendeeList.add(attendee);
      }
      return attendeeList;
    } catch (Exception e) {
      logger.error("Failed to retrieve attendees.  Returning empty list.", e);
      throw e;
    }

  }

  @SuppressWarnings("unused")
  private List<Attendee> defaultList() {
    return new ArrayList<>();
  }

  private HttpEntity<String> getHttpEntity() {
    HttpHeaders headers = new HttpHeaders();
    List<MediaType> accepts = new ArrayList<>();
    accepts.add(MediaType.APPLICATION_JSON);
    headers.setAccept(accepts);
    return new HttpEntity<>(headers);
  }

}
