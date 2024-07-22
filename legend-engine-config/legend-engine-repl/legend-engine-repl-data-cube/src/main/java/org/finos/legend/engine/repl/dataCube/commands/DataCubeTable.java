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

package org.finos.legend.engine.repl.dataCube.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_SIGNATURE;

public class DataCubeTable implements Command
{
    private final DataCube parentCommand;
    private final Client client;
    private final REPLServer replServer;

    public DataCubeTable(DataCube parentCommand, Client client, REPLServer replServer)
    {
        this.parentCommand = parentCommand;
        this.client = client;
        this.replServer = replServer;
    }

    @Override
    public String documentation()
    {
        return "datacube table <table name>";
    }

    @Override
    public String description()
    {
        return "launch DataCube with the specified table";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube table"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 3)
            {
                throw new RuntimeException("Error: command should be used as '" + this.documentation() + "'");
            }

            String tableName = tokens[2];
            String functionBodyCode = "#>{" + DataCube.getLocalDatabasePath() + "." + tableName + "}#->sort([])->from(" + DataCube.getLocalRuntimePath() + ")";
            String functionCode = "###Pure\n" +
                    "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + functionBodyCode + ";\n}";
            PureModelContextData pureModelContextData = client.getModelState().parseWithTransient(functionCode);
            this.replServer.initializeStateFromTable(pureModelContextData);
            Show.launchDataCube(client, replServer);
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
