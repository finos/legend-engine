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

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.util.List;

import static org.finos.legend.engine.repl.Client.buildState;

public class MyCompleter implements Completer
{
    Completers.FilesCompleter completer = new Completers.FilesCompleter(new File("/"));

    List<Candidate> candidates = Lists.mutable.with(new Candidate("show"), new Candidate("list"), new Candidate("load"));

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list)
    {
        String inScope = parsedLine.line().substring(0, parsedLine.cursor());

        if (inScope.endsWith("#>"))
        {
            PureModelContextData d = Client.replInterface.parse(buildState().makeString("\n"));
            list.addAll(ListIterate.collect(ListIterate.select(d.getElements(), c -> c instanceof Store), c -> buildCandidate("#>{" + PureGrammarComposerUtility.convertPath(c.getPath()) + ".")));
        }
        if (inScope.startsWith("#>") && inScope.endsWith("."))
        {
            String store = inScope.substring(3, inScope.length() - 1);
            Database s = (Database) ListIterate.select(Client.replInterface.parse(buildState().makeString("\n")).getElements(), c -> c.getPath().equals(store)).getFirst();
            list.addAll(ListIterate.collect(s.schemas.get(0).tables, c -> buildCandidate(inScope + c.name + "}#")));
        }
        if (inScope.endsWith("from("))
        {
            PureModelContextData d = Client.replInterface.parse(buildState().makeString("\n"));
            list.addAll(ListIterate.collect(ListIterate.select(d.getElements(), c -> c instanceof PackageableRuntime), c -> new Candidate(inScope.substring(inScope.lastIndexOf(" ") + 1) + PureGrammarComposerUtility.convertPath(c.getPath()) + ")")));
        }
        else if (inScope.startsWith("show"))
        {
            PureModelContextData d = Client.replInterface.parse(buildState().makeString("\n"));
            list.addAll(ListIterate.collect(ListIterate.select(d.getElements(), c -> !c._package.equals("__internal__")), c -> new Candidate(PureGrammarComposerUtility.convertPath(c.getPath()))));
        }
        else if (inScope.startsWith("load"))
        {
            completer.complete(lineReader, parsedLine, list);
        }
        else if (!inScope.contains(" "))
        {
            list.addAll(candidates);
        }
    }

    private Candidate buildCandidate(String s)
    {
        return new Candidate(s, s, (String) null, (String) null, (String) null, (String) null, false, 0);
    }
}
