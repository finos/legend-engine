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
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_SIGNATURE;
import static org.finos.legend.engine.repl.relational.RelationalReplExtension.getCachedSerializedResultPath;
import static org.jline.jansi.Ansi.ansi;

public class DataCubeCache implements Command
{
    private final DataCube parentCommand;
    private final Client client;
    private final REPLServer replServer;

    public DataCubeCache(DataCube parentCommand, Client client, REPLServer replServer)
    {
        this.parentCommand = parentCommand;
        this.client = client;
        this.replServer = replServer;
    }

    @Override
    public String documentation()
    {
        return "datacube cache <table name>";
    }

    @Override
    public String description()
    {
        return "cache the result for the last executed query and launch DataCube";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube cache"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 3)
            {
                throw new RuntimeException("Error: command should be used as '" + this.documentation() + "'");
            }

            Execute.ExecuteResultSummary lastExecuteResultSummary = this.client.getExecuteCommand().getLastExecuteResultSummary();
            if (lastExecuteResultSummary == null)
            {
                this.client.getTerminal().writer().println("Can't retrieve result for the last executed query. Try to run a query in REPL first...");
            }
            else
            {
                if (lastExecuteResultSummary.result instanceof RelationalResult)
                {
                    DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());

                    try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
                    {
                        String tableName = tokens[2];
                        try (Statement statement = connection.createStatement())
                        {
                            Path serializedCsvResultPath = getCachedSerializedResultPath(lastExecuteResultSummary.executionId, client);
                            if (Files.notExists(serializedCsvResultPath))
                            {
                                this.client.getTerminal().writer().println("Can't cache result: cached CSV result file (" + serializedCsvResultPath + ") not found. Try to run a query in REPL again...");
                                return false;
                            }
                            statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, serializedCsvResultPath.toString()));
                            this.client.getTerminal().writer().println(ansi().fgBrightBlack().a("Cached into table: '" + tableName + "'. Launching DataCube...").reset());

                            String functionBodyCode = "#>{" + DataCube.getLocalDatabasePath() + "." + tableName + "}#->sort([])->from(" + DataCube.getLocalRuntimePath() + ")";
                            String functionCode = "###Pure\n" +
                                    "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + functionBodyCode + ";\n}";
                            PureModelContextData pureModelContextData = client.getModelState().parseWithTransient(functionCode);
                            this.replServer.initializeStateFromTable(pureModelContextData);
                            Show.launchDataCube(client, replServer);
                        }
                    }
                }
                else
                {
                    this.client.getTerminal().writer().println("Can't cache result: only relational result supported, got result of type: " + lastExecuteResultSummary.result.getClass().getCanonicalName());
                }
            }
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
