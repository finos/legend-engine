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
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.RelationalGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalOperationElementGrammarRoundtrip
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    protected static void test(String val, String expectedErrorMsg)
    {
        RelationalOperationElement operation = null;
        try
        {
            RelationalOperationElement op = RelationalGrammarParserExtension.parseRelationalOperationElement(val, "", 0, 0, true);
            String json = objectMapper.writeValueAsString(op);
            operation = objectMapper.readValue(json, RelationalOperationElement.class);
            if (expectedErrorMsg != null)
            {
                Assert.fail("Test did not fail with error '" + expectedErrorMsg + "' as expected");
            }
        }
        catch (Exception e)
        {
            LogInfo errorResponse = new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.PARSE_ERROR, e);
            Assert.assertNotNull("No source information provided in error", errorResponse.sourceInformation);
            MatcherAssert.assertThat(EngineException.buildPrettyErrorMessage(errorResponse.message, errorResponse.sourceInformation, EngineErrorType.PARSER),
                    CoreMatchers.startsWith(expectedErrorMsg));
        }

        String renderedOperation = RelationalGrammarComposerExtension.renderRelationalOperationElement(operation);
        Assert.assertEquals(null, val, renderedOperation);
    }

    // Round-trips through the protocol like test() above, but allows the composed form to differ
    // from the source, for syntax the composer canonicalises.
    protected static void testWithSourceAndExpected(String source, String expectedComposed) throws Exception
    {
        RelationalOperationElement op = RelationalGrammarParserExtension.parseRelationalOperationElement(source, "", 0, 0, true);
        String json = objectMapper.writeValueAsString(op);
        RelationalOperationElement operation = objectMapper.readValue(json, RelationalOperationElement.class);
        Assert.assertEquals(expectedComposed, RelationalGrammarComposerExtension.renderRelationalOperationElement(operation));
    }

    @Test
    public void testSimplePropertyMapping()
    {
        test("[store::TESTDB]SCHEMA.TABLE.COL", null);
    }

    @Test
    public void testArrayFilterLambda()
    {
        test("array_filter(extractFromSemiStructured([store::TESTDB]SCHEMA.TABLE.DOC, 'divisions', 'SEMISTRUCTURED[]'), d | extractFromSemiStructured($d, 'headcount', 'INTEGER'))", null);
    }

    @Test
    public void testArrayFilterLambdaOverColumn()
    {
        test("array_filter([store::TESTDB]SCHEMA.TABLE.DOC, d | extractFromSemiStructured($d, 'name', 'VARCHAR'))", null);
    }

    @Test
    public void testArrayTransformLambda()
    {
        test("array_transform(extractFromSemiStructured([store::TESTDB]SCHEMA.TABLE.DOC, 'tags', 'SEMISTRUCTURED[]'), t | extractFromSemiStructured($t, 'label', 'VARCHAR'))", null);
    }

    // The parameter is referenced with a sigil because a bare identifier is already a column
    // reference; this pins that the two stay distinguishable.
    @Test
    public void testLambdaParameterIsDistinctFromAColumn()
    {
        test("array_filter([store::TESTDB]SCHEMA.TABLE.DOC, d | equal($d, [store::TESTDB]SCHEMA.TABLE.COL))", null);
    }

    @Test
    public void testArrayReduceLambdaWithTwoParameters()
    {
        test("array_reduce(extractFromSemiStructured([store::TESTDB]SCHEMA.TABLE.DOC, 'divisions', 'SEMISTRUCTURED[]'), (d, acc | plus($acc, extractFromSemiStructured($d, 'headcount', 'INTEGER'))), 0)", null);
    }

    // A single parameter may be written parenthesised, but composes back bare: parentheses are
    // only required to keep several names from reading as further arguments, so one name has a
    // single canonical form. This asserts the normalised output rather than the input.
    @Test
    public void testSingleParameterLambdaInParenthesesNormalises() throws Exception
    {
        testWithSourceAndExpected(
                "array_filter([store::TESTDB]SCHEMA.TABLE.DOC, (d | extractFromSemiStructured($d, 'name', 'VARCHAR')))",
                "array_filter([store::TESTDB]SCHEMA.TABLE.DOC, d | extractFromSemiStructured($d, 'name', 'VARCHAR'))");
    }
}
