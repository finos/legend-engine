// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;

public class TestGrammarRoundtrip
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public static class TestGrammarRoundtripTestSuite
    {
        /**
         * This method is used for testing when the grammar and the protocol is 100% bijective
         * This also checks for indentation formatting
         */
        public static void test(String code)
        {
            test(code, null);
        }

        private static void test(String code, String message)
        {
            PureModelContextData modelData = null;
            try
            {
                modelData = PureGrammarParser.newInstance().parseModel(code);
                String json = objectMapper.writeValueAsString(modelData);
                modelData = objectMapper.readValue(json, PureModelContextData.class);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
            Assert.assertEquals(message, code, grammarTransformer.renderPureModelContextData(modelData));
        }

        public static void testFormatWithoutSectionIndex(String code, String unformattedCode)
        {
            testFormat(code, unformattedCode, true);
        }

        public static void testFormat(String code, String unformattedCode)
        {
            testFormat(code, unformattedCode, false);
        }

        /**
         * This method is used for detect when the grammar and the protocol is NOT 100% bijective
         * Which implies there is some "smart" thing (i.e. inference) that we probably shouldn't do in the parser or the transformer
         * <p>
         * For example:
         * // ORIGINAL
         * ...
         * field1: false;
         * ...
         * // FORMATTED
         * ...
         * // (does not appear as in the parser we convert `false` -> `null` in the protocol and in the transformer we do not print anything for `null`)
         * ...
         * // end of example
         */
        private static void testFormat(String code, String unformattedCode, boolean omitSectionIndex)
        {
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
            PureModelContextData parsedModel = PureGrammarParser.newInstance().parseModel(unformattedCode);
            if (omitSectionIndex)
            {
                parsedModel.sectionIndices = new ArrayList<>();
            }
            String formatted = grammarTransformer.renderPureModelContextData(parsedModel);
            Assert.assertEquals(code, formatted);
            // NOTE: do not remove the round-trip test for formatted code as this is a very good way to ensure that grammar <-> >protocol is bijective
            test(formatted, "Expected formatted code to pass round-trip test");
        }
    }
}
