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
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.relational.commands.DB;
import org.finos.legend.engine.repl.relational.commands.Load;
import org.finos.legend.engine.repl.relational.local.LocalConnectionManagement;
import org.finos.legend.engine.repl.relational.local.LocalConnectionType;

import org.finos.legend.engine.repl.relational.commands.Show;
import org.finos.legend.engine.repl.relational.httpServer.ReplGridServer;

import java.awt.*;
import java.sql.SQLException;

import static org.finos.legend.engine.repl.relational.grid.Grid.prettyGridPrint;

public class RelationalReplExtension implements ReplExtension
{
    private Client client;
    public ReplGridServer replGridServer;

    private LocalConnectionManagement localConnectionManagement;

    static
    {
        int port = 1024 + (int)(Math.random() * 10000);
        System.setProperty("legend.test.h2.port", String.valueOf(port));
        try
        {
            AlloyH2Server.startServer(port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String type()
    {
        return "relational";
    }

    private boolean canShowGrid()
    {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    public void initialize(Client client)
    {
        this.client = client;
        this.localConnectionManagement = new LocalConnectionManagement(client);
        this.localConnectionManagement.addLocalConnection(LocalConnectionType.H2, "MyTestH2");
        this.localConnectionManagement.addLocalConnection(LocalConnectionType.DuckDB, "DuckDuck");

        try
        {
            this.replGridServer = new ReplGridServer(this.client);
            this.replGridServer.initializeServer();
        }
        catch (Exception e)
        {
           throw new RuntimeException(e);
        }
    }

    @Override
    public MutableList<String> generateDynamicContent(String code)
    {
        return localConnectionManagement.generateDynamicContent(code);
    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        MutableList<Command> extraCommands = Lists.mutable.with(new DB(this.client, this), new Load(this.client, this));
        extraCommands.add(new Show(this.client, this.replGridServer));
        return extraCommands;
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
