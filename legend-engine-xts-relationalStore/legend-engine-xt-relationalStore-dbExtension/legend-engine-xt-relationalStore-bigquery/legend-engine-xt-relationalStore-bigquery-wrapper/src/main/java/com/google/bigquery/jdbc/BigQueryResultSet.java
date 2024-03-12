// Copyright 2023 Google LLC
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

package com.google.bigquery.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;

public class BigQueryResultSet implements ResultSet 
{

    private final TableResult tableResult;
    private final int columnCount;
    private Iterator<FieldValueList> iterator;
    private FieldValueList currentRow;
    Logger logger = Logger.getLogger(BigQueryResultSet.class.getName());

    public BigQueryResultSet(TableResult tableResult) throws SQLException 
    {
        this.tableResult = tableResult;
        columnCount = getMetaData().getColumnCount();
        this.iterator = tableResult.iterateAll().iterator();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException 
    {
        return false;
    }

    public boolean next() throws SQLException 
    {
        if (iterator.hasNext()) 
        {
            currentRow = iterator.next();
            return true;
        } 
        else 
        {
            currentRow = null;
            return false;
        }
    }

    public void close() throws SQLException 
    {
        // TODO Auto-generated method stub
    }

    public boolean wasNull() throws SQLException 
    {
        return false;
    }

    public String getString(int columnIndex) throws SQLException 
    {
        String columnValue = null;
        if (columnIndex <= columnCount) 
        {
            columnValue = currentRow.get(columnIndex - 1).getStringValue();
        }
        return columnValue;
    }

    public boolean getBoolean(int columnIndex) throws SQLException 
    {
        boolean columnValue = false;
        if (columnIndex <= columnCount) 
        {
            columnValue = currentRow.get(columnIndex - 1).getBooleanValue();
        }
        return columnValue;
    }

    public byte getByte(int columnIndex) throws SQLException 
    {
        byte columnValue = 0;
        String byteStr = null;
        if (columnIndex <= columnCount) 
        {
            byteStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Byte.parseByte(byteStr);
        }
        return columnValue;
    }

    public short getShort(int columnIndex) throws SQLException 
    {
        short columnValue = 0;
        String shortStr = null;
        if (columnIndex <= columnCount) 
        {
            shortStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Short.parseShort(shortStr);
        }
        return columnValue;
    }

    public int getInt(int columnIndex) throws SQLException 
    {
        int columnValue = 0;
        String integerStr = null;
        if (columnIndex <= columnCount) 
        {
            integerStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Integer.parseInt(integerStr);
        }
        return columnValue;
    }

    public long getLong(int columnIndex) throws SQLException 
    {
        long columnValue = 0;
        if (columnIndex <= columnCount) 
        {
            columnValue = currentRow.get(columnIndex - 1).getLongValue();
        }
        return columnValue;
    }

    public float getFloat(int columnIndex) throws SQLException 
    {
        float columnValue = 0;
        String floatValue = null;
        if (columnIndex <= columnCount) 
        {
            floatValue = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Float.valueOf(floatValue);
        }
        return columnValue;
    }

    public double getDouble(int columnIndex) throws SQLException 
    {
        double columnValue = 0;
        if (columnIndex <= columnCount) 
        {
            columnValue = currentRow.get(columnIndex - 1).getDoubleValue();
        }
        return columnValue;
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException 
    {
        BigDecimal columnValue = null;
        String floatValue = null;
        if (columnIndex <= columnCount) 
        {
            floatValue = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = new BigDecimal(floatValue);
        }
        return columnValue;
    }

    public byte[] getBytes(int columnIndex) throws SQLException 
    {
        byte[] columnValue = null;
        if (columnIndex <= columnCount) 
        {
            columnValue = currentRow.get(columnIndex - 1).getBytesValue();
        }
        return columnValue;
    }

    public Date getDate(int columnIndex) throws SQLException 
    {
        Date columnValue = null;
        String dateStr = null;
        if (columnIndex <= columnCount) 
        {
            dateStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Date.valueOf(dateStr);
        }
        return columnValue;
    }

    public Time getTime(int columnIndex) throws SQLException 
    {
        Time columnValue = null;
        String timesStr = null;
        if (columnIndex <= columnCount) 
        {
            timesStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Time.valueOf(timesStr);
        }
        return columnValue;
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException 
    {
        Timestamp columnValue = null;
        String timestampStr = null;
        if (columnIndex <= columnCount) 
        {
            timestampStr = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = Timestamp.valueOf(timestampStr);
        }
        return columnValue;
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public String getString(String columnLabel) throws SQLException 
    {
        String columnValue = null;
        if (columnLabel != null) 
        {
            columnValue = currentRow.get(columnLabel).getStringValue();
        }
        return columnValue;
    }

    public boolean getBoolean(String columnLabel) throws SQLException 
    {
        boolean columnValue = false;
        if (columnLabel != null) 
        {
            columnValue = currentRow.get(columnLabel).getBooleanValue();
        }
        return columnValue;
    }

    public byte getByte(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public short getShort(String columnLabel) throws SQLException 
    {
        short columnValue = 0;
        if (columnLabel != null) 
        {
            columnValue = (Short) currentRow.get(columnLabel).getValue();
        }
        return columnValue;
    }

    public int getInt(String columnLabel) throws SQLException 
    {
        int columnValue = 0;
        if (columnLabel != null) 
        {
            columnValue = (Integer) currentRow.get(columnLabel).getValue();
        }
        return columnValue;
    }

    public long getLong(String columnLabel) throws SQLException 
    {
        long columnValue = 0;
        if (columnLabel != null) 
        {
            columnValue = currentRow.get(columnLabel).getLongValue();
        }
        return columnValue;
    }

    public float getFloat(String columnLabel) throws SQLException 
    {
        float columnValue = 0;
        if (columnLabel != null) 
        {
            columnValue = (Float) currentRow.get(columnLabel).getValue();
        }
        return columnValue;
    }

    public double getDouble(String columnLabel) throws SQLException 
    {
        double columnValue = 0;
        if (columnLabel != null) 
        {
            columnValue = currentRow.get(columnLabel).getDoubleValue();
        }
        return columnValue;
    }

    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public byte[] getBytes(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Date getDate(String columnLabel) throws SQLException 
    {
        Date columnValue = null;
        String dateStr = null;
        if (columnLabel != null) 
        {
            dateStr = currentRow.get(columnLabel).getStringValue();
            columnValue = Date.valueOf(dateStr);
        }
        return columnValue;
    }

    public Time getTime(String columnLabel) throws SQLException 
    {
        Time columnValue = null;
        String timeStr = null;
        if (columnLabel != null) 
        {
            timeStr = currentRow.get(columnLabel).getStringValue();
            columnValue = Time.valueOf(timeStr);
        }
        return columnValue;
    }

    public Timestamp getTimestamp(String columnLabel) throws SQLException 
    {
        Timestamp columnValue = null;
        String timestampStr = null;
        if (columnLabel != null) 
        {
            timestampStr = currentRow.get(columnLabel).getStringValue();
            columnValue = Timestamp.valueOf(timestampStr);
        }
        return columnValue;
    }

    public InputStream getAsciiStream(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public InputStream getUnicodeStream(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public InputStream getBinaryStream(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public SQLWarning getWarnings() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void clearWarnings() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public String getCursorName() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public BigQueryResultSetMetaData getMetaData() throws SQLException 
    {
        return new BigQueryResultSetMetaData(tableResult);
    }

    public Object getObject(int columnIndex) throws SQLException 
    {
        Object columnValue = null;
        int columnType = getMetaData().getColumnType(columnIndex);

        switch (columnType) 
        {
        case Types.INTEGER: 
        {
            return getLong(columnIndex);
        }
        case Types.DATE: 
        {
            return getDate(columnIndex);
        }
        case Types.FLOAT: 
        {
            return getBigDecimal(columnIndex);
        }
        case Types.BOOLEAN: 
        {
            return getBoolean(columnIndex);
        }
        case Types.TIMESTAMP: 
        {
            return getTimestamp(columnIndex);
        }
        default:
            columnValue = currentRow.get(columnIndex - 1).getValue();
        }
        return columnValue;
    }

    public Object getObject(String columnLabel) throws SQLException 
    {
        Object columnValue = null;
        if (columnLabel != null) 
        {
            columnValue = currentRow.get(columnLabel).getValue();
        }
        return columnValue;
    }

    public int findColumn(String columnLabel) throws SQLException 
    {
        return 0;
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Reader getCharacterStream(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException 
    {
        BigDecimal columnValue = null;
        String floatValue = null;
        if (columnIndex <= columnCount) 
        {
            floatValue = currentRow.get(columnIndex - 1).getStringValue();
            columnValue = new BigDecimal(floatValue);
        }
        return columnValue;
    }

    public BigDecimal getBigDecimal(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isBeforeFirst() throws SQLException 
    {
        return false;
    }

    public boolean isAfterLast() throws SQLException 
    {
        return false;
    }

    public boolean isFirst() throws SQLException 
    {
        return false;
    }

    public boolean isLast() throws SQLException 
    {
        return false;
    }

    public void beforeFirst() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void afterLast() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public boolean first() throws SQLException 
    {
        return false;
    }

    public boolean last() throws SQLException 
    {
        return false;
    }

    public int getRow() throws SQLException 
    {
        return 0;
    }

    public boolean absolute(int row) throws SQLException 
    {
        return false;
    }

    public boolean relative(int rows) throws SQLException 
    {
        return false;
    }

    public boolean previous() throws SQLException 
    {
        return false;
    }

    public void setFetchDirection(int direction) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getFetchDirection() throws SQLException 
    {
        return 0;
    }

    public void setFetchSize(int rows) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public int getFetchSize() throws SQLException 
    {
        return 0;
    }

    public int getType() throws SQLException 
    {
        return 0;
    }

    public int getConcurrency() throws SQLException 
    {
        return 0;
    }

    public boolean rowUpdated() throws SQLException 
    {
        return false;
    }

    public boolean rowInserted() throws SQLException 
    {
        return false;
    }

    public boolean rowDeleted() throws SQLException 
    {
        return false;
    }

    public void updateNull(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateByte(int columnIndex, byte x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateShort(int columnIndex, short x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateInt(int columnIndex, int x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateLong(int columnIndex, long x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateFloat(int columnIndex, float x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateDouble(int columnIndex, double x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateString(int columnIndex, String x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateDate(int columnIndex, Date x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateTime(int columnIndex, Time x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateObject(int columnIndex, Object x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNull(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBoolean(String columnLabel, boolean x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateByte(String columnLabel, byte x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateShort(String columnLabel, short x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateInt(String columnLabel, int x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateLong(String columnLabel, long x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateFloat(String columnLabel, float x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateDouble(String columnLabel, double x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateString(String columnLabel, String x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBytes(String columnLabel, byte[] x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateDate(String columnLabel, Date x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateTime(String columnLabel, Time x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateObject(String columnLabel, Object x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void insertRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void deleteRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void refreshRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void cancelRowUpdates() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void moveToInsertRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void moveToCurrentRow() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public Statement getStatement() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Ref getRef(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Blob getBlob(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Clob getClob(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Array getArray(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Ref getRef(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Blob getBlob(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Clob getClob(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Array getArray(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Date getDate(String columnLabel, Calendar cal) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Time getTime(String columnLabel, Calendar cal) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException 
    {
        Timestamp columnValue = null;
        String timestampStr = null;
        OffsetDateTime odt = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        if (columnIndex <= columnCount) 
        {
            timestampStr = currentRow.get(columnIndex - 1).getStringValue();
            try 
            {
                // First, try parsing as a double for epoch time
                double epochTimeWithFraction = Double.parseDouble(timestampStr);
                long epochTimeSeconds = (long) epochTimeWithFraction;
                int nanoSeconds = (int) ((epochTimeWithFraction - epochTimeSeconds) * 1_000_000_000);
                Instant instant = Instant.ofEpochSecond(epochTimeSeconds, nanoSeconds).atOffset(odt.getOffset()).toInstant();
                columnValue = Timestamp.from(instant);
            } 
            catch (NumberFormatException e) 
            {
                try 
                {
                    OffsetDateTime dateTime = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME).atOffset(odt.getOffset());
                    columnValue = Timestamp.from(dateTime.toInstant());
                } 
                catch (DateTimeParseException e2) 
                {
                    // If parsing as ISO date-time format fails, try parsing as ISO date
                    try 
                    {
                        LocalDate date = LocalDate.parse(timestampStr, DateTimeFormatter.ISO_DATE);
                        columnValue = Timestamp.valueOf(date.atStartOfDay());
                    } 
                    catch (DateTimeParseException e3) 
                    {
                        logger.info("Invalid date or date-time format: " + timestampStr);
                    }
                }
            }
        }
        return columnValue;
    }

    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public URL getURL(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public URL getURL(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateRef(String columnLabel, Ref x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(String columnLabel, Blob x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(int columnIndex, Clob x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(String columnLabel, Clob x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateArray(int columnIndex, Array x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateArray(String columnLabel, Array x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public RowId getRowId(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public RowId getRowId(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public int getHoldability() throws SQLException 
    {
        return 0;
    }

    public boolean isClosed() throws SQLException 
    {
        return false;
    }

    public void updateNString(int columnIndex, String nString) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNString(String columnLabel, String nString) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public NClob getNClob(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public NClob getNClob(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public String getNString(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public String getNString(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

}
