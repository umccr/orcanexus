package dev.ccgcm.platform.orcanexus.domain.port;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;

public interface LimsQueryPort {

  PageResult<LimsRecord> getLims(PageRequest pageRequest);
}
