// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.modelManager.sdlc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.alloy.AlloySDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.pure.PureServerLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.workspace.WorkspaceSDLCLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCollection;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.SubjectCache;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.opentracing.HttpRequestHeaderMap;
import org.slf4j.Logger;

import static io.opentracing.propagation.Format.Builtin.HTTP_HEADERS;
import static org.finos.legend.engine.shared.core.kerberos.ExecSubject.exec;

public class SDLCLoader implements ModelLoader
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SDLCLoader.class);
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private SubjectCache subjectCache = new SubjectCache(null);
    private final Supplier<Subject> subjectProvider;
    private final PureServerLoader pureLoader;
    private final AlloySDLCLoader alloyLoader;
    private final WorkspaceSDLCLoader workspaceLoader;
    private final Function<Identity, CloseableHttpClient> httpClientProvider;

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider)
    {
        this(metaDataServerConfiguration, subjectProvider, new PureServerLoader(metaDataServerConfiguration), null);
    }

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, AlloySDLCLoader alloyLoader)
    {
        this(metaDataServerConfiguration, subjectProvider, new PureServerLoader(metaDataServerConfiguration), null, alloyLoader);
    }

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, PureServerLoader pureLoader)
    {
        this(metaDataServerConfiguration, subjectProvider, pureLoader, null);
    }

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, PureServerLoader pureLoader, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        this(metaDataServerConfiguration, subjectProvider, pureLoader, httpClientProvider, new AlloySDLCLoader(metaDataServerConfiguration));
    }

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, PureServerLoader pureLoader, Function<Identity, CloseableHttpClient> httpClientProvider, AlloySDLCLoader alloyLoader)
    {
        this(subjectProvider, pureLoader, httpClientProvider, alloyLoader, new WorkspaceSDLCLoader(metaDataServerConfiguration.sdlc));
    }

    public SDLCLoader(Supplier<Subject> subjectProvider, PureServerLoader pureLoader, Function<Identity, CloseableHttpClient> httpClientProvider, AlloySDLCLoader alloyLoader, WorkspaceSDLCLoader workspaceLoader)
    {
        this.subjectProvider = subjectProvider;
        this.pureLoader = pureLoader;
        this.alloyLoader = alloyLoader;
        this.httpClientProvider = httpClientProvider;
        this.workspaceLoader = workspaceLoader;
    }

    private Subject getSubject()
    {
        if (this.subjectProvider == null)
        {
            return null;
        }
        if (!this.subjectCache.isValid())
        {
            this.subjectCache = new SubjectCache(this.subjectProvider.get());
        }
        return this.subjectCache.getSubject();
    }

    @Override
    public void setModelManager(ModelManager modelManager)
    {
        this.workspaceLoader.setModelManager(modelManager);
    }

    @Override
    public boolean shouldCache(PureModelContext context)
    {
        return this.supports(context) && (
                    (context instanceof PureModelContextCollection && isCacheableCollection((PureModelContextCollection) context)) ||
                    (isCacheablePureSDLC(((PureModelContextPointer) context).sdlcInfo) || isCacheableAlloySDLC(((PureModelContextPointer) context).sdlcInfo))
                );
    }

    private boolean isCacheableCollection(PureModelContextCollection contextCollection)
    {
        return contextCollection.getContexts().stream().allMatch(pureModelContext -> pureModelContext instanceof PureModelContextPointer) &&
            contextCollection.getContexts().stream().map(pureModelContext -> (PureModelContextPointer) pureModelContext).allMatch(pureModelContextPointer -> pureModelContextPointer.sdlcInfo instanceof AlloySDLC) &&
            contextCollection.getContexts().stream().map(pureModelContext -> (PureModelContextPointer) pureModelContext).map(pureModelContextPointer -> (AlloySDLC)pureModelContextPointer.sdlcInfo).allMatch(this::isCacheableAlloySDLC);
    }

    private boolean isCacheablePureSDLC(SDLC sdlc)
    {
        return sdlc instanceof PureSDLC;
    }

    private boolean isCacheableAlloySDLC(SDLC sdlc)
    {
        if (sdlc instanceof AlloySDLC)
        {
            return !this.alloyLoader.isLatestRevision((AlloySDLC) sdlc); // If AlloySLDC refers to latest revision, metadata can change. Hence, it should not be cached
        }
        return false;
    }

    @Override
    public PureModelContext cacheKey(PureModelContext context, Identity identity)
    {
        if(context instanceof PureModelContextPointer)
        {
            if (isCacheablePureSDLC(((PureModelContextPointer) context).sdlcInfo))
            {
                final Subject executionSubject = getSubject();
                Function0<PureModelContext> pureModelContextFunction = () -> this.pureLoader.getCacheKey(context, identity, executionSubject);
                return executionSubject == null ? pureModelContextFunction.value() : exec(executionSubject, pureModelContextFunction::value);
            }
            else
            {
                return this.alloyLoader.getCacheKey(context);
            }
        }
        else if (context instanceof PureModelContextCollection)
        {
            PureModelContextCollection pureModelContextCollection = (PureModelContextCollection) context;
            Assert.assertTrue(pureModelContextCollection.getContexts().stream().allMatch(pureModelContext -> pureModelContext instanceof PureModelContextPointer), () -> "Invalid type of PureModelContext in PureModelContextCollection for cacheKey");
            Assert.assertTrue(pureModelContextCollection.getContexts().stream().map(pureModelContext -> (PureModelContextPointer)pureModelContext).allMatch(pureModelContextPointer -> pureModelContextPointer.sdlcInfo instanceof AlloySDLC), () -> "Invalid type of SDLC in PureModelContextPointer (in PureModelContextCollection) for cacheKey");
            return pureModelContextCollection;
        }
        throw new RuntimeException("Invalid PureModelContext type for generating cache key");
    }

    @Override
    public boolean supports(PureModelContext context)
    {
        return context instanceof PureModelContextPointer || context instanceof PureModelContextCollection;
    }

    @Override
    public PureModelContextData load(Identity identity, PureModelContext ctx, String clientVersion, Span parentSpan)
    {
        SDLCFetcher fetcher = new SDLCFetcher(
                parentSpan,
                clientVersion,
                this.httpClientProvider,
                identity,
                this.pureLoader,
                this.alloyLoader,
                this.workspaceLoader
        );
        Subject subject = getSubject();
        PureModelContextData metaData;
        Assert.assertTrue(clientVersion != null, () -> "Client version should be set when pulling metadata from the metadata repository");

        if(ctx instanceof PureModelContextPointer)
        {
            PureModelContextPointer context = (PureModelContextPointer) ctx;
            metaData = subject == null ? context.sdlcInfo.accept(fetcher) : exec(subject, () -> context.sdlcInfo.accept(fetcher));
        }
        else if (ctx instanceof PureModelContextCollection)
        {
            PureModelContextCollection pureModelContextCollection = (PureModelContextCollection) ctx;
            Assert.assertTrue(pureModelContextCollection.getContexts().stream().allMatch(pureModelContext -> pureModelContext instanceof PureModelContextPointer), () -> "All elements in PureModelContextCollection should be of type PureModelContextPointer");
            Collection<SDLC> sdlcList = pureModelContextCollection.getContexts().stream().map(context1 -> ((PureModelContextPointer) context1).sdlcInfo).collect(Collectors.toList());
            Assert.assertTrue(sdlcList.stream().allMatch(sdlc -> sdlc instanceof AlloySDLC), () -> "All elements in PureModelContextCollection should have AlloySDLC info");

            metaData = subject == null ? fetcher.visit(sdlcList) : exec(subject, () -> fetcher.visit(sdlcList));
        }
        else
        {
            throw new RuntimeException("Invalid call to SDLCLoader.load");
        }

        if (metaData.origin != null)
        {
            Assert.assertTrue("none".equals(metaData.origin.sdlcInfo.version), () -> "Version can't be set in the pointer");
            metaData.origin.sdlcInfo.version = metaData.origin.sdlcInfo.baseVersion;
            metaData.origin.sdlcInfo.baseVersion = null;
        }

        return metaData;
    }

    public static PureModelContextData loadMetadataFromHTTPURL(Identity identity, LoggingEventType startEvent, LoggingEventType stopEvent, String url)
    {
        return loadMetadataFromHTTPURL(identity, startEvent, stopEvent, url, null);
    }

    public static PureModelContextData loadMetadataFromHTTPURL(Identity identity, LoggingEventType startEvent, LoggingEventType stopEvent, String url, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        return loadMetadataFromHTTPURL(identity, startEvent, stopEvent, url, httpClientProvider, null);
    }

    public static PureModelContextData loadMetadataFromHTTPURL(Identity identity, LoggingEventType startEvent, LoggingEventType stopEvent, String url, Function<Identity, CloseableHttpClient> httpClientProvider, Function<String, HttpRequestBase> httpRequestProvider)
    {
        Scope scope = GlobalTracer.get().scopeManager().active();
        CloseableHttpClient httpclient;

        if (httpClientProvider != null)
        {
            httpclient = httpClientProvider.apply(identity);
        }
        else
        {
            httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
        }

        long start = System.currentTimeMillis();

        LogInfo info = new LogInfo(identity.getName(), startEvent, "Requesting metadata");
        LOGGER.info(info.toString());
        Span span = GlobalTracer.get().activeSpan();
        if (span != null)
        {
            span.log(info.eventType + ": " + info.message);
            scope.span().setOperationName(startEvent.toString());
            span.log(url);
        }
        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.METADATA_LOAD_FROM_URL, "Loading from URL " + url).toString());

        HttpRequestBase httpRequest;
        if (httpRequestProvider != null)
        {
            httpRequest = httpRequestProvider.apply(url);
        }
        else
        {
            httpRequest = new HttpGet(url);
        }

        if (span != null)
        {
            GlobalTracer.get().inject(scope.span().context(), HTTP_HEADERS, new HttpRequestHeaderMap(httpRequest));
        }

        try
        {
            HttpEntity entity1 = execHttpRequest(span, httpclient, httpRequest);
            PureModelContextData modelContextData = objectMapper.readValue(entity1.getContent(), PureModelContextData.class);
            Assert.assertTrue(modelContextData.getSerializer() != null, () -> "Engine was unable to load information from the Pure SDLC <a href='" + url + "'>link</a>");
            LOGGER.info(new LogInfo(identity.getName(), stopEvent, (double) System.currentTimeMillis() - start).toString());
            if (span != null)
            {
                scope.span().log(String.valueOf(stopEvent));
            }
            return modelContextData;
        }
        catch (Exception e)
        {
            if (span != null)
            {
                Tags.ERROR.set(span, true);
                Map<String, Object> errorLogs = new HashMap<>(2);
                errorLogs.put("event", Tags.ERROR.getKey());
                errorLogs.put("error.object", e.getMessage());
                span.log(errorLogs);
            }
            throw new EngineException("Engine was unable to load information from the Pure SDLC using: <a href='" + url + "' target='_blank'>link</a>", e);
        }
    }

    private static final IntSet HTTP_RESPONSE_CODE_TO_RETRY = IntSets.immutable.with(
            HttpStatus.SC_BAD_GATEWAY,
            HttpStatus.SC_SERVICE_UNAVAILABLE,
            HttpStatus.SC_GATEWAY_TIMEOUT
    );

    public static HttpEntity execHttpRequest(Span span, CloseableHttpClient client, HttpRequestBase httpRequest) throws Exception
    {
        int statusCode = -1;
        CloseableHttpResponse response = null;
        int i = 0;
        while (i++ < 5)
        {
            response = client.execute(httpRequest);
            statusCode = response.getStatusLine().getStatusCode();
            if (!HTTP_RESPONSE_CODE_TO_RETRY.contains(statusCode))
            {
                break;
            }
            else
            {
                if (span != null)
                {
                    span.log(String.format("Try %d failed with status code %d.  Retrying...", i, statusCode));
                }
                response.close();
                Thread.sleep(200L);
            }
        }

        if (span != null)
        {
            span.setTag("httpRequestTries", i);
        }

        HttpEntity entity = response.getEntity();

        if (statusCode < 200 || statusCode >= 300)
        {
            String msg = entity != null ? EntityUtils.toString(entity) : response.getStatusLine().getReasonPhrase();
            response.close();
            throw new EngineException("Error response from " + httpRequest.getURI() + ", HTTP" + statusCode + "\n" + msg);
        }

        return entity;
    }
}
