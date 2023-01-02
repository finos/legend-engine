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
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
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
            LogInfo errorResponse = new LogInfo((String)null, LoggingEventType.PARSE_ERROR, e);
            Assert.assertNotNull("No source information provided in error", errorResponse.sourceInformation);
            Assert.assertEquals(expectedErrorMsg, EngineException.buildPrettyErrorMessage(errorResponse.message, errorResponse.sourceInformation, EngineErrorType.PARSER));
        }

        String renderedOperation = RelationalGrammarComposerExtension.renderRelationalOperationElement(operation);
        Assert.assertEquals(null, val, renderedOperation);
    }

    @Test
    public void testSimplePropertyMapping()
    {
        test("[store::TESTDB]SCHEMA.TABLE.COL", null);
    }
}
