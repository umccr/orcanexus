package dev.ccgcm.platform.orcanexus.rest.config;

import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.SortDirection;
import dev.ccgcm.platform.orcanexus.domain.model.SortOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Maps the framework's {@link Pageable} to the framework-free domain {@link PageRequest}.
 *
 * <p>Sort properties are passed through unchanged; deciding which fields may be sorted on is the
 * responsibility of the adapter that knows the underlying resource.
 */
@Component
public class PageRequestMapper {

  public PageRequest toDomain(Pageable pageable) {
    var sort =
        pageable.getSort().stream()
            .map(
                order ->
                    new SortOrder(
                        order.getProperty(),
                        order.isAscending() ? SortDirection.ASC : SortDirection.DESC))
            .toList();

    return new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }
}
