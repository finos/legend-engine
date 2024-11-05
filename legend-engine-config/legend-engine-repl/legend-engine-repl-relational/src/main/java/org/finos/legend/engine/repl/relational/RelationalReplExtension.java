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
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
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

import static org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResultGridPrintUtility.prettyGridPrint;

public class RelationalReplExtension implements ReplExtension
{
    public static String DUCKDB_LOCAL_CONNECTION_BASE_NAME = "DuckDuck";
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
        return res instanceof RelationalResult || res instanceof RealizedRelationalResult;
    }

    @Override
    public String print(Result res)
    {
        if (res instanceof RelationalResult)
        {
            RelationalResult relationalResult = (RelationalResult) res;
            return prettyGridPrint(relationalResult, maxRowSize, 60);
        }
        else
        {
            RealizedRelationalResult relationalResult = (RealizedRelationalResult) res;
            return prettyGridPrint(relationalResult, maxRowSize, 60);
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
