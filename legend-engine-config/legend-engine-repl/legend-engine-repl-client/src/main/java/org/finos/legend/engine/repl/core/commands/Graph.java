// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.core.commands;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class Graph implements Command
{
    private final Client client;

    public Graph(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "graph (<element path>)";
    }

    @Override
    public String description()
    {
        return "show graph element definition in Pure";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("graph"))
        {
            MutableList<String> all = Lists.mutable.with(line.split(" "));
            MutableList<String> showArgs = all.subList(1, all.size());
            if (showArgs.isEmpty())
            {
                PureModelContextData d = this.client.getModelState().parse();
                ListIterate.forEach(
                        ListIterate.collect(
                                ListIterate.select(d.getElements(), c -> !c._package.equals("__internal__")),
                                c ->
                                {
                                    AttributedStringBuilder ab = new AttributedStringBuilder();
                                    ab.append("   ");
                                    drawPath(ab, c._package, c.name);
                                    return ab.toAnsi();
                                }
                        ),
                        e -> this.client.getTerminal().writer().println(e));
            }
            else
            {
                PureModelContextData d = this.client.getModelState().parse();
                PackageableElement element = ListIterate.select(d.getElements(), c -> c.getPath().equals(showArgs.getFirst())).getFirst();
                Section section = LazyIterate.selectInstancesOf(d.getElements(), SectionIndex.class).flatCollect(c -> c.sections).select(c -> c.elements.contains(PureGrammarComposerUtility.convertPath(element.getPath()))).getFirst();
                PureGrammarComposer composer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
                this.client.getTerminal().writer().println(composer.render(element, section.parserName));
            }
            return true;
        }
        return false;
    }

    public static void drawPath(AttributedStringBuilder ab, String _package, String name)
    {
        ab.style(new AttributedStyle().foreground(AttributedStyle.WHITE).italic());
        ab.append(_package == null || _package.isEmpty() ? "" : _package + "::");
        ab.style(new AttributedStyle().foreground(AttributedStyle.WHITE).italic());
        ab.append(name);
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("graph"))
        {
            PureModelContextData d = this.client.getModelState().parse();
            return ListIterate.collect(ListIterate.select(d.getElements(), c -> !c._package.equals("__internal__")), c -> new org.jline.reader.Candidate(PureGrammarComposerUtility.convertPath(c.getPath())));
        }
        return null;
    }

    public static void drawPath(AttributedStringBuilder ab, String path)
    {
        MutableList<String> spl = Lists.mutable.with(path.split("::"));
        if (path.endsWith("::"))
        {
            spl.add("");
        }
        if (spl.size() == 1)
        {
            drawPath(ab, null, spl.get(0));
        }
        else
        {
            drawPath(ab, spl.subList(0, spl.size() - 1).makeString("::"), spl.getLast());
        }
    }
}
