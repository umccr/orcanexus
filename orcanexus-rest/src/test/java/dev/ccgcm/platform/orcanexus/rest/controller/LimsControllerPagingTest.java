package dev.ccgcm.platform.orcanexus.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import dev.ccgcm.platform.orcanexus.rest.config.PageRequestMapper;
import dev.ccgcm.platform.orcanexus.rest.config.PaginationWebConfiguration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LimsController.class)
@Import({
  LimsControllerPagingTest.StubConfig.class,
  PageRequestMapper.class,
  PaginationWebConfiguration.class
})
@TestPropertySource(
    properties = {
      "spring.data.web.pageable.default-page-size=7",
      "spring.data.web.pageable.max-page-size=50",
      "spring.mvc.problemdetails.enabled=true"
    })
class LimsControllerPagingTest {

  private static final AtomicReference<PageRequest> LAST_REQUEST = new AtomicReference<>();

  @SpringBootApplication
  static class TestApplication {}

  static class StubConfig {
    @Bean
    FindLimsRecordsUseCase findLimsRecordsUseCase() {
      return pageRequest -> {
        LAST_REQUEST.set(pageRequest);
        var content =
            List.of(
                new LimsRecord(Map.of("library_id", "L001")),
                new LimsRecord(Map.of("library_id", "L002")));
        return new PageResult<>(content, pageRequest.page(), pageRequest.size(), 23L);
      };
    }
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsPagedModelEnvelope() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("page", "1").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].library_id").value("L001"))
        .andExpect(jsonPath("$.page.size").value(5))
        .andExpect(jsonPath("$.page.number").value(1))
        .andExpect(jsonPath("$.page.totalElements").value(23))
        .andExpect(jsonPath("$.page.totalPages").value(5));

    assertThat(LAST_REQUEST.get()).isEqualTo(new PageRequest(1, 5));
  }

  @ParameterizedTest
  @CsvSource({
    "page,-1,page must not be negative",
    "page,abc,page must be an integer",
    "size,0,size must be at least 1",
    "size,-5,size must be at least 1",
    "size,xyz,size must be an integer",
    "size,51,size must not exceed 50"
  })
  void rejectsInvalidPagingParameters(String name, String value, String message) throws Exception {
    LAST_REQUEST.set(null);

    mockMvc
        .perform(get("/api/v1/lims").param(name, value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(message));

    assertThat(LAST_REQUEST.get()).isNull();
  }

  @Test
  void acceptsSizeEqualToConfiguredMaximum() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("size", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.size").value(50));
  }

  @Test
  void appliesFrameworkDefaultPageSize() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.size").value(7))
        .andExpect(jsonPath("$.page.number").value(0));

    assertThat(LAST_REQUEST.get()).isEqualTo(new PageRequest(0, 7));
  }
}
