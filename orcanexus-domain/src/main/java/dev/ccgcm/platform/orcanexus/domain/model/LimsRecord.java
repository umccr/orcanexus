package dev.ccgcm.platform.orcanexus.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record LimsRecord(Map<String, Object> columns) {

  public LimsRecord {
    Objects.requireNonNull(columns, "columns must not be null");
    columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
  }
}
