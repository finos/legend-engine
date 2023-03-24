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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class InternalTaggedUnionSerializationTest
{

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void emptyUnion() throws Exception
    {
        Union union = new Union();
        JsonNode node = OBJECT_MAPPER.valueToTree(union);
        Assert.assertTrue(node.isNull());

        Union parsed = OBJECT_MAPPER.convertValue(node, Union.class);
        Assert.assertNull(parsed.unionValue());
    }

    @Test
    public void readWriteUnion1() throws Exception
    {
        Union union = new Union();
        union.pojo_1 = new Pojo1();
        JsonNode node = OBJECT_MAPPER.valueToTree(union);
        Assert.assertEquals("pojo_1", node.get("typeProp").asText());
        Union parsed = OBJECT_MAPPER.convertValue(node, Union.class);
        Assert.assertTrue(parsed.unionValue() instanceof Pojo1);
    }

    @Test
    public void readWriteUnion2() throws Exception
    {
        Union union = new Union();
        union.pojo_2 = new Pojo2();
        JsonNode node = OBJECT_MAPPER.valueToTree(union);
        Assert.assertEquals("pojoTwo", node.get("typeProp").asText());
        Union parsed = OBJECT_MAPPER.convertValue(node, Union.class);
        Assert.assertTrue(parsed.unionValue() instanceof Pojo2);
    }

    public static class Pojo1
    {
        public String typeProp = "pojo_1";
    }

    public static class Pojo2
    {
        public String typeProp = "pojoTwo";
    }

    @JsonTypeName("typeProp")
    @JsonDeserialize(using = InternalTaggedUnionDeserializer.class)
    public static class Union
    {
        public Pojo1 pojo_1;

        @JsonProperty("pojoTwo")
        public Pojo2 pojo_2;

        @JsonValue
        public Object unionValue()
        {
            return Stream.of(this.pojo_1, this.pojo_2).filter(Objects::nonNull).findFirst().orElse(null);
        }
    }
}