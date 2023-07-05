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

import org.finos.legend.engine.persistence.components.relational.sqldom.common.ShowType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.ShowCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowCommandTest
{

    @Test
    public void testShowCommand()
    {
        Table table = new Table("CITIBIKE", "public", "trips", null, BaseTest.QUOTE_IDENTIFIER);
        ShowCommand command = new ShowCommand(ShowType.TABLES, table.getSchema());
        String sql = BaseTest.genSqlIgnoringErrors(command);
        String expected = "SHOW TABLES FROM public";
        assertEquals(expected, sql);
    }

    @Test
    public void testShowCommandWithoutSchema()
    {
        Table table = new Table("CITIBIKE", null, "trips", null, BaseTest.QUOTE_IDENTIFIER);
        ShowCommand command = new ShowCommand(ShowType.TABLES);
        String sql = BaseTest.genSqlIgnoringErrors(command);
        String expected = "SHOW TABLES";
        assertEquals(expected, sql);
    }
}
