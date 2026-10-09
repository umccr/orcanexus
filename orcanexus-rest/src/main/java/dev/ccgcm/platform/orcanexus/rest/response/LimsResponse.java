package dev.ccgcm.platform.orcanexus.rest.response;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LimsResponse(
    List<Map<String, Object>> content,
    int page,
    int size,
    long totalElements,
    long totalPages,
    boolean hasNext,
    boolean hasPrevious) {

  public LimsResponse {
    content = content.stream().map(map -> Collections.unmodifiableMap(new HashMap<>(map))).toList();
  }

  @Override
  public List<Map<String, Object>> content() {
    return new ArrayList<>(content);
  }

  public static LimsResponse from(PageResult<LimsRecord> result) {
    var content = result.content().stream().map(LimsRecord::columns).toList();

    return new LimsResponse(
        content,
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.hasNext(),
        result.hasPrevious());
  }
}
