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

package org.finos.legend.engine.repl.client.jline3;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.File;
import java.util.List;

import static org.finos.legend.engine.repl.client.Client.*;

public class JLine3Completer implements Completer
{
    Completers.FilesCompleter completer = new Completers.FilesCompleter(new File("/"));

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list)
    {
        String inScope = parsedLine.line().substring(0, parsedLine.cursor());

        if (inScope.startsWith("graph"))
        {
            PureModelContextData d = Client.replInterface.parse(buildState().makeString("\n"));
            list.addAll(ListIterate.collect(ListIterate.select(d.getElements(), c -> !c._package.equals("__internal__")), c -> new Candidate(PureGrammarComposerUtility.convertPath(c.getPath()))));
        }
        else if (inScope.startsWith("load "))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
            if (words.detect(" "::equals) == null)
            {
                String compressed = words.makeString("");
                completer.complete(lineReader, new JLine3Parser.MyParsedLine(new JLine3Parser.ParserResult(parsedLine.line(), Lists.mutable.with("load", " ", compressed))), list);
                List<Candidate> ca = ListIterate.collect(list, c ->
                {
                    String val = compressed.length() == 1 ? c.value() : c.value().substring(1);
                    return new Candidate(val, val, (String) null, (String) null, (String) null, (String) null, false, 0);
                });
                list.clear();
                list.addAll(ca);
            }
            else
            {
                // Connection?!
            }

        }
        else
        {
            try
            {
                CompletionResult result = new org.finos.legend.engine.repl.autocomplete.Completer(buildState().makeString("\n")).complete(inScope);
                if (result.getEngineException() == null)
                {
                    list.addAll(result.getCompletion().collect(this::buildCandidate));
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
        }
    }

    private Candidate buildCandidate(CompletionItem s)
    {
        return new Candidate(s.getCompletion(), s.getDisplay(), (String) null, (String) null, (String) null, (String) null, false, 0);
    }
}
