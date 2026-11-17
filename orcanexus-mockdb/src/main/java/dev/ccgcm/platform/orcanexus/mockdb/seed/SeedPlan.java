package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

/**
 * Deterministic plan built once per run from the {@link SeedContext}: library plans, run dates,
 * workflow plans and row counts. Stored as compact arrays; all accessors are by index.
 */
public final class SeedPlan {

  public static final LocalDate RANGE_START = LocalDate.of(2022, 1, 1);
  public static final LocalDate RANGE_END = LocalDate.of(2025, 12, 31);
  public static final int MIN_ROWS = 4;

  private static final List<String> WORKFLOW_NAMES =
      List.of("mock-wgs-align", "mock-wts-align", "mock-somatic-call", "mock-germline-call");
  private static final List<String> WORKFLOW_VERSIONS = List.of("1.0.0", "1.1.0", "2.0.0", "2.1.3");
  private static final List<String> WORKFLOW_STATUSES =
      List.of("SUCCEEDED", "FAILED", "ABORTED", "RUNNING");

  private static final int MIN_DURATION_SECONDS = 60;
  private static final int MAX_DURATION_SECONDS = 3 * 86_400;

  private final int scale;
  private final int subjects;
  private final int runs;
  private final int workflows;
  private final int[] runEpochDays;
  private final long[] workflowStart;
  private final int[] workflowDuration;
  private final byte[] workflowName;
  private final byte[] workflowVersion;
  private final byte[] workflowStatus;
  private final KeyPools pools;

  private SeedPlan(SeedContext context) {
    this.scale = context.scale();
    this.subjects = KeyPools.subjectCount(scale);
    this.runs = KeyPools.runCount(scale);
    this.workflows = Math.max(2 * scale, MIN_ROWS);

    Random random = context.stream("plan");

    // Run dates: run 0 is the first day of the range, the last run (if any) the last day.
    this.runEpochDays = new int[runs];
    int first = Math.toIntExact(RANGE_START.toEpochDay());
    int last = Math.toIntExact(RANGE_END.toEpochDay());
    int span = last - first + 1;
    for (int r = 0; r < runs; r++) {
      runEpochDays[r] = first + random.nextInt(span);
    }
    runEpochDays[0] = first;
    if (runs > 1) {
      runEpochDays[runs - 1] = last;
    }

    // Workflow plan.
    this.workflowStart = new long[workflows];
    this.workflowDuration = new int[workflows];
    this.workflowName = new byte[workflows];
    this.workflowVersion = new byte[workflows];
    this.workflowStatus = new byte[workflows];
    var workflowDates = new LocalDate[workflows];
    for (int j = 0; j < workflows; j++) {
      long dayStart = LocalDate.ofEpochDay(libraryEpochDay(j % scale)).toEpochDay() * 86_400L;
      long start = dayStart + random.nextInt(86_400);
      workflowStart[j] = start;
      workflowDuration[j] =
          MIN_DURATION_SECONDS + random.nextInt(MAX_DURATION_SECONDS - MIN_DURATION_SECONDS + 1);
      workflowName[j] = (byte) random.nextInt(WORKFLOW_NAMES.size());
      workflowVersion[j] = (byte) random.nextInt(WORKFLOW_VERSIONS.size());
      workflowStatus[j] = (byte) random.nextInt(WORKFLOW_STATUSES.size());
      workflowDates[j] = LocalDate.ofEpochDay(Math.floorDiv(start, 86_400L));
    }

    var runDates = new LocalDate[runs];
    for (int r = 0; r < runs; r++) {
      runDates[r] = LocalDate.ofEpochDay(runEpochDays[r]);
    }
    this.pools = new KeyPools(scale, runDates, workflowDates);
  }

  /** Builds the plan; the result depends only on the scale and the seed value. */
  public static SeedPlan of(SeedContext context) {
    return new SeedPlan(context);
  }

  public int scale() {
    return scale;
  }

  public KeyPools pools() {
    return pools;
  }

  // ---- libraries -------------------------------------------------------------------------

  public int libraryCount() {
    return scale;
  }

