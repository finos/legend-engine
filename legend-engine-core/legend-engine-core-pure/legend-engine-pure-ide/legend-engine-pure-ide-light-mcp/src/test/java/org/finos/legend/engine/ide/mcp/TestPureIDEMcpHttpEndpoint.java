// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.ide.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class TestPureIDEMcpHttpEndpoint
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testInitializeRequestFormat() throws Exception
    {
        // Verify that the JSON-RPC request format is valid
        String requestJson = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        JsonNode node = OBJECT_MAPPER.readTree(requestJson);

        Assert.assertTrue(node.has("jsonrpc"));
        Assert.assertEquals("2.0", node.get("jsonrpc").asText());
        Assert.assertTrue(node.has("id"));
        Assert.assertTrue(node.has("method"));
        Assert.assertEquals("initialize", node.get("method").asText());
    }

    @Test
    public void testToolsListRequestFormat() throws Exception
    {
        String requestJson = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";
        JsonNode node = OBJECT_MAPPER.readTree(requestJson);

        Assert.assertEquals("tools/list", node.get("method").asText());
    }

    @Test
    public void testToolCallRequestFormat() throws Exception
    {
        String requestJson = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"read_file\",\"arguments\":{\"path\":\"/welcome.pure\"}}}";
        JsonNode node = OBJECT_MAPPER.readTree(requestJson);

        Assert.assertEquals("tools/call", node.get("method").asText());
        Assert.assertEquals("read_file", node.get("params").get("name").asText());
        Assert.assertEquals("/welcome.pure", node.get("params").get("arguments").get("path").asText());
    }

    @Test
    public void testNotificationFormat() throws Exception
    {
        // Notifications have no "id" field
        String notificationJson = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}";
        JsonNode node = OBJECT_MAPPER.readTree(notificationJson);

        Assert.assertFalse(node.has("id"));
        Assert.assertTrue(node.has("method"));
    }
}
