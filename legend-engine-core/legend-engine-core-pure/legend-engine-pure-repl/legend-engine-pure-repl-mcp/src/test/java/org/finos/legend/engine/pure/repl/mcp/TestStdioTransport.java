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

package org.finos.legend.engine.pure.repl.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.ContentBlock;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.TextContent;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestStdioTransport
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testInitializeRequestResponse() throws Exception
    {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}\n";
        String output = runTransport(input);

        JsonNode response = objectMapper.readTree(output.trim());
        Assert.assertEquals("2.0", response.get("jsonrpc").asText());
        Assert.assertEquals(1, response.get("id").asInt());
        Assert.assertNotNull(response.get("result"));
        Assert.assertNotNull(response.get("result").get("serverInfo"));
        Assert.assertEquals("test-server", response.get("result").get("serverInfo").get("name").asText());
    }

    @Test
    public void testPingRequest() throws Exception
    {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\",\"params\":{}}\n";
        String output = runTransport(input);

        JsonNode response = objectMapper.readTree(output.trim());
        Assert.assertEquals(2, response.get("id").asInt());
        Assert.assertNotNull(response.get("result"));
    }

    @Test
    public void testToolsListRequest() throws Exception
    {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{}}\n";
        String output = runTransport(input);

        JsonNode response = objectMapper.readTree(output.trim());
        Assert.assertEquals(3, response.get("id").asInt());
        Assert.assertNotNull(response.get("result"));
        Assert.assertNotNull(response.get("result").get("tools"));
        Assert.assertEquals(1, response.get("result").get("tools").size());
        Assert.assertEquals("echo", response.get("result").get("tools").get(0).get("name").asText());
    }

    @Test
    public void testToolCallRequest() throws Exception
    {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"message\":\"hello\"}}}\n";
        String output = runTransport(input);

        JsonNode response = objectMapper.readTree(output.trim());
        Assert.assertEquals(4, response.get("id").asInt());
        Assert.assertNotNull(response.get("result"));
        Assert.assertNotNull(response.get("result").get("content"));
        Assert.assertEquals("echo: hello", response.get("result").get("content").get(0).get("text").asText());
    }

    @Test
    public void testNotificationProducesNoOutput() throws Exception
    {
        // Notification has no "id" field - should produce no output
        String input = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}\n";
        String output = runTransport(input);

        Assert.assertTrue("Notification should produce no output", output.trim().isEmpty());
    }

    @Test
    public void testMalformedJsonReturnsError() throws Exception
    {
        String input = "not valid json\n";
        String output = runTransport(input);

        JsonNode response = objectMapper.readTree(output.trim());
        Assert.assertEquals("2.0", response.get("jsonrpc").asText());
        Assert.assertNotNull(response.get("error"));
        Assert.assertEquals(-32700, response.get("error").get("code").asInt());
    }

    @Test
    public void testEmptyLinesAreSkipped() throws Exception
    {
        String input = "\n\n{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"ping\",\"params\":{}}\n\n";
        String output = runTransport(input);

        // Should only have one response line
        String[] lines = output.trim().split("\n");
        Assert.assertEquals(1, lines.length);
        JsonNode response = objectMapper.readTree(lines[0]);
        Assert.assertEquals(5, response.get("id").asInt());
    }

    @Test
    public void testEofTerminatesLoop() throws Exception
    {
        // Empty input (immediate EOF)
        String output = runTransport("");
        Assert.assertTrue(output.isEmpty());
    }

    @Test
    public void testMultipleRequests() throws Exception
    {
        String input = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":{}}\n" +
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\",\"params\":{}}\n";
        String output = runTransport(input);

        String[] lines = output.trim().split("\n");
        Assert.assertEquals(2, lines.length);

        JsonNode response1 = objectMapper.readTree(lines[0]);
        Assert.assertEquals(1, response1.get("id").asInt());

        JsonNode response2 = objectMapper.readTree(lines[1]);
        Assert.assertEquals(2, response2.get("id").asInt());
    }

    private String runTransport(String input) throws Exception
    {
        // Create a simple echo tool
        Tool echoTool = new Tool(
                null,
                null,
                "Echo tool",
                null,
                null,
                new Tool.Schema(null, Collections.singletonMap("message",
                        Collections.singletonMap("type", "string")),
                        Collections.singletonList("message")),
                "echo",
                null,
                null
        );

        Implementation impl = new Implementation(null, null, "test-server", null, "1.0.0", null);
        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(
                impl,
                Collections.singletonList(echoTool),
                (tool, params, identity) ->
                {
                    String message = (String) params.get("message");
                    List<ContentBlock> content = Collections.singletonList(
                            new TextContent(null, null, "echo: " + message)
                    );
                    return content;
                }
        );

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name());

        StdioTransport transport = new StdioTransport(orchestrator, Identity.getAnonymousIdentity(),
                inputStream, printStream);
        transport.run();

        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
