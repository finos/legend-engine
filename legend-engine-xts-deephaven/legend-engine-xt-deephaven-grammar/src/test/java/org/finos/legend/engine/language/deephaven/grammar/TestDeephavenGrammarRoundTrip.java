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

package org.finos.legend.engine.language.deephaven.grammar;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.deephaven.from.DeephavenGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Assert;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestDeephavenGrammarRoundTrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testParserExtensionAvailable()
    {
        MutableList<Class<?>> compilerExtensions =
                Lists.mutable.withAll(ServiceLoader.load(PureGrammarParserExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(compilerExtensions.contains(DeephavenGrammarParserExtension.class));
    }

    @Test
    public void testDeephavenRoundTrip()
    {
        test("###Deephaven\n" +
                        "Deephaven test::Store::foo\n" +
                        "(\n" +
                        "    Table xyz\n" +
                        "    (\n" +
                        "      prop1: String,\n" +
                        "      prop2: Integer,\n" +
                        "      prop3: Boolean,\n" +
                        "      prop4: DateTime\n" +
                        "    )\n" +
                        ")\n" +
                        "\n" +
                        "\n" +
                        "###Connection\n" +
                        "DeephavenConnection test::DeephavenConnection\n" +
                        "{\n" +
                        "  store: test::DeephavenStore;\n" +
                        "  serverUrl: 'http://dummyurl.com:12345'\n" +
                        "  authentication: # PSK {\n" +
                        "    psk: 'abcde';\n" +
                        "  }#;\n" +
                        "}\n");
    }
}
