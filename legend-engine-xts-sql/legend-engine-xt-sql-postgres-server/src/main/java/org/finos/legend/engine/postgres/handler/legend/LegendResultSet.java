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

package org.finos.legend.engine.postgres.handler.legend;

import com.google.common.base.Function;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;

import static org.finos.legend.engine.postgres.handler.legend.LegendDataType.*;

public class LegendResultSet implements PostgresResultSet
{

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
    private List<Object> currentRow;

    public LegendResultSet(LegendExecutionResult legendExecutionResult)
    {
        this.legendExecutionResult = legendExecutionResult;
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        return new LegendResultSetMetaData(legendExecutionResult.getLegendColumns());
    }

    @Override
    public Object getObject(int i) throws Exception
    {
        LegendColumn legendColumn = legendExecutionResult.getLegendColumns().get(i - 1);
        Object value = currentRow.get(i - 1);
        switch (legendColumn.getType())
        {

            //2020-06-07T04:15:27.000000000+0000
            case STRICT_DATE:
                return extractValue(value, legendColumn, String.class, "Date (YYYY-MM-DD)",
                        f ->
                        {
                            LocalDate localDate = DATE_FORMAT.parse((String) value, LocalDate::from);
                            long toEpochMilli = localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                            return toEpochMilli;
                        });
            case DATE:
            case DATE_TIME:
                return extractValue(value, legendColumn, String.class, "Date (YYYY-MM-DD) or Timestamp (YYYY-MM-DDThh:mm:ss.000000000+0000)",
                        f ->
                        {
                            TemporalAccessor temporalAccessor = TIMESTAMP_FORMATTER.parseBest((String) value, Instant::from, LocalDate::from);
                            if (temporalAccessor instanceof Instant)
                            {    //if date is a valid time stamp
                                return ((Instant) temporalAccessor).toEpochMilli();
                            }
                            else
                            {
                                //if date is a date parse as date and convert to time tamp
                                return ((LocalDate) temporalAccessor).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                            }
                        });
            case INTEGER:
                return extractValue(value, legendColumn, Number.class, "INTEGER",
                        f ->
                        {
                            return ((Number) value).longValue();
                        });
            case FLOAT:
            case NUMBER:
                return extractValue(value, legendColumn, Number.class, "DECIMAL (FLOAT/DOUBLE)",
                        f ->
                        {
                            return ((Number) value).doubleValue();
                        });
            case BOOLEAN:
                return extractValue(value, legendColumn, Boolean.class, "BOOLEAN",
                        f ->
                        {
                            return (Boolean) value;
                        });
            case STRING:
                return extractValue(value, legendColumn, String.class, "STRING",
                        f ->
                        {
                            return (String) value;
                        });
            default:
                return value;
        }
    }

    private Object extractValue(Object value, LegendColumn column, Class expectedClassType, String expectedFormat, Function<Object, Object> function)
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

    private String obfuscateValue(Object value)
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
}
