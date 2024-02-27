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

package org.finos.legend.engine.repl.relational;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.relational.commands.DB;
import org.finos.legend.engine.repl.relational.commands.Load;

import java.sql.Connection;

import static org.finos.legend.engine.repl.relational.grid.Grid.prettyGridPrint;
import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class RelationalReplExtension implements ReplExtension
{
    private final Client client;

    static
    {
        System.setProperty("legend.test.h2.port", "1975");
    }

    @Override
    public String type()
    {
        return "relational";
    }

    public RelationalReplExtension(Client client)
    {
        this.client = client;
    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        return Lists.mutable.with(new DB(this.client, this), new Load(this.client, this));
    }

    @Override
    public MutableList<String> getExtraState()
    {
        MutableList<String> res = Lists.mutable.empty();

        res.add("###Relational\n" +
                "Database test::TestDatabase" +
                "(" +
                getTables(getConnection()).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
                ")\n");

        res.add("###Connection\n" +
                "RelationalDatabaseConnection test::TestConnection\n" +
                "{\n" +
                "   store: test::TestDatabase;" +
                "   specification: LocalH2{};" +
                "   type: H2;" +
                "   auth: DefaultH2;" +
                "}\n");

        res.add("###Runtime\n" +
                "Runtime test::TestRuntime\n" +
                "{\n" +
                "   mappings : [];" +
                "   connections:\n" +
                "   [\n" +
                "       test::TestDatabase : [connection: test::TestConnection]\n" +
                "   ];\n" +
                "}\n");

        return res;
    }

    public Connection getConnection()
    {
        RelationalStoreExecutor r = (RelationalStoreExecutor) ((Execute) this.client.commands.getLast()).getPlanExecutor().getExecutorsOfType(StoreType.Relational).getFirst();
        return r.getStoreState().getRelationalExecutor().getConnectionManager().getTestDatabaseConnection();
    }

    @Override
    public boolean supports(Result res)
    {
        return res instanceof RelationalResult;
    }

    @Override
    public String print(Result res)
    {
        return prettyGridPrint((RelationalResult) res, 60);
//            Serializer s = new RelationalResultToCSVSerializer((RelationalResult) res);
//            return s.flush().toString();
    }
}
