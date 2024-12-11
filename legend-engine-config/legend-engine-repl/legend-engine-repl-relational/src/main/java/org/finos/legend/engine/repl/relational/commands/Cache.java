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

package org.finos.legend.engine.repl.relational.commands;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializerWithTransformersApplied;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.executeCode;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.printExecutionTime;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class Cache implements Command
{
    private final Client client;

    public Cache(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "cache <connection> (<table name>)";
    }

    @Override
    public String description()
    {
        return "run the query and cache the result";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("cache"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length < 2)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            String connectionPath = tokens[1];
            final String specifiedTableName = tokens.length == 3 ? tokens[2] : null;
            String expression = this.client.getLastCommand(1);
            if (expression == null)
            {
                this.client.printError("Failed to retrieve the last command");
                return true;
            }
            RelationalDatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), connectionPath);

            try
            {
                long startTime = System.currentTimeMillis();
                executeCode(expression, this.client, (Result res, PureModelContextData pmcd, PureModel pureModel, SingleExecutionPlan plan) ->
                {
                    if (res instanceof RelationalResult)
                    {
                        RelationalResult relationalResult = (RelationalResult) res;
                        List<Column> relationalResultColumns = relationalResult.getResultSetColumns();
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

                            List<Table> tables = ConnectionHelper.getTables(databaseConnection, client.getPlanExecutor()).collect(Collectors.toList());

                            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
                            {
                                String tableName = specifiedTableName != null ? specifiedTableName : "test" + (tables.size() + 1);
                                try (Statement statement = connection.createStatement())
                                {
                                    statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tempFile.getTemporaryPathForFile(), relationalResultColumns));
                                    this.client.println("Cached into table: '" + tableName + "'");
                                    this.client.printDebug(printExecutionTime(startTime));
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
        if (inScope.startsWith("cache "))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
            if (!words.contains(" "))
            {
                String start = words.get(0);
                PureModelContextData d = this.client.getModelState().parse();
                return ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> !c._package.equals("__internal__"))
                        .collect(c -> PureGrammarComposerUtility.convertPath(c.getPath()))
                        .select(c -> c.startsWith(start))
                        .collect(Candidate::new);
            }
        }
        return null;
    }
}
