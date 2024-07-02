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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.RelationalReplExtension;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;

import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class DB implements Command
{
    private final Client client;
    private final RelationalReplExtension relationalReplExtension;

    public DB(Client client, RelationalReplExtension relationalReplExtension)
    {
        this.client = client;
        this.relationalReplExtension = relationalReplExtension;
    }

    @Override
    public String documentation()
    {
        return "db <connection>";
    }

    @Override
    public String description()
    {
        return "show schema summary of the database";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("db"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 2)
            {
                throw new RuntimeException("Error, load should be used as 'db <connection>'");
            }

            PureModelContextData d = this.client.getModelState().parse();
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(d, tokens[1]);

            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
            {
                this.client.getTerminal().writer().println(
                        getTables(connection).collect(c -> c.schema + "." + c.name + "(" + c.columns.collect(col -> col.name + " " + col.type).makeString(", ") + ")").makeString("\n")
                );
            }
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("db "))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
            String start = words.get(0);
            PureModelContextData d = this.client.getModelState().parse();
            return
                    ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> !c._package.equals("__internal__"))
                            .collect(c -> PureGrammarComposerUtility.convertPath(c.getPath()))
                            .select(c -> c.startsWith(start))
                            .collect(Candidate::new);
        }
        return null;
    }
}
