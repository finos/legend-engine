// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.transformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestSetImplTransformers
{
    public SetImplTransformers testBooleanTransformerSetup()
    {
        List<TransformerInput<Integer>> testTransformerInput = new ArrayList<>();
        testTransformerInput.add(new TransformerInput<>(1, "Boolean", o -> false, null));
        return new SetImplTransformers(testTransformerInput);
    }

    @Test
    public void testBooleanTransformerBooleanValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(true);
        Assert.assertTrue(trueValue instanceof Boolean);
        Assert.assertTrue((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(false);
        Assert.assertTrue(falseValue instanceof Boolean);
        Assert.assertFalse((Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerStringValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf("true");
        Assert.assertTrue(trueValue instanceof Boolean);
        Assert.assertTrue((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf("false");
        Assert.assertTrue(falseValue instanceof Boolean);
        Assert.assertFalse((Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerLongValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1L);
        Assert.assertTrue(trueValue instanceof Boolean);
        Assert.assertTrue((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0L);
        Assert.assertTrue(falseValue instanceof Boolean);
        Assert.assertFalse((Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerIntValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1);
        Assert.assertTrue(trueValue instanceof Boolean);
        Assert.assertTrue((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0);
        Assert.assertTrue(falseValue instanceof Boolean);
        Assert.assertFalse((Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerDoubleValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1.0);
        Assert.assertTrue(trueValue instanceof Boolean);
        Assert.assertTrue((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0.0);
        Assert.assertTrue(falseValue instanceof Boolean);
        Assert.assertFalse((Boolean) falseValue);
    }

    public SetImplTransformers testFloatTransformerSetup()
    {
        List<TransformerInput<Integer>> testTransformerInput = new ArrayList<>();
        testTransformerInput.add(new TransformerInput<>(1, "Float", o -> false, null));
        return new SetImplTransformers(testTransformerInput);
    }

    @Test
    public void testFloatTransformerFromDouble()
    {
        SetImplTransformers s = testFloatTransformerSetup();
        Object val = s.transformers.get(0).valueOf(1.0d);
        Assert.assertTrue(val instanceof Double);
        Assert.assertEquals(1.0,  val);
    }

    @Test
    public void testFloatTransformerFromInteger()
    {
        SetImplTransformers s = testFloatTransformerSetup();
        Object val = s.transformers.get(0).valueOf(1L);
        Assert.assertTrue(val instanceof Double);
        Assert.assertEquals(1.0,  val);
    }

    @Test
    public void testFloatTransformerFromBigDecimal()
    {
        SetImplTransformers s = testFloatTransformerSetup();
        Object val = s.transformers.get(0).valueOf(new BigDecimal("0.000"));
        Assert.assertTrue(val instanceof Double);
        Assert.assertEquals(0.0,  val);
    }


    public SetImplTransformers testIntegerTransformerSetup()
    {
        List<TransformerInput<Integer>> testTransformerInput = new ArrayList<>();
        testTransformerInput.add(new TransformerInput<>(1, "Integer", o -> false, null));
        return new SetImplTransformers(testTransformerInput);
    }

    @Test
    public void testIntegerTransformerFromInt()
    {
        SetImplTransformers s = testIntegerTransformerSetup();
        Object val = s.transformers.get(0).valueOf(1);
        Assert.assertTrue(val instanceof Long);
        Assert.assertEquals(1L,  val);
    }

    @Test
    public void testIntegerTransformerFromLong()
    {
        SetImplTransformers s = testIntegerTransformerSetup();
        Object val = s.transformers.get(0).valueOf(1L);
        Assert.assertTrue(val instanceof Long);
        Assert.assertEquals(1L,  val);
    }

    @Test
    public void testIntegerTransformerFromBigDecimal()
    {
        SetImplTransformers s = testIntegerTransformerSetup();
        Object val = s.transformers.get(0).valueOf(new BigDecimal("123456"));
        Assert.assertTrue(val instanceof Long);
        Assert.assertEquals(123456L,  val);
    }
}