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

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.ElasticsearchObjectMapperProvider;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.BulkRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.CreateOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.DeleteOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.IndexOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.OperationBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.OperationBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.OperationType;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.UpdateOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.WriteOperation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.closepointintime.ClosePointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.count.CountRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.index.IndexRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.openpointintime.OpenPointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.delete.DeleteRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.get.GetRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.AbstractRequestBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBase;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

public class ElasticsearchV7RequestToHttpRequestVisitor extends AbstractRequestBaseVisitor<HttpUriRequest>
{
    private static final ObjectMapper PROTOCOL_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper();
    private final URI url;
    private final ExecutionState executionState;

    public ElasticsearchV7RequestToHttpRequestVisitor(URI url)
    {
        this(url, null);
    }

    public ElasticsearchV7RequestToHttpRequestVisitor(URI url, ExecutionState executionState)
    {
        this.url = url;
        this.executionState = executionState;
    }

    @Override
    protected HttpUriRequest defaultValue(RequestBase val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpUriRequest visit(BulkRequest val)
    {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out, true);

            boolean parseAsProtocol = true;
            for (Object operation : val.operations)
            {
                if (parseAsProtocol)
                {
                    operation = PROTOCOL_MAPPER.convertValue(((LiteralOrExpression) operation).getLiteral(), OperationBase.class).accept(new OperationBaseVisitor<Object>()
                    {
                        @Override
                        public Object visit(CreateOperation val)
                        {
                            return Collections.singletonMap(OperationType.create, val);
                        }

                        @Override
                        public Object visit(DeleteOperation val)
                        {
                            return Collections.singletonMap(OperationType.delete, val);
                        }

                        @Override
                        public Object visit(IndexOperation val)
                        {
                            return Collections.singletonMap(OperationType.index, val);
                        }

                        @Override
                        public Object visit(UpdateOperation val)
                        {
                            return Collections.singletonMap(OperationType.update, val);
                        }

                        @Override
                        public Object visit(WriteOperation val)
                        {
                            throw new UnsupportedOperationException("should not happen");
                        }
                    });
                    parseAsProtocol = false;
                }
                else
                {
                    parseAsProtocol = true;
                }
                ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writeValue(writer, operation);
                writer.print('\n');
            }

            writer.close();

            String path = "/_bulk";
            if (val.index != null)
            {
                path = "/" + val.index + path;
            }

            path += "?refresh=" + (val.refresh == null ? false : val.refresh.esName());

            if (val.wait_for_active_shards != null)
            {
                path += "&wait_for_active_shards=" + val.wait_for_active_shards.unionValue();
            }

            HttpPost httpPost = new HttpPost(this.url + path);
            httpPost.setEntity(new EntityWithToString(out.toByteArray(), ContentType.create("application/x-ndjson")));
            return httpPost;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public HttpUriRequest visit(OpenPointInTimeRequest val)
    {
        return new HttpPost(this.url + "/" + indexName(val.index) + "/_pit?keep_alive=" + val.keep_alive.unionValue());
    }

    private static String indexName(List<LiteralOrExpression<String>> index)
    {
        return index.stream().map(LiteralOrExpression::getLiteral).collect(Collectors.joining(","));
    }

    @Override
    public HttpUriRequest visit(ClosePointInTimeRequest val)
    {
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase()
        {
            @Override
            public String getMethod()
            {
                return HttpDelete.METHOD_NAME;
            }
        };
        request.setURI(URI.create(this.url + "/_pit"));
        return setEntity(request, val.body);
    }

    @Override
    public HttpUriRequest visit(CountRequest val)
    {
        HttpPost httpPost = new HttpPost(this.url + "/" + indexName(val.index) + "/_count");
        return setEntity(httpPost, val.body);
    }

    @Override
    public HttpUriRequest visit(CreateRequest val)
    {
        HttpPut request = new HttpPut(url + "/" + val.index.getLiteral());
        return setEntity(request, val.body);
    }

    @Override
    public HttpUriRequest visit(DeleteRequest val)
    {
        String uri = this.url + "/" + indexName(val.index);
        if (val.ignore_unavailable != null)
        {
            uri += "?ignore_unavailable=" + val.ignore_unavailable;
        }
        return new HttpDelete(uri);
    }

    @Override
    public HttpUriRequest visit(GetRequest val)
    {
        return new HttpGet(this.url + "/" + indexName(val.index));
    }

    @Override
    public HttpUriRequest visit(IndexRequest val)
    {
        HttpEntityEnclosingRequestBase request;
        if (val.id == null)
        {
            request = new HttpPost(url + "/" + val.index + "/_doc");
        }
        else
        {
            request = new HttpPut(url + "/" + val.index + "/_doc/" + val.id);
        }
        return setEntity(request, val.document);
    }

    @Override
    public HttpUriRequest visit(SearchRequest val)
    {
        HttpPost httpPost = new HttpPost(this.url + "/" + indexName(val.index) + "/_search?typed_keys=true");
        return setEntity(httpPost, val.body);
    }

    private HttpUriRequest setEntity(HttpEntityEnclosingRequestBase request, Object body)
    {
        try
        {
            String template = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(body);
            String content = template;
            if (this.executionState != null)
            {
                content = FreeMarkerExecutor.process(template, this.executionState);
            }
            request.setEntity(new EntityWithToString(content.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON));
            return request;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class EntityWithToString extends ByteArrayEntity
    {
        public EntityWithToString(byte[] content, ContentType contentType) throws UnsupportedCharsetException
        {
            super(content, contentType);
        }

        @Override
        public String toString()
        {
            return new String(this.content);
        }
    }
}
