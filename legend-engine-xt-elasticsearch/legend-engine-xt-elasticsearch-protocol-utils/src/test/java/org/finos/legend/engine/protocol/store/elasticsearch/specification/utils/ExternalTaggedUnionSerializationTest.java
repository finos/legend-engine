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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ExternalTaggedUnionSerializationTest
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void singleValueSerializeProperly()
    {
        Wrapper wrapper = new Wrapper();
        Union union = new Union();
        union.pojo_1 = new Pojo1("from pojo1");
        wrapper.union.put("hello", union);

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("union").get("pojo_1#hello").get("value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertTrue(fromJson.union.containsKey("hello"));
        Assert.assertEquals("from pojo1", fromJson.union.get("hello").pojo_1.value);
    }

    @Test
    public void additionalPropertySerializeProperly()
    {
        Wrapper2 wrapper = new Wrapper2();
        Union union = new Union();
        union.pojo_1 = new Pojo1("from pojo1");
        wrapper.union.put("hello", union);

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("pojo_1#hello").get("value").asText());
        Wrapper2 fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper2.class);
        Assert.assertTrue(fromJson.union.containsKey("hello"));
        Assert.assertEquals("from pojo1", fromJson.union.get("hello").pojo_1.value);
    }

    @Test
    public void multiValueSerializeProperly()
    {
        Wrapper wrapper = new Wrapper();
        Union union1 = new Union();
        union1.pojo_1 = new Pojo1("from pojo1");
        Union union2 = new Union();
        union2.pojo_2 = new Pojo2("from pojo2");
        wrapper.unions.put("hello", union1);
        wrapper.unions.put("bye", union2);

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("unions").get("pojo_1#hello").get("value").asText());
        Assert.assertEquals("from pojo2", jsonNode.get("unions").get("pojo_2#bye").get("_value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertTrue(fromJson.unions.containsKey("hello"));
        Assert.assertEquals("from pojo1", fromJson.unions.get("hello").pojo_1.value);
        Assert.assertTrue(fromJson.unions.containsKey("bye"));
        Assert.assertEquals("from pojo2", fromJson.unions.get("bye").pojo_2._value);
    }

    @Test
    public void multiValuesSerializeProperly() throws JsonProcessingException
    {
        Wrapper wrapper = new Wrapper();
        Union union1 = new Union();
        union1.pojo_1 = new Pojo1("from pojo1");
        Union union11 = new Union();
        union11.pojo_1 = new Pojo1("from pojo11");

        Union union2 = new Union();
        union2.pojo_2 = new Pojo2("from pojo2");
        Union union22 = new Union();
        union22.pojo_2 = new Pojo2("from pojo22");

        wrapper.lists.put("hello", Arrays.asList(union1, union11));
        wrapper.lists.put("bye", Arrays.asList(union2, union22));

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("lists").get("pojo_1#hello").get(0).get("value").asText());
        Assert.assertEquals("from pojo11", jsonNode.get("lists").get("pojo_1#hello").get(1).get("value").asText());
        Assert.assertEquals("from pojo2", jsonNode.get("lists").get("pojo_2#bye").get(0).get("_value").asText());
        Assert.assertEquals("from pojo22", jsonNode.get("lists").get("pojo_2#bye").get(1).get("_value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertTrue(fromJson.lists.containsKey("hello"));
        Assert.assertEquals("from pojo1", fromJson.lists.get("hello").get(0).pojo_1.value);
        Assert.assertEquals("from pojo11", fromJson.lists.get("hello").get(1).pojo_1.value);
        Assert.assertTrue(fromJson.lists.containsKey("bye"));
        Assert.assertEquals("from pojo2", fromJson.lists.get("bye").get(0).pojo_2._value);
        Assert.assertEquals("from pojo22", fromJson.lists.get("bye").get(1).pojo_2._value);
    }

    public static class Wrapper
    {
        @JsonSerialize(using = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, Union> union = new HashMap<>(0);

        @JsonSerialize(using = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, Union> unions = new HashMap<>(0);

        @JsonSerialize(using = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, List<Union>> lists = new HashMap<>();
    }

    public static class Pojo1
    {
        public final String value;

        public Pojo1(@JsonProperty("value") String value)
        {
            this.value = value;
        }
    }

    public static class Pojo2
    {
        public final String _value;

        public Pojo2(@JsonProperty("_value") String _value)
        {
            this._value = _value;
        }
    }

    public static class Wrapper2
    {
        @JsonIgnore()
        public Map<String, Union> union = new HashMap<>(0);

        @JsonAnyGetter
        @JsonSerialize(using = org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.ExternalTaggedUnionSerializer.class)
        public Map<String, Union> additionalProperties()
        {
            return this.union;
        }

        @JsonAnySetter
        @JsonDeserialize(keyUsing = org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.ExternalTaggedUnionKeyDeserializer.class, contentUsing = org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.ExternalTaggedUnionContentDeserializer.class)
        public void additionalProperties(String key, Union value)
        {
            this.union.put(key, value);
        }
    }

    public static class Union
    {
        public Pojo1 pojo_1;
        public Pojo2 pojo_2;
    }
}
