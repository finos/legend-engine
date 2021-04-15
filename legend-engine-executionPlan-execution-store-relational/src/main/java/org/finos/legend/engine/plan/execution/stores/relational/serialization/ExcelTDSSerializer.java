// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ExcelTDSSerializer extends Serializer
{
    private final RelationalResult relationalResult;

    public ExcelTDSSerializer(RelationalResult relationalResult)
    {
        this.relationalResult = relationalResult;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        try
        {
            ResultSet resultSet = this.relationalResult.resultSet;
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            SXSSFWorkbook wb = new SXSSFWorkbook(500);
            SXSSFSheet sheet = wb.createSheet();
            sheet.trackAllColumnsForAutoSizing();

            int currentRow = 0;

            // Generate headers
            SXSSFRow row = sheet.createRow(currentRow);
            for (int i = 0; i < this.relationalResult.columnCount; i++)
            {
                String columnTitle = resultSetMetaData.getColumnName(i + 1);
                generateCell(row, i, columnTitle, ValueType.STRING);
            }

            ValueType[] valueTypes = new ValueType[this.relationalResult.columnCount];
            for (int i = 0; i < valueTypes.length; i++)
            {
                valueTypes[i] = getValueType(resultSetMetaData, i + 1);
            }
            currentRow++;
            while (resultSet.next())
            {
                row = sheet.createRow(currentRow++);
                for (int i = 0; i < this.relationalResult.columnCount; i++)
                {
                    generateCell(row, i, this.relationalResult.getValue(i + 1), valueTypes[i]);
                }
            }

            resizeColumns(sheet, this.relationalResult.columnCount);

            try
            {
                wb.write(targetStream);
            }
            finally
            {
                targetStream.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Excel serialization exception: " + e.getMessage(), e);
        }
    }

    private void resizeColumns(SXSSFSheet sheet, int numCols)
    {
        for (int i = 0; i < numCols; i++)
        {
            sheet.autoSizeColumn(i);
        }
    }

    private void generateCell(SXSSFRow row, int col, Object value, ValueType columnType)
    {
        SXSSFCell cell = row.createCell(col);
        if ((columnType != null) && (value != null))
        {
            switch (columnType)
            {
                case NUMERIC:
                {
                    cell.setCellValue(((Number) value).doubleValue());
                    break;
                }
                case DATE:
                {
                    cell.setCellValue((java.util.Date) value);
                    break;
                }
                default:
                {
                    cell.setCellValue(value.toString());
                }
            }
        }
    }

    private static ValueType getValueType(ResultSetMetaData resultSetMetaData, int column) throws ClassNotFoundException, SQLException
    {
        Class<?> columnClass = Class.forName(resultSetMetaData.getColumnClassName(column));
        if (Number.class.isAssignableFrom(columnClass))
        {
            return ValueType.NUMERIC;
        }
        if (java.util.Date.class.isAssignableFrom(columnClass))
        {
            return ValueType.DATE;
        }
        return ValueType.OTHER;
    }

    private enum ValueType
    {
        NUMERIC, DATE, STRING, OTHER
    }
}
