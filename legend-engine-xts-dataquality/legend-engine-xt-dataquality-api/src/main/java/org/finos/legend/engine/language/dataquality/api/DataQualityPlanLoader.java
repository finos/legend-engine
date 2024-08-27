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
import java.util.function.Function;

public class DataQualityPlanLoader
{
    private static final TypeReference<List<Artifact>> ARTIFACT_TYPE = new TypeReference<List<Artifact>>()
    {
    };

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    public static final String EXECUTION_PLAN_FILE_NAME = "dataQualityValidationExecutionPlan.json";

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
        return getPlanFromArtifactResponse(metaDataArtifactList);
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
        return String.format("%s/generations/%s/%s/versions/%s/%s", sdlcServerConnectionConfig.getBaseUrl(), alloySDLC.groupId, alloySDLC.artifactId, alloySDLC.version, elementPath);
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

    private SingleExecutionPlan getPlanFromArtifactResponse(List<Artifact> metaDataArtifactList)
    {
        Artifact planArtifact = metaDataArtifactList.stream().filter(artifact -> artifact.path.contains(EXECUTION_PLAN_FILE_NAME)).findAny().orElseThrow(() -> new EngineException("Error fetching DataQuality Execution plan"));
        return PlanGenerator.stringToPlan(planArtifact.content);
    }
}
