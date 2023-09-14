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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.security.auth.Subject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.alloy.AlloySDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.pure.PureServerLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.SubjectCache;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.opentracing.HttpRequestHeaderMap;
import org.pac4j.core.profile.CommonProfile;
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
    private final Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider;

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

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, PureServerLoader pureLoader, Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider)
    {
        this.subjectProvider = subjectProvider;
        this.pureLoader = pureLoader;
        this.alloyLoader = new AlloySDLCLoader(metaDataServerConfiguration);
        this.httpClientProvider = httpClientProvider;
    }

    public SDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration, Supplier<Subject> subjectProvider, PureServerLoader pureLoader, Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider, AlloySDLCLoader alloyLoader)
    {
        this.subjectProvider = subjectProvider;
        this.pureLoader = pureLoader;
        this.alloyLoader = alloyLoader;
        this.httpClientProvider = httpClientProvider;
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
    }

    @Override
    public boolean shouldCache(PureModelContext context)
    {
        return this.supports(context) && (isCacheablePureSDLC(((PureModelContextPointer) context).sdlcInfo) || isCacheableAlloySDLC(((PureModelContextPointer) context).sdlcInfo));
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
    public PureModelContext cacheKey(PureModelContext context, MutableList<CommonProfile> pm)
    {
        if (isCacheablePureSDLC(((PureModelContextPointer) context).sdlcInfo))
        {
            final Subject executionSubject = getSubject();
            Function0<PureModelContext> pureModelContextFunction = () -> this.pureLoader.getCacheKey(context, pm, executionSubject);
            return executionSubject == null ? pureModelContextFunction.value() : exec(executionSubject, pureModelContextFunction::value);
        }
        else
        {
            return this.alloyLoader.getCacheKey(context);
        }
    }

    @Override
    public boolean supports(PureModelContext context)
    {
        return context instanceof PureModelContextPointer;
    }

    @Override
    public PureModelContextData load(MutableList<CommonProfile> pm, PureModelContext ctx, String clientVersion, Span parentSpan)
    {
        PureModelContextPointer context = (PureModelContextPointer) ctx;
        Assert.assertTrue(clientVersion != null, () -> "Client version should be set when pulling metadata from the metadata repository");

        Function0<PureModelContextData> fetchMetadata;

        final Subject subject = getSubject();

        if (context.sdlcInfo instanceof PureSDLC)
        {
            fetchMetadata = () ->
            {
                parentSpan.setTag("sdlc", "pure");
                try (Scope scope = GlobalTracer.get().buildSpan("Request Pure Metadata").startActive(true))
                {
                    return ListIterate.injectInto(
                            new PureModelContextData.Builder(),
                            context.sdlcInfo.packageableElementPointers,
                            (builder, pointers) -> builder.withPureModelContextData(this.pureLoader.loadPurePackageableElementPointer(pm, pointers, clientVersion, subject == null ? "" : "?auth=kerberos", ((PureSDLC) context.sdlcInfo).overrideUrl))
                    ).distinct().sorted().build();
                }
            };
        }
        else if (context.sdlcInfo instanceof AlloySDLC)
        {
            fetchMetadata = () ->
            {
                parentSpan.setTag("sdlc", "alloy");
                try (Scope scope = GlobalTracer.get().buildSpan("Request Alloy Metadata").startActive(true))
                {
                    AlloySDLC sdlc = (AlloySDLC) context.sdlcInfo;
                    PureModelContextData loadedProject = this.alloyLoader.loadAlloyProject(pm, sdlc, clientVersion, this.httpClientProvider);
                    loadedProject.origin.sdlcInfo.packageableElementPointers = sdlc.packageableElementPointers;
                    List<String> missingPaths = this.alloyLoader.checkAllPathsExist(loadedProject, sdlc);
                    if (missingPaths.isEmpty())
                    {
                        return loadedProject;
                    }
                    else
                    {
                        throw new EngineException("The following entities:" + missingPaths + " do not exist in the project data loaded from the metadata server. " +
                                "Please make sure the corresponding Gitlab pipeline for version " + (this.alloyLoader.isLatestRevision(sdlc) ? "latest" : sdlc.version) + " has completed and also metadata server has updated with corresponding entities " +
                                "by confirming the data returned from <a href=\"" + this.alloyLoader.getMetaDataApiUrl(pm, sdlc, clientVersion) + "\"/> this API </a>.");
                    }
                }
            };
        }
        else
        {
            throw new UnsupportedOperationException("To Code");
        }

        PureModelContextData metaData = subject == null ? fetchMetadata.value() : exec(subject, fetchMetadata::value);

        if (metaData.origin != null)
        {
            Assert.assertTrue("none".equals(metaData.origin.sdlcInfo.version), () -> "Version can't be set in the pointer");
            metaData.origin.sdlcInfo.version = metaData.origin.sdlcInfo.baseVersion;
            metaData.origin.sdlcInfo.baseVersion = null;
        }

        return metaData;
    }

    public static PureModelContextData loadMetadataFromHTTPURL(MutableList<CommonProfile> pm, LoggingEventType startEvent, LoggingEventType stopEvent, String url)
    {
        return loadMetadataFromHTTPURL(pm, startEvent, stopEvent, url, null);
    }

    public static PureModelContextData loadMetadataFromHTTPURL(MutableList<CommonProfile> pm, LoggingEventType startEvent, LoggingEventType stopEvent, String url, Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider)
    {
        Scope scope = GlobalTracer.get().scopeManager().active();
        CloseableHttpClient httpclient;

        if (httpClientProvider != null)
        {
            httpclient = httpClientProvider.apply(pm);
        }
        else
        {
            httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
        }

        long start = System.currentTimeMillis();

        LogInfo info = new LogInfo(pm, startEvent, "Requesting metadata");
        LOGGER.info(info.toString());
        Span span = GlobalTracer.get().activeSpan();
        if (span != null)
        {
            span.log(info.eventType + ": " + info.message);
            scope.span().setOperationName(startEvent.toString());
            span.log(url);
        }
        LOGGER.info(new LogInfo(pm, LoggingEventType.METADATA_LOAD_FROM_URL, "Loading from URL " + url).toString());

        HttpGet httpGet = new HttpGet(url);
        if (span != null)
        {
            GlobalTracer.get().inject(scope.span().context(), HTTP_HEADERS, new HttpRequestHeaderMap(httpGet));
        }

        try
        {
            HttpEntity entity1 = execHttpRequest(span, httpclient, httpGet);
            PureModelContextData modelContextData = objectMapper.readValue(entity1.getContent(), PureModelContextData.class);
            Assert.assertTrue(modelContextData.getSerializer() != null, () -> "Engine was unable to load information from the Pure SDLC <a href='" + url + "'>link</a>");
            LOGGER.info(new LogInfo(pm, stopEvent, (double) System.currentTimeMillis() - start).toString());
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

    private static HttpEntity execHttpRequest(Span span, CloseableHttpClient client, HttpGet httpGet) throws Exception
    {
        int statusCode = -1;
        CloseableHttpResponse response = null;
        int i = 0;
        while (i++ < 5)
        {
            response = client.execute(httpGet);
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
            String msg = EntityUtils.toString(entity);
            response.close();
            throw new EngineException("Error response from " + httpGet.getURI() + ", HTTP" + statusCode + "\n" + msg);
        }

        return entity;
    }
}
