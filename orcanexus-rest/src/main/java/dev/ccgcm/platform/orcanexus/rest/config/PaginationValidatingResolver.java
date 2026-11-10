package dev.ccgcm.platform.orcanexus.rest.config;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rejects invalid raw {@code page} and {@code size} request parameters with HTTP 400 and then
 * delegates to the framework's {@link Pageable} resolver.
 *
 * <p>The framework resolver is lenient: it silently replaces invalid values with defaults and
 * clamps an oversized {@code size}. Validation therefore has to happen on the raw values before
 * delegation. Parameter names, indexing and the maximum size are read from {@link
 * DataWebProperties}, so everything stays driven by the {@code spring.data.web.*} configuration.
 */
final class PaginationValidatingResolver implements HandlerMethodArgumentResolver {

  private final DataWebProperties properties;
  private final Supplier<HandlerMethodArgumentResolver> delegate;

  PaginationValidatingResolver(
      DataWebProperties properties, Supplier<HandlerMethodArgumentResolver> delegate) {
    this.properties = properties;
    this.delegate = delegate;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.equals(parameter.getParameterType());
  }

  @Override
  public @Nullable Object resolveArgument(
      MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory)
      throws Exception {
    validate(webRequest);

    return delegate.get().resolveArgument(parameter, mavContainer, webRequest, binderFactory);
  }

  private void validate(NativeWebRequest request) {
    var pageable = properties.getPageable();

    var page = parse(request, pageable.getPrefix() + pageable.getPageParameter(), "page");
    if (page != null) {
      int firstPage = pageable.isOneIndexedParameters() ? 1 : 0;
      if (page < firstPage) {
        throw badRequest(
            firstPage == 0 ? "page must not be negative" : "page must be at least " + firstPage);
      }
    }

    var size = parse(request, pageable.getPrefix() + pageable.getSizeParameter(), "size");
    if (size != null) {
      if (size < 1) {
        throw badRequest("size must be at least 1");
      }
      if (size > pageable.getMaxPageSize()) {
        throw badRequest("size must not exceed " + pageable.getMaxPageSize());
      }
    }
  }

  private @Nullable Integer parse(NativeWebRequest request, String parameterName, String label) {
    var value = request.getParameter(parameterName);
    if (!StringUtils.hasText(value)) {
      return null;
    }

    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      throw badRequest(label + " must be an integer");
    }
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
