// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.DuckDBDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.DuckDBJdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBigint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBinary;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBoolean;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colChar;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDate;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDatetime;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDecimal;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDecimalDefault;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDouble;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colFloat;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colInt;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colInteger;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colJson;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colReal;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colSmallint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colString;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTime;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTimestamp;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTinyint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colVarchar;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.BOOL_TRUE_STRING_VALUE;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.COLUMN_SIZE;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.DATA_TYPE;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.DECIMAL_DIGITS;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.IS_AUTOINCREMENT;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.IS_NULLABLE;
import static org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper.TYPE_NAME;

public class DuckDBDatatypeTest extends BaseTest
{

    @Test
    protected void testDataMapping() throws SQLException
    {
        duckDBSink.executeStatement("DROP TABLE IF EXISTS \"TEST\".\"data_mapping_test\"");
        duckDBSink.executeStatement("CREATE TABLE \"TEST\".\"data_mapping_test\" (" +
                    "\"bigint_col\" BIGINT, " +
                    "\"int8_col\" INT8, " +
                    "\"long_col\" LONG, " +
                    "\"bit_col\" BIT, " +
                    "\"bitstring_col\" BITSTRING, " +
                    "\"blob_col\" BLOB, " +
                    "\"bytea_col\" BYTEA, " +
                    "\"binary_col\" BINARY, " +
                    "\"varbinary_col\" VARBINARY, " +
                    "\"boolean_col\" BOOLEAN, " +
                    "\"bool_col\" BOOL, " +
                    "\"logical_col\" LOGICAL, " +
                    "\"date_col\" DATE, " +
                    "\"decimal_col\" DECIMAL, " +
                    "\"numeric_col\" NUMERIC, " +
                    "\"decimal_col_1\" DECIMAL(2, 2), " +
                    "\"numeric_col_1\" NUMERIC(2, 2), " +
                    "\"double_col\" DOUBLE, " +
                    "\"float8_col\" FLOAT8, " +
                    "\"integer_col\" INTEGER, " +
                    "\"int4_col\" INT4, " +
                    "\"int_col\" INT, " +
                    "\"signed_col\" SIGNED, " +
                    "\"real_col\" REAL, " +
                    "\"float4_col\" FLOAT4, " +
                    "\"float_col\" FLOAT, " +
                    "\"smallint_col\" SMALLINT, " +
                    "\"int2_col\" INT2, " +
                    "\"short_col\" SHORT, " +
                    "\"time_col\" TIME, " +
                    "\"timestamp_with_zone_col\" TIMESTAMP WITH TIME ZONE, " +
                    "\"timestampz_col\" TIMESTAMPTZ, " +
                    "\"timestamp_col\" TIMESTAMP, " +
                    "\"datetime_col\" DATETIME, " +
                    "\"tinyint_col\" TINYINT, " +
                    "\"int1_col\" INT1, " +
                    "\"varchar_col\" VARCHAR, " +
                    "\"char_col\" CHAR, " +
                    "\"bpchar_col\" BPCHAR, " +
                    "\"json_col\" JSON, " +
                    "\"text_col\" TEXT, " +
                    "\"string_col\" TEXT" +
//                    "\"array_col\" ARRAY, " +
//                    "\"list_col\" LIST, " +
//                    "\"map_col\" MAP, " +
//                    "\"struct_col\" STRUCT, " +
//                    "\"union_col\" UNION" +
                ");");

        DatabaseMetaData dbMetaData = duckDBSink.connection().getMetaData();

        List<Field> fields = new ArrayList<>();
        ResultSet columnResult = dbMetaData.getColumns(TEST_DATABASE, "TEST", "data_mapping_test", null);
        while (columnResult.next())
        {
            String columnName = columnResult.getString(RelationalExecutionHelper.COLUMN_NAME);
            String typeName = columnResult.getString(TYPE_NAME);
            String dataType = JDBCType.valueOf(columnResult.getInt(DATA_TYPE)).getName();
            int columnSize = columnResult.getInt(COLUMN_SIZE);
            int decimalDigits = columnResult.getInt(DECIMAL_DIGITS);
            String isNullable = columnResult.getString(IS_NULLABLE);
            String isIdentity = columnResult.getString(IS_AUTOINCREMENT);

            // Construct type
            DuckDBJdbcPropertiesToLogicalDataTypeMapping mapping = new DuckDBJdbcPropertiesToLogicalDataTypeMapping();
            FieldType fieldType = mapping.getDataType(typeName.toUpperCase(), dataType.toUpperCase(), columnSize, decimalDigits);

            // Get primary keys
            Set<String> primaryKeys = new HashSet<>();
            ResultSet primaryKeyResult = dbMetaData.getPrimaryKeys(TEST_DATABASE, "TEST", "data_mapping_test");
            while (primaryKeyResult.next())
            {
                primaryKeys.add(primaryKeyResult.getString(RelationalExecutionHelper.COLUMN_NAME));
            }

            // Get all unique constraints and indices
            Set<String> uniqueKeys = new HashSet<>();

            // Construct constraints
            boolean nullable = isNullable.equals(BOOL_TRUE_STRING_VALUE);
            boolean identity = isIdentity.equals(BOOL_TRUE_STRING_VALUE);
            boolean primaryKey = primaryKeys.contains(columnName);
            boolean unique = uniqueKeys.contains(columnName);

            Field field = Field.builder().name(columnName).type(fieldType).nullable(nullable).identity(identity).primaryKey(primaryKey).unique(unique).build();

            fields.add(field);
        }

    }

