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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

public class DictionarySerialization
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void readAndWritesDictionaryEntriesCorrectly() throws Exception
    {
        Pojo pojo = new Pojo();
        pojo.singleEntries = new DictionaryEntry<>("123", new Person("jose"));
        pojo.multiEntries = Arrays.asList(
                new DictionaryEntry<>("abc", new Person("juan")),
                new DictionaryEntry<>("xyz", new Person("carlos"))
        );
        pojo.groupEntries = new DictionaryEntry<>("group",
                Arrays.asList(
                        new Person("jose"),
                        new Person("juan")
                )
        );

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(pojo);
        Pojo pojoFromJson = OBJECT_MAPPER.convertValue(jsonNode, Pojo.class);
        Assert.assertEquals(pojo, pojoFromJson);

        Assert.assertEquals("jose", jsonNode.get("singleEntries").get("123").get("name").asText());
        Assert.assertEquals("juan", jsonNode.get("multiEntries").get("abc").get("name").asText());
        Assert.assertEquals("carlos", jsonNode.get("multiEntries").get("xyz").get("name").asText());
        Assert.assertEquals("jose", jsonNode.get("groupEntries").get("group").get(0).get("name").asText());
        Assert.assertEquals("juan", jsonNode.get("groupEntries").get("group").get(1).get("name").asText());
    }

    @Test
    public void handleEmptyAndNullDictionaryEntries() throws Exception
    {
        Pojo pojo = new Pojo();
        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(pojo);
        Pojo pojoFromJson = OBJECT_MAPPER.convertValue(jsonNode, Pojo.class);
        Assert.assertEquals(pojo, pojoFromJson);

        Assert.assertTrue(jsonNode.path("singleEntries").isMissingNode());
        Assert.assertTrue(jsonNode.path("multiEntries").isMissingNode());
        Assert.assertTrue(jsonNode.path("groupEntries").isMissingNode());
    }

    public static class Pojo
    {
        @JsonSerialize(using = DictionarySerializer.class)
        @JsonDeserialize(using = DictionaryDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<DictionaryEntry<Person>> multiEntries = Collections.emptyList();

        @JsonSerialize(using = DictionarySerializer.class)
        @JsonDeserialize(using = DictionaryDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public DictionaryEntry<Person> singleEntries;

        @JsonSerialize(using = DictionarySerializer.class)
        @JsonDeserialize(using = DictionaryDeserializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public DictionaryEntry<List<Person>> groupEntries;

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            Pojo pojo = (Pojo) o;
            return Objects.equals(multiEntries, pojo.multiEntries) && Objects.equals(singleEntries, pojo.singleEntries) && Objects.equals(groupEntries, pojo.groupEntries);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(multiEntries, singleEntries, groupEntries);
        }
    }

    public static class Person
    {
        public String name;

        public Person()
        {
        }

        public Person(String name)
        {
            this.name = name;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            Person person = (Person) o;
            return Objects.equals(name, person.name);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name);
        }
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

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            DictionaryEntry<?> that = (DictionaryEntry<?>) o;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(key, value);
        }
    }
}
