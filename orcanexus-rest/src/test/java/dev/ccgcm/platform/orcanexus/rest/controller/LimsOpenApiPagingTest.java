package dev.ccgcm.platform.orcanexus.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import dev.ccgcm.platform.orcanexus.rest.config.PageRequestMapper;
import dev.ccgcm.platform.orcanexus.rest.config.PaginationWebConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LimsController.class)
@ImportAutoConfiguration({
  org.springdoc.core.configuration.SpringDocConfiguration.class,
  org.springdoc.core.properties.SpringDocConfigProperties.class,
  org.springdoc.core.configuration.SpringDocPageableConfiguration.class,
  org.springdoc.core.configuration.SpringDocSortConfiguration.class,
  org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class
})
@Import({
  LimsOpenApiPagingTest.StubConfig.class,
  PageRequestMapper.class,
  PaginationWebConfiguration.class
})
class LimsOpenApiPagingTest {

  @SpringBootApplication
  static class TestApplication {}

  static class StubConfig {
    @Bean
    FindLimsRecordsUseCase findLimsRecordsUseCase() {
      return pageRequest -> {
        throw new UnsupportedOperationException();
      };
    }
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void documentsPageableParameters() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/lims'].get.parameters[?(@.name=='page')]").exists())
        .andExpect(jsonPath("$.paths['/api/v1/lims'].get.parameters[?(@.name=='size')]").exists())
        .andExpect(jsonPath("$.paths['/api/v1/lims'].get.parameters[?(@.name=='sort')]").exists());
  }
}
