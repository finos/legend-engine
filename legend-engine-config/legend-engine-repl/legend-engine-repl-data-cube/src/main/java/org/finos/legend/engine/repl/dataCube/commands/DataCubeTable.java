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

import java.util.stream.Collectors;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

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
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            String tableName = tokens[2];
            String functionBodyCode = "#>{" + DataCube.getLocalDatabasePath() + "." + tableName + "}#->from(" + DataCube.getLocalRuntimePath() + ")";
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
        if (inScope.startsWith("datacube table "))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(3);
            {
                String start = words.subList(words.indexOf(" ") + 1, words.size()).get(0);
                PureModelContextData d = this.client.getModelState().parse();
                MutableList<PackageableConnection> foundConnections = ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> !c._package.equals("__internal__"))
                        .select(c -> PureGrammarComposerUtility.convertPath(c.getPath()).equals(DataCube.getLocalConnectionPath()));
                if (!foundConnections.isEmpty() && foundConnections.getFirst().connectionValue instanceof DatabaseConnection)
                {
                    return ConnectionHelper.getTables((RelationalDatabaseConnection) foundConnections.getFirst().connectionValue, client.getPlanExecutor()).filter(c -> c.name.startsWith(start)).map(c -> c.name).map(Candidate::new).collect(Collectors.toCollection(Lists.mutable::empty));
                }
                return null;
            }
        }
        return null;
    }
}
