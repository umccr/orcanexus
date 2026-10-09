package dev.ccgcm.platform.orcanexus.rest.controller;

import dev.ccgcm.platform.orcanexus.rest.config.SystemProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "System information")
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

  private final SystemProperties systemProperties;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring-managed immutable configuration object is retained by the controller")
  public SystemController(SystemProperties systemProperties) {
    this.systemProperties = systemProperties;
  }

  public record InfoResponse(String name, String description, String environment) {}

  @GetMapping("/info")
  public InfoResponse info() {
    return new InfoResponse(
        systemProperties.getName(),
        systemProperties.getDescription(),
        systemProperties.getEnvironment());
  }
}
