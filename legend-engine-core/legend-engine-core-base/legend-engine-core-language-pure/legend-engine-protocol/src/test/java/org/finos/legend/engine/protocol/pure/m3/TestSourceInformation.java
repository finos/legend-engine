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

package org.finos.legend.engine.protocol.pure.m3;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * A position reads from the object the engine writes and from the compact string the Rust grammar parser writes.
 */
public class TestSourceInformation
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class Holder
    {
        public SourceInformation sourceInformation;
    }

    @Test
    public void testObjectForm() throws Exception
    {
        Holder holder = MAPPER.readValue("{\"sourceInformation\":{\"sourceId\":\"a.pure\",\"startLine\":3,\"startColumn\":5,\"endLine\":4,\"endColumn\":2}}", Holder.class);
        assertPosition("a.pure", 3, 5, 4, 2, holder.sourceInformation);
    }

    @Test
    public void testCompactForm() throws Exception
    {
        assertPosition("a.pure", 3, 5, 4, 2, MAPPER.readValue("{\"sourceInformation\":\"a.pure:3:5-4:2\"}", Holder.class).sourceInformation);
        assertPosition("a.pure", 3, 5, 3, 12, MAPPER.readValue("{\"sourceInformation\":\"a.pure:3:5-12\"}", Holder.class).sourceInformation);
        // One line written with its end line.
        assertPosition("a.pure", 3, 5, 3, 12, MAPPER.readValue("{\"sourceInformation\":\"a.pure:3:5-3:12\"}", Holder.class).sourceInformation);
        Assert.assertNull(MAPPER.readValue("{\"sourceInformation\":null}", Holder.class).sourceInformation);
    }

    @Test
    public void testStillWritesTheObjectForm() throws Exception
    {
        Assert.assertEquals(
                "{\"sourceId\":\"a.pure\",\"startLine\":3,\"startColumn\":5,\"endLine\":3,\"endColumn\":12}",
                MAPPER.writeValueAsString(SourceInformation.fromString("a.pure:3:5-12")));
    }

    @Test
    public void testSourceIdsHoldAnything() throws Exception
    {
        // Read from the end: ':' and '-' in the id are the id's.
        for (String id : new String[]{"", "a::B", "file:///c:/x-1.pure", "model::A@derivedProperty@name@1-2", "dir\\with \"quotes\"", "4:7-13"})
        {
            String json = MAPPER.writeValueAsString(new TextNode(id + ":4:7-6:2"));
            assertPosition(id, 4, 7, 6, 2, MAPPER.readValue(json, SourceInformation.class));
            assertPosition(id, 4, 7, 4, 13, SourceInformation.fromString(id + ":4:7-13"));
        }
    }

    @Test
    public void testRefusesWhatIsNotAPosition()
    {
        for (String text : new String[]{"", "4:7-4:13", "x:4:7", "x:4:7-4:", "x:4:7:4:13", "x:4-7:4-13", "x4:7-4:13", "x:4:7--13", "x:4:7-4:13 ", "x:4:7-99999999999", "x:-4:7-13"})
        {
            IllegalArgumentException e = Assert.assertThrows(text, IllegalArgumentException.class, () -> SourceInformation.fromString(text));
            Assert.assertEquals("Not a source information: '" + text + "' (expected sourceId:line:column-column or sourceId:line:column-line:column)", e.getMessage());
            Assert.assertThrows(text, JsonMappingException.class, () -> MAPPER.readValue(MAPPER.writeValueAsString(new TextNode(text)), SourceInformation.class));
        }
    }

    private static void assertPosition(String sourceId, int startLine, int startColumn, int endLine, int endColumn, SourceInformation actual)
    {
        Assert.assertEquals(sourceId, actual.sourceId);
        Assert.assertEquals(startLine, actual.startLine);
        Assert.assertEquals(startColumn, actual.startColumn);
        Assert.assertEquals(endLine, actual.endLine);
        Assert.assertEquals(endColumn, actual.endColumn);
    }
}
