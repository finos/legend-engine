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

package org.finos.legend.engine.plan.execution.stores.elasticsearch.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.nio.entity.NStringEntity;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequestBody;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticsearchTestServer extends ExternalResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTestServer.class);
    public static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper();

    private final ElasticsearchContainer container;
    private CloseableHttpClient restClient;

    public ElasticsearchTestServer(String imageTag)
    {
        this.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + imageTag);
    }

    @Override
    protected void before() throws Throwable
    {
        super.before();
        Assume.assumeTrue(DockerClientFactory.instance().isDockerAvailable());
        this.run();
    }

    public void run() throws Exception
    {
        this.startContainer();
        this.initRestClient();
        this.printTestClusterHealth();
        this.initTestIndex();
    }

    @Override
    protected void after()
    {
        try
        {
            this.restClient.close();
        }
        catch (IOException ignore)
        {
            // ignore
        }
        this.container.close();
        super.after();
    }

    private void initTestIndex() throws IOException, InterruptedException
    {
        this.createTestIndex();
        this.printTestIndexMapping();
        this.bulkInsertToTestIndex();
        // wait for insert to propagate
        Thread.sleep(1000);
        this.printTestIndexCount();
    }

    private void initRestClient()
    {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectTimeout(1000).setSocketTimeout(30000);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,  new UsernamePasswordCredentials("elastic", "s3cret"));

        this.restClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    private void startContainer()
    {
        this.container.withPassword("s3cret").start();
        LOGGER.info("ES Test cluster running on: " + container.getHttpHostAddress());
    }

    private void createTestIndex() throws IOException
    {
        try (InputStream resource = Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("mapping.json"), "mapping.json not found"))
        {
            CreateRequestBody createRequestBody = OBJECT_MAPPER.readValue(resource, CreateRequestBody.class);
            HttpPut httpPut = new HttpPut(this.container.getHttpHostAddress() + "/omdb-mapped");
            httpPut.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(createRequestBody), ContentType.APPLICATION_JSON));
            performRequestAndPrintOutput(this.restClient, httpPut);
        }
    }

    private void printTestIndexMapping() throws IOException
    {
        HttpGet get = new HttpGet(this.container.getHttpHostAddress() + "/omdb-mapped");
        performRequestAndPrintOutput(this.restClient, get);
    }

    private void bulkInsertToTestIndex() throws IOException
    {
        try (InputStream resource = Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("omdb.json"), "mapping.json not found"))
        {
            HttpPost request = new HttpPost(this.container.getHttpHostAddress() + "/_bulk");
            String records = new String(IOUtils.toByteArray(resource), StandardCharsets.UTF_8);
            request.setEntity(new NStringEntity(records, ContentType.parse("application/x-ndjson")));
            performRequestAndPrintOutput(this.restClient, request);
        }
    }

    private void printTestIndexCount() throws IOException
    {
        HttpGet get = new HttpGet(this.container.getHttpHostAddress() + "/omdb-mapped/_count");
        performRequestAndPrintOutput(this.restClient, get);
    }

    private void printTestClusterHealth() throws IOException
    {
        HttpGet get = new HttpGet(this.container.getHttpHostAddress() + "/_cluster/health");
        performRequestAndPrintOutput(this.restClient, get);
    }

    private void performRequestAndPrintOutput(CloseableHttpClient client, HttpRequestBase request) throws IOException
    {
//        request.addParameter("pretty", Boolean.toString(true));
        String response = client.execute(request, new BasicResponseHandler());
        LOGGER.info("{} -> {}", request, response);
    }
}
