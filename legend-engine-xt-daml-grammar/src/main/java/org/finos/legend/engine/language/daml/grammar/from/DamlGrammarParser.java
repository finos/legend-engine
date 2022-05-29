package org.finos.legend.engine.language.daml.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.haskell.grammar.from.HaskellGrammarParser;
import org.finos.legend.engine.language.haskell.grammar.from.antlr4.HaskellParser;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.finos.legend.engine.protocol.haskell.metamodel.NamedConstructor;

import java.util.List;
import java.util.function.Consumer;

public class DamlGrammarParser extends HaskellGrammarParser
{
    private DamlGrammarParser() {
        super();
    }

    public static DamlGrammarParser newInstance()
    {
        return new DamlGrammarParser();
    }

    public HaskellModule parseModule(String code) {
        //Remove the syntactic sugar and make it look like regular haskell
        StringBuilder builder = new StringBuilder();
        boolean insideRecord = false;
        String previousLine = "";
        for(String line:code.split("\n"))
        {
            if(line.contains(" with"))
            {
                builder.append(line.replace("with", "{"));
                builder.append("\n");
                insideRecord = true;
            }
            else
            {
                if(insideRecord)
                {
                    if(line.contains(":"))
                    {
                        if(previousLine.contains(":"))
                        {
                            builder.append(",");
                        }
                        builder.append(line.replace(":", "::"));

                    }
                    else {
                        builder.append("}\n");
                        builder.append(line);
                        insideRecord = false;
                    }
                }
                else {
                    builder.append(line);
                }
                builder.append("\n");
            }
            previousLine = line;
        }

        return super.parseModule(builder.toString());
    }
}
