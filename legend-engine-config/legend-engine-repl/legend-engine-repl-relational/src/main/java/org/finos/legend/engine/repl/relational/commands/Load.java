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
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.relational.RelationalReplExtension;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class Load implements Command
{
    private final Client client;
    private final Completers.FilesCompleter completer = new Completers.FilesCompleter(new File("/"));
    private final RelationalReplExtension relationalReplExtension;

    public Load(Client client, RelationalReplExtension relationalReplExtension)
    {
        this.client = client;
        this.relationalReplExtension = relationalReplExtension;
    }

    @Override
    public String documentation()
    {
        return "load <path> <connection>";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("load"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 3)
            {
                throw new RuntimeException("Error, load should be used as 'load <path> <connection>'");
            }
            String tableName = "test" + getTables(this.relationalReplExtension.getConnection()).size() + 1;

            try (Connection connection = this.relationalReplExtension.getConnection())
            {
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate("CREATE TABLE " + tableName + " AS SELECT * FROM CSVREAD('" + tokens[1] + "');");
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("load "))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
            if (!words.contains(" "))
            {
                String compressed = words.makeString("");
                MutableList<Candidate> list = Lists.mutable.empty();
                completer.complete(lineReader, new JLine3Parser.MyParsedLine(new JLine3Parser.ParserResult(parsedLine.line(), Lists.mutable.with("load", " ", compressed))), list);
                MutableList<Candidate> ca = ListIterate.collect(list, c ->
                {
                    String val = compressed.length() == 1 ? c.value() : c.value().substring(1);
                    return new Candidate(val, val, (String) null, (String) null, (String) null, (String) null, false, 0);
                });
                list.clear();
                list.addAll(ca);
                return list;
            }
            else
            {
                String start = words.subList(words.indexOf(" ") + 1, words.size()).get(0);
                PureModelContextData d = this.client.getLegendInterface().parse(this.client.buildState().makeString("\n"));
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
