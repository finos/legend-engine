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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Standalone expressions in {@code on} mode: the Rust parser's results reach the engine.
 */
public class TestRustExpressionParser
{
    private static final PureGrammarParser JAVA = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser()));
    private static final PureGrammarParser RUST = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), new RustGrammarParserExtension()));

    private String previousMode;
    private String previousFallback;

    @Before
    public void useRustParser()
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
        this.previousFallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        System.setProperty(RustGrammarMode.PROPERTY, "on");
    }

    @After
    public void restoreMode()
    {
        restore(RustGrammarMode.PROPERTY, this.previousMode);
        restore(RustGrammarMode.FALLBACK_PROPERTY, this.previousFallback);
    }

    private static void restore(String property, String value)
    {
        if (value == null)
        {
            System.clearProperty(property);
        }
        else
        {
            System.setProperty(property, value);
        }
    }

    @Test
    public void testLambdasParsedByRust() throws JsonProcessingException
    {
        for (String lambda : new String[]{"x|$x + 1", "|model::Person.all()->filter(p|$p.name == 'x')", "let a = 1;\n$a + 1;"})
        {
            Assert.assertEquals(lambda, json(JAVA.parseLambda(lambda, "query", 2, 5, true)), json(RUST.parseLambda(lambda, "query", 2, 5, true)));
        }
    }

    @Test
    public void testValueSpecificationsAndGraphFetchTreesParsedByRust() throws JsonProcessingException
    {
        Assert.assertEquals(json(JAVA.parseValueSpecification("[1, 'a']", "", 0, 0, true)), json(RUST.parseValueSpecification("[1, 'a']", "", 0, 0, true)));
        Assert.assertEquals(json(JAVA.parseGraphFetch("#{a::A{b, c{d}}}#", "", 0, 0, true)), json(RUST.parseGraphFetch("#{a::A{b, c{d}}}#", "", 0, 0, true)));
    }

    @Test
    public void testDecimalsKeepTheirDigitsAndScale()
    {
        // Read from the Rust parser's JSON exactly as the Java parser builds them, beyond what a double holds.
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        for (String decimal : new String[]{"30000.00", "3.14159265358979323846264338327950288", "1", "0.00001"})
        {
            String lambda = "|" + decimal + "D";
            Assert.assertEquals(lambda, new BigDecimal(decimal), decimalValue(RUST.parseLambda(lambda, "", 0, 0, true)));
            Assert.assertEquals(lambda, decimalValue(JAVA.parseLambda(lambda, "", 0, 0, true)), decimalValue(RUST.parseLambda(lambda, "", 0, 0, true)));
        }
    }

    @Test
    public void testColumnSpecificationsKeepTheirMultiplicity()
    {
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        String lambda = "|~[a:Integer[0..1]]";
        Assert.assertEquals(Multiplicity.ZERO_ONE, columnMultiplicity(RUST.parseLambda(lambda, "", 0, 0, true)));
        Assert.assertEquals(columnMultiplicity(JAVA.parseLambda(lambda, "", 0, 0, true)), columnMultiplicity(RUST.parseLambda(lambda, "", 0, 0, true)));
    }

    @Test
    public void testSyntaxErrorFallsBackToJava()
    {
        EngineException java = parseError(JAVA);
        EngineException rust = parseError(RUST);
        Assert.assertEquals(java.getMessage(), rust.getMessage());
    }

    @Test
    public void testWithoutFallbackTheRustErrorIsReported()
    {
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        EngineException rust = parseError(RUST);
        Assert.assertEquals(EngineErrorType.PARSER, rust.getErrorType());
        Assert.assertNotEquals(parseError(JAVA).getMessage(), rust.getMessage());
    }

    private static EngineException parseError(PureGrammarParser parser)
    {
        return Assert.assertThrows(EngineException.class, () -> parser.parseLambda("x|$x +", "query", 0, 0, true));
    }

    private static BigDecimal decimalValue(LambdaFunction lambda)
    {
        return ((CDecimal) lambda.body.get(0)).value;
    }

    private static Multiplicity columnMultiplicity(LambdaFunction lambda)
    {
        return ((ColSpecArray) ((ClassInstance) lambda.body.get(0)).value).colSpecs.get(0).multiplicity;
    }

    private static String json(Object value) throws JsonProcessingException
    {
        return RustSectionResult.MAPPER.writeValueAsString(value);
    }
}
