// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.ExpressionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.ExpressionParserOverride;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * An extension can take over the parsing of standalone expressions, and delegate to the engine's own parser.
 */
public class TestExpressionParserOverride
{
    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testOverrideDelegatesToTheEngineParser() throws JsonProcessingException
    {
        Overriding overriding = new Overriding();
        PureGrammarParser overridden = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), overriding));
        PureGrammarParser plain = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser()));
        Assert.assertEquals(MAPPER.writeValueAsString(plain.parseLambda("x|$x + 1")), MAPPER.writeValueAsString(overridden.parseLambda("x|$x + 1")));
        Assert.assertEquals(MAPPER.writeValueAsString(plain.parseValueSpecification("[1, 2]", "", 0, 0, true)), MAPPER.writeValueAsString(overridden.parseValueSpecification("[1, 2]", "", 0, 0, true)));
        Assert.assertEquals(MAPPER.writeValueAsString(plain.parseGraphFetch("#{a::A{b}}#", "", 0, 0, true)), MAPPER.writeValueAsString(overridden.parseGraphFetch("#{a::A{b}}#", "", 0, 0, true)));
        Assert.assertEquals(3, overriding.expressionsParsed);
    }

    @Test
    public void testOneExpressionParserOverride()
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> PureGrammarParserExtensions.fromExtensions(new Overriding(), new Overriding()));
        Assert.assertEquals("Conflicting expression parser overrides: 2", e.getMessage());
    }

    /**
     * Takes over standalone expressions, counts them, and delegates them to the parser it replaces.
     */
    private static class Overriding implements PureGrammarParserExtension
    {
        private int expressionsParsed;

        @Override
        public Iterable<? extends ExpressionParserOverride> getExpressionParserOverrides()
        {
            return Collections.singletonList(replaced -> new ExpressionParser()
            {
                @Override
                public LambdaFunction parseLambda(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
                {
                    Overriding.this.expressionsParsed++;
                    return replaced.parseLambda(code, sourceId, lineOffset, columnOffset, returnSourceInfo);
                }

                @Override
                public ValueSpecification parseValueSpecification(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
                {
                    Overriding.this.expressionsParsed++;
                    return replaced.parseValueSpecification(code, sourceId, lineOffset, columnOffset, returnSourceInfo);
                }

                @Override
                public RootGraphFetchTree parseGraphFetch(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
                {
                    Overriding.this.expressionsParsed++;
                    return replaced.parseGraphFetch(code, sourceId, lineOffset, columnOffset, returnSourceInfo);
                }
            });
        }
    }
}
