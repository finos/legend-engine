// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.persistence.assertion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.ActiveRowsEquivalentToJson;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.status.ActiveRowsEquivalentToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.testable.persistence.model.ActiveRowsFilterCondition;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistenceTestAssertionEvaluatorActiveRowsEquivalentTest
{

    @Test
    public void testEvaluatorActiveRowsEquivalent() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentWithRowsNotInOrder() throws Exception
    {
        String result = "[{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"NAME\":\"TOM\", \"ID\":4}," + "{\"ID\":3, \"NAME\":\"CATHY\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentWithFieldsToIgnore() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\", \"digest\" : \"d1\"}," + "{\"ID\":2, \"NAME\":\"BRAD\", \"digest\" : \"d2\"}," + "{\"NAME\":\"CATHY\", \"ID\":3, \"digest\" : \"d3\"}," + "{\"ID\":4, \"NAME\":\"TOM\", \"digest\" : \"d4\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\", \"digest\" : \"d4\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        fieldsToIgnore.add("digest");
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentWithActiveRowsFilterConditions() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\", \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":1}," + "{\"ID\":2, \"NAME\":\"BRAD\", \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":999999999}," + "{\"NAME\":\"CATHY\", \"ID\":3, \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":999999999}," + "{\"ID\":4, \"NAME\":\"TOM\", \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":1}]";
        String expected = "[{\"ID\":2, \"NAME\":\"BRAD\", \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":999999999}," + "{\"ID\":3, \"NAME\":\"CATHY\", \"BATCH_ID_IN\":1, \"BATCH_ID_OUT\":999999999}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = new ActiveRowsFilterCondition("BATCH_ID_OUT", 999999999L);

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentObjectsValuesMismatch() throws Exception
    {
        String result = "[{\"ID\":1,\"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOMS\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof ActiveRowsEquivalentToJsonAssertFail);
        ActiveRowsEquivalentToJsonAssertFail activeRowsEquivalentToJsonAssertFail = (ActiveRowsEquivalentToJsonAssertFail) status;
        Assert.assertTrue(activeRowsEquivalentToJsonAssertFail.message.contains("AssertionError: Results do not match the expected data"));
        Assert.assertEquals(expected, activeRowsEquivalentToJsonAssertFail.expected);
        Assert.assertEquals(result, activeRowsEquivalentToJsonAssertFail.actual);
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentUnequalRows() throws Exception
    {
        String result = "[{\"ID\":1,\"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOMS\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof ActiveRowsEquivalentToJsonAssertFail);
        ActiveRowsEquivalentToJsonAssertFail activeRowsEquivalentToJsonAssertFail = (ActiveRowsEquivalentToJsonAssertFail) status;
        Assert.assertTrue(activeRowsEquivalentToJsonAssertFail.message.contains("Number of rows in results [4] does not match number of rows in expected data [3]"));
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentExpectedJsonBadFormat() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\",}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof AssertFail);
        AssertFail assertFail = (AssertFail) status;
        Assert.assertTrue(assertFail.message.contains("Unexpected character ('}' (code 125)): was expecting double-quote to start field name"));
    }

    @Test
    public void testEvaluatorActiveRowsEquivalentColumnNamesMismatch() throws Exception
    {
        String result = "[{\"ID\":1, \"NAMES\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        ActiveRowsFilterCondition activeRowsFilterCondition = null;

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, activeRowsFilterCondition);
        ActiveRowsEquivalentToJson activeRowsEquivalentToJson = new ActiveRowsEquivalentToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        activeRowsEquivalentToJson.expected = data;
        AssertionStatus status = evaluator.visit(activeRowsEquivalentToJson);
        Assert.assertTrue(status instanceof ActiveRowsEquivalentToJsonAssertFail);
        ActiveRowsEquivalentToJsonAssertFail activeRowsEquivalentToJsonAssertFail = (ActiveRowsEquivalentToJsonAssertFail) status;
        Assert.assertTrue(activeRowsEquivalentToJsonAssertFail.message.contains("AssertionError: Results do not match the expected data"));
    }

}
