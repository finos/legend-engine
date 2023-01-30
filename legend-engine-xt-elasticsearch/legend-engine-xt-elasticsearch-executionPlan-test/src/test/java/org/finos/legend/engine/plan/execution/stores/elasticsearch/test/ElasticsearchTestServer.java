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

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ElasticsearchTestServer extends ExternalResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTestServer.class);

    private final ElasticsearchContainer container;
    private RestClient restClient;

    public ElasticsearchTestServer(String imageTag)
    {
        this.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + imageTag);
    }

    @Override
    protected void before() throws Throwable
    {
        super.before();
        this.run();
    }

    public void run() throws Exception
    {
        this.startContainer();
        this.initRestClient();
        this.printTestClusterHealth();
        this.intTestIndex();
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

    private void intTestIndex() throws IOException, InterruptedException
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
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,  new UsernamePasswordCredentials("elastic", "s3cret"));

        this.restClient = RestClient
                .builder(HttpHost.create(this.container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
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
            Request request = new Request("PUT", "/omdb-mapped");
            String mapping = new String(IOUtils.toByteArray(resource), StandardCharsets.UTF_8);
            request.setJsonEntity(mapping);
            performRequestAndPrintOutput(this.restClient, request);
        }
    }

    private void printTestIndexMapping() throws IOException
    {
        Request request = new Request("GET", "/omdb-mapped");
        performRequestAndPrintOutput(this.restClient, request);
    }

    private void bulkInsertToTestIndex() throws IOException
    {
        try (InputStream resource = Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("omdb.json"), "mapping.json not found"))
        {
            Request request = new Request("POST", "/_bulk");
            String records = new String(IOUtils.toByteArray(resource), StandardCharsets.UTF_8);
            request.setEntity(new NStringEntity(records, ContentType.parse("application/x-ndjson")));
            performRequestAndPrintOutput(this.restClient, request);
        }
    }

    private void printTestIndexCount() throws IOException
    {
        Request request = new Request("GET", "/omdb-mapped/_count");
        performRequestAndPrintOutput(this.restClient, request);
    }

    private void printTestClusterHealth() throws IOException
    {
        Request request = new Request("GET", "/_cluster/health");
        performRequestAndPrintOutput(this.restClient, request);
    }

    private void performRequestAndPrintOutput(RestClient client, Request request) throws IOException
    {
        request.addParameter("pretty", Boolean.toString(true));
        Response response = client.performRequest(request);
        Assert.assertTrue("Response should be 2xx but got " + response,response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);
        LOGGER.info("{} -> {}", request, response);
        LOGGER.info("Output:{}{}", System.lineSeparator(), outputStream);
    }
}
