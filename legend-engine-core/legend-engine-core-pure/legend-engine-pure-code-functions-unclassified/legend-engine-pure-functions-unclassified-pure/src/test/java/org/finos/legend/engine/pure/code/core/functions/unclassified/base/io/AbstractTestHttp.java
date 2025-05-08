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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.io;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class AbstractTestHttp extends AbstractPureTestWithCoreCompiled
{
    private static HttpServer httpServer;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/test", AbstractTestHttp::handle);
        httpServer.start();
    }

    @AfterClass
    public static void stopHttp()
    {
        httpServer.stop(0);
    }

    private static void handle(HttpExchange exchange) throws IOException
    {
        exchange.sendResponseHeaders(200, 0);
        exchange.close();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testHttp.pure");
        runtime.compile();
    }

    @Test
    public void testHttpGet()
    {
        compileTestSource("testHttp.pure", "function testHttp():Any[*]\n" +
                "{\n" +
                "    assert(200 == meta::pure::functions::io::http::executeHTTPRaw(^meta::pure::functions::io::http::URL(host='127.0.0.1', port=" + httpServer.getAddress().getPort() + ", path='/test'), meta::pure::functions::io::http::HTTPMethod.GET, [], []).statusCode, |'error');\n" +
                "}\n");
        this.execute("testHttp():Any[*]");
    }

    @Test
    public void testHttpGetWithScheme()
    {
        compileTestSource("testHttp.pure", "function testHttp():Any[*]\n" +
                "{\n" +
                "    assert(200 == meta::pure::functions::io::http::executeHTTPRaw(^meta::pure::functions::io::http::URL(scheme=meta::pure::functions::io::http::URLScheme.http, host='127.0.0.1', port=" + httpServer.getAddress().getPort() + ", path='/test'), meta::pure::functions::io::http::HTTPMethod.GET, [], []).statusCode, |'error');\n" +
                "}\n");
        this.execute("testHttp():Any[*]");
    }
}
