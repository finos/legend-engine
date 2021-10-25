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

package org.finos.legend.engine.plan.execution.stores.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class TestServer
{
    private final Server server;

    public TestServer(int port, String resourcePath) throws Exception
    {
        this.server = new Server(port);

        Objects.requireNonNull(TestServer.class.getResource(resourcePath));
        Map<String, String> pathContentMap = new ObjectMapper().readValue(TestServer.class.getResourceAsStream(resourcePath), Map.class);

        List<Handler> handlers = Lists.mutable.empty();
        pathContentMap.forEach((path, content) -> handlers.add(registerService(path, content)));

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(handlers.toArray(new Handler[0]));

        this.server.setHandler(handlerCollection);
        this.server.start();

        System.out.println("Started Test MetaData server on port " + port);
    }

    public void shutDown() throws Exception
    {
        this.server.stop();
    }

    private static AbstractHandler registerService(String path, String content)
    {
        String pathWithoutQueryParams = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
        MutableMap<String, List<String>> map = Maps.mutable.empty();

        if (path.contains("?"))
        {
            String queryParams = path.substring(path.indexOf("?") + 1);
            Lists.mutable.with(queryParams.split("&")).forEach(keyVal -> {
                String key = keyVal.substring(0, keyVal.indexOf("="));
                String value = keyVal.substring(keyVal.indexOf("=") + 1);
                map.getIfAbsentPut(key, ArrayList::new).add(value);
            });
        }

        ContextHandler contextHandler = new ContextHandler(pathWithoutQueryParams);
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                validateRequestParams(map, request.getParameterMap());
                OutputStream stream = httpServletResponse.getOutputStream();
                stream.write(content.getBytes());
                stream.flush();
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private static void validateRequestParams(Map<String, List<String>> expected, Map<String, String[]> actual)
    {
        Set<String> expectedKeys = expected.keySet();
        Set<String> actualKeys = actual.keySet();

        if(!Objects.equals(expectedKeys, actualKeys))
        {
            throw new RuntimeException("Unexpected parameters for request. Expected keys - " + expectedKeys + ". Actual keys - " + actualKeys);
        }
        expectedKeys.forEach(key -> {
            List<String> expectedValue = expected.get(key);
            List<String> actualValue = Lists.mutable.with(actual.get(key));

            if (!Objects.equals(expectedValue, actualValue))
            {
                throw new RuntimeException("Unexpected values for key : " + key + ". Expected values - " + expectedValue + ". Actual values - " + actualValue);
            }
        });
    }
}
