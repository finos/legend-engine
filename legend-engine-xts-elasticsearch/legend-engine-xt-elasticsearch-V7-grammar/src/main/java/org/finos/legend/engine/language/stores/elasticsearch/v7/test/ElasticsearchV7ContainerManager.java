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

package org.finos.legend.engine.language.stores.elasticsearch.v7.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreURLSourceSpecification;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndex;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7EmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7IndexEmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.ElasticsearchObjectMapperProvider;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.BulkRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.IndexOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequestBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.delete.DeleteRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.ElasticHttpResponseHandler;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.ElasticsearchV7RequestToHttpRequestVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.Refresh;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.Property;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.TypeMapping;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

final class ElasticsearchV7ContainerManager
{
    private static final String TAG = "7.17.0"; // todo parameterize?
    static final ElasticsearchV7ContainerManager INSTANCE = new ElasticsearchV7ContainerManager();
    private static final String PASSWORD_SYSTEM_PROPERTY = "org.finos.legend.engine.plan.execution.stores.elasticsearch.test.password";

    private final Semaphore accessSemaphore = new Semaphore(1);
    private final CloseableHttpClient httpClient = getRestClient();
    private transient ElasticsearchContainer container;
    private transient URI uri;

    private ElasticsearchV7ContainerManager()
    {

    }

    public Pair<Connection, List<Closeable>> setupServer(Elasticsearch7Store store, ElasticsearchV7EmbeddedData data)
    {
        Assert.assertTrue(DockerClientFactory.instance().isDockerAvailable(), () -> "Environment not configured to support Elasticsearch embedded data - cannot spin docker test container");

        boolean acquired = false;

        try
        {
            acquired = this.accessSemaphore.tryAcquire(10, TimeUnit.MINUTES);
        }
        catch (InterruptedException e)
        {
            // fail to acquire...
        }

        Assert.assertTrue(acquired, () -> "Unable to acquire connection to Elasticsearch test server");

        try
        {
            URI url = this.startServerIfUnavailable();

            Elasticsearch7StoreURLSourceSpecification sourceSpecification = new Elasticsearch7StoreURLSourceSpecification();
            sourceSpecification.url = url;

            SystemPropertiesSecret password = new SystemPropertiesSecret();
            password.systemPropertyName = PASSWORD_SYSTEM_PROPERTY;

            UserPasswordAuthenticationSpecification authSpec = new UserPasswordAuthenticationSpecification();
            authSpec.username = "elastic";
            authSpec.password = password;


            Elasticsearch7StoreConnection testConnection = new Elasticsearch7StoreConnection();
            testConnection.element = store.getPath();
            testConnection.sourceSpec = sourceSpecification;
            testConnection.authSpec = authSpec;

            this.createIndicesAndInsertData(store, url, data);

            return Tuples.pair(testConnection, Collections.singletonList(new Teardown(url, data)));
        }
        catch (Throwable e)
        {
            this.accessSemaphore.release();
            throw e;
        }
    }

    private void createIndicesAndInsertData(Elasticsearch7Store store, URI url, ElasticsearchV7EmbeddedData data)
    {
        for (ElasticsearchV7IndexEmbeddedData indexEmbeddedData : data.indexData)
        {
            this.createIndex(store, url, indexEmbeddedData, data.sourceInformation);
            this.indexEmbeddedData(url, data, indexEmbeddedData);
        }
    }

    private void createIndex(Elasticsearch7Store store, URI url, ElasticsearchV7IndexEmbeddedData indexEmbeddedData, SourceInformation sourceInformation)
    {
        String index = indexEmbeddedData.index;

        Elasticsearch7StoreIndex elasticsearch7StoreIndex = store.indices.stream()
                .filter(x -> x.indexName.equals(index))
                .findAny()
                .orElseThrow(() -> new EngineException(String.format("Index '%s' not found on store '%s'", index, store.getPath()), sourceInformation, EngineErrorType.PARSER));

        Map<String, Property> properties = elasticsearch7StoreIndex.properties.stream()
                .collect(Collectors.toMap(x -> x.propertyName, x -> x.property));

        TypeMapping typeMapping = new TypeMapping();
        typeMapping.properties = properties;

        CreateRequestBody createRequestBody = new CreateRequestBody();
        createRequestBody.mappings = typeMapping;

        CreateRequest createRequest = new CreateRequest();
        createRequest.index = LiteralOrExpression.literal(index);
        createRequest.body = createRequestBody;

        this.executeRequest(url, createRequest);
    }

