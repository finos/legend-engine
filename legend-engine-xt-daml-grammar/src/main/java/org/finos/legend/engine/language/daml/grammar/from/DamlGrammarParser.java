//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.daml.grammar.from;

import org.finos.legend.engine.language.haskell.grammar.from.HaskellGrammarParser;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;

public class DamlGrammarParser extends HaskellGrammarParser
{
    private DamlGrammarParser()
    {
        super();
    }

    public static DamlGrammarParser newInstance()
    {
        return new DamlGrammarParser();
    }

    @Override
    public HaskellModule parseModule(String code)
    {
        //Remove the syntactic sugar and make it look like regular haskell
        // DAML grammar is like Haskell except for a few syntax changes
        // See https://medium.com/daml-driven/four-tweaks-to-improve-haskell-b1de9c87f816
        code = code.replace(":", "::");
        StringBuilder builder = new StringBuilder();
        boolean insideRecord = false;
        String previousLine = "";
        for (String line : code.split("\n"))
        {
            if (line.contains(" with"))
            {
                builder.append(line.replace("with", "{"));
                builder.append("\n");
                insideRecord = true;
            }
            else
            {
                if (insideRecord)
                {
                    if (line.contains("::"))
                    {
                        if (previousLine.contains("::"))
                        {
                            builder.append(",");
                        }
                        builder.append(line);
                    }
                    else
                    {
                        builder.append("}\n");
                        builder.append(line);
                        insideRecord = false;
                    }
                }
                else
                {
                    builder.append(line);
                }
                builder.append("\n");
            }
            previousLine = line;
        }

        return super.parseModule(builder.toString());
    }
}
