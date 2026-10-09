package dev.ccgcm.platform.orcanexus.aurora.repository;

import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import dev.ccgcm.platform.orcanexus.domain.port.LimsQueryPort;
import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class LimsRepository implements LimsQueryPort {

  private final DSLContext dsl;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "DSLContext is a framework-managed jOOQ dependency intentionally shared by the repository")
  public LimsRepository(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public PageResult<LimsRecord> getLims(PageRequest pageRequest) {
    var table = Lims.LIMS;

    var content =
        dsl.selectFrom(table)
            .orderBy(table.fields()[0].asc())
            .limit(pageRequest.size())
            .offset(pageRequest.offset())
            .fetch(record -> new LimsRecord(record.intoMap()));

    var totalElements = dsl.selectCount().from(table).fetchOne(0, Long.class);

    return new PageResult<>(
        content,
        pageRequest.page(),
        pageRequest.size(),
        totalElements == null ? 0L : totalElements);
  }
}
