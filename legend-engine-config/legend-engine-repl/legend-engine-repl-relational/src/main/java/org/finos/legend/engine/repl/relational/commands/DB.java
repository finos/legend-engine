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
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class DB implements Command
{
    private final Client client;

    public DB(Client client)
    {
        this.client = client;
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
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            PureModelContextData d = this.client.getModelState().parse();
            RelationalDatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(d, tokens[1]);

            Database database = ConnectionHelper.getDatabase(databaseConnection, "local", "DB", this.client.getPlanExecutor());
            this.client.println(
                    PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).render(database, RelationalGrammarParserExtension.NAME)
            );
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
