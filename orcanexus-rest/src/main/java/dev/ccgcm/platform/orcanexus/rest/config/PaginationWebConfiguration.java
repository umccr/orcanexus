package dev.ccgcm.platform.orcanexus.rest.config;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link PaginationValidatingResolver} ahead of the framework's {@code Pageable}
 * resolver.
 *
 * <p>The resolver is deliberately not declared as a {@link PageableHandlerMethodArgumentResolver}
 * bean: doing so would switch off Spring Boot's Spring Data web auto-configuration, including the
 * application of {@code spring.data.web.*} settings.
 */
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaginationWebConfiguration implements WebMvcConfigurer {

  private final DataWebProperties properties;
  private final ObjectProvider<PageableHandlerMethodArgumentResolver> frameworkResolver;

  public PaginationWebConfiguration(
      DataWebProperties properties,
      ObjectProvider<PageableHandlerMethodArgumentResolver> frameworkResolver) {
    this.properties = properties;
    this.frameworkResolver = frameworkResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new PaginationValidatingResolver(properties, frameworkResolver::getObject));
  }
}
