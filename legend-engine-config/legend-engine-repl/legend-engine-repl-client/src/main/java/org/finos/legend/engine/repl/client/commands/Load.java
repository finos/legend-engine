package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import static org.finos.legend.engine.repl.database.MetadataReader.getTables;

public class Load implements Command
{
    private Client client;
    private Completers.FilesCompleter completer = new Completers.FilesCompleter(new File("/"));

    public Load(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "load <path> <connection>";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("load "))
        {
            String path = line.substring("load ".length()).trim();

            String tableName = "test" + getTables(client.getConnection()).size() + 1;

            try (Connection connection = client.getConnection())
            {
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate("CREATE TABLE " + tableName + " AS SELECT * FROM CSVREAD('" + path + "');");
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
            if (words.detect(" "::equals) == null)
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
                // Connection?!
            }
        }
        return null;
    }
}