    private void indexEmbeddedData(URI url, ElasticsearchV7EmbeddedData data, ElasticsearchV7IndexEmbeddedData indexEmbeddedData)
    {
        Object documentsRaw;
        try
        {
            documentsRaw = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.readValue(indexEmbeddedData.documentsAsJson, Object.class);
        }
        catch (JsonProcessingException e)
        {
            throw new EngineException("Embedded data cannot be read as JSON", data.sourceInformation, EngineErrorType.PARSER, e);
        }

        List<Map<?, ?>> documents = Lists.mutable.empty();

        if (documentsRaw instanceof Map)
        {
            documents.add((Map<?, ?>) documentsRaw);
        }
        else if (documentsRaw instanceof List)
        {
            for (Object documentRaw : (List<?>) documentsRaw)
            {
                if (documentRaw instanceof Map)
                {
                    documents.add((Map<?, ?>) documentRaw);
                }
                else
                {
                    throw new EngineException("Embedded data not on right format.  Expected map inside array", data.sourceInformation, EngineErrorType.PARSER);
                }
            }
        }
        else
        {
            throw new EngineException("Embedded data not on right format.  Expected map or array", data.sourceInformation, EngineErrorType.PARSER);
        }

        List<LiteralOrExpression<Object>> operations = Lists.mutable.empty();

        for (Map<?, ?> record : documents)
        {
            Object _id = record.remove("_id");
            IndexOperation indexOperation = new IndexOperation();
            if (_id != null)
            {
                indexOperation._id = LiteralOrExpression.literal(_id.toString());
            }

            operations.add(LiteralOrExpression.literal(indexOperation));
            operations.add(LiteralOrExpression.literal(record));
        }

        BulkRequest<Object, Object> bulkRequest = new BulkRequest<Object, Object>();
        bulkRequest.operations = operations;
        bulkRequest.index = LiteralOrExpression.literal(indexEmbeddedData.index);
        bulkRequest.refresh = Refresh._true;

        this.executeRequest(url, bulkRequest);
    }

    private void executeRequest(URI url, RequestBase esRequest)
    {
        try
        {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", getTestServerPassword()));
            HttpUriRequest httpRequest = esRequest.accept(new ElasticsearchV7RequestToHttpRequestVisitor(url));
            HttpClientContext httpClientContext = HttpClientContext.create();
            httpClientContext.setCredentialsProvider(credentialsProvider);
            this.httpClient.execute(httpRequest, new ElasticHttpResponseHandler(), httpClientContext);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private URI startServerIfUnavailable()
    {
        if (this.container != null)
        {
            if (this.canReuse())
            {
                return this.uri;
            }
            else
            {
                this.tearDown();
                return this.startServerIfUnavailable();
            }
        }
        else
        {
            System.setProperty(PASSWORD_SYSTEM_PROPERTY, UUID.randomUUID().toString());

            DockerImageName image = DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "docker.elastic.co") + "/elasticsearch/elasticsearch:" + TAG)
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch:" + TAG);
            this.container = new ElasticsearchContainer(image)
                    .withPassword(getTestServerPassword());

            this.container.start();
            this.uri = URI.create("http://" + this.container.getHttpHostAddress());

            return this.uri;
        }
    }

    private boolean canReuse()
    {
        if (this.container.isRunning())
        {
            try
            {
                DeleteRequest deleteRequest = new DeleteRequest();
                deleteRequest.index = Collections.singletonList(LiteralOrExpression.literal("_all"));
                deleteRequest.ignore_unavailable = LiteralOrExpression.literal(true);
                this.executeRequest(this.uri, deleteRequest);
                return true;
            }
            catch (Exception e)
            {
                // failed, will return false to restart container
            }
        }

        return false;
    }

    void tearDown()
    {
        System.clearProperty(PASSWORD_SYSTEM_PROPERTY);
        if (container != null)
        {
            this.container.stop();
            this.container = null;
            this.uri = null;
        }
    }

    private class Teardown implements Closeable
    {

        private final URI url;
        private final ElasticsearchV7EmbeddedData data;

        private Teardown(URI url, ElasticsearchV7EmbeddedData data)
        {
            this.url = url;
            this.data = data;
        }

        @Override
        public void close()
        {
            try
            {
                DeleteRequest deleteRequest = new DeleteRequest();
                deleteRequest.index = this.data.indexData.stream().map(x -> LiteralOrExpression.literal(x.index)).collect(Collectors.toList());
                deleteRequest.ignore_unavailable = LiteralOrExpression.literal(true);
                ElasticsearchV7ContainerManager.this.executeRequest(this.url, deleteRequest);
            }
            finally
            {
                ElasticsearchV7ContainerManager.this.accessSemaphore.release();
            }
        }

    }

    private static CloseableHttpClient getRestClient()
    {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectTimeout(1000).setSocketTimeout(30000);
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .build();
    }

    private static String getTestServerPassword()
    {
        return Objects.requireNonNull(System.getProperty(PASSWORD_SYSTEM_PROPERTY), "Missing ES test cluster password system property");
    }
}
