package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class Doc implements Command
{
    MutableList<Command> commands;
    Client client;

    public Doc(Client client, MutableList<Command> commands)
    {
        this.commands = commands;
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "doc";
    }

    @Override
    public boolean process(String cmd) throws Exception
    {
        if (cmd.isEmpty())
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
