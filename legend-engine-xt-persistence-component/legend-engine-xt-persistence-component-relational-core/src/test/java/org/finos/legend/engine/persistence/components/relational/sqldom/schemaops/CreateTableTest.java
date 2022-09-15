// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.UniqueColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.PrimaryKeyTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.IfNotExistsTableModifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.BigInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Char;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Decimal;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Double;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Real;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.SmallInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TinyInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.CreateTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateTableTest
{

    @Test
    void testCreateTable()
    {
        ColumnConstraint notNull = new NotNullColumnConstraint();
        ColumnConstraint nullable = new NullColumnConstraint();
        ColumnConstraint unique = new UniqueColumnConstraint();
        PrimaryKeyTableConstraint pkConstraint = new PrimaryKeyTableConstraint(Arrays.asList("column_varchar", "column_timestamp"), BaseTest.QUOTE_IDENTIFIER);

        Column c1 = new Column("convolve_digest", new VarChar(32), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c2 = new Column("column_tinyint", new TinyInt(), Arrays.asList(nullable), BaseTest.QUOTE_IDENTIFIER);
        Column c3 = new Column("column_smallint", new SmallInt(), Arrays.asList(nullable), BaseTest.QUOTE_IDENTIFIER);
        Column c4 = new Column("column_int", new Integer(), Arrays.asList(notNull, unique), BaseTest.QUOTE_IDENTIFIER);
        Column c5 = new Column("column_bigint", new BigInt(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c6 = new Column("column_varchar", new VarChar(8), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c7 = new Column("column_char", new Char(8), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c8 = new Column("column_timestamp", new Timestamp(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c9 = new Column("column_date", new Date(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c10 = new Column("column_float", new Real(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c11 = new Column("column_real", new Real(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c12 = new Column("column_decimal", new Decimal(27, 9), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);
        Column c13 = new Column("column_double", new Double(), Arrays.asList(notNull), BaseTest.QUOTE_IDENTIFIER);

        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        CreateTable create = new CreateTable(
            table,
            Collections.singletonList(new IfNotExistsTableModifier()),
            Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13),
            Collections.singletonList(pkConstraint),
            Collections.emptyList());

        String sql = BaseTest.genSqlIgnoringErrors(create);

        String expected = "CREATE TABLE IF NOT EXISTS \"mydb\".\"mytable\"" +
            "(\"convolve_digest\" VARCHAR(32) NOT NULL," +
            "\"column_tinyint\" TINYINT NULL," +
            "\"column_smallint\" SMALLINT NULL," +
            "\"column_int\" INTEGER NOT NULL UNIQUE," +
            "\"column_bigint\" BIGINT NOT NULL," +
            "\"column_varchar\" VARCHAR(8) NOT NULL," +
            "\"column_char\" CHAR(8) NOT NULL," +
            "\"column_timestamp\" TIMESTAMP NOT NULL," +
            "\"column_date\" DATE NOT NULL," +
            "\"column_float\" REAL NOT NULL," +
            "\"column_real\" REAL NOT NULL," +
            "\"column_decimal\" DECIMAL(27,9) NOT NULL," +
            "\"column_double\" DOUBLE NOT NULL," +
            "PRIMARY KEY (\"column_varchar\", \"column_timestamp\"))";

        assertEquals(expected, sql);
    }

    @Test
    void testCreateTableMissingTable()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        CreateTable create = new CreateTable(
            null,
            Collections.singletonList(new IfNotExistsTableModifier()),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        try
        {
            BaseTest.genSql(create);
        }
        catch (Exception e)
        {
            assertEquals("Table is mandatory for Create Table Command", e.getMessage());
        }
    }

    @Test
    void testCreateTableMissingColumns()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        CreateTable create = new CreateTable(
            table,
            Collections.singletonList(new IfNotExistsTableModifier()),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        try
        {
            BaseTest.genSql(create);
        }
        catch (Exception e)
        {
            assertEquals("Columns list is mandatory for Create Table Command", e.getMessage());
        }
    }
}
