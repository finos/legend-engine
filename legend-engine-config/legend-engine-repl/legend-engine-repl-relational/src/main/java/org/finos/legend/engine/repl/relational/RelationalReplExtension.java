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
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.ModelState;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.relational.commands.DB;
import org.finos.legend.engine.repl.relational.commands.Load;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;

import java.sql.Connection;
import java.sql.SQLException;

import static org.finos.legend.engine.repl.relational.grid.Grid.prettyGridPrint;
import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class RelationalReplExtension implements ReplExtension
{
    private Client client;

    private MutableList<String> connectionsForDynamicDBs = Lists.mutable.empty();

    static
    {
        System.setProperty("legend.test.h2.port", "1975");
    }

    @Override
    public String type()
    {
        return "relational";
    }

    public void setClient(Client client)
    {
        this.client = client;

        addLocalConnection(this.client.getModelState());
    }

    private void addLocalConnection(ModelState modelState)
    {
        modelState.addElement(
                "###Connection\n" +
                        "RelationalDatabaseConnection local::DuckDBConnection\n" +
                        "{\n" +
                        "   specification: DuckDB{path:'~/duck';};" +
                        "   type: DuckDB;" +
                        "   auth: Test;" +
                        "}\n");

        connectionsForDynamicDBs.add("local::DuckDBConnection");
    }

    @Override
    public MutableList<String> generateDynamicContent(String code)
    {
        PureModelContextData pureModelContextData = client.getLegendInterface().parse(code);
        return this.connectionsForDynamicDBs.flatCollect(conn ->
        {
            DatabaseConnection db = ConnectionHelper.getDatabaseConnection(pureModelContextData, conn);
            MutableList<String> res = Lists.mutable.empty();

            try (Connection connection = ConnectionHelper.getConnection(db, client.getPlanExecutor()))
            {
                res.add("###Relational\n" +
                        "Database local::TestDatabase" +
                        "(" +
                        getTables(connection).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
                        ")\n");
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }

            res.add("###Runtime\n" +
                    "Runtime local::DuckDBRuntime\n" +
                    "{\n" +
                    "   mappings : [];" +
                    "   connections:\n" +
                    "   [\n" +
                    "       local::TestDatabase : [connection: local::DuckDBConnection]\n" +
                    "   ];\n" +
                    "}\n");
            return res;
        });

    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        return Lists.mutable.with(new DB(this.client, this), new Load(this.client, this));
    }

//    @Override
//    public MutableList<String> getExtraState()
//    {
//        MutableList<String> res = Lists.mutable.empty();
//
//        res.add("###Relational\n" +
//                "Database local::TestDatabase" +
//                "(" +
//                getTables(getConnection(null)).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
//                ")\n");
//
//        res.add("###Connection\n" +
//                "RelationalDatabaseConnection local::H2Connection\n" +
//                "{\n" +
//                //"   store: test::TestDatabase;" +
//                "   specification: LocalH2{};" +
//                "   type: H2;" +
//                "   auth: DefaultH2;" +
//                "}\n");
//
//        res.add("###Runtime\n" +
//                "Runtime local::H2Runtime\n" +
//                "{\n" +
//                "   mappings : [];" +
//                "   connections:\n" +
//                "   [\n" +
//                "       local::TestDatabase : [connection: local::H2Connection]\n" +
//                "   ];\n" +
//                "}\n");
//
//        res.add("###Relational\n" +
//                "Database local::TestDatabase" +
//                "(" +
//                getTables(getConnection(null)).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
//                ")\n");
//
//        res.add("###Connection\n" +
//                "RelationalDatabaseConnection local::DuckDBConnection\n" +
//                "{\n" +
//                //"   store: test::TestDatabase;" +
//                "   specification: DuckDB{path:'~/duck';};" +
//                "   type: DuckDB;" +
//                "   auth: Test;" +
//                "}\n");
//
//        res.add("###Runtime\n" +
//                "Runtime local::DuckDBRuntime\n" +
//                "{\n" +
//                "   mappings : [];" +
//                "   connections:\n" +
//                "   [\n" +
//                "       local::TestDatabase : [connection: local::DuckDBConnection]\n" +
//                "   ];\n" +
//                "}\n");
//
//        return res;
//    }


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
