package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;

import static org.finos.legend.engine.repl.database.MetadataReader.getTables;

public class DB implements Command
{
    private Client client;

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
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("db"))
        {
            try (Connection connection = client.getConnection())
            {
                client.terminal.writer().println(
                        getTables(connection).collect(c -> c.schema + "." + c.name + "(" + c.columns.collect(col -> col.name + " " + col.type).makeString(", ") + ")").makeString("\n")
                );
            }
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
