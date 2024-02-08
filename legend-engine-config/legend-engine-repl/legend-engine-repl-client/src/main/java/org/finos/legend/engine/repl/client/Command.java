package org.finos.legend.engine.repl.client;

import org.eclipse.collections.api.list.MutableList;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public interface Command
{
    public boolean process(String cmd) throws Exception;

    public String documentation();

    public MutableList<Candidate> complete(String cmd, LineReader lineReader, ParsedLine parsedLine);
}
