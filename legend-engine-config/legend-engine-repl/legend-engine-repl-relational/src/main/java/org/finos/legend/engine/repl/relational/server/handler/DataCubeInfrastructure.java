// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.relational.server.handler;

import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.repl.relational.server.REPLServer;

import java.io.InputStream;
import java.io.OutputStream;

import static org.finos.legend.engine.repl.relational.server.REPLServerHelpers.*;

public class DataCubeInfrastructure
{
    public static class GridLicenseKey implements DataCubeServerHandler
    {
        @Override
        public HttpHandler getHandler(REPLServerState state)
        {
            return exchange ->
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        String licenseKey = System.getProperty("legend.repl.dataCube.gridLicenseKey") == null ? "" : System.getProperty("legend.repl.dataCube.gridLicenseKey");
                        String key = state.objectMapper.writeValueAsString(licenseKey);
                        handleResponse(exchange, 200, key, state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class StaticContent implements DataCubeServerHandler
    {
        @Override
        public HttpHandler getHandler(REPLServerState state)
        {
            return exchange ->
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    String[] path = exchange.getRequestURI().getPath().split("/repl/");
                    String resourcePath = "/web-content/dist/repl/" + (path[1].equals("dataCube") ? "index.html" : path[1]);
                    try (OutputStream os = exchange.getResponseBody();
                         InputStream is = REPLServer.class.getResourceAsStream(resourcePath)
                    )
                    {
                        if (is == null)
                        {
                            exchange.sendResponseHeaders(404, -1);
                        }
                        else
                        {
                            if (resourcePath.endsWith(".html"))
                            {
                                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                            }
                            if (resourcePath.endsWith(".js"))
                            {
                                exchange.getResponseHeaders().add("Content-Type", "text/javascript; charset=utf-8");
                            }
                            else if (resourcePath.endsWith(".css"))
                            {
                                exchange.getResponseHeaders().add("Content-Type", "text/css; charset=utf-8");
                            }

                            exchange.sendResponseHeaders(200, 0);
                            IOUtils.copy(is, os);
                        }
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }
}
