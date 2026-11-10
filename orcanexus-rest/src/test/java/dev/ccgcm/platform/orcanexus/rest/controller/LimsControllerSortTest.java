package dev.ccgcm.platform.orcanexus.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ccgcm.platform.orcanexus.domain.model.InvalidSortFieldException;
import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import dev.ccgcm.platform.orcanexus.domain.model.SortDirection;
import dev.ccgcm.platform.orcanexus.domain.model.SortOrder;
import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import dev.ccgcm.platform.orcanexus.rest.config.PageRequestMapper;
import dev.ccgcm.platform.orcanexus.rest.config.PaginationWebConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LimsController.class)
@Import({
  LimsControllerSortTest.StubConfig.class,
  PageRequestMapper.class,
  PaginationWebConfiguration.class
})
class LimsControllerSortTest {

  private static final Set<String> SORTABLE = Set.of("library_id", "sequencing_run_date");
  private static final AtomicReference<PageRequest> LAST_REQUEST = new AtomicReference<>();

  @SpringBootApplication
  static class TestApplication {}

  static class StubConfig {
    @Bean
    FindLimsRecordsUseCase findLimsRecordsUseCase() {
      return pageRequest -> {
        for (var order : pageRequest.sort()) {
          if (!SORTABLE.contains(order.field())) {
            throw new InvalidSortFieldException(order.field());
          }
        }
        LAST_REQUEST.set(pageRequest);
        return new PageResult<>(
            List.of(new LimsRecord(Map.of("library_id", "L001"))),
            pageRequest.page(),
            pageRequest.size(),
            1L);
      };
    }
  }

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void reset() {
    LAST_REQUEST.set(null);
  }

  @Test
  void noSortParameterGivesEmptySort() throws Exception {
    mockMvc.perform(get("/api/v1/lims")).andExpect(status().isOk());

    assertThat(LAST_REQUEST.get().sort()).isEmpty();
  }

  @Test
  void sortsDescending() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("sort", "library_id,desc"))
        .andExpect(status().isOk());

    assertThat(LAST_REQUEST.get().sort())
        .containsExactly(new SortOrder("library_id", SortDirection.DESC));
  }

  @Test
  void defaultsToAscendingAndIgnoresDirectionCase() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/lims")
                .param("sort", "library_id")
                .param("sort", "sequencing_run_date,DESC"))
        .andExpect(status().isOk());

    assertThat(LAST_REQUEST.get().sort())
        .containsExactly(
            new SortOrder("library_id", SortDirection.ASC),
            new SortOrder("sequencing_run_date", SortDirection.DESC));
  }

  @Test
  void keepsSortOrderTogetherWithPaging() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/lims")
                .param("page", "2")
                .param("size", "5")
                .param("sort", "library_id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.number").value(2));

    assertThat(LAST_REQUEST.get())
        .isEqualTo(new PageRequest(2, 5, List.of(new SortOrder("library_id", SortDirection.ASC))));
  }

  @Test
  void unknownSortFieldIsBadRequestNamingTheField() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("sort", "not_a_column,desc"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Unknown sort field: not_a_column"));

    assertThat(LAST_REQUEST.get()).isNull();
  }

  @Test
  void malformedDirectionIsTreatedAsUnknownFieldAndRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("sort", "library_id,sideways"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unknown sort field: sideways"));

    assertThat(LAST_REQUEST.get()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"LIBRARY_ID", "library_id; drop table lims", "library id", "1"})
  void nearMissAndHostileFieldNamesAreRejected(String field) throws Exception {
    mockMvc
        .perform(get("/api/v1/lims").param("sort", field))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unknown sort field: " + field));
  }
}
