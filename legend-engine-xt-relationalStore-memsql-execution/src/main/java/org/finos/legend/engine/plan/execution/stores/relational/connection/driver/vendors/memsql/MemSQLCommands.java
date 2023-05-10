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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.memsql;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class MemSQLCommands extends RelationalDatabaseCommands
{
    @Override
    public String dropTempTable(String tableName)
    {
        return "DROP TEMPORARY TABLE IF EXISTS " + tableName;
    }

    @Override
    /*
    Temporary Table
        https://docs.memsql.com/v6.8/reference/sql-reference/data-definition-language-ddl/create-table/
        https://dev.mysql.com/doc/refman/8.0/en/create-temporary-table.html
        https://www.mysqltutorial.org/import-csv-file-mysql-table/

    */
    public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String clientFileName)
    {
        return Lists.mutable.with(
                "CREATE TEMPORARY TABLE " + tableName + " (" + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(", ")) + ")",
                "LOAD DATA LOCAL INFILE '" + clientFileName.replace("\\", "/") + "' \n" +
                        "INTO TABLE `" + tableName + "` \n" +
                        "IGNORE 1 LINES;"
        );
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        return IngestionMethod.CLIENT_FILE;
    }

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return ((RelationalDatabaseCommandsVisitor<T>)visitor).visit(this);
    }
}
