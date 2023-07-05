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

package org.finos.legend.engine.language.haskell.grammar.test;

import org.finos.legend.engine.language.daml.grammar.from.DamlGrammarParser;
import org.finos.legend.engine.language.daml.grammar.to.DamlGrammarComposer;
import org.finos.legend.engine.language.haskell.grammar.from.HaskellGrammarParser;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.junit.Assert;
import org.junit.Test;

public class TestGrammar
{

    @Test
    public void testDamlSyntaxToString()
    {
        String text = "module Gs.Finance\n" +
                "  where\n" +
                "\n" +
                "data Person = Person { id :: Int, name :: String }\n" +
                "    deriving (Eq, Ord, Show)\n";

        String expectedDamlResult = "module Gs.Finance\n" +
                "  where\n" +
                "\n" +
                "data Person = Person with\n" +
                "  id : Int\n" +
                "  name : String\n" +
                "    deriving (Eq, Ord, Show)\n";

        HaskellGrammarParser parser = HaskellGrammarParser.newInstance();
        HaskellModule module = parser.parseModule(text);
        DamlGrammarComposer composer = DamlGrammarComposer.newInstance();

        String result = composer.renderModule(module);
        Assert.assertEquals(expectedDamlResult, result);
    }

    @Test
    public void testRoundtrip()
    {
        String text = "module Gs.Finance\n" +
                "  where\n" +
                "\n" +
                "data Person = Person with\n" +
                "  id : Int\n" +
                "  name : String\n" +
                "    deriving (Eq, Ord, Show)\n";

        DamlGrammarParser parser = DamlGrammarParser.newInstance();
        HaskellModule module = parser.parseModule(text);
        DamlGrammarComposer composer = DamlGrammarComposer.newInstance();

        String result = composer.renderModule(module);
        Assert.assertEquals(text, result);
    }
}
