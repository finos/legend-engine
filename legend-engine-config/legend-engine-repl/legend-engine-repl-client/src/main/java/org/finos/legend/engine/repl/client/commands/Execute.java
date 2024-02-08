package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static org.finos.legend.engine.repl.client.Client.*;

public class Execute implements Command
{
    private Client client;

    public Execute(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "<pure expression>";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        client.terminal.writer().println(client.execute(line));
        return true;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        try
        {
            MutableList<Candidate> list = Lists.mutable.empty();
            CompletionResult result = new org.finos.legend.engine.repl.autocomplete.Completer(buildState().makeString("\n")).complete(inScope);
            if (result.getEngineException() == null)
            {
                list.addAll(result.getCompletion().collect(this::buildCandidate));
                return list;
            }
            else
            {
                printError(result.getEngineException(), parsedLine.line());
                AttributedStringBuilder ab = new AttributedStringBuilder();
                ab.append("> ");
                ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
                ab.append(parsedLine.line());
                terminal.writer().print(ab.toAnsi());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Candidate buildCandidate(CompletionItem s)
    {
        return new Candidate(s.getCompletion(), s.getDisplay(), (String) null, (String) null, (String) null, (String) null, false, 0);
    }
}
