// Copyright 2021 Goldman Sachs
// Licensed under the Apache License, Version 2.0

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.github.luben.zstd.ZstdOutputStream;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.stores.relational.activity.AggregationAwareActivity;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtension;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public class RelationalResultToArrowIPCSerializer extends Serializer
{
    private static final int DEFAULT_BATCH_SIZE = 1024;

    private final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final RelationalResult relationalResult;
    private final int batchSize;

    public RelationalResultToArrowIPCSerializer(RelationalResult relationalResult)
    {
        this(relationalResult, DEFAULT_BATCH_SIZE);
    }

    public RelationalResultToArrowIPCSerializer(RelationalResult relationalResult,
                                                int batchSize)
    {
        this.relationalResult = relationalResult;
        this.batchSize = batchSize;


        this.objectMapper.registerSubtypes(new NamedType(AggregationAwareActivity.class, "aggregationAware"));
        this.objectMapper.registerSubtypes(new NamedType(RelationalExecutionActivity.class, "relational"));
        Iterate.addAllTo(ResultInterpreterExtensionLoader.extensions(), Lists.mutable.empty())
                .flatCollect(ResultInterpreterExtension::additionalMappers)
                .forEach(e -> this.objectMapper.registerSubtypes(new NamedType(e.getOne(), e.getTwo())));
    }

    @Override
    public void stream(OutputStream out)
    {
        try (BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE))
        {
            ResultSet rs = relationalResult.getResultSet();
            Schema schema = buildSchema(relationalResult);

            try (VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator))
            {
                try (ZstdOutputStream zstdOut = new ZstdOutputStream(out);
                     WritableByteChannel channel = Channels.newChannel(zstdOut);
                     ArrowStreamWriter writer = new ArrowStreamWriter(root, null, channel))
                {
                    writer.start();
                    // Write your record batches here
                    streamRows(rs, root, writer, relationalResult.columnCount);
                    writer.end();
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            relationalResult.close();
        }
    }


    // ----------------------------------------------------------------------
    // Schema construction (with Legend metadata as schema-level custom_metadata)
    // ----------------------------------------------------------------------
    private Schema buildSchema(RelationalResult rr) throws Exception
    {
        ResultSetMetaData md = rr.resultSetMetaData;
        int columnCount = rr.columnCount;
        List<Field> fields = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++)
        {
            String name = md.getColumnLabel(i);
            int jdbcType = md.getColumnType(i);
            int precision = md.getPrecision(i);
            int scale = md.getScale(i);
            ArrowType arrowType = jdbcToArrow(jdbcType, precision, scale);
            fields.add(new Field(name, FieldType.nullable(arrowType), null));
        }

        Map<String, String> meta = new HashMap<>();
        meta.put("legend.builder", objectMapper.writeValueAsString(relationalResult.builder));
        meta.put("legend.activities", objectMapper.writeValueAsString(relationalResult.activities));
        if (relationalResult.generationInfo != null)
        {
            meta.put("legend.generationInfo",
                    ObjectMapperFactory.getNewStandardObjectMapper()
                            .writeValueAsString(relationalResult.generationInfo));
        }
        meta.put("legend.columns",
                objectMapper.writeValueAsString(relationalResult.getColumnListForSerializer()));

        return new Schema(fields, meta);
    }

    private ArrowType jdbcToArrow(int sqlType, int precision, int scale)
    {
        switch (sqlType)
        {
            case Types.BIT:
            case Types.BOOLEAN:
                return ArrowType.Bool.INSTANCE;
            case Types.TINYINT:
                return new ArrowType.Int(8, true);
            case Types.SMALLINT:
                return new ArrowType.Int(16, true);
            case Types.INTEGER:
                return new ArrowType.Int(32, true);
            case Types.BIGINT:
                return new ArrowType.Int(64, true);
            case Types.FLOAT:
            case Types.REAL:
                return new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE);
            case Types.DOUBLE:
                return new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
            case Types.NUMERIC:
            case Types.DECIMAL:
            {
                // Arrow 128-bit decimals cap precision at 38.
                int p = (precision <= 0) ? 38 : Math.min(precision, 38);
                // Scale must be non-negative and not exceed precision.
                int s = Math.max(scale, 0);
                if (s > p)
                {
                    s = p;
                }
                return new ArrowType.Decimal(p, s, 128);
            }
            case Types.DATE:
                return new ArrowType.Date(DateUnit.DAY);
            case Types.TIME:
                return new ArrowType.Time(TimeUnit.MILLISECOND, 32);
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return new ArrowType.Timestamp(TimeUnit.MICROSECOND, "UTC");
            default:
                return ArrowType.Utf8.INSTANCE; // fallback (CHAR, VARCHAR, CLOB, etc.)
        }
    }

    // ----------------------------------------------------------------------
    // Row streaming with batching + tracing (mirrors the original spans)
    // ----------------------------------------------------------------------
    private void streamRows(ResultSet rs, VectorSchemaRoot root,
                            ArrowStreamWriter writer, int columnCount) throws Exception
    {
        int totalRows = 0;
        int rowsInBatch = 0;
        root.allocateNew();

        try (Scope ignored = GlobalTracer.get().buildSpan("Arrow Streaming: Fetch first row").startActive(true))
        {
            if (!rs.isClosed() && rs.next())
            {
                writeRow(rs, root, rowsInBatch, columnCount);
                rowsInBatch++;
                totalRows++;

                if (rowsInBatch >= batchSize)
                {
                    root.setRowCount(rowsInBatch);
                    writer.writeBatch();
                    root.allocateNew();   // re-allocate after writeBatch resets vectors
                    rowsInBatch = 0;
                }
            }
        }

        try (Scope rest = GlobalTracer.get().buildSpan("Arrow Streaming: remaining rows").startActive(true))
        {
            while (!rs.isClosed() && rs.next())
            {
                writeRow(rs, root, rowsInBatch, columnCount);
                rowsInBatch++;
                totalRows++;

                if (rowsInBatch >= batchSize)
                {
                    root.setRowCount(rowsInBatch);
                    writer.writeBatch();
                    root.allocateNew();   // re-allocate after writeBatch resets vectors
                    rowsInBatch = 0;
                }
            }

            if (rowsInBatch > 0)
            {
                root.setRowCount(rowsInBatch);
                writer.writeBatch();
            }

            rest.span().setTag("rowCount", totalRows);
            if (relationalResult.topSpan != null)
            {
                relationalResult.topSpan.setTag("lastQueryRowCount", totalRows);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Per-row column dispatch
    // ----------------------------------------------------------------------
    private void writeRow(ResultSet rs, VectorSchemaRoot root, int rowIdx, int columnCount) throws Exception
    {
        for (int i = 1; i <= columnCount; i++)
        {
            FieldVector v = root.getVector(i - 1);
            Object value = relationalResult.getValue(i);
            if (value == null || rs.wasNull())
            {
                setNull(v, rowIdx);
                continue;
            }
            setValue(v, rowIdx, value);
        }
    }

    private void setNull(FieldVector v, int idx)
    {
        if (v instanceof BaseFixedWidthVector)
        {
            v.setNull(idx);
        }
        else if (v instanceof BaseVariableWidthVector)
        {
            v.setNull(idx);
        }
    }

    @SuppressWarnings("unchecked")
    private void setValue(FieldVector v, int idx, Object value) throws Exception
    {
        if (v instanceof BitVector)
        {
            ((BitVector) v).setSafe(idx, ((Boolean) value) ? 1 : 0);
        }
        else if (v instanceof TinyIntVector)
        {
            ((TinyIntVector) v).setSafe(idx, ((Number) value).byteValue());
        }
        else if (v instanceof SmallIntVector)
        {
            ((SmallIntVector) v).setSafe(idx, ((Number) value).shortValue());
        }
        else if (v instanceof IntVector)
        {
            ((IntVector) v).setSafe(idx, ((Number) value).intValue());
        }
        else if (v instanceof BigIntVector)
        {
            ((BigIntVector) v).setSafe(idx, ((Number) value).longValue());
        }
        else if (v instanceof Float4Vector)
        {
            ((Float4Vector) v).setSafe(idx, ((Number) value).floatValue());
        }
        else if (v instanceof Float8Vector)
        {
            ((Float8Vector) v).setSafe(idx, ((Number) value).doubleValue());
        }
        else if (v instanceof DecimalVector)
        {
            BigDecimal bd = (value instanceof BigDecimal) ? (BigDecimal) value : new BigDecimal(value.toString());
            DecimalVector dv = (DecimalVector) v;
            ((DecimalVector) v).setSafe(idx, bd.setScale(dv.getScale(), RoundingMode.HALF_UP));
        }
        else if (v instanceof DateDayVector)
        {
            long epochDay = ((java.sql.Date) value).toLocalDate().toEpochDay();
            ((DateDayVector) v).setSafe(idx, (int) epochDay);
        }
        else if (v instanceof TimeMilliVector)
        {
            ((TimeMilliVector) v).setSafe(idx, (int) ((java.sql.Time) value).getTime());
        }
        else if (v instanceof TimeStampMicroTZVector)
        {
            long micros = ((java.sql.Timestamp) value).getTime() * 1000L
                    + (((java.sql.Timestamp) value).getNanos() % 1_000_000) / 1000;
            ((TimeStampMicroTZVector) v).setSafe(idx, micros);
        }
        else if (v instanceof VarCharVector)
        {
            ((VarCharVector) v).setSafe(idx, value.toString().getBytes(StandardCharsets.UTF_8));
        }
        else
        {
            throw new IllegalStateException("Unsupported vector type: " + v.getClass());
        }
    }
}
//