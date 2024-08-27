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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.relational.commands.Cache;
import org.finos.legend.engine.repl.relational.commands.DB;
import org.finos.legend.engine.repl.relational.commands.Drop;
import org.finos.legend.engine.repl.relational.commands.Load;
import org.finos.legend.engine.repl.relational.local.LocalConnectionManagement;
import org.finos.legend.engine.repl.relational.local.LocalConnectionType;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.finos.legend.engine.repl.relational.shared.ResultHelper.prettyGridPrint;

public class RelationalReplExtension implements ReplExtension
{
    public static String DUCKDB_LOCAL_CONNECTION_BASE_NAME = "DuckDuck";

    static
    {
        try
        {
            int port = AlloyH2Server.startServer(0).getPort();
            System.setProperty("legend.test.h2.port", String.valueOf(port));
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Client client;
    private int maxRowSize = 40;
    private LocalConnectionManagement localConnectionManagement;

    @Override
    public String type()
    {
        return "relational";
    }

    @Override
    public void initialize(Client client)
    {
        this.client = client;
        this.localConnectionManagement = new LocalConnectionManagement(client);
        // this.localConnectionManagement.addLocalConnection(LocalConnectionType.H2, "MyTestH2");
        this.localConnectionManagement.addLocalConnection(LocalConnectionType.DuckDB, DUCKDB_LOCAL_CONNECTION_BASE_NAME);
    }

    @Override
    public MutableList<String> generateDynamicContent(String code)
    {
        return localConnectionManagement.generateDynamicContent(code);
    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        return Lists.mutable.with(
                new DB(this.client),
                new Load(this.client),
                new Drop(this.client),
                new Cache(this.client)
        );
    }

    @Override
    public boolean supports(Result res)
    {
        return res instanceof RelationalResult;
    }

    @Override
    public String print(Result res)
    {
        RelationalResult relationalResult = (RelationalResult) res;
        try (ResultSet rs = relationalResult.resultSet)
        {
            return prettyGridPrint(rs, relationalResult.sqlColumns, ListIterate.collect(relationalResult.getSQLResultColumns(), col -> col.dataType), maxRowSize, 60);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getMaxRowSize()
    {
        return maxRowSize;
    }

    public void setMaxRowSize(int maxRowSize)
    {
        this.maxRowSize = maxRowSize;
    }
}
