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

package org.finos.legend.engine.protocol.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * TaggedValue's value is a {@link CString} in the POJO but stays a plain JSON string on the wire unless the literal
 * was authored multi-line, so models written before the flag existed - and models written by anything that still
 * emits a plain string - deserialize unchanged, and models without multi-line values serialize byte-identically to
 * the legacy shape.
 */
public class TestTaggedValueCompatibility
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testLegacyPlainStringValueDeserializes() throws Exception
    {
        TaggedValue taggedValue = objectMapper.readValue("{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":\"a doc\"}", TaggedValue.class);
        Assert.assertEquals("a doc", taggedValue.value.value);
        Assert.assertFalse(taggedValue.value.multiLine);
    }

    @Test
    public void testLegacyPlainStringValueWithNewlinesDeserializes() throws Exception
    {
        TaggedValue taggedValue = objectMapper.readValue("{\"value\":\"line one\\nline two\"}", TaggedValue.class);
        Assert.assertEquals("line one\nline two", taggedValue.value.value);
        Assert.assertFalse(taggedValue.value.multiLine);
    }

    @Test
    public void testObjectValueDeserializes() throws Exception
    {
        TaggedValue taggedValue = objectMapper.readValue("{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":{\"_type\":\"string\",\"multiLine\":true,\"value\":\"line one\\nline two\"}}", TaggedValue.class);
        Assert.assertEquals("line one\nline two", taggedValue.value.value);
        Assert.assertTrue(taggedValue.value.multiLine);
    }

    @Test
    public void testObjectValueWithoutTheFlagDeserializesAsSingleLine() throws Exception
    {
        TaggedValue taggedValue = objectMapper.readValue("{\"value\":{\"_type\":\"string\",\"value\":\"a doc\"}}", TaggedValue.class);
        Assert.assertEquals("a doc", taggedValue.value.value);
        Assert.assertFalse(taggedValue.value.multiLine);
    }

    @Test
    public void testSingleLineValueSerializesToTheLegacyShape() throws Exception
    {
        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(taggedValue("a doc", false)));
        Assert.assertTrue(node.get("value").isTextual());
        Assert.assertEquals("a doc", node.get("value").asText());
    }

    @Test
    public void testMultiLineValueSerializesToAnObject() throws Exception
    {
        JsonNode value = objectMapper.readTree(objectMapper.writeValueAsString(taggedValue("line one\nline two", true))).get("value");
        Assert.assertTrue(value.isObject());
        Assert.assertEquals("string", value.get("_type").asText());
        Assert.assertTrue(value.get("multiLine").asBoolean());
        Assert.assertEquals("line one\nline two", value.get("value").asText());
    }

    @Test
    public void testBothShapesRoundTrip() throws Exception
    {
        for (TaggedValue original : new TaggedValue[] {taggedValue("a doc", false), taggedValue("line one\nline two", true)})
        {
            TaggedValue reread = objectMapper.readValue(objectMapper.writeValueAsString(original), TaggedValue.class);
            Assert.assertEquals(original.value.value, reread.value.value);
            Assert.assertEquals(original.value.multiLine, reread.value.multiLine);
        }
    }

    private static TaggedValue taggedValue(String value, boolean multiLine)
    {
        TaggedValue taggedValue = new TaggedValue();
        taggedValue.tag = new TagPtr();
        taggedValue.tag.profile = "meta::pure::profiles::doc";
        taggedValue.tag.value = "doc";
        taggedValue.value = new CString(value);
        taggedValue.value.multiLine = multiLine;
        return taggedValue;
    }
}
