package tech.stackable.nifi.processors.iceberg;

import java.lang.reflect.Method;
import java.util.function.Function;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.parquet.ParquetValueWriter;
import org.apache.parquet.schema.MessageType;

// Iceberg 1.8.1 has
// public static ParquetValueWriter<Record> buildWriter(MessageType type) {
//
// Iceberg 1.10.0 has
// public static ParquetValueWriter<Record> create(Schema schema, MessageType type) {
//
// As we want to to be able to compile against both versions (and Java sucks at this), we need to
// fall back to
// reflection.
//
// FIXME: Remove once Iceberg 1.8.1 support is dropped
public class TemporaryGenericParquetWriterCompat {
  private static final Method CREATE_METHOD;
  private static final Method BUILD_METHOD;

  static {
    Method create = null;
    Method build = null;
    try {
      create = GenericParquetWriter.class.getMethod("create", Schema.class, MessageType.class);
    } catch (NoSuchMethodException ignored) {
    }
    try {
      build = GenericParquetWriter.class.getMethod("buildWriter", MessageType.class);
    } catch (NoSuchMethodException ignored) {
    }
    CREATE_METHOD = create;
    BUILD_METHOD = build;
    if (CREATE_METHOD == null && BUILD_METHOD == null) {
      throw new IllegalStateException(
          "Neither create(Schema, MessageType) nor buildWriter(MessageType) found");
    }
  }

  private TemporaryGenericParquetWriterCompat() {}

  public static Function<MessageType, ParquetValueWriter<?>> writerFunc(Schema schema) {
    if (CREATE_METHOD != null) {
      // v2
      return type -> {
        try {
          return (ParquetValueWriter<?>) CREATE_METHOD.invoke(null, schema, type);
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      };
    } else {
      // v1
      return type -> {
        try {
          return (ParquetValueWriter<?>) BUILD_METHOD.invoke(null, type);
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      };
    }
  }
}
