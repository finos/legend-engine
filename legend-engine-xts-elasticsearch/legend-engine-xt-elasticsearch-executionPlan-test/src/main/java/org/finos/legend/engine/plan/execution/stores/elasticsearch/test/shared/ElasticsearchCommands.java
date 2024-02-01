// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.test.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBase;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.http.ElasticsearchV7RequestToHttpRequestVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class ElasticsearchCommands
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchCommands.class);
    public static final Map<String, ElasticsearchContainer> CONTAINERS = Maps.mutable.empty();
    public static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public static String START_SERVER_FUNCTION = "startElasticsearchTestServer_String_1__URL_1_";

    public static Root_meta_pure_functions_io_http_URL startServer(String imageTag)
    {
        System.setProperty("org.finos.legend.engine.plan.execution.stores.elasticsearch.test.password", UUID.randomUUID().toString());
        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("esUrl");
        ElasticsearchContainer container = CONTAINERS.computeIfAbsent(imageTag, ElasticsearchCommands::createContainer);
        url._host(container.getHost());
        url._port(container.getMappedPort(9200));
        url._path("/");
        return url;
    }

    public static String STOP_SERVER_FUNCTION = "stopElasticsearchTestServer_String_1__Nil_0_";

    public static void stopServer(String imageTag)
    {
        System.clearProperty("org.finos.legend.engine.plan.execution.stores.elasticsearch.test.password");
        Optional.ofNullable(CONTAINERS.remove(imageTag)).ifPresent(ElasticsearchContainer::stop);
    }

    private static ElasticsearchContainer createContainer(String imageTag)
    {
        DockerImageName image = DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "docker.elastic.co") + "/elasticsearch/elasticsearch:" + imageTag)
                .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch:" + imageTag);
        ElasticsearchContainer container = new ElasticsearchContainer(image);

        long start = System.currentTimeMillis();
        container.withPassword(getPassword())
                .withEnv("xpack.security.enabled", "false")
                .start();
        LOGGER.info("ES Test cluster for version {} running on {}.  Took {}ms to start.", imageTag, container.getHttpHostAddress(), System.currentTimeMillis() - start);
        return container;
    }

    private static CloseableHttpClient getRestClient()
    {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectTimeout(1000).setSocketTimeout(30000);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", getPassword()));

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    private static String getPassword()
    {
        return Objects.requireNonNull(System.getProperty("org.finos.legend.engine.plan.execution.stores.elasticsearch.test.password"), "Missing ES test cluster password system property");
    }

    private static String execute(HttpUriRequest request)
    {
        try (CloseableHttpClient closeableHttpClient = getRestClient())
        {
            return closeableHttpClient.execute(request, new BasicResponseHandler());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static String REQUEST_SERVER_FUNCTION = "requestElasticsearchTestServer_String_1__String_1__String_1_";

    public static String request(String imageTag, String json)
    {
        try
        {
            URI url = URI.create("http://" + CONTAINERS.get(imageTag).getHttpHostAddress());

            switch (imageTag.charAt(0))
            {
                case '7':
                case '8':
                    return requestV7(url, json);
                default:
                    throw new RuntimeException("Version not supported yet: " + imageTag);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static String requestV7(URI url, String json) throws IOException
    {
        RequestBase requestBase = OBJECT_MAPPER.readValue(json, RequestBase.class);
        HttpUriRequest httpRequest = requestBase.accept(new ElasticsearchV7RequestToHttpRequestVisitor(url));
        return execute(httpRequest);
    }
}
