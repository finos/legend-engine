/*
 * Copyright 2020 Google LLC
 */

package com.deloitte.legend.engine.bigquery;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.TableResult;

public class BigQueryResultSetMetaData implements ResultSetMetaData 
{

    private final TableResult tableResult;
    private final Iterator<Field> fieldList;
    ArrayList<Field> arrayList = new ArrayList<Field>();
    
    public BigQueryResultSetMetaData(TableResult tableResult) 
    {
        this.tableResult = tableResult;
        fieldList = tableResult.getSchema().getFields().iterator();
        while (fieldList.hasNext()) 
        {
            arrayList.add(fieldList.next());
        }
    }

    public <T> T unwrap(Class<T> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException 
    {
        return false;
    }

    public int getColumnCount() throws SQLException 
    {
        return tableResult.getSchema().getFields().size();
    }

    public boolean isAutoIncrement(int column) throws SQLException 
    {
        return false;
    }

    public boolean isCaseSensitive(int column) throws SQLException 
    {
        return false;
    }

    public boolean isSearchable(int column) throws SQLException 
    {
        return false;
    }

    public boolean isCurrency(int column) throws SQLException 
    {        
        return false;
    }

    public int isNullable(int column) throws SQLException 
    {
        return 0;
    }

    public boolean isSigned(int column) throws SQLException 
    {
        return false;
    }

    public int getColumnDisplaySize(int column) throws SQLException 
    {
        return 0;
    }

    public String getColumnLabel(int column) throws SQLException 
    {
        int size = arrayList.size();
        if (size == column) 
        {
            return arrayList.get(column - 1).getName();
        } 
        else if (column < size) 
        {
            return arrayList.get(column).getName();
        }
        return null;
    }

    public String getColumnName(int column) throws SQLException 
    {
        int size = arrayList.size();
        if (size == column) 
        {
            return arrayList.get(column - 1).getName();
        } 
        else if (column < size) 
        {
            return arrayList.get(column).getName();
        }
        return null;
    }

    public String getSchemaName(int column) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getPrecision(int column) throws SQLException 
    {        
        return 0;
    }

    public int getScale(int column) throws SQLException 
    {
        return 0;
    }

    public String getTableName(int column) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public String getCatalogName(int column) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getColumnType(int column) throws SQLException 
    {
        String columnType = getColumnTypeName(column);
        switch (columnType) 
        {
        case "STRING":
            return Types.VARCHAR;
        case "INT64":
            return Types.BIGINT;
        case "FLOAT64":
            return Types.DOUBLE;
        case "BOOLEAN":
            return Types.BOOLEAN;
        case "DATE":
            return Types.DATE;
        case "DATETIME":
            return Types.DATE;
        case "NUMERIC":
            return Types.NUMERIC;
        case "JSON":
            return Types.NVARCHAR;
        case "ARRAY":
            return Types.ARRAY;
        case "FLOAT":
            return Types.FLOAT;
        case "INTEGER":
            return Types.INTEGER;
        case "DECIMAL":
            return Types.DECIMAL;
        case "TIMESTAMP":
            return Types.TIMESTAMP;
        default:
            throw new SQLException("Unsupported BigQuery type: " + columnType);
        }
    }

    public String getColumnTypeName(int column) throws SQLException 
    {
        int size = arrayList.size();
        String columnType = null;
        if (size == column) 
        {
            columnType = arrayList.get(column - 1).getType().toString();
        } 
        else if (column < size) 
        {
            columnType = arrayList.get(column).getType().toString();
        }
        return columnType;
    }

    public boolean isReadOnly(int column) throws SQLException 
    {
        return false;
    }

    public boolean isWritable(int column) throws SQLException 
    {
        return false;
    }

    public boolean isDefinitelyWritable(int column) throws SQLException 
    {
        return false;
    }

    public String getColumnClassName(int column) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

}
