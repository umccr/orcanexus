package dev.ccgcm.platform.orcanexus.domain.service;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import dev.ccgcm.platform.orcanexus.domain.port.LimsQueryPort;
import java.util.Objects;

public final class FindLimsRecordsService implements FindLimsRecordsUseCase {

  private final LimsQueryPort limsQueryPort;

  public FindLimsRecordsService(LimsQueryPort limsQueryPort) {
    this.limsQueryPort = Objects.requireNonNull(limsQueryPort);
  }

  @Override
  public PageResult<LimsRecord> getLims(PageRequest pageRequest) {
    return limsQueryPort.getLims(pageRequest);
  }
}
