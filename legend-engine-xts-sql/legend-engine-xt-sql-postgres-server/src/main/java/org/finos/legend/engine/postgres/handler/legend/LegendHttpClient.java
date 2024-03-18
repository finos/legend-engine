// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.handler.legend;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicHeader;
import org.finos.legend.engine.postgres.utils.OpenTelemetry;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendHttpClient implements LegendClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendHttpClient.class);
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final TextMapSetter<HttpRequest> TEXT_MAP_SETTER = new TextMapSetter<HttpRequest>()
    {
        @Override
        public void set(@Nullable HttpRequest httpRequest, String key, String value)
        {
            httpRequest.addHeader(new BasicHeader(key, value));
        }
    };

    private final String protocol;
    private final String host;
    private final String port;

    public LegendHttpClient(String protocol, String host, String port)
    {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public InputStream executeQueryApi(String query)
    {
        LOGGER.info("executing query " + query);
        String apiPath = "/api/sql/v1/execution/executeQueryString";
        return executeApi(query, apiPath);
    }


    public InputStream executeSchemaApi(String query)
    {
        LOGGER.info("executing schema query " + query);
        String apiPath = "/api/sql/v1/execution/getSchemaFromQueryString";
        return executeApi(query, apiPath);
    }

    private InputStream executeApi(String query, String apiPath)
    {
        String uri = protocol + "://" + this.host + ":" + this.port + apiPath;
        HttpPost req = new HttpPost(uri);

        StringEntity stringEntity = new StringEntity(query, UTF_8);
        stringEntity.setContentType(TEXT_PLAIN);
        req.setEntity(stringEntity);

        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("LegendHttpClient Execute Query").startSpan();
        try (Scope scope = span.makeCurrent();)
        {
            OpenTelemetry.getPropagators().inject(Context.current(), req, TEXT_MAP_SETTER);
            HttpClient client = HttpClientBuilder.getHttpClient(new BasicCookieStore());
            HttpResponse res = client.execute(req);
            return handleResponse(query, () -> res.getEntity().getContent(), () -> res.getStatusLine().getStatusCode());

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            span.end();
        }
    }


    protected static InputStream handleResponse(String query, Callable<InputStream> responseContentSupplier, Supplier<Integer> responseStatusCodeSupplier)
    {
        String errorResponse = null;
        try
        {
            InputStream in = responseContentSupplier.call();
            if (responseStatusCodeSupplier.get() == 200)
            {
                return in;
            }
            else
            {
                errorResponse = IOUtils.toString(in, UTF_8);
                ExceptionError exceptionError = mapper.readValue(errorResponse, ExceptionError.class);
                String errorMessage;
                if ("error".equalsIgnoreCase(exceptionError.status))
                {
                    errorMessage = exceptionError.getMessage();
                    LOGGER.error("Failed to execute query: [{}], trace: [{}]", query, exceptionError.getTrace());
                }
                else
                {
                    errorMessage = String.format("Status: [%s], Response: [%s]", in, exceptionError);
                }
                throw new LegendTdsClientException(errorMessage);
            }
        }
        catch (Exception e)
        {
            if (e instanceof LegendTdsClientException)
            {
                throw (LegendTdsClientException) e;
            }
            else
            {
                String message = String.format("Unable to parse json. Execution API response status[%s]", responseStatusCodeSupplier.get());
                if (responseStatusCodeSupplier.get() != 200)
                {
                    message = String.format("%s, response: [%s]", message, errorResponse);
                }
                throw new LegendTdsClientException(message);
            }

        }
    }

}
