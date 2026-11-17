package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.Iterator;
import java.util.List;
import org.jooq.TableRecord;

/**
 * Produces the synthetic rows of one table.
 *
 * <p>Contract, shared by every seeder:
 *
 * <ul>
 *   <li>{@link #table()} is the physical table name as in the jOOQ model (for example {@code
 *       lims}).
 *   <li>{@link #rowCount(SeedPlan)} depends only on the plan (so on the scale), never on the seed.
 *   <li>{@link #rows} returns a lazy iterator that yields exactly {@code rowCount(plan)} records in
 *       row-index order. Records are detached (not bound to a connection); the runner inserts them
 *       in batches of 1000 with {@code dsl.batchInsert(records)}.
 *   <li>Every field of every record is set explicitly (a value or {@code null}), so all records are
 *       "changed" on the same columns and {@code batchInsert} stays one statement per batch.
 *   <li>All randomness comes from named streams of the {@link SeedContext}; no clock, no unordered
 *       collections. Same plan, context and spec give the same rows.
 *   <li>Notes (for example {@link SeedNote.Kind#BOUNDARY_SKIPPED}) are added to the {@code notes}
 *       list while {@code rows} runs its set-up, that is before it returns; the iterator itself
 *       adds none.
 *   <li>The returned iterator is single use and not thread safe.
 * </ul>
 */
public interface TableSeeder {

  /** Physical table name in the model. */
  String table();

  /** Number of rows this seeder yields for the plan. */
  int rowCount(SeedPlan plan);

  /**
   * Lazily yields the table's records.
   *
   * @param plan the run plan (key pools, library and workflow plans)
   * @param context scale, seed and the named random streams
   * @param spec relationship spec; decides which columns are pool columns (never null)
   * @param notes receives warnings and skips found during set-up; mutable
   * @return iterator of {@code rowCount(plan)} detached records
   */
  Iterator<? extends TableRecord<?>> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes);
}
