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

package org.finos.legend.engine.repl.core.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.shared.REPLHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class Help implements Command
{
    private final MutableList<Command> commands;
    private final Client client;

    public Help(Client client, MutableList<Command> commands)
    {
        this.commands = commands;
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "help";
    }

    @Override
    public String description()
    {
        return "show available commands and their usage";
    }

    @Override
    public boolean process(String cmd) throws Exception
    {
        if (cmd.trim().isEmpty() || cmd.trim().equals("help"))
        {
            MutableList<Command> commands = this.commands.select(c -> c.parentCommand() == null);
            this.client.println(REPLHelper.generateCommandsHelp(commands));
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String cmd, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
