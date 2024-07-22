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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.Collections;
import java.util.Comparator;

import static org.finos.legend.engine.repl.relational.RelationalReplExtension.DUCKDB_LOCAL_CONNECTION_BASE_NAME;

public class DataCube implements Command
{
    private final Client client;

    public DataCube(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "datacube*";
    }

    @Override
    public String description()
    {
        return "show available DataCube commands and their usage";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.trim().equals("datacube") || line.trim().equals("datacube help"))
        {
            MutableList<Command> commands = this.client.commands.select(c -> this.equals(c.parentCommand()));
            int maxDocLength = commands.maxBy(c -> c.documentation().length()).documentation().length();
            this.client.getTerminal().writer().println(commands
                    .toSortedList(Comparator.comparing(c ->
                    {
                        String doc = c.documentation();
                        // Make sure DEV commands are shown last
                        return doc.contains("DEV__") ? "ZZZZZZ__" + doc : doc;
                    }))
                    // pad right to align the command description
                    .collect(c -> "  " + c.documentation() + String.join("", Collections.nCopies(maxDocLength - c.documentation().length() + 2, " ")) + c.description())
                    .makeString("\n"));
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (StringUtils.stripStart(inScope, null).startsWith("datacube"))
        {
            return Lists.mutable.empty();
        }
        return null;
    }

    // NOTE: for DataCube, we default to always use local DuckDB
    public static String getLocalConnectionPath()
    {
        return "local::" + DUCKDB_LOCAL_CONNECTION_BASE_NAME + "Connection";
    }

    public static String getLocalDatabasePath()
    {
        return "local::" + DUCKDB_LOCAL_CONNECTION_BASE_NAME + "Database";
    }

    public static String getLocalRuntimePath()
    {
        return "local::" + DUCKDB_LOCAL_CONNECTION_BASE_NAME + "Runtime";
    }
}
