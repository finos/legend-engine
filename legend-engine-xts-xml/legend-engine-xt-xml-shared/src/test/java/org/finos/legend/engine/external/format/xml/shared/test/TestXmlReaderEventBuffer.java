// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.xml.shared.test;

import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Tests for XmlReader's dynamic buffer growth.
 */
public class TestXmlReaderEventBuffer
{

    @Test
    public void testBufferGrowth()
    {
        // Create XML with enough nesting to exceed 4096 events but stay under 8192
        int depth = 4000;
        String xml = generateNestedXml(depth);

        XmlReader reader = createReader(xml);

        try (XmlReader.Transaction tx = reader.newTransaction())
        {
            // Read through all events - this should trigger buffer growth to 2x initial buffer size.
            int eventCount = 0;
            while (reader.hasNext())
            {
                reader.next();
                eventCount++;
            }

            Assert.assertTrue("Should have read > 4096 events", eventCount > 4096);
            Assert.assertTrue("Should have read < 8192 events", eventCount < 8192);
        }

        // After rollback, should be able to read again from start
        Assert.assertTrue(reader.hasNext());
    }

    @Test
    public void testBufferLimitExceeded()
    {
        // Create XML with enough nesting to exceed 16384 events (max buffer size)
        int depth = 9000;
        String xml = generateNestedXml(depth);

        XmlReader reader = createReader(xml);

        try (XmlReader.Transaction tx = reader.newTransaction())
        {
            // Try to read through all events - this should throw an exception
            Assert.assertThrows(IllegalStateException.class, () ->
            {
                while (reader.hasNext())
                {
                    reader.next();
                }
            });
        }
    }

    /**
     * Helper method to generate deeply nested XML for testing buffer growth.
     * Creates XML like: <level0><level1><level2>...data...</level2></level1></level0>
     */
    private String generateNestedXml(int depth)
    {
        StringBuilder xml = new StringBuilder();

        // Opening tags
        for (int i = 0; i < depth; i++)
        {
            xml.append("<level").append(i).append(">");
        }

        // Content at the deepest level
        xml.append("data");

        // Closing tags
        for (int i = depth - 1; i >= 0; i--)
        {
            xml.append("</level").append(i).append(">");
        }

        return xml.toString();
    }

    /**
     * Helper method to create an XmlReader from a string.
     */
    private XmlReader createReader(String xml)
    {
        InputStream stream = new ByteArrayInputStream(xml.getBytes());
        return XmlReader.newReader(stream, "test");
    }
}
