// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Date;
import java.sql.Timestamp;

public class TestValueTransformer
{
    @Test
    public void testValueTransformer()
    {
        ValueTransformer valueTransformer = new ValueTransformer();
        Timestamp time = new Timestamp(1696532242123L);
        Date date = new Date(1696532242123L);


        Function<Object, String> transform = value -> value + "Transformed";
        Assert.assertEquals("2023-10-05T18:57:22.123000000+0000", valueTransformer.transformRelationalValue(time, transform));
        Assert.assertEquals("2023-10-05", valueTransformer.transformRelationalValue(date, transform));
        Assert.assertEquals("otherTransformed", valueTransformer.transformRelationalValue("other", transform));

        Assert.assertEquals("\"2023-10-05T18:57:22.123000000+0000\"", valueTransformer.transformWrappedRelationalValueForJSON(time, transform));
        Assert.assertEquals("\"2023-10-05\"", valueTransformer.transformWrappedRelationalValueForJSON(date, transform));
        Assert.assertEquals("otherTransformed", valueTransformer.transformWrappedRelationalValueForJSON("other", transform));

    }

}
