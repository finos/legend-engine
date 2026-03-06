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
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
// implied. See the License for the specific language governing
// permissions and limitations under the License.

package org.finos.legend.engine.ide.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.mcp.protocol.v20251125.notification.Notification;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class PureIDEMcpStdioServer
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception
    {
        MutableRepositoryCodeStorage codeStorage = buildCodeStorage();
        Message message = new Message("");
        PureRuntime pureRuntime = new PureRuntimeBuilder(codeStorage).withMessage(message).setUseFastCompiler(true).build();

        FunctionExecution functionExecution = new FunctionExecutionInterpreted();
        functionExecution.init(pureRuntime, message);
        codeStorage.initialize(message);

        System.err.println("Initializing Pure runtime...");
        pureRuntime.initialize(new Message("")
        {
            @Override
            public void setMessage(String msg)
            {
                super.setMessage(msg);
                System.err.println(msg);
            }
        });
        System.err.println("Pure runtime initialized.");

        LegendStatelessMcpServerOrchestrator orch = PureIDEMcpToolDefinitions.createOrchestrator(pureRuntime, codeStorage, functionExecution);

        runStdioLoop(orch);
    }

    private static MutableRepositoryCodeStorage buildCodeStorage()
    {
        List<RepositoryCodeStorage> repos = CodeRepositoryProviderHelper.findCodeRepositories().collect(ClassLoaderCodeStorage::new)
                        .toList()
                        .stream()
                        .map(cs -> (RepositoryCodeStorage) cs)
                        .collect(Collectors.toList());

        return new CompositeCodeStorage(repos.toArray(new RepositoryCodeStorage[0]));
    }

    private static void runStdioLoop(LegendStatelessMcpServerOrchestrator orch) throws Exception
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }

            try
            {
                JsonNode node = OBJECT_MAPPER.readTree(line);

                if (node.has("id"))
                {
                    Request request = OBJECT_MAPPER.treeToValue(node, Request.class);
                    Response response = orch.handleRequest(request, Identity.getAnonymousIdentity());
                    String responseJson = OBJECT_MAPPER.writeValueAsString(response);
                    System.out.println(responseJson);
                    System.out.flush();
                }
                else
                {
                    Notification notification = OBJECT_MAPPER.treeToValue(node, Notification.class);
                    orch.handleNotification(notification, Identity.getAnonymousIdentity());
                }
            }
            catch (Exception e)
            {
                String msg = e.getMessage().replace("\"", "\\\"");
                String errorJson =
                        "{\"jsonrpc\":\"2.0\","
                                + "\"id\":null,"
                                + "\"error\":{\"code\":"
                                + "-32700"
                                + ",\"message\":"
                                + "\"Parse error: "
                                + msg + "\"}}";
                System.out.println(errorJson);
                System.out.flush();
            }
        }
    }
}
