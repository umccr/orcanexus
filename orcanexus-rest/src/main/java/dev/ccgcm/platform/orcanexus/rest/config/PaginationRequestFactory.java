package dev.ccgcm.platform.orcanexus.rest.config;

import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaginationRequestFactory {

  private final PaginationProperties properties;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring-managed immutable configuration object is retained by the controller")
  public PaginationRequestFactory(PaginationProperties properties) {
    this.properties = properties;
  }

  public PageRequest create(Integer page, Integer size) {
    int requestedPage = page == null ? properties.getDefaultPage() : page;

    int requestedSize = size == null ? properties.getDefaultSize() : size;

    if (requestedPage < 0) {
      throw badRequest("page must not be negative");
    }

    if (requestedSize < 1) {
      throw badRequest("size must be at least 1");
    }

    if (requestedSize > properties.getMaxSize()) {
      throw badRequest("size must not exceed " + properties.getMaxSize());
    }

    return new PageRequest(requestedPage, requestedSize);
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
