package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.TableRecord;

/**
 * Type-valid, deterministic values for columns without a dedicated strategy.
 *
 * <p>Values depend only on the JDBC type of the column, the supplied {@link Random} and the row
 * index. Text is {@code MOCK}-style, dates and timestamps stay inside the seeded range, numbers
 * respect precision and scale. No clock is read.
 */
public final class GenericValues {

  private static final int MAX_INTEGER_DIGITS = 6;
  private static final int MAX_UNSCALED_DIGITS = 17;
  private static final int DEFAULT_PRECISION = 10;
  private static final int DEFAULT_SCALE = 2;
  private static final int SECONDS_PER_DAY = 86_400;

  private GenericValues() {}

  /**
   * Sets every field of the record that has not been changed to a generic value. A field whose type
   * has no generic value is set to null when nullable and left unset otherwise.
   */
  public static void fillUnset(TableRecord<?> record, Random random, int row) {
    for (Field<?> field : record.fields()) {
      if (!record.changed(field)) {
        setGeneric(record, field, random, row);
      }
    }
  }

  private static <T> void setGeneric(
      TableRecord<?> record, Field<T> field, Random random, int row) {
    Object value = value(field.getDataType(), random, row);
    if (value != null) {
      record.set(field, field.getDataType().convert(value));
    } else if (field.getDataType().nullable()) {
      record.set(field, null);
    }
  }

  /**
   * A type-valid value for the data type, or {@code null} when the Java type is not supported
   * (callers then leave the column null).
   *
   * @param type the column type from the jOOQ model
   * @param random the stream to draw from; advanced identically for a given type
   * @param row the row index, used in text values
   */
  public static Object value(DataType<?> type, Random random, int row) {
    Class<?> java = type.getType();
    if (java == String.class) {
      return text(type, random, row);
    }
    if (java == Long.class || java == long.class) {
      return (long) random.nextInt(1_000_000);
    }
    if (java == Integer.class || java == int.class) {
      return random.nextInt(10_000);
    }
    if (java == Short.class || java == short.class) {
      return (short) random.nextInt(1_000);
    }
    if (java == Byte.class || java == byte.class) {
      return (byte) random.nextInt(100);
    }
    if (java == BigDecimal.class) {
      return numeric(type, random);
    }
    if (java == Double.class || java == double.class) {
      return random.nextInt(1_000_000) / 100.0;
    }
    if (java == Float.class || java == float.class) {
      return random.nextInt(10_000) / 100.0f;
    }
    if (java == Boolean.class || java == boolean.class) {
      return random.nextBoolean();
    }
    if (java == LocalDate.class) {
      return date(random);
    }
    if (java == LocalDateTime.class) {
      return timestamp(random);
    }
    if (java == OffsetDateTime.class) {
      return timestamp(random).atOffset(ZoneOffset.UTC);
    }
    if (java == Instant.class) {
      return timestamp(random).toInstant(ZoneOffset.UTC);
    }
    if (java == LocalTime.class) {
      return LocalTime.ofSecondOfDay(random.nextInt(SECONDS_PER_DAY));
    }
    if (java == UUID.class) {
      return new UUID(random.nextLong(), random.nextLong());
    }
    if (java == byte[].class) {
      byte[] bytes = new byte[8];
      random.nextBytes(bytes);
      return bytes;
    }
    return null;
  }

  private static String text(DataType<?> type, Random random, int row) {
    String text = String.format(Locale.ROOT, "MOCK-%05d-%06x", row, random.nextInt(0x1000000));
    if (type.hasLength() && type.length() > 0 && text.length() > type.length()) {
      return text.substring(0, type.length());
    }
    return text;
  }

  private static BigDecimal numeric(DataType<?> type, Random random) {
    int precision =
        type.hasPrecision() && type.precision() > 0 ? type.precision() : DEFAULT_PRECISION;
    int scale =
        type.hasPrecision() && type.precision() > 0 ? Math.max(0, type.scale()) : DEFAULT_SCALE;
    scale = Math.min(scale, precision);
    int integerDigits = Math.min(precision - scale, MAX_INTEGER_DIGITS);
    int digits = Math.min(integerDigits + scale, MAX_UNSCALED_DIGITS);
    long bound = 1;
    for (int i = 0; i < digits; i++) {
      bound *= 10;
    }
    return BigDecimal.valueOf(random.nextLong(bound), scale);
  }

  private static LocalDate date(Random random) {
    long days = ChronoUnit.DAYS.between(SeedPlan.RANGE_START, SeedPlan.RANGE_END);
    return SeedPlan.RANGE_START.plusDays(random.nextLong(days + 1));
  }

  private static LocalDateTime timestamp(Random random) {
    return date(random).atStartOfDay().plusSeconds(random.nextInt(SECONDS_PER_DAY));
  }
}
