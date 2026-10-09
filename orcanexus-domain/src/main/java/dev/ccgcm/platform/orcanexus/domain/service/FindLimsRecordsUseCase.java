package dev.ccgcm.platform.orcanexus.domain.service;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;

public interface FindLimsRecordsUseCase {

  PageResult<LimsRecord> getLims(PageRequest pageRequest);
}
