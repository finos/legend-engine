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

package org.finos.legend.engine.ide;

import io.dropwizard.core.Application;
import io.dropwizard.testing.DropwizardTestSupport;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestEmptyPathSegments
{
    @TempDir
    static Path directory;

    private static Path config;

    @BeforeAll
    public static void writeWelcomeFileAndConfig() throws Exception
    {
        Files.write(directory.resolve("welcome.pure"), "function go():Any[*]\n{\n  1\n}\n".getBytes(StandardCharsets.UTF_8));
        config = Files.write(directory.resolve("config.json"), ("{\n" +
                "  \"logging\": {\"level\": \"warn\"},\n" +
                "  \"swagger\": {\"title\": \"Legend Pure IDE Light\", \"resourcePackage\": \"org.finos.legend\", \"uriPrefix\": \"/\"},\n" +
                "  \"server\": {\n" +
                "    \"type\": \"simple\",\n" +
                "    \"applicationContextPath\": \"/\",\n" +
                "    \"adminContextPath\": \"/admin\",\n" +
                "    \"connector\": {\"type\": \"http\", \"port\": 0},\n" +
                "    \"requestLog\": {\"appenders\": []}\n" +
                "  },\n" +
                "  \"debugMode\": false,\n" +
                "  \"initPureRuntimeBeforeStart\": false,\n" +
                "  \"sourceLocationConfiguration\": {\"welcomeFileDirectory\": \"" + directory.toString().replace("\\", "\\\\") + "\"}\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(classes = {PureIDELight_NoExtension.class, MinimalPureIDEServer.class})
    public void aFilePathStartingWithASlashIsServed(Class<? extends Application<ServerConfiguration>> ide) throws Exception
    {
        HttpResponse<String> response = get(ide, "/fileAsJson//welcome.pure");

        Assertions.assertEquals(200, response.statusCode(), response.body());
        Assertions.assertTrue(response.body().contains("function go():Any[*]"), response.body());
    }

    @ParameterizedTest
    @ValueSource(classes = {PureIDELight_NoExtension.class, MinimalPureIDEServer.class})
    public void anEncodedSlashIsStillRejected(Class<? extends Application<ServerConfiguration>> ide) throws Exception
    {
        HttpResponse<String> response = get(ide, "/fileAsJson/%2Fwelcome.pure");

        Assertions.assertEquals(400, response.statusCode(), response.body());
        Assertions.assertTrue(response.body().contains("Ambiguous URI path separator"), response.body());
    }

    private static HttpResponse<String> get(Class<? extends Application<ServerConfiguration>> ide, String path) throws Exception
    {
        DropwizardTestSupport<ServerConfiguration> server = new DropwizardTestSupport<>(ide, config.toString());
        server.before();
        try
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getLocalPort() + path)).GET().build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        }
        finally
        {
            server.after();
        }
    }

    public static class MinimalPureIDEServer extends PureIDEServer
    {
        @Override
        protected MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration)
        {
            return Lists.mutable.empty();
        }
    }
}