    @Test
    void testDataType() throws SqlDomException
    {
        DuckDBDataTypeMapping mapping = new DuckDBDataTypeMapping();
        Assertions.assertEquals("INTEGER", getGeneratedSql(mapping.getDataType(colInt.type())));
        Assertions.assertEquals("INTEGER", getGeneratedSql(mapping.getDataType(colInteger.type())));
        Assertions.assertEquals("BIGINT", getGeneratedSql(mapping.getDataType(colBigint.type())));
        Assertions.assertEquals("TINYINT", getGeneratedSql(mapping.getDataType(colTinyint.type())));
        Assertions.assertEquals("SMALLINT", getGeneratedSql(mapping.getDataType(colSmallint.type())));
        Assertions.assertEquals("DECIMAL(18,3)", getGeneratedSql(mapping.getDataType(colDecimalDefault.type())));
        Assertions.assertEquals("DECIMAL(10,4)", getGeneratedSql(mapping.getDataType(colDecimal.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(mapping.getDataType(colChar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(mapping.getDataType(colVarchar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(mapping.getDataType(colString.type())));
        Assertions.assertEquals("TIMESTAMP", getGeneratedSql(mapping.getDataType(colTimestamp.type())));
        Assertions.assertEquals("TIMESTAMP", getGeneratedSql(mapping.getDataType(colDatetime.type())));
        Assertions.assertEquals("DATE", getGeneratedSql(mapping.getDataType(colDate.type())));
        Assertions.assertEquals("REAL", getGeneratedSql(mapping.getDataType(colReal.type())));
        Assertions.assertEquals("REAL", getGeneratedSql(mapping.getDataType(colFloat.type())));
        Assertions.assertEquals("DOUBLE", getGeneratedSql(mapping.getDataType(colDouble.type())));
        Assertions.assertEquals("BINARY", getGeneratedSql(mapping.getDataType(colBinary.type())));
        Assertions.assertEquals("TIME", getGeneratedSql(mapping.getDataType(colTime.type())));
        Assertions.assertEquals("BOOLEAN", getGeneratedSql(mapping.getDataType(colBoolean.type())));
        Assertions.assertEquals("JSON", getGeneratedSql(mapping.getDataType(colJson.type())));

    }

    private String getGeneratedSql(SqlGen sqlGen) throws SqlDomException
    {
        StringBuilder builder = new StringBuilder();
        sqlGen.genSql(builder);
        return builder.toString();
    }
}
