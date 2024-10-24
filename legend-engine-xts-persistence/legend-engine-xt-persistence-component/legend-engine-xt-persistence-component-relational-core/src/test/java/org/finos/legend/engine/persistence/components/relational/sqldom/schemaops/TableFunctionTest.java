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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops;

import java.util.ArrayList;
import java.util.Collections;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableFunction;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TableFunctionTest
{
    @Test
    public void testTableFunctionWithoutSchemaAndParams()
    {
        TableFunction tableFunction = new TableFunction(
                null,
                null,
                "STATS",
                new ArrayList<>(),
                BaseTest.QUOTE_IDENTIFIER
        );
        String sql = BaseTest.genSql(tableFunction);
        assertEquals("TABLE(STATS())", sql);
    }

    @Test
    public void testTableFunctionWithParams()
    {
        TableFunction tableFunction = new TableFunction(
                null,
                null,
                "STATS",
                Collections.singletonList(new Field(null, "id", BaseTest.QUOTE_IDENTIFIER, null)),
                BaseTest.QUOTE_IDENTIFIER
        );
        String sql = BaseTest.genSql(tableFunction);
        assertEquals("TABLE(STATS(\"id\"))", sql);
    }

    @Test
    public void testTableFunctionWithSchema()
    {
        TableFunction tableFunction = new TableFunction(
                null,
                "information_schema",
                "STATS",
                null,
                BaseTest.QUOTE_IDENTIFIER
        );
        String sql = BaseTest.genSql(tableFunction);
        assertEquals("\"information_schema\".TABLE(STATS())", sql);
    }

    @Test
    public void testTableFunctionWithDbAndSchema()
    {
        TableFunction tableFunction = new TableFunction(
                "prod",
                "information_schema",
                "STATS",
                null,
                BaseTest.QUOTE_IDENTIFIER
        );
        String sql = BaseTest.genSql(tableFunction);
        assertEquals("\"prod\".\"information_schema\".TABLE(STATS())", sql);
    }
}