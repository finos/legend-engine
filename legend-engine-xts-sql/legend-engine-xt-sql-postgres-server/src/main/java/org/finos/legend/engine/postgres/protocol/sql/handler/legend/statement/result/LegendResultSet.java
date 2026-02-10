// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result;

import com.google.common.base.Function;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendColumn;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecutionResult;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSetMetaData;

import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.*;

public class LegendResultSet implements PostgresResultSet
{
    private static MutableMap<String, Function2<LegendColumn, Object, Object>> _processors = registerProcessors();

    public static final DateTimeFormatter TIMESTAMP_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(ISO_LOCAL_DATE)
                    .optionalStart()
                    .appendLiteral('T')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .appendOffset("+HHMM", "+0000")
                    .toFormatter();
    public static final DateTimeFormatter DATE_FORMAT = ISO_LOCAL_DATE;


    private LegendExecutionResult legendExecutionResult;

    private List<Function2<LegendColumn, Object, Object>> processors = Lists.mutable.empty();

    private List<Object> currentRow;


    public LegendResultSet(LegendExecutionResult legendExecutionResult)
    {
        this.legendExecutionResult = legendExecutionResult;
        this.processors = ListIterate.collect(legendExecutionResult.getLegendColumns(), c ->
        {
            Function<Function2<LegendColumn, Object, Object>, Function2<LegendColumn, Object, Object>> ifNullReturnsIdentity = (f -> f == null ? (col, o) -> o : f);
            return ifNullReturnsIdentity.apply(c.getLinearizedInheritances().isEmpty() ?
                    _processors.get(c.getType()) :
                    _processors.get(ListIterate.detect(c.getLinearizedInheritances(), v -> _processors.get(v) != null))
            );
        });
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        return new LegendResultSetMetaData(legendExecutionResult.getLegendColumns());
    }

    @Override
    public Object getObject(int i) throws Exception
    {
        return this.processors.get(i - 1).apply(legendExecutionResult.getLegendColumns().get(i - 1), currentRow.get(i - 1));
    }

    private static Object extractValue(Object value, LegendColumn column, Class expectedClassType, String expectedFormat, Function<Object, Object> function)
    {
        if (value == null)
        {
            return null;
        }

        if (!expectedClassType.isInstance(value))
        {
            throw new PostgresServerException(String.format("Unexpected data type for value '%s' in column '%s'. Expected data type '%s', actual data type '%s'",
                    obfuscateValue(value), column.getName(), expectedClassType.getName(), value.getClass().getName()));
        }

        try
        {
            return function.apply(value);
        }
        catch (Exception e)
        {
            throw new PostgresServerException(String.format("Unexpected value '%s' in column '%s'." +
                    " Expected data type '%s', value format '%s'", obfuscateValue(value), column.getName(), expectedClassType.getName(), expectedFormat), e);
        }
    }

    private static String obfuscateValue(Object value)
    {
        if (value == null)
        {
            return "";
        }
        String stringValue = value.toString();
        if (stringValue.length() <= 5)
        {
            return stringValue + "....";
        }
        return stringValue.substring(0, 5) + "....";
    }


    @Override
    public boolean next() throws Exception
    {
        if (legendExecutionResult.hasNext())
        {
            currentRow = legendExecutionResult.next();
            return true;
        }
        return false;
    }

    @Override
    public void close()
    {
        if (legendExecutionResult != null)
        {
            legendExecutionResult.close();
        }
    }

    @Override
    public void finished()
    {
    }


    private static MutableMap<String, Function2<LegendColumn, Object, Object>> registerProcessors()
    {
        MutableMap<String, Function2<LegendColumn, Object, Object>> processors = Maps.mutable.empty();
        registerProcessor(
                STRICT_DATE,
                (column, value) ->
                        extractValue(value, column, String.class, "Date (YYYY-MM-DD)",
                                f -> DATE_FORMAT.parse((String) value, LocalDate::from).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                processors
        );

        registerProcessor(
                Lists.mutable.with(DATE, DATE_TIME),
                (column, value) ->
                        extractValue(value, column, String.class, "Date (YYYY-MM-DD) or Timestamp (YYYY-MM-DDThh:mm:ss.000000000+0000)",
                                f ->
                                {
                                    TemporalAccessor temporalAccessor = TIMESTAMP_FORMATTER.parseBest((String) value, Instant::from, LocalDate::from);
                                    return (temporalAccessor instanceof Instant) ?
                                            ((Instant) temporalAccessor).toEpochMilli()
                                            : ((LocalDate) temporalAccessor).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                                }),
                processors
        );

        registerProcessor(
                Lists.mutable.with(INT),
                (column, value) -> extractValue(value, column, Number.class, "INT", f -> ((Number) value).intValue()),
                processors
        );

        registerProcessor(
                Lists.mutable.with(INTEGER),
                (column, value) -> extractValue(value, column, Number.class, "INTEGER", f -> ((Number) value).longValue()),
                processors
        );

        registerProcessor(
                Lists.mutable.with(FLOAT, DECIMAL, NUMBER),
                (column, value) -> extractValue(value, column, Number.class, "DECIMAL (FLOAT/DOUBLE)", f -> ((Number) value).doubleValue()),
                processors
        );

        registerProcessor(
                Lists.mutable.with(BOOLEAN),
                (column, value) -> extractValue(value, column, Boolean.class, "BOOLEAN", f -> value),
                processors
        );

        registerProcessor(
                Lists.mutable.with(STRING),
                (column, value) -> extractValue(value, column, String.class, "STRING", f -> value),
                processors
        );

        return processors;
    }

    private static void registerProcessor(MutableList<String> types, Function2<LegendColumn, Object, Object> processor, MutableMap<String, Function2<LegendColumn, Object, Object>> processors)
    {
        types.forEach(t -> processors.put(t, processor));
    }

    private static void registerProcessor(String type, Function2<LegendColumn, Object, Object> processor, MutableMap<String, Function2<LegendColumn, Object, Object>> processors)
    {
        processors.put(type, processor);
    }

}
