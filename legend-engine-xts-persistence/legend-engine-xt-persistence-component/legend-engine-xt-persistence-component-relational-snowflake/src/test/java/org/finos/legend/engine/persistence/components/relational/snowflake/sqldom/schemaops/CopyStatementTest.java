// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.expressions.table.StagedFilesTable;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.CopyStatement;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.StagedFilesField;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopyStatementTest
{
    public static String QUOTE_IDENTIFIER = "\"%s\"";

    @Test
    void testSelectStageStatement() throws SqlDomException
    {
        StagedFilesTable stagedFiles = new StagedFilesTable("t","@my_stage");
        List<Value> selectItems = Arrays.asList(
                new StagedFilesField(QUOTE_IDENTIFIER, 1, "t", "field1"),
                new StagedFilesField(QUOTE_IDENTIFIER, 2, "t", "field2"),
                new StagedFilesField(QUOTE_IDENTIFIER, 3, "t", "field3"),
                new StagedFilesField(QUOTE_IDENTIFIER, 4, "t", "field4")
        );
        SelectStatement selectStatement = new SelectStatement(null, selectItems, Arrays.asList(stagedFiles), null, null);

        Table table = new Table("mydb", null, "mytable1", "sink", QUOTE_IDENTIFIER);
        List<Field> columns = Arrays.asList(
                new Field("field1", QUOTE_IDENTIFIER),
                new Field("field2", QUOTE_IDENTIFIER),
                new Field("field3", QUOTE_IDENTIFIER),
                new Field("field4", QUOTE_IDENTIFIER)
        );

        CopyStatement copyStatement = new CopyStatement(table, columns, selectStatement);
        String sql1 = genSqlIgnoringErrors(copyStatement);
        assertEquals("COPY INTO \"mydb\".\"mytable1\" " +
                "(\"field1\", \"field2\", \"field3\", \"field4\") " +
                "FROM " +
                "(SELECT t.$1 as \"field1\",t.$2 as \"field2\",t.$3 as \"field3\",t.$4 as \"field4\" FROM @my_stage as t) " +
                "on_error = 'ABORT_STATEMENT'", sql1);
    }

    @Test
    void testSelectStageStatementWithPatternAndFileFormat() throws SqlDomException
    {
        StagedFilesTable stagedFiles = new StagedFilesTable("t","@my_stage");
        stagedFiles.setFileFormat("my_file_format");
        stagedFiles.setFilePattern("my_pattern");

        List<Value> selectItems = Arrays.asList(
                new StagedFilesField(QUOTE_IDENTIFIER, 1, "t", "field1", "field1"),
                new StagedFilesField(QUOTE_IDENTIFIER, 1, "t", "field2", "field2"),
                new StagedFilesField(QUOTE_IDENTIFIER, 1, "t", "field3", "field3"),
                new StagedFilesField(QUOTE_IDENTIFIER, 1, "t", "field4","field4")
        );

        SelectStatement selectStatement = new SelectStatement(null, selectItems, Arrays.asList(stagedFiles), null, null);

        Table table = new Table("mydb", null, "mytable1", "sink", QUOTE_IDENTIFIER);
        List<Field> columns = Arrays.asList(
                new Field("field1", QUOTE_IDENTIFIER),
                new Field("field2", QUOTE_IDENTIFIER),
                new Field("field3", QUOTE_IDENTIFIER),
                new Field("field4", QUOTE_IDENTIFIER)
        );

        CopyStatement copyStatement = new CopyStatement(table, columns, selectStatement);
        String sql1 = genSqlIgnoringErrors(copyStatement);
        assertEquals("COPY INTO \"mydb\".\"mytable1\" " +
                "(\"field1\", \"field2\", \"field3\", \"field4\") " +
                "FROM " +
                "(SELECT t.$1:field1 as \"field1\",t.$1:field2 as \"field2\",t.$1:field3 as \"field3\",t.$1:field4 as \"field4\" " +
                "FROM @my_stage (FILE_FORMAT => 'my_file_format', PATTERN => 'my_pattern') as t) " +
                "on_error = 'ABORT_STATEMENT'", sql1);
    }

    public static String genSqlIgnoringErrors(SqlGen item)
    {
        StringBuilder builder = new StringBuilder();
        try
        {
            item.genSql(builder);
        }
        catch (SqlDomException e)
        {
            // Ignore
        }
        return builder.toString();
    }
}