  public String libraryId(int i) {
    return pools.id(Pool.LIBRARY, checkLibrary(i));
  }

  public int librarySubjectIndex(int i) {
    return checkLibrary(i) % subjects;
  }

  public String librarySubjectId(int i) {
    return pools.id(Pool.SUBJECT, librarySubjectIndex(i));
  }

  public int librarySampleIndex(int i) {
    return checkLibrary(i);
  }

  public String librarySampleId(int i) {
    return pools.id(Pool.SAMPLE, librarySampleIndex(i));
  }

  public int libraryRunIndex(int i) {
    return checkLibrary(i) % runs;
  }

  public String libraryRunId(int i) {
    return pools.id(Pool.SEQUENCING_RUN, libraryRunIndex(i));
  }

  /** Library date: the date of its sequencing run. */
  public LocalDate libraryDate(int i) {
    return LocalDate.ofEpochDay(libraryEpochDay(i));
  }

  // ---- runs ------------------------------------------------------------------------------

  public int runCount() {
    return runs;
  }

  public String runId(int r) {
    return pools.id(Pool.SEQUENCING_RUN, r);
  }

  public LocalDate runDate(int r) {
    if (r < 0 || r >= runs) {
      throw new IndexOutOfBoundsException("run " + r + " of " + runs);
    }
    return LocalDate.ofEpochDay(runEpochDays[r]);
  }

  // ---- workflows -------------------------------------------------------------------------

  public int workflowCount() {
    return workflows;
  }

  public String workflowPortalRunId(int j) {
    return pools.id(Pool.PORTAL_RUN, checkWorkflow(j));
  }

  public int workflowLibraryIndex(int j) {
    return checkWorkflow(j) % scale;
  }

  public String workflowLibraryId(int j) {
    return libraryId(workflowLibraryIndex(j));
  }

  public String workflowName(int j) {
    return WORKFLOW_NAMES.get(workflowName[checkWorkflow(j)]);
  }

  public String workflowVersion(int j) {
    return WORKFLOW_VERSIONS.get(workflowVersion[checkWorkflow(j)]);
  }

  public String workflowStatus(int j) {
    return WORKFLOW_STATUSES.get(workflowStatus[checkWorkflow(j)]);
  }

  public Instant workflowStart(int j) {
    return Instant.ofEpochSecond(workflowStart[checkWorkflow(j)]);
  }

  public Instant workflowEnd(int j) {
    return workflowStart(j).plus(workflowDuration(j));
  }

  public Duration workflowDuration(int j) {
    return Duration.ofSeconds(workflowDuration[checkWorkflow(j)]);
  }

  /** Start as a UTC date-time, for timestamp columns without zone. */
  public LocalDateTime workflowStartLocal(int j) {
    return LocalDateTime.ofEpochSecond(workflowStart[checkWorkflow(j)], 0, ZoneOffset.UTC);
  }

  // ---- row counts ------------------------------------------------------------------------

  /** Rows for a mart table; depends only on the scale. Unknown tables get {@code max(L, 4)}. */
  public int rowCount(String table) {
    return switch (table) {
      case "lims" -> scale;
      case "workflow", "fastq", "workflow_output", "workflow_cost_report" -> workflows;
      default -> Math.max(scale, MIN_ROWS);
    };
  }

  /** Sum of the row counts of the seven known tables. */
  public long totalKnownRows() {
    long total = 0;
    for (String t :
        List.of(
            "lims", "workflow", "fastq", "bam", "vcf", "workflow_output", "workflow_cost_report")) {
      total += rowCount(t);
    }
    return total;
  }

  // ---- internals -------------------------------------------------------------------------

  private int libraryEpochDay(int i) {
    return runEpochDays[checkLibrary(i) % runs];
  }

  private int checkLibrary(int i) {
    if (i < 0 || i >= scale) {
      throw new IndexOutOfBoundsException("library " + i + " of " + scale);
    }
    return i;
  }

  private int checkWorkflow(int j) {
    if (j < 0 || j >= workflows) {
      throw new IndexOutOfBoundsException("workflow " + j + " of " + workflows);
    }
    return j;
  }
}
