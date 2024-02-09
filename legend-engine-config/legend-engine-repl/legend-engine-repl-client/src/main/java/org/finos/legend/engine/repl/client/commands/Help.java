package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class Help implements Command
{
    MutableList<Command> commands;
    Client client;

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
    public boolean process(String cmd) throws Exception
    {
        if (cmd.isEmpty() || cmd.equals("help"))
        {
            client.terminal.writer().println(commands.collect(c -> "   "+c.documentation()).makeString("\n"));
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
