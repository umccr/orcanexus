package dev.ccgcm.platform.orcanexus.rest.controller;

import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import dev.ccgcm.platform.orcanexus.rest.config.PaginationRequestFactory;
import dev.ccgcm.platform.orcanexus.rest.response.LimsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "LIMS",
    description = "Listing of the Centre genomic sequencing LIMS metadata in flat table model")
@RestController
@RequestMapping("/api/v1/lims")
public class LimsController {

  private final FindLimsRecordsUseCase findLimsRecordsUseCase;
  private final PaginationRequestFactory paginationRequestFactory;

  public LimsController(
      FindLimsRecordsUseCase findLimsRecordsUseCase,
      PaginationRequestFactory paginationRequestFactory) {
    this.findLimsRecordsUseCase = findLimsRecordsUseCase;
    this.paginationRequestFactory = paginationRequestFactory;
  }

  @GetMapping
  public LimsResponse getLims(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    var pageRequest = paginationRequestFactory.create(page, size);

    return LimsResponse.from(findLimsRecordsUseCase.getLims(pageRequest));
  }
}
