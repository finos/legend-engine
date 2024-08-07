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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializerWithTransformersApplied;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.executeCode;

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
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            String specifiedTableName = tokens[2];
            String expression = this.client.getLastCommand(1);
            if (expression == null)
            {
                this.client.printError("Failed to retrieve the last command");
                return true;
            }
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());

            try
            {
                executeCode(expression, this.client, (Result res, PureModelContextData pmcd, PureModel pureModel) ->
                {
                    if (res instanceof RelationalResult)
                    {
                        RelationalResult relationalResult = (RelationalResult) res;
                        String tempDir = ((RelationalStoreState) this.client.getPlanExecutor().getExecutorsOfType(StoreType.Relational).getOnly().getStoreState()).getRelationalExecutor().getRelationalExecutionConfiguration().tempPath;
                        try (TemporaryFile tempFile = new TemporaryFile(tempDir))
                        {
                            RelationalResultToCSVSerializerWithTransformersApplied serializer = new RelationalResultToCSVSerializerWithTransformersApplied(relationalResult, true);
                            try
                            {
                                tempFile.writeFile(serializer);
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }

                            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
                            {
                                String tableName = specifiedTableName != null ? specifiedTableName : "test" + (getTables(connection).size() + 1);
                                try (Statement statement = connection.createStatement())
                                {
                                    statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tempFile.getTemporaryPathForFile()));
                                    this.client.printInfo("Cached into table: '" + tableName + "'. Launching DataCube...");

                                    String functionBodyCode = "#>{" + DataCube.getLocalDatabasePath() + "." + tableName + "}#->sort([])->from(" + DataCube.getLocalRuntimePath() + ")";
                                    String functionCode = "###Pure\n" +
                                            "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + functionBodyCode + ";\n}";
                                    PureModelContextData pureModelContextData = client.getModelState().parseWithTransient(functionCode);
                                    this.replServer.initializeStateFromTable(pureModelContextData);
                                    Show.launchDataCube(client, replServer);
                                }
                            }
                            catch (SQLException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    else
                    {
                        this.client.printError("Failed to cache: can cache only relational result (got result of type: " + res.getClass().getCanonicalName() + ")");
                    }
                    return null;
                });
            }
            catch (Exception e)
            {
                this.client.printError("Last command run is not an execution of a Pure expression (command run: '" + expression + "')");
                if (e instanceof EngineException)
                {
                    this.client.printEngineError((EngineException) e, expression);
                }
                else
                {
                    throw e;
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
