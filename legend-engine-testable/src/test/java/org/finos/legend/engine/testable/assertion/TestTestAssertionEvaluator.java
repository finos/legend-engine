//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.assertion;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.junit.Assert;
import org.junit.Test;


public class TestTestAssertionEvaluator
{
    @Test
    public void testEqualToAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult(1L);

        CInteger pureInteger = new CInteger(1L);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = pureInteger;

        AssertionStatus assertionStatus = equalTo.accept(new TestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertPass);
    }

    @Test
    public void testEqualToAssertionWithConstantResultAndList()
    {
        ConstantResult constantResult = new ConstantResult(Lists.mutable.with(1L, 2L));

        CInteger pureInteger1 = new CInteger(1L);
        CInteger pureInteger2 = new CInteger(2L);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = new Collection(Lists.mutable.with(pureInteger1, pureInteger2));

        AssertionStatus assertionStatus = equalTo.accept(new TestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertPass);
    }

    @Test
    public void testEqualToFailingAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult(Lists.mutable.with(1L, 5L));

        CInteger pureInteger1 = new CInteger(1L);
        CInteger pureInteger2 = new CInteger(2L);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = new Collection(Lists.mutable.with(pureInteger1, pureInteger2));;

        AssertionStatus assertionStatus = equalTo.accept(new TestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertFail);
        Assert.assertEquals("expected:[1, 2], Found : [1, 5]", ((AssertFail) assertionStatus).message);
    }

    @Test
    public void testEqualToJsonAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult("{\"some\":\"data\"}");

        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = "{\"some\":\"data\"}";

        EqualToJson equalToJson = new EqualToJson();
        equalToJson.expected = data;

        AssertionStatus assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertPass);
    }

    @Test
    public void testEqualToJsonFailingAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult("{\"some\":\"wrong_data\"}");

        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";
        data.data = "{\"some\":\"data\"}";

        EqualToJson equalToJson = new EqualToJson();
        equalToJson.id = "assert1";
        equalToJson.expected = data;

        AssertionStatus assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : \"data\"%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : \"wrong_data\"%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);
    }

    @Test
    public void testEqualToJsonAssertionWithDecimalPrecision()
    {
        ConstantResult constantResult = new ConstantResult("{\"some\":1234567890.123456789}");

        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";

        EqualToJson equalToJson = new EqualToJson();
        equalToJson.id = "assert1";
        equalToJson.expected = data;

        data.data = "{\"some\":1234567890.1234567}";
        AssertionStatus assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals(String.format("{%n  \"some\" : 1234567890.1234567%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 1234567890.123456789%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":1234567890.123456789012}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 1234567890.123456789012%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 1234567890.123456789%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":1234567890.123456789000}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof AssertPass);
        Assert.assertEquals("assert1", assertionStatus.id);
    }

    @Test
    public void testEqualToAssertionWithFloatPrecision()
    {
        ConstantResult constantResult = new ConstantResult("{\"some\":2.0}");

        ExternalFormatData data = new ExternalFormatData();
        data.contentType = "application/json";

        EqualToJson equalToJson = new EqualToJson();
        equalToJson.id = "assert1";
        equalToJson.expected = data;

        data.data = "{\"some\":2}";
        AssertionStatus assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 2%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.0%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":2.000000000000000001}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.000000000000000001%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.0%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":2000000000000000000}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 2000000000000000000%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.0%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":20}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 20%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.0%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":2.00000000000000000}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult));
        Assert.assertTrue(assertionStatus instanceof AssertPass);
        Assert.assertEquals("assert1", assertionStatus.id);

        ConstantResult constantResult1 = new ConstantResult("{\"some\":2}");

        data.data = "{\"some\":2.0}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult1));
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals(String.format("{%n  \"some\" : 2.0%n}"), ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals(String.format("{%n  \"some\" : 2%n}"), ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);

        data.data = "{\"some\":2}";
        assertionStatus = equalToJson.accept(new TestAssertionEvaluator(constantResult1));
        Assert.assertTrue(assertionStatus instanceof AssertPass);
        Assert.assertEquals("assert1", assertionStatus.id);
    }
}
