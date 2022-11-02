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
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistenceTestAssertionEvaluatorTest
{

    @Test
    public void testEvaluatorObjectsEqual() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorObjectsEqualWithRowsNotInOrder() throws Exception
    {
        String result = "[{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"NAME\":\"TOM\", \"ID\":4}," + "{\"ID\":3, \"NAME\":\"CATHY\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorObjectsEqualWithFieldsToIgnore() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\", \"digest\" : \"d1\"}," + "{\"ID\":2, \"NAME\":\"BRAD\", \"digest\" : \"d2\"}," + "{\"NAME\":\"CATHY\", \"ID\":3, \"digest\" : \"d3\"}," + "{\"ID\":4, \"NAME\":\"TOM\", \"digest\" : \"d4\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\", \"digest\" : \"d4\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        fieldsToIgnore.add("digest");
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof AssertPass);
    }

    @Test
    public void testEvaluatorObjectsValuesMismatch() throws Exception
    {
        String result = "[{\"ID\":1,\"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOMS\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof EqualToJsonAssertFail);
        EqualToJsonAssertFail equalToJsonAssertFail = (EqualToJsonAssertFail) status;
        Assert.assertTrue(equalToJsonAssertFail.message.contains("AssertionError: Results do not match the expected data"));
        Assert.assertEquals(expected, equalToJsonAssertFail.expected);
        Assert.assertEquals(result, equalToJsonAssertFail.actual);
    }

    @Test
    public void testEvaluatorUnequalRows() throws Exception
    {
        String result = "[{\"ID\":1,\"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}," + "{\"ID\":4,\"NAME\":\"TOMS\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2,\"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\",\"ID\":3}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof EqualToJsonAssertFail);
        EqualToJsonAssertFail equalToJsonAssertFail = (EqualToJsonAssertFail) status;
        Assert.assertTrue(equalToJsonAssertFail.message.contains("Number of rows in results [4] does not match number of rows in expected data [3]"));
    }

    @Test
    public void testEvaluatorExpectedJsonBadFormat() throws Exception
    {
        String result = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\",}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof AssertFail);
        AssertFail assertFail = (AssertFail) status;
        Assert.assertTrue(assertFail.message.contains("Unexpected character ('}' (code 125)): was expecting double-quote to start field name"));
    }

    @Test
    public void testEvaluatorColumnNamesMismatch() throws Exception
    {
        String result = "[{\"ID\":1, \"NAMES\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"NAME\":\"CATHY\", \"ID\":3}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";
        String expected = "[{\"ID\":1, \"NAME\":\"ANDY\"}," + "{\"ID\":2, \"NAME\":\"BRAD\"}," + "{\"ID\":3, \"NAME\":\"CATHY\"}," + "{\"ID\":4, \"NAME\":\"TOM\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultData = mapper.readValue(result, new TypeReference<List<Map<String, Object>>>()
        {
        });
        Set<String> fieldsToIgnore = new HashSet<>();
        Map<String, Object> milestoningMap = new HashMap<>();

        PersistenceTestAssertionEvaluator evaluator = new PersistenceTestAssertionEvaluator(resultData, fieldsToIgnore, milestoningMap);
        EqualToJson equalToJson = new EqualToJson();
        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = expected;

        equalToJson.expected = data;
        AssertionStatus status = evaluator.visit(equalToJson);
        Assert.assertTrue(status instanceof EqualToJsonAssertFail);
        EqualToJsonAssertFail equalToJsonAssertFail = (EqualToJsonAssertFail) status;
        Assert.assertTrue(equalToJsonAssertFail.message.contains("AssertionError: Results do not match the expected data"));
    }

}
