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

package org.finos.legend.engine.repl.dataCube;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.dataCube.commands.*;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;

public class DataCubeReplExtension implements ReplExtension
{
    private Client client;
    public REPLServer replServer;

    @Override
    public String type()
    {
        return "relational";
    }

    public void initialize(Client client)
    {
        this.client = client;

        try
        {
            this.replServer = new REPLServer(this.client);
            this.replServer.initialize();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        DataCube dataCubeCommand = new DataCube(this.client);
        return Lists.mutable.with(
                new Show(this.client, this.replServer),
                new DataCubeCache(dataCubeCommand, this.client, this.replServer),
                new DataCubeTable(dataCubeCommand, this.client, this.replServer),
                new DataCubeLoadCSV(dataCubeCommand, this.client, this.replServer),
                new DataCubeRun(dataCubeCommand, this.client, this.replServer),
                new DataCubeWalkthrough(dataCubeCommand, this.client, this.replServer),
                new DataCube__DEV__runDuckDBSelectSQL(dataCubeCommand, this.client),
                new DataCube__DEV__runDuckDBUpdateSQL(dataCubeCommand, this.client),
                dataCubeCommand // NOTE: this has to be the last datacube command to ensure autocomplete works properly
        );
    }

    @Override
    public boolean supports(Result res)
    {
        return false;
    }

    @Override
    public String print(Result res)
    {
        return null;
    }

    @Override
    public MutableList<String> generateDynamicContent(String code)
    {
        return Lists.mutable.empty();
    }
}
