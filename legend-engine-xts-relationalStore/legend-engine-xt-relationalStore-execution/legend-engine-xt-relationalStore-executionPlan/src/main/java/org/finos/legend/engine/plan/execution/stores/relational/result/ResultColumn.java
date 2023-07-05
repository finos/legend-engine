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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;
import java.util.function.BiFunction;

public class ResultColumn
{
    private int columnIndex;
    private String label;
    private String dataType;
    private int dbMetaDataType;

    private BiFunction<ResultSet, Calendar, Object> valueExtractor;
    private BiFunction<ResultSet, Calendar, Object> transformedValueExtractor;

    ResultColumn(int columnIndex, String label, String dataType, int dbMetaDataType)
    {
        this.columnIndex = columnIndex;
        this.label = label;
        this.dataType = dataType;
        this.dbMetaDataType = dbMetaDataType;

        this.createValueExtractors();
    }

    private void createValueExtractors()
    {
        /* Value Extractor */
        if ((this.dbMetaDataType == Types.DATE) || "DATE".equalsIgnoreCase(this.dataType))
        {
            this.valueExtractor = BiFunctionHelper.unchecked(
                    (resultSet, calendar) -> resultSet.getDate(this.columnIndex)
            );
        }
        else if ((this.dbMetaDataType == Types.TIMESTAMP) || "TIMESTAMP".equalsIgnoreCase(this.dataType))
        {
            this.valueExtractor = BiFunctionHelper.unchecked(
                    (resultSet, calendar) -> resultSet.getTimestamp(this.columnIndex, calendar)
            );
        }
        else
        {
            this.valueExtractor = BiFunctionHelper.unchecked(
                    (resultSet, calendar) -> resultSet.getObject(this.columnIndex)
            );
        }

        /* Transformed Value Extractor */
        switch (this.dbMetaDataType)
        {
            case Types.DATE:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            java.sql.Date date = resultSet.getDate(this.columnIndex);
                            return date != null ? PureDate.fromSQLDate(date) : null;
                        }
                );
                break;
            }
            case Types.TIMESTAMP:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            java.sql.Timestamp timestamp = resultSet.getTimestamp(this.columnIndex, calendar);
                            return timestamp != null ? PureDate.fromSQLTimestamp(timestamp) : null;
                        }
                );
                break;
            }
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            long num = resultSet.getLong(this.columnIndex);
                            return !resultSet.wasNull() ? num : null;
                        }
                );
                break;
            }
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            double num = resultSet.getDouble(this.columnIndex);
                            return !resultSet.wasNull() ? num : null;
                        }
                );
                break;
            }
            case Types.DECIMAL:
            case Types.NUMERIC:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) -> resultSet.getBigDecimal(this.columnIndex)
                );
                break;
            }
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.OTHER:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) -> resultSet.getString(this.columnIndex)
                );
                break;
            }
            case Types.BIT:
            case Types.BOOLEAN:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            boolean bool = resultSet.getBoolean(this.columnIndex);
                            return !resultSet.wasNull() ? bool : null;
                        }
                );
                break;
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) ->
                        {
                            byte[] bytes = resultSet.getBytes(this.columnIndex);
                            return bytes != null ? BinaryUtils.encodeHex(bytes) : null;
                        }
                );
                break;
            }
            case Types.NULL:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) -> null
                );
                break;
            }
            default:
            {
                this.transformedValueExtractor = BiFunctionHelper.unchecked(
                        (resultSet, calendar) -> resultSet.getObject(this.columnIndex)
                );
            }
        }
    }

    public String getLabel()
    {
        return this.label;
    }

    public String getNonQuotedLabel()
    {
        return this.label.startsWith("\"") && this.label.endsWith("\"") ?
                this.label.substring(1, this.label.length() - 1) :
                this.label;
    }

    public String getDataType()
    {
        return this.dataType;
    }

    public int getDbMetaDataType()
    {
        return this.dbMetaDataType;
    }

    public Object getValue(ResultSet resultSet, Calendar calendar)
    {
        return this.valueExtractor.apply(resultSet, calendar);
    }

    public Object getTransformedValue(ResultSet resultSet, Calendar calendar)
    {
        return this.transformedValueExtractor.apply(resultSet, calendar);
    }

    @JsonIgnore
    public Pair<String, String> labelTypePair()
    {
        if (this.dataType == null)
        {
            return Tuples.pair(this.label, "String"); //TODO: This should not be null. Change after all relational types are available
        }
        String type = this.dataType.toUpperCase();
        if (type.startsWith("VARCHAR") || type.startsWith("CHAR"))
        {
            return Tuples.pair(this.label, "String");
        }
        if (type.startsWith("FLOAT") || type.startsWith("DOUBLE") || type.startsWith("DECIMAL") || type.startsWith("NUMERIC") || type.startsWith("REAL"))
        {
            return Tuples.pair(this.label, "Float");
        }
        if (type.startsWith("INTEGER") || type.startsWith("BIGINT") || type.startsWith("SMALLINT") || type.startsWith("TINYINT"))
        {
            return Tuples.pair(this.label, "Integer");
        }
        if (type.startsWith("BIT"))
        {
            return Tuples.pair(this.label, "Boolean");
        }
        if (type.startsWith("TIMESTAMP"))
        {
            return Tuples.pair(this.label, "DateTime");
        }
        if (type.startsWith("DATE"))
        {
            return Tuples.pair(this.label, "StrictDate");
        }
        return Tuples.pair(this.label, "String"); // Default is String. But shouldn't go here
    }
}
