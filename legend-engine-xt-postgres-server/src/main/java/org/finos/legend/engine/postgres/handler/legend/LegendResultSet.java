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

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;

public class LegendResultSet implements PostgresResultSet
{

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
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
        return LocalDate.from(ISO_LOCAL_DATE.parse(value)).toEpochDay();
      case "Date":
        return LocalDate.from(ISO_LOCAL_DATE.parse(value)).toEpochDay();
      case "DateTime":
        //TODO HANDLE TIMESTAMP

        //return Types.TIMESTAMP;
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
