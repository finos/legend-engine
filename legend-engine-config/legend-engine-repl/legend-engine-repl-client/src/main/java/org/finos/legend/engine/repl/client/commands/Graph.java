package org.finos.legend.engine.repl.client.commands;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.Command;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;

public class Graph implements Command
{
    public List<PureGrammarComposerExtension> grammarComposers = PureGrammarComposerExtensionLoader.extensions();
    private Client client;

    public Graph(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "graph (<Packageable Element>)";
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
                PureModelContextData d = client.replInterface.parse(client.buildState().makeString("\n"));
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
                        e -> client.terminal.writer().println(e));
            }
            else
            {
                PureModelContextData d = client.replInterface.parse(client.buildState().makeString("\n"));
                PackageableElement element = ListIterate.select(d.getElements(), c -> c.getPath().equals(showArgs.getFirst())).getFirst();
                String result = ListIterate.flatCollect(this.grammarComposers, c -> c.getExtraPackageableElementComposers().collect(x -> x.apply(element, PureGrammarComposerContext.Builder.newInstance().build()))).select(Predicates.notNull()).getFirst();
                client.terminal.writer().println(result);
            }
            return true;
        }
        return false;
    }

    public static void drawPath(AttributedStringBuilder ab, String _package, String name)
    {
        ab.style(new AttributedStyle().foreground(100, 100, 100).italic());
        ab.append(_package == null || _package.isEmpty() ? "" : _package + "::");
        ab.style(new AttributedStyle().foreground(200, 200, 200).italic());
        ab.append(name);
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("graph"))
        {
            PureModelContextData d = Client.replInterface.parse(client.buildState().makeString("\n"));
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
