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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SnowflakeCommands extends RelationalDatabaseCommands
{
    @Override
    public String processTempTableName(String tempTableName)
    {
        return "LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA." + tempTableName;
    }

    public String tempStageName()
    {
        return "LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE";
    }

    @Override
    public String dropTempTable(String tableName)
    {
        return "Drop table if exists " + tableName;
    }

    @Override
    public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String optionalCSVFileLocation)
    {
        List<String> strings = Arrays.asList(
                "CREATE TEMPORARY TABLE " + tableName + " " + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(",", "(", ")")),
                "CREATE OR REPLACE TEMPORARY STAGE " + tempStageName(),
                "PUT file://" + optionalCSVFileLocation + " @" + tempStageName() + "/" + optionalCSVFileLocation + " PARALLEL = 16 AUTO_COMPRESS = TRUE",
                "COPY INTO " + tableName + " FROM @" + tempStageName() + " file_format = (type = CSV field_optionally_enclosed_by= '\"')",
                "DROP STAGE " + tempStageName()
        );
        return strings;
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        return IngestionMethod.CLIENT_FILE;
    }

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

}
