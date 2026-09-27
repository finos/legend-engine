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

import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * How the module reads protocol JSON: numbers as the Java parser builds them. Neither the parity harness nor the golden
 * corpus can see a difference here, as the JSON is the same either way.
 */
public class TestRustProtocolMapper
{
    public static class Values
    {
        public Object value;
        public Map<String, Object> map;
        public List<Object> list;
        public Object integer;
        public Number number;
    }

    @Test
    public void testUntypedFloatsAreDoubles() throws Exception
    {
        // As a relational literal's `1.5`, which the Java parser builds as a Double.
        String json = "{\"value\": 1.5, \"map\": {\"a\": 2.5, \"b\": [3.5]}, \"list\": [4.5, {\"c\": 5.5}], \"integer\": 7, \"number\": 6.5}";
        for (Values values : new Values[]{RustSectionResult.MAPPER.readValue(json, Values.class), RustSectionResult.MAPPER.treeToValue(RustSectionResult.MAPPER.readTree(json), Values.class)})
        {
            Assert.assertEquals(1.5, values.value);
            Assert.assertEquals(2.5, values.map.get("a"));
            Assert.assertEquals(3.5, ((List<?>) values.map.get("b")).get(0));
            Assert.assertEquals(4.5, values.list.get(0));
            Assert.assertEquals(5.5, ((Map<?, ?>) values.list.get(1)).get("c"));
            Assert.assertEquals(7, values.integer);
            Assert.assertEquals(6.5, values.number);
        }
    }

    @Test
    public void testDecimalsAreExact() throws Exception
    {
        ValueSpecification decimal = RustSectionResult.MAPPER.readValue("{\"_type\": \"decimal\", \"value\": 30000.00}", ValueSpecification.class);
        Assert.assertEquals(new BigDecimal("30000.00"), ((CDecimal) decimal).value);
    }
}
