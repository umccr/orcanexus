package dev.ccgcm.platform.orcanexus.aurora.repository;

import dev.ccgcm.platform.orcanexus.domain.model.InvalidSortFieldException;
import dev.ccgcm.platform.orcanexus.domain.model.LimsRecord;
import dev.ccgcm.platform.orcanexus.domain.model.PageRequest;
import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import dev.ccgcm.platform.orcanexus.domain.model.SortDirection;
import dev.ccgcm.platform.orcanexus.domain.model.SortOrder;
import dev.ccgcm.platform.orcanexus.domain.port.LimsQueryPort;
import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.Table;
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
            .orderBy(orderBy(table, pageRequest.sort()))
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

  /**
   * Translates the requested sort orders into jOOQ sort fields.
   *
   * <p>The table's own column names are the whitelist: a requested field must match a column name
   * exactly, otherwise {@link InvalidSortFieldException} is thrown before any SQL is built. When a
   * field is requested more than once, the first request wins. The first column ascending is
   * appended as a tiebreaker unless it is already part of the ordering, which also keeps the
   * default ordering when no sort is requested and makes paging deterministic.
   */
  static List<SortField<?>> orderBy(Table<?> table, List<SortOrder> sort) {
    var fields = table.fields();
    var orderBy = new ArrayList<SortField<?>>();
    var used = new HashSet<String>();

    for (var order : sort) {
      var field = findField(fields, order.field());

      if (used.add(field.getName())) {
        orderBy.add(order.direction() == SortDirection.DESC ? field.desc() : field.asc());
      }
    }

    var tiebreaker = fields[0];
    if (used.add(tiebreaker.getName())) {
      orderBy.add(tiebreaker.asc());
    }

    return orderBy;
  }

  private static Field<?> findField(Field<?>[] fields, String name) {
    for (var field : fields) {
      if (field.getName().equals(name)) {
        return field;
      }
    }

    throw new InvalidSortFieldException(name);
  }
}
