//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TaggedUnionSerializationTest
{

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testLong()
    {
        Union union = new Union();
        union._long = 123L;
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isIntegralNumber());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals(123L, fromJson.unionValue());
    }

    @Test
    public void testDouble()
    {
        Union union = new Union();
        union._double = 12.3;
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isFloatingPointNumber());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals(12.3, fromJson.unionValue());
    }


    @Test
    public void testBoolean()
    {
        Union union = new Union();
        union._boolean = true;
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isBoolean());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals(true, fromJson.unionValue());
    }

    @Test
    public void testNull()
    {
        Union union = new Union();
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isNull());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertNotNull(fromJson);
        Assert.assertNull(fromJson.unionValue());
    }

    @Test
    public void testString()
    {
        Union union = new Union();
        union.string = "hello";
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isTextual());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals("hello", fromJson.unionValue());
    }

    @Test
    public void testPojo1()
    {
        Union union = new Union();
        union.pojo1 = new Pojo1();
        union.pojo1.name = "hello";
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isObject());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals("hello", ((Pojo1) fromJson.unionValue()).name);
    }

    @Test
    public void testPojo2()
    {
        Union union = new Union();
        union.pojo2 = new Pojo2();
        union.pojo2.firstName = "hello";
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isObject());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals("hello", ((Pojo2) fromJson.unionValue()).firstName);
    }

    @Test
    public void testList()
    {
        Union union = new Union();
        union.lists = Arrays.asList(1.0, 2.0, 3.0);
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(jsonNode.isArray());
        Union fromJson = OBJECT_MAPPER.convertValue(jsonNode, Union.class);
        Assert.assertEquals(Arrays.asList(1.0, 2.0, 3.0), fromJson.unionValue());
    }

    @JsonDeserialize(using = TaggedUnionDeserializer.class)
    public static class Union
    {
        public String string;
        public Boolean _boolean;
        public Long _long;
        public Double _double;
        public Pojo1 pojo1;
        public Pojo2 pojo2;
        public List<Double> lists;

        @JsonValue
        public Object unionValue()
        {
            return Stream.of(
                    this.string, this._boolean, this._long, this._double, this.lists, this.pojo1, this.pojo2
            ).filter(Objects::nonNull).findFirst().orElse(null);
        }
    }

    public static class Pojo1
    {
        public String name;
    }

    public static class Pojo2
    {
        public String firstName;
    }
}
