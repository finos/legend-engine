package org.finos.legend.engine.testable.service.assertion;

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

import java.util.Collections;

public class TestServiceTestAssertionEvaluator
{
    @Test
    public void testEqualToAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult(1L);

        CInteger pureInteger = new CInteger();
        pureInteger.values = Collections.singletonList(1L);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = pureInteger;

        AssertionStatus assertionStatus = equalTo.accept(new ServiceTestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertPass);
    }

    @Test
    public void testEqualToAssertionWithConstantResultAndList()
    {
        ConstantResult constantResult = new ConstantResult(Lists.mutable.with(1l, 2L));

        CInteger pureInteger1 = new CInteger();
        pureInteger1.values = Lists.mutable.with(1l);
        CInteger pureInteger2 = new CInteger();
        pureInteger2.values = Lists.mutable.with(2L);

        Collection collection = new Collection();
        collection.values = Lists.mutable.with(pureInteger1, pureInteger2);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = collection;

        AssertionStatus assertionStatus = equalTo.accept(new ServiceTestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertPass);
    }

    @Test
    public void testEqualToFailingAssertionWithConstantResult()
    {
        ConstantResult constantResult = new ConstantResult(Lists.mutable.with(1l, 5L));

        CInteger pureInteger1 = new CInteger();
        pureInteger1.values = Lists.mutable.with(1l);
        CInteger pureInteger2 = new CInteger();
        pureInteger2.values = Lists.mutable.with(2L);

        Collection collection = new Collection();
        collection.values = Lists.mutable.with(pureInteger1, pureInteger2);

        EqualTo equalTo = new EqualTo();
        equalTo.expected = collection;

        AssertionStatus assertionStatus = equalTo.accept(new ServiceTestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof AssertFail);
        Assert.assertEquals("Expected : [1, 2], Found : [1, 5]", ((AssertFail) assertionStatus).message);
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

        AssertionStatus assertionStatus = equalToJson.accept(new ServiceTestAssertionEvaluator(constantResult));

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

        AssertionStatus assertionStatus = equalToJson.accept(new ServiceTestAssertionEvaluator(constantResult));

        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", assertionStatus.id);
        Assert.assertEquals("{\n  \"some\" : \"data\"\n}", ((EqualToJsonAssertFail) assertionStatus).expected);
        Assert.assertEquals("{\n  \"some\" : \"wrong_data\"\n}", ((EqualToJsonAssertFail) assertionStatus).actual);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) assertionStatus).message);
    }
}
