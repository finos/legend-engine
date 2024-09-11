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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;
import java.sql.Statement;

import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class Drop implements Command
{
    private final Client client;

    public Drop(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "drop <connection> <table name>";
    }

    @Override
    public String description()
    {
        return "remove the specified table";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("drop"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), tokens[1]);

            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
            {
                String tableName = tokens[2];
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().dropTable(tableName, tokens[1]));
                    this.client.println("Dropped table: '" + tableName + "'");
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("drop "))
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
            else
            {
                String connectionPath = words.subList(0, words.indexOf(" ") + 1).makeString("").trim();
                String start = words.subList(words.indexOf(" ") + 1, words.size()).get(0);
                PureModelContextData d = this.client.getModelState().parse();
                MutableList<PackageableConnection> foundConnections = ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> !c._package.equals("__internal__"))
                        .select(c -> PureGrammarComposerUtility.convertPath(c.getPath()).equals(connectionPath));
                if (!foundConnections.isEmpty() && foundConnections.getFirst().connectionValue instanceof DatabaseConnection)
                {
                    try (Connection connection = ConnectionHelper.getConnection((DatabaseConnection) foundConnections.getFirst().connectionValue, client.getPlanExecutor()))
                    {
                        return getTables(connection).select(c -> c.name.startsWith(start)).collect(c -> c.name).collect(Candidate::new);
                    }
                    catch (Exception e)
                    {
                        // do nothing
                    }
                }
                return null;
            }
        }
        return null;
    }
}
