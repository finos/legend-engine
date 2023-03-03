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

import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

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


    private Iterator<TDSRow> tdsRowIterator;
    private List<LegendColumn> columns;
    private TDSRow currentRow;

    public LegendResultSet(Iterable<TDSRow> tdsRowIterable, List<LegendColumn> columns)
    {
        this.tdsRowIterator = tdsRowIterable.iterator();
        this.columns = columns;
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        return new LegendResultSetMetaData(columns);
    }

    @Override
    public Object getObject(int i) throws Exception
    {
        LegendColumn legendColumn = columns.get(i - 1);
        String value = currentRow.get(i - 1);
        switch (legendColumn.getType())
        {
            case "StrictDate":
                LocalDate localDate = ISO_LOCAL_DATE.parse(value, LocalDate::from);
                long toEpochMilli = localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                System.out.println("actual : " + toEpochMilli);
                return toEpochMilli;
            case "Date":
            case "DateTime":
                TemporalAccessor temporalAccessor = TIMESTAMP_FORMATTER.parseBest(value, Instant::from, LocalDate::from);
                if (temporalAccessor instanceof Instant)
                {                    //if date is a valid time stamp
                    return ((Instant) temporalAccessor).toEpochMilli();
                }
                else
                {
                    //if date is a date parse as date and convert to time tamp
                    return ((LocalDate) temporalAccessor).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                }
            case "Integer":
                return Integer.parseInt(value);
            case "Float":
                return Float.parseFloat(value);
            case "Number":
                return Double.parseDouble(value);
            case "Boolean":
                return Boolean.parseBoolean(value);
            default:
                return value;
        }
    }

    @Override
    public boolean next() throws Exception
    {
        if (tdsRowIterator.hasNext())
        {
            currentRow = tdsRowIterator.next();
            return true;
        }
        return false;
    }
}
