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

package org.finos.legend.engine.language.daml.grammar.to;

import org.finos.legend.engine.language.haskell.grammar.to.HaskellGrammarComposer;
import org.finos.legend.engine.protocol.haskell.metamodel.Field;
import org.finos.legend.engine.protocol.haskell.metamodel.NamedConstructor;
import org.finos.legend.engine.protocol.haskell.metamodel.RecordTypeConstructor;

/**
 * DAML grammar is like Haskell except for a few syntax changes
 * See https://medium.com/daml-driven/four-tweaks-to-improve-haskell-b1de9c87f816
 * <p>
 * DAML switches the colon convention for when ":" is used vs "::"
 * DAML introduces a "with" keyword for Record types
 */
public class DamlGrammarComposer extends HaskellGrammarComposer
{
    private DamlGrammarComposer()
    {
        super(":", "::");
    }

    public static DamlGrammarComposer newInstance()
    {
        return new DamlGrammarComposer();
    }

    @Override
    protected void renderNamedConstructor(StringBuilder builder, NamedConstructor constructor)
    {
        builder.append(constructor.name).append(" with");

        if (constructor instanceof RecordTypeConstructor)
        {
            renderRecordTypeConstructor(builder, (RecordTypeConstructor) constructor);
        }
    }

    @Override
    protected void renderRecordTypeConstructor(StringBuilder builder, RecordTypeConstructor constructor)
    {
        for (Field field : constructor.fields)
        {
            builder.append("\n  ");
            renderFieldConstructor(builder, field);
        }
    }
}
