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
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.relational.server.REPLServer;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.awt.*;
import java.net.URI;

import static org.jline.jansi.Ansi.ansi;

public class Show implements Command
{
    private final Client client;

    public REPLServer REPLServer;

    public Show(Client client, REPLServer REPLServer)
    {
        this.client = client;
        this.REPLServer = REPLServer;
    }

    @Override
    public String documentation()
    {
        return "show";
    }

    @Override
    public String description()
    {
        return "show the result for the last executed query in GUI mode (DataCube)";
    }

    @Override
    public boolean process(String line)
    {
        if (line.startsWith("show"))
        {
            Execute.ExecuteResult lastExecuteResult = this.client.getExecuteCommand().getLastExecuteResult();
            if (lastExecuteResult == null)
            {
                this.client.getTerminal().writer().println("Can't show result grid in DataCube. Try to run a query in REPL first...");
            }
            else
            {
                this.REPLServer.setExecuteResult(lastExecuteResult);
                try
                {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                    {
                        Desktop.getDesktop().browse(URI.create(REPLServer.getUrl()));
                    }
                    else
                    {
                        this.client.getTerminal().writer().println(REPLServer.getUrl());
                    }
                }
                catch (Exception e)
                {
                    this.client.getTerminal().writer().println(ansi().fgRed().a(e.getMessage()).reset());
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
