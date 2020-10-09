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
import org.antlr.v4.runtime.Vocabulary;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestGrammarParser
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public abstract static class TestGrammarParserTestSuite
    {
        public abstract Vocabulary getParserGrammarVocabulary();

        public List<Vocabulary> getDelegatedParserGrammarVocabulary()
        {
            return new ArrayList<>();
        }

        public abstract String getParserGrammarIdentifierInclusionTestCode(List<String> keywords);

        /**
         * The rationale behind this test is that often we have a rule for `identifier` in the parser grammar
         * which usually takes from of VALID_STRING, this will clash with any keyword that we define so we
         * have to include all the keywords in the lexer grammar in the `identifier` rule of the parser grammar
         * in order for parsing to be done correctly, as such, any keyword that we wish to not allow to be considered
         * as identifier should not be included and we should write tests for those
         */
        @Test
        public void testParserGrammarIdentifierInclusion()
        {
            List<Vocabulary> vocabularies = FastList.newListWith(this.getParserGrammarVocabulary());
            vocabularies.addAll(this.getDelegatedParserGrammarVocabulary());
            final List<String> keywords = new ArrayList<>();
            vocabularies.forEach(vocabulary ->
            {
                for (int i = 0; i < vocabulary.getMaxTokenType(); ++i)
                {
                    String literal = StringUtils.unwrap(vocabulary.getLiteralName(i), "'");
                    if (literal != null && literal.matches("[A-Za-z0-9_][A-Za-z0-9_$]*"))
                    {
                        keywords.add(literal);
                    }
                }
            });
            String testCode = this.getParserGrammarIdentifierInclusionTestCode(ListIterate.distinct(keywords));
            if (testCode != null)
            {
                test(testCode);
            }
        }

        protected static void test(String val)
        {
            PureGrammarParser.newInstance().parseModel(val);
        }

        protected static void test(String val, String expectedErrorMsg)
        {
            try
            {
                PureGrammarParser.newInstance().parseModel(val);
                if (expectedErrorMsg != null)
                {
                    Assert.fail("Test did not fail with error '" + expectedErrorMsg + "' as expected");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                LogInfo errorResponse = new LogInfo(null, LoggingEventType.PARSE_ERROR, e);
                Assert.assertNotNull("No source information provided in error", errorResponse.sourceInformation);
                Assert.assertEquals(expectedErrorMsg, EngineException.buildPrettyErrorMessage(errorResponse.message, errorResponse.sourceInformation, EngineErrorType.PARSER));
            }
        }
    }

    /**
     * ANTLR throws quite complicated parser error messages, such as:
     * "mismatched input ... expecting"
     * "extraneous input ... expecting"
     * "missing ... at ..."
     * ...
     * We dont' really want to show the `expecting` part as it makes little sense to users
     */
    @Test
    public void testParserSimplifyingUnexpectedTokenError()
    {
        // INVALID token found at the wrong places (default error message: "extraneous input ... expecting")
        test("###Pure\n" +
                "asd\n" +
                "Class test::tClass\n" +
                "{\n" +
                "}\n", "PARSER error at [2:1-3]: Unexpected token");
        test("###Pure\n" +
                "Class test::tClass\n" +
                "{\n" +
                "} randomToken\n", "PARSER error at [4:3-13]: Unexpected token");
        // missing token (default error message: "missing ... at ...")
        test("Class A {", "PARSER error at [1:9]: Unexpected token");
    }

    // NOTE: right now we have the boolean value keywords in core parser
    // but we don't allow them to be part of `identifier` so this test is here to
    // ensure we don't forget about that.
    @Test
    public void testElementPathWithReservedBooleanKeywords()
    {
        test("Class false::me\n" +
                "{\n" +
                "}\n", "PARSER error at [1:7-11]: Unexpected token");
        test("Class true::me\n" +
                "{\n" +
                "}\n", "PARSER error at [1:7-10]: Unexpected token");
    }

    public static void testFromJson(Class<?> _class, String path, String code)
    {
        PureModelContextData modelData = null;
        try
        {
            String json = getJsonString(_class, path);
            modelData = objectMapper.readValue(json, PureModelContextData.class);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        Assert.assertEquals(code, grammarTransformer.renderPureModelContextData(Objects.requireNonNull(modelData)));
    }

    public static String getJsonString(Class<?> _class, String path)
    {
        return new java.util.Scanner(Objects.requireNonNull(_class.getClassLoader().getResourceAsStream(path), "Can't find resource '" + path + "'")).useDelimiter("\\A").next();
    }

    private static void test(String val)
    {
        TestGrammarParserTestSuite.test(val);
    }

    private static void test(String val, String expectedErrorMsg)
    {
        TestGrammarParserTestSuite.test(val, expectedErrorMsg);
    }
}
