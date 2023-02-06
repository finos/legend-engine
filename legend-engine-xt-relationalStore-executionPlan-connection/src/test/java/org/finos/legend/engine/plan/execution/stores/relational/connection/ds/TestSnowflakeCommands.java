//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeCommands;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSnowflakeCommands
{
    @Test
    public void testTempTableCommands() throws IOException
    {
        SnowflakeCommands snowflakeCommands = new SnowflakeCommands();

        ImmutableList<Column> columns = Lists.immutable.of(
                new Column("a", "VARCHAR(100)"),
                new Column("b", "VARCHAR(100)")
        );

        List<String> sqlStatements = snowflakeCommands.createAndLoadTempTable("temp_1", columns.castToList(), "/tmp/temp.csv");
        ImmutableList<String> expectedSQLStatements = Lists.immutable.of(
                "CREATE TEMPORARY TABLE temp_1 (a VARCHAR(100),b VARCHAR(100))",
                "CREATE OR REPLACE TEMPORARY STAGE LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE",
                "PUT file:///tmp/temp.csv @LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE//tmp/temp.csv PARALLEL = 16 AUTO_COMPRESS = TRUE",
                "COPY INTO temp_1 FROM @LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE file_format = (type = CSV field_optionally_enclosed_by= '\"')",
                "DROP STAGE LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE"
        );
        assertEquals(expectedSQLStatements, sqlStatements);
    }
}
