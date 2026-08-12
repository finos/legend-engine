// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.protocol.dataquality.model.DataQualityExecuteInput;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.lang.String.format;

public class DataQualityPlanLoader
{
    private static final TypeReference<List<Artifact>> ARTIFACT_TYPE = new TypeReference<List<Artifact>>()
    {
    };

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final String SNAPSHOT = "-SNAPSHOT";
    private static final String COLON = ":";
    public static final String EXECUTION_PLAN_FILE_NAME = "dataQualityValidationExecutionPlan.json";
    public static final String RECON_EXECUTION_PLAN_FILE_NAME = "dataQualityRelationComparisonPlan.json";

    // Shared in-memory cache of pre-generated recon plans. Keyed by groupId:artifactId:version:elementPath. SNAPSHOT versions are never cached so dev iteration stays correct.
    public static final Cache<String, SingleExecutionPlan> RECON_PLAN_CACHE = CacheBuilder.newBuilder()
            .recordStats()
            .softValues()
            .maximumSize(1024)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    private final ServerConnectionConfiguration sdlcServerConnectionConfig;
    private final Function<Identity, CloseableHttpClient> httpClientProvider;

    public DataQualityPlanLoader(ServerConnectionConfiguration sdlcServerConnectionConfig, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        this.sdlcServerConnectionConfig = sdlcServerConnectionConfig;
        this.httpClientProvider = httpClientProvider;
    }

    public SingleExecutionPlan fetchPlanFromSDLC(Identity identity, DataQualityExecuteInput dataQualityParameterValue)
    {
        Assert.assertTrue(dataQualityParameterValue != null && dataQualityParameterValue.elementPath != null && dataQualityParameterValue.sdlc != null, () -> "DataQualityParameter info must contain Element Path and sdlc to access metadata services");
        List<Artifact> metaDataArtifactList = loadPlanDataQualityFromHTTPURL(identity, getMetaDataApiUrl(dataQualityParameterValue.elementPath, dataQualityParameterValue.sdlc));
        return getPlanFromArtifactResponse(metaDataArtifactList, EXECUTION_PLAN_FILE_NAME);
    }

    /*
    Attempts to load the pre-generated reconciliation plan artifact for a DataQualityRelationComparison element.
    Consults the in-memory cache first; on miss, hits the metadata server and caches the result unless the version is a SNAPSHOT.
    */
    public SingleExecutionPlan fetchReconPlanFromSDLC(Identity identity, String elementPath, SDLC sdlc)
    {
        String key = cacheKey(elementPath, sdlc);
        if (key != null)
        {
            SingleExecutionPlan cached = RECON_PLAN_CACHE.getIfPresent(key);
            if (cached != null)
            {
                return cached;
            }
        }
        List<Artifact> metaDataArtifactList = loadPlanDataQualityFromHTTPURL(identity, getMetaDataApiUrl(elementPath, sdlc));
        SingleExecutionPlan plan = getPlanFromArtifactResponse(metaDataArtifactList, RECON_EXECUTION_PLAN_FILE_NAME);
        cacheReconPlan(elementPath, sdlc, plan);
        return plan;
    }

    public static void cacheReconPlan(String elementPath, SDLC sdlc, SingleExecutionPlan plan)
    {
        if (plan == null)
        {
            return;
        }
        String key = cacheKey(elementPath, sdlc);
        if (key != null)
        {
            RECON_PLAN_CACHE.put(key, plan);
        }
    }

    static String cacheKey(String elementPath, SDLC sdlc)
    {
        if (elementPath == null || !(sdlc instanceof AlloySDLC))
        {
            return null;
        }
        AlloySDLC alloy = (AlloySDLC) sdlc;
        if (alloy.groupId == null || alloy.artifactId == null || alloy.version == null)
        {
            return null;
        }
        if (alloy.version.endsWith(SNAPSHOT))
        {
            return null;
        }
        return alloy.groupId + COLON + alloy.artifactId + COLON + alloy.version + COLON + elementPath;
    }

    private String getMetaDataApiUrl(String elementPath, SDLC sdlc)
    {
        if (sdlc instanceof AlloySDLC)
        {
            AlloySDLC alloySDLC = (AlloySDLC) sdlc;
            return getMetaDataApiUrlFromAlloySDLC(elementPath, alloySDLC);
        }
        else
        {
            throw new EngineException("Unsupported SDLC type:" + sdlc.getClass().getSimpleName());
        }
    }

    private String getMetaDataApiUrlFromAlloySDLC(String elementPath, AlloySDLC alloySDLC)
    {
        Assert.assertTrue(alloySDLC != null && alloySDLC.groupId != null && alloySDLC.artifactId != null && alloySDLC.version != null, () -> "AlloySDLC info must contain and group and artifact IDs to access metadata services");
        return format("%s/generations/%s/%s/versions/%s/%s", sdlcServerConnectionConfig.getBaseUrl(), alloySDLC.groupId, alloySDLC.artifactId, alloySDLC.version, elementPath);
    }

    private List<Artifact> loadPlanDataQualityFromHTTPURL(Identity identity, String url)
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

        try
        {
            HttpGet httpRequest = new HttpGet(url);
            HttpEntity entity = SDLCLoader.execHttpRequest(scope.span(), httpclient, httpRequest);
            try (InputStream content = entity.getContent())
            {
                return objectMapper.readValue(content, ARTIFACT_TYPE);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private SingleExecutionPlan getPlanFromArtifactResponse(List<Artifact> metaDataArtifactList, String fileName)
    {
        Artifact planArtifact = metaDataArtifactList.stream().filter(artifact -> artifact.path.contains(fileName)).findAny().orElseThrow(() -> new EngineException(format("No DataQuality Execution plan found in artifacts for %s", fileName)));
        return PlanGenerator.stringToPlan(planArtifact.content);
    }
}
