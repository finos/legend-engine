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

package org.finos.legend.engine.server.test.shared;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class TestMetaDataServerRequestParsing
{
    private static final String MAPPING_BASE = "/alloy/pureModelFromMapping";

    @Test
    public void kerberosAuthSuffixIsNotPartOfTheElementPath() throws Exception
    {
        assertEquals("v1_33_0 | my::pkg::MyMapping", get("/v1_33_0/my::pkg::MyMapping?auth=kerberos"));
    }

    @Test
    public void plainRequestStillParses() throws Exception
    {
        assertEquals("v1_33_0 | my::pkg::MyMapping", get("/v1_33_0/my::pkg::MyMapping"));
    }

    private static String get(String pathAndQuery) throws Exception
    {
        AtomicReference<String> seen = new AtomicReference<>();
        Server server = new Server(0);
        server.setHandler(TestMetaDataServer.registerService(MAPPING_BASE, (_package, version) ->
        {
            seen.set(version + " | " + _package);
            return "ok";
        }));
        server.start();
        try
        {
            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + MAPPING_BASE + pathAndQuery).openConnection();
            try
            {
                assertEquals(200, connection.getResponseCode());
                assertEquals("ok", read(connection.getInputStream()));
            }
            finally
            {
                connection.disconnect();
            }
            return seen.get();
        }
        finally
        {
            server.stop();
        }
    }

    private static String read(InputStream stream) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int read = stream.read(buffer); read != -1; read = stream.read(buffer))
        {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
