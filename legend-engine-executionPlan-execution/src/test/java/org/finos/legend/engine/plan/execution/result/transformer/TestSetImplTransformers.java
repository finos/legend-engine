package org.finos.legend.engine.plan.execution.result.transformer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestSetImplTransformers 
{
    public SetImplTransformers testBooleanTransformerSetup()
    {
        List<TransformerInput<Integer>> testTransformerInput= new ArrayList<>();
        testTransformerInput.add(new TransformerInput<Integer>(1, "Boolean", o -> false, null));
        return new SetImplTransformers(testTransformerInput);
    }
    
    @Test 
    public void testBooleanTransformerBooleanValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(true);
        assert(trueValue instanceof Boolean);
        assert((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(false);
        assert(falseValue instanceof Boolean);
        assert(!(Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerStringValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf("true");
        assert(trueValue instanceof Boolean);
        assert((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf("false");
        assert(falseValue instanceof Boolean);
        assert(!(Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerLongValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1L);
        assert(trueValue instanceof Boolean);
        assert((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0L);
        assert(falseValue instanceof Boolean);
        assert(!(Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerIntValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1);
        assert(trueValue instanceof Boolean);
        assert((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0);
        assert(falseValue instanceof Boolean);
        assert(!(Boolean) falseValue);
    }

    @Test
    public void testBooleanTransformerDoubleValue()
    {
        SetImplTransformers s = testBooleanTransformerSetup();
        Object trueValue = s.transformers.get(0).valueOf(1.0);
        assert(trueValue instanceof Boolean);
        assert((Boolean) trueValue);

        Object falseValue = s.transformers.get(0).valueOf(0.0);
        assert(falseValue instanceof Boolean);
        assert(!(Boolean) falseValue);
    }
}