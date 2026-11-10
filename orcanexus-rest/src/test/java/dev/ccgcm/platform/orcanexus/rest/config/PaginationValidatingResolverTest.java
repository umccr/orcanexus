package dev.ccgcm.platform.orcanexus.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class PaginationValidatingResolverTest {

  private DataWebProperties properties;
  private AtomicInteger delegateCalls;
  private PaginationValidatingResolver resolver;
  private MethodParameter parameter;

  @SuppressWarnings("unused")
  private static void handler(Pageable pageable, String other) {}

  @BeforeEach
  void setUp() throws Exception {
    properties = new DataWebProperties();
    properties.getPageable().setMaxPageSize(100);

    delegateCalls = new AtomicInteger();
    resolver =
        new PaginationValidatingResolver(
            properties,
            () ->
                new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                  @Override
                  public boolean supportsParameter(MethodParameter parameter) {
                    return true;
                  }

                  @Override
                  public Object resolveArgument(
                      MethodParameter parameter,
                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                      org.springframework.web.context.request.NativeWebRequest webRequest,
                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                    delegateCalls.incrementAndGet();
                    return Pageable.unpaged();
                  }
                });

    Method method = getClass().getDeclaredMethod("handler", Pageable.class, String.class);
    parameter = new MethodParameter(method, 0);
  }

  private void resolve(String... namesAndValues) throws Exception {
    var request = new MockHttpServletRequest();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      request.addParameter(namesAndValues[i], namesAndValues[i + 1]);
    }
    resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
  }

  private void assertBadRequest(String message, String... namesAndValues) {
    assertThatThrownBy(() -> resolve(namesAndValues))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> {
              assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(e.getReason()).isEqualTo(message);
            });
    assertThat(delegateCalls).hasValue(0);
  }

  @Test
  void supportsOnlyPageableParameters() throws Exception {
    Method method = getClass().getDeclaredMethod("handler", Pageable.class, String.class);

    assertThat(resolver.supportsParameter(new MethodParameter(method, 0))).isTrue();
    assertThat(resolver.supportsParameter(new MethodParameter(method, 1))).isFalse();
  }

  @Test
  void delegatesWhenNoParametersAreGiven() {
    assertThatCode(this::resolve).doesNotThrowAnyException();
    assertThat(delegateCalls).hasValue(1);
  }

  @Test
  void delegatesWhenValuesAreValid() {
    assertThatCode(() -> resolve("page", "0", "size", "100")).doesNotThrowAnyException();
    assertThat(delegateCalls).hasValue(1);
  }

  @Test
  void rejectsNegativePage() {
    assertBadRequest("page must not be negative", "page", "-1");
  }

  @Test
  void rejectsNonIntegerPage() {
    assertBadRequest("page must be an integer", "page", "one");
  }

  @Test
  void rejectsSizeBelowOne() {
    assertBadRequest("size must be at least 1", "size", "0");
  }

  @Test
  void rejectsNonIntegerSize() {
    assertBadRequest("size must be an integer", "size", "many");
  }

  @Test
  void rejectsSizeAboveConfiguredMaximum() {
    assertBadRequest("size must not exceed 100", "size", "101");
  }

  @Test
  void ignoresBlankValues() {
    assertThatCode(() -> resolve("page", " ", "size", "")).doesNotThrowAnyException();
    assertThat(delegateCalls).hasValue(1);
  }

  @Test
  void honoursConfiguredParameterNamesAndOneIndexing() {
    properties.getPageable().setPageParameter("p");
    properties.getPageable().setSizeParameter("n");
    properties.getPageable().setOneIndexedParameters(true);

    assertBadRequest("page must be at least 1", "p", "0");
    assertThatCode(() -> resolve("page", "-9", "size", "-9")).doesNotThrowAnyException();
  }
}
