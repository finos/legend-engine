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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ExternalTaggedUnionSerializationTest
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void singleValueSerializeProperly() throws JsonProcessingException
    {
        Wrapper wrapper = new Wrapper();
        Union union = new Union();
        union.pojo_1 = new Pojo1("from pojo1");
        wrapper.union = new DictionaryEntry<>("hello", union);

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("union").get("pojo_1#hello").get("value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertEquals("hello", fromJson.union.key);
        Assert.assertEquals("from pojo1", fromJson.union.value.pojo_1.value);
    }

    @Test
    public void multiValueSerializeProperly() throws JsonProcessingException
    {
        Wrapper wrapper = new Wrapper();
        Union union1 = new Union();
        union1.pojo_1 = new Pojo1("from pojo1");
        Union union2 = new Union();
        union2.pojo_2 = new Pojo2("from pojo2");
        wrapper.unions = Arrays.asList(
                new DictionaryEntry<>("hello", union1),
                new DictionaryEntry<>("bye", union2)
        );

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("unions").get("pojo_1#hello").get("value").asText());
        Assert.assertEquals("from pojo2", jsonNode.get("unions").get("pojo_2#bye").get("_value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertEquals("hello", fromJson.unions.get(0).key);
        Assert.assertEquals("from pojo1", fromJson.unions.get(0).value.pojo_1.value);
        Assert.assertEquals("bye", fromJson.unions.get(1).key);
        Assert.assertEquals("from pojo2", fromJson.unions.get(1).value.pojo_2._value);
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

        wrapper.lists = Arrays.asList(
                new DictionaryEntry<>("hello", Arrays.asList(union1, union11)),
                new DictionaryEntry<>("bye", Arrays.asList(union2, union22))
        );

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(wrapper);
        Assert.assertEquals("from pojo1", jsonNode.get("lists").get("pojo_1#hello").get(0).get("value").asText());
        Assert.assertEquals("from pojo11", jsonNode.get("lists").get("pojo_1#hello").get(1).get("value").asText());
        Assert.assertEquals("from pojo2", jsonNode.get("lists").get("pojo_2#bye").get(0).get("_value").asText());
        Assert.assertEquals("from pojo22", jsonNode.get("lists").get("pojo_2#bye").get(1).get("_value").asText());
        Wrapper fromJson = OBJECT_MAPPER.convertValue(jsonNode, Wrapper.class);
        Assert.assertEquals("hello", fromJson.lists.get(0).key);
        Assert.assertEquals("from pojo1", fromJson.lists.get(0).value.get(0).pojo_1.value);
        Assert.assertEquals("from pojo11", fromJson.lists.get(0).value.get(1).pojo_1.value);
        Assert.assertEquals("bye", fromJson.lists.get(1).key);
        Assert.assertEquals("from pojo2", fromJson.lists.get(1).value.get(0).pojo_2._value);
        Assert.assertEquals("from pojo22", fromJson.lists.get(1).value.get(1).pojo_2._value);
    }

    public static class Wrapper
    {
        @JsonSerialize(using = ExternalTaggedUnionSerializer.class, nullsUsing = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        public DictionaryEntry<Union> union;

        @JsonSerialize(using = ExternalTaggedUnionSerializer.class, nullsUsing = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        public List<DictionaryEntry<Union>> unions;

        @JsonSerialize(using = ExternalTaggedUnionSerializer.class, nullsUsing = ExternalTaggedUnionSerializer.class)
        @JsonDeserialize(using = ExternalTaggedUnionDeserializer.class)
        public List<DictionaryEntry<List<Union>>> lists;
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

    public static class Union
    {
        public Pojo1 pojo_1;
        public Pojo2 pojo_2;
    }

    public static class DictionaryEntry<V>
    {
        public String key;
        public V value;

        public DictionaryEntry()
        {
        }

        public DictionaryEntry(String key, V value)
        {
            this.key = key;
            this.value = value;
        }
    }
}
