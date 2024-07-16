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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializerWithTransformersApplied;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.Helpers;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_print;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_SIGNATURE;
import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class Cache implements Command
{
    private final Client client;
    private final PlanExecutor planExecutor;

    public Cache(Client client, PlanExecutor planExecutor)
    {
        this.client = client;
        this.planExecutor = planExecutor;
    }

    @Override
    public String documentation()
    {
        return "cache <connection> <pure expression>";
    }

    @Override
    public String description()
    {
        return "cache the result of the last executed query into a table";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("cache"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length <= 2)
            {
                throw new RuntimeException("Error, cache command should be used as 'cache <connection> <pure expression>'");
            }

            String argsString = line.substring("cache".length() + 1);
            String connectionPath = argsString.substring(0, argsString.indexOf(" "));
            String expression = argsString.substring(argsString.indexOf(" ") + 1);
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), connectionPath);

            String code = "###Pure\n" +
                    "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + expression + ";\n}";
            PureModelContextData parsed = this.client.getModelState().parseWithTransient(code);
            PureModel pureModel = this.client.getLegendInterface().compile(parsed);
            RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            Root_meta_pure_executionPlan_ExecutionPlan plan = this.client.getLegendInterface().generatePlan(pureModel, this.client.isDebug());
            if (this.client.isDebug())
            {
                this.client.getTerminal().writer().println("Generated Plan:");
                this.client.getTerminal().writer().println(core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Boolean_1__Extension_MANY__String_1_(plan, true, extensions, pureModel.getExecutionSupport()));
            }
            String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);

            Identity identity = Helpers.resolveIdentityFromLocalSubject(this.client);
            try (Result res = this.planExecutor.execute((SingleExecutionPlan) PlanExecutor.readExecutionPlan(planStr), new HashMap<>(), identity.getName(), identity, null))
            {
                if (res instanceof RelationalResult)
                {
                    RelationalResult relationalResult = (RelationalResult) res;
                    if (this.client.isDebug())
                    {
                        this.client.getTerminal().writer().println("Executed SQL: " + relationalResult.executedSQl);
                    }
                    String tempDir = ((RelationalStoreState) this.planExecutor.getExecutorsOfType(StoreType.Relational).getOnly().getStoreState()).getRelationalExecutor().getRelationalExecutionConfiguration().tempPath;
                    try (TemporaryFile tempFile = new TemporaryFile(tempDir))
                    {
                        RelationalResultToCSVSerializerWithTransformersApplied serializer = new RelationalResultToCSVSerializerWithTransformersApplied(relationalResult, true);
                        tempFile.writeFile(serializer);

                        try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
                        {
                            String tableName = "test" + (getTables(connection).size() + 1);
                            try (Statement statement = connection.createStatement())
                            {
                                statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tempFile.getTemporaryPathForFile()));
                                this.client.getTerminal().writer().println("Cached into table: '" + tableName + "'");
                            }
                        }
                    }
                }
                else
                {
                    this.client.getTerminal().writer().println("Unable to cache: Can cache only relational result. Got result of type: " + res.getClass().getCanonicalName());
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
            if (parsedLine.words().size() >= 4)
            {
                // In expression block
                try
                {
                    String argsString = inScope.substring("cache".length() + 1);
                    String expression = argsString.substring(argsString.indexOf(" ") + 1);

                    MutableList<Candidate> list = Lists.mutable.empty();
                    CompletionResult result = new org.finos.legend.engine.repl.autocomplete.Completer(this.client.getModelState().getText(), this.client.getCompleterExtensions()).complete(expression);
                    if (result.getEngineException() == null)
                    {
                        list.addAll(result.getCompletion().collect(c -> new Candidate(c.getCompletion(), c.getDisplay(), null, null, null, null, false, 0)));
                        return list;
                    }
                    else
                    {
                        this.client.printError(result.getEngineException(), expression);
                        AttributedStringBuilder ab = new AttributedStringBuilder();
                        ab.append("> ");
                        ab.style(new AttributedStyle().foreground(AttributedStyle.GREEN));
                        ab.append(parsedLine.line());
                        this.client.getTerminal().writer().print(ab.toAnsi());
                        return Lists.mutable.empty();
                    }
                }
                catch (Exception ignored)
                {
                }
                return Lists.mutable.empty();
            }
            else
            {
                // Choosing connection
                MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
                String start = words.get(0);
                PureModelContextData d = this.client.getModelState().parse();
                return
                        ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> !c._package.equals("__internal__"))
                                .collect(c -> PureGrammarComposerUtility.convertPath(c.getPath()))
                                .select(c -> c.startsWith(start))
                                .collect(Candidate::new);
            }
        }
        return null;
    }
}
