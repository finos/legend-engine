// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.language.deephaven.from;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.protocol.deephaven.metamodel.DeephavenApp;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

public class TestDeephavenGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DeephavenParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        // TODO - current grammar parser will pick up "\'" as unknown token - need to enable single quote for col names that may contain space or as a method of identifier escape
        return "###Deephaven\n" +
                "import abc::abc::*;\n" +
                "Deephaven test::Store" + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                "    Table xyz\n" +
                "    (\n" +
                "        prop1: STRING,\n" +
                "        prop2: INT\n" +
                "    )\n" +
                ")";
    }

    @Test
    public void testDeephavenAppParsing()
    {
        String grammar =
                "###Deephaven\n" +
                "DeephavenApp test::MyApp\n" +
                "{\n" +
                "    applicationName: 'MyTestApp';\n" +
                "    function: test::myFunc():Any[*];\n" +
                "    description: 'A test app';\n" +
                "    ownership: Deployment { identifier: 'owner123' };\n" +
                "}\n";

        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(grammar);
        Assert.assertNotNull(pmcd);

        Optional<DeephavenApp> appOpt = pmcd.getElements().stream()
                .filter(e -> e instanceof DeephavenApp)
                .map(e -> (DeephavenApp) e)
                .findFirst();

        Assert.assertTrue("DeephavenApp should be parsed from the grammar", appOpt.isPresent());
        DeephavenApp app = appOpt.get();
        Assert.assertEquals("MyTestApp", app.applicationName);
        Assert.assertEquals("test::myFunc():Any[*]", app.function.path);
        Assert.assertEquals("A test app", app.description);
        Assert.assertEquals("MyApp", app.name);
        Assert.assertEquals("test", app._package);
    }
}
