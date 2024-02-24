// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.repl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.predicate.checked.CheckedPredicate;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.repl.client.commands.*;
import org.finos.legend.engine.repl.client.jline3.JLine3Completer;
import org.finos.legend.engine.repl.client.jline3.JLine3Highlighter;
import org.finos.legend.engine.repl.REPLInterface;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.LocalREPL;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.sql.*;

import static org.finos.legend.engine.repl.database.MetadataReader.getTables;

public class Client
{
    public static final REPLInterface replInterface = new LocalREPL();

    public static MutableList<String> state = Lists.mutable.empty();

    public static Terminal terminal;

    public static boolean debug = false;

    public static ObjectMapper objectMapper = new ObjectMapper();

    static
    {
        System.setProperty("legend.test.h2.port", "1975");
    }

    public static void main(String[] args) throws Exception
    {
        new Client().loop();
    }

    public LineReader reader;

    public MutableList<Command> commands;

    public Client() throws Exception
    {
        terminal = TerminalBuilder.terminal();

        terminal.writer().println("\n" + Logos.logos.get((int) (Logos.logos.size() * Math.random())) + "\n");

        this.commands = Lists.mutable.with(new DB(this), new Ext(this), new Debug(this), new Graph(this), new Load(this), new Execute(this));
        this.commands.add(0, new Help(this, this.commands));

        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(new JLine3Highlighter())
                .parser(new JLine3Parser())//new DefaultParser().quoteChars(new char[]{'"'}))
                .completer(new JLine3Completer(this.commands))
                .build();

        terminal.writer().println("Warming up...");
        terminal.flush();
        ((Execute) this.commands.getLast()).execute("1+1");
        terminal.writer().println("Ready!\n");


    }

    public void loop()
    {
        while (true)
        {
            String line = this.reader.readLine("> ");
            if (line == null || line.equalsIgnoreCase("exit"))
            {
                break;
            }

            this.reader.getHistory().add(line);

            try
            {
                commands.detect(new CheckedPredicate<Command>()
                {
                    @Override
                    public boolean safeAccept(Command c) throws Exception
                    {
                        return c.process(line);
                    }
                });
            }
            catch (EngineException e)
            {
                printError(e, line);
            }
            catch (Exception ee)
            {
                terminal.writer().println(ee.getMessage());
                if (debug)
                {
                    ee.printStackTrace();
                }
            }
        }
    }

    public static void printError(EngineException e, String line)
    {
        int e_start = e.getSourceInformation().startColumn;
        int e_end = e.getSourceInformation().endColumn;
        if (e_start <= line.length())
        {
            String beg = line.substring(0, e_start - 1);
            String mid = line.substring(e_start - 1, e_end);
            String end = line.substring(e_end, line.length());
            AttributedStringBuilder ab = new AttributedStringBuilder();
            ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
            ab.append(beg);
            ab.style(new AttributedStyle().underline().bold().foreground(200, 0, 0));
            ab.append(mid);
            ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
            ab.append(end);
            terminal.writer().println("");
            terminal.writer().println(ab.toAnsi());
        }
        terminal.writer().println(e.getMessage());
        if (debug)
        {
            e.printStackTrace();
        }
    }

    public Connection getConnection()
    {
        RelationalStoreExecutor r = (RelationalStoreExecutor) ((Execute) this.commands.getLast()).planExecutor.getExecutorsOfType(StoreType.Relational).getFirst();
        return r.getStoreState().getRelationalExecutor().getConnectionManager().getTestDatabaseConnection();
    }

    public MutableList<String> buildState()
    {
        MutableList<String> res = Lists.mutable.withAll(state);

        res.add("###Relational\n" +
                "Database test::TestDatabase" +
                "(" +
                getTables(getConnection()).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
                ")\n");

        res.add("###Connection\n" +
                "RelationalDatabaseConnection test::TestConnection\n" +
                "{\n" +
                "   store: test::TestDatabase;" +
                "   specification: LocalH2{};" +
                "   type: H2;" +
                "   auth: DefaultH2;" +
                "}\n");

        res.add("###Runtime\n" +
                "Runtime test::TestRuntime\n" +
                "{\n" +
                "   mappings : [];" +
                "   connections:\n" +
                "   [\n" +
                "       test::TestDatabase : [connection: test::TestConnection]\n" +
                "   ];\n" +
                "}\n");

        return res;
    }


}
