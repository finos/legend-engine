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

package org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.expressions.table.FileRead;
import org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.statements.CopyStatement;
import org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.values.StagedFilesField;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DateTime;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopyStatementTest
{
    public static String QUOTE_IDENTIFIER = "\"%s\"";

    @Test
    void testCopyStatementWithCSVFilesWithOptions() throws SqlDomException
    {
        Map<String, Object> options = new HashMap<>();
        options.put("COMPRESSION", "AUTO");
        options.put("HEADER", true);
        options.put("SKIP", 5);

        FileRead fileRead = new FileRead(FileRead.FileType.CSV,
            Arrays.asList("path1", "path2"),
            Arrays.asList("column1", "column2", "column3"),
            Arrays.asList(new Integer(), new VarChar(), new DateTime()),
            options);

        List<Value> selectItems = Arrays.asList(
                new StagedFilesField(QUOTE_IDENTIFIER, "column1"),
                new StagedFilesField(QUOTE_IDENTIFIER, "column2"),
                new StagedFilesField(QUOTE_IDENTIFIER, "column3")
        );
        SelectStatement selectStatement = new SelectStatement(null, selectItems, Arrays.asList(fileRead), null);

        Table table = new Table("mydb", null, "mytable1", "sink", QUOTE_IDENTIFIER);
        List<Field> columns = Arrays.asList(
                new Field("column1", QUOTE_IDENTIFIER),
                new Field("column2", QUOTE_IDENTIFIER),
                new Field("column3", QUOTE_IDENTIFIER));

        CopyStatement copyStatement = new CopyStatement(table, columns, selectStatement);

        StringBuilder builder = new StringBuilder();
        copyStatement.genSql(builder);
        String sql1 = builder.toString();
        assertEquals("INSERT INTO \"mydb\".\"mytable1\" " +
            "(\"column1\", \"column2\", \"column3\") " +
            "SELECT \"column1\",\"column2\",\"column3\" " +
            "FROM READ_CSV(['path1','path2'], " +
            "COLUMNS = {'column1':'INTEGER', 'column2':'VARCHAR', 'column3':'DATETIME'}, " +
            "AUTO_DETECT = FALSE, COMPRESSION='AUTO', HEADER=true, SKIP=5)", sql1);
    }

    @Test
    void testCopyStatementWithParquetFiles() throws SqlDomException
    {
        FileRead fileRead = new FileRead(FileRead.FileType.PARQUET,
            Arrays.asList("path1"),
            Arrays.asList("column1", "column2", "column3"),
            Arrays.asList(new Integer(), new VarChar(), new DateTime()),
            null);

        List<Value> selectItems = Arrays.asList(
            new StagedFilesField(QUOTE_IDENTIFIER, "column1"),
            new StagedFilesField(QUOTE_IDENTIFIER, "column2"),
            new StagedFilesField(QUOTE_IDENTIFIER, "column3")
        );
        SelectStatement selectStatement = new SelectStatement(null, selectItems, Arrays.asList(fileRead), null);

        Table table = new Table("mydb", null, "mytable1", "sink", QUOTE_IDENTIFIER);
        List<Field> columns = Arrays.asList(
            new Field("column1", QUOTE_IDENTIFIER),
            new Field("column2", QUOTE_IDENTIFIER),
            new Field("column3", QUOTE_IDENTIFIER));

        CopyStatement copyStatement = new CopyStatement(table, columns, selectStatement);

        StringBuilder builder = new StringBuilder();
        copyStatement.genSql(builder);
        String sql1 = builder.toString();
        assertEquals("INSERT INTO \"mydb\".\"mytable1\" " +
            "(\"column1\", \"column2\", \"column3\") " +
            "SELECT \"column1\",\"column2\",\"column3\" " +
            "FROM READ_PARQUET(['path1'])", sql1);
    }
}