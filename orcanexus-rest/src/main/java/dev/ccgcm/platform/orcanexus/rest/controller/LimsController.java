package dev.ccgcm.platform.orcanexus.rest.controller;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import dev.ccgcm.platform.orcanexus.rest.config.PageRequestMapper;
import dev.ccgcm.platform.orcanexus.rest.response.PagedModels;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "LIMS",
    description = "Listing of the Centre genomic sequencing LIMS metadata in flat table model")
@RestController
@RequestMapping("/api/v1/lims")
public class LimsController {

  private final FindLimsRecordsUseCase findLimsRecordsUseCase;
  private final PageRequestMapper pageRequestMapper;

  public LimsController(
      FindLimsRecordsUseCase findLimsRecordsUseCase, PageRequestMapper pageRequestMapper) {
    this.findLimsRecordsUseCase = findLimsRecordsUseCase;
    this.pageRequestMapper = pageRequestMapper;
  }

  @GetMapping
  public PagedModel<Map<String, Object>> getLims(@ParameterObject Pageable pageable) {
    var result = findLimsRecordsUseCase.getLims(pageRequestMapper.toDomain(pageable));

    return PagedModels.from(result, LimsRecord::columns);
  }
}
