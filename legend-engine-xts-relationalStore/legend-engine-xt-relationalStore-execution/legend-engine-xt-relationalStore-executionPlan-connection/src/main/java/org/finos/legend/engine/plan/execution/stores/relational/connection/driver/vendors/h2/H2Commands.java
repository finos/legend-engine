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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;
import org.finos.legend.engine.shared.core.util.ResourceHelpers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class H2Commands extends RelationalDatabaseCommands
{

    @Override
    public String createTempTable(String tableName, List<Column> columns)
    {
        return "CREATE LOCAL TEMPORARY TABLE " + tableName + "(" + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(", ")) + ");";
    }

    @Override
    public String dropTempTable(String tableName)
    {
        return "Drop table if exists " + tableName;
    }

    @Override
    public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String optionalCSVFileLocation)
    {
        return Lists.mutable.with("CREATE LOCAL TEMPORARY TABLE " + tableName + "(" + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(", ")) + ") AS SELECT * FROM CSVREAD('" + optionalCSVFileLocation + "');");
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        return IngestionMethod.CLIENT_FILE;
    }

    @Override
    public String load(String tableName, String location)
    {
        return "CREATE TABLE " + tableName + " AS SELECT * FROM CSVREAD('" + location + "');";
    }

    @Override
    public String load(String tableName, String location, List<Column> columns)
    {
        return "CREATE TABLE " + tableName + "(" + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(", ")) + ") AS SELECT * FROM CSVREAD('" + location + "');";
    }

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public String createNorthwindDataLoaderStoredProc()
    {
        // Load the templates from resources
        String northwindSqlRaw = ResourceHelpers.getResourceAsString("/org/finos/legend/engine/plan/execution/stores/relational/connection/driver/vendors/h2/h2NorthwindDdl.sql");
        String northwindProcRaw = ResourceHelpers.getResourceAsString("/org/finos/legend/engine/plan/execution/stores/relational/connection/driver/vendors/h2/h2NorthwindProc.txt");

        // split/translate the raw SQL string to java string builder operations that build the string
        // 1. it uses the string builder because it's too long as a string constant / static string
        // 2. We clean up unnecessary line breaks as otherwise the Java file is too big for the compiler
        String northwindSql = String.join(";\n", Arrays.stream(northwindSqlRaw.split(";")).map(x -> x.replace("\n", " ").replace("\r", "")).collect(Collectors.toList()));
        String strEol = "\\n\");\n    sb.append(\"";
        String northwindSqlString = "\"" + strEol + String.join(strEol, northwindSql.replace("\"", "\\\"").split("(\r)?\n")) + "\"";

        // Merge the two to create the finalised SQL that needs to be executed
        String sql = northwindProcRaw.replace("{$NORTHWIND_SQL}", northwindSqlString);
        return sql;
    }

}
