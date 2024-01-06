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

package org.finos.legend.engine.repl;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.client.Client;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.util.List;

import static org.finos.legend.engine.repl.client.Client.buildState;

public class MyCompleter implements Completer
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
        else if (inScope.startsWith("load"))
        {
            completer.complete(lineReader, parsedLine, list);
        }
//        else if (!inScope.contains(" "))
//        {
//            list.addAll(candidates);
//        }
        else
        {
            try
            {
                list.addAll(new org.finos.legend.engine.repl.autocomplete.Completer(buildState().makeString("\n")).complete(inScope).getCompletion().collect(this::buildCandidate));
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
