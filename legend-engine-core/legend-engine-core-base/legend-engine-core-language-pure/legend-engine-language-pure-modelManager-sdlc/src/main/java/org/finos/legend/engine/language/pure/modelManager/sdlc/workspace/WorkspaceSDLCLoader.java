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

package org.finos.legend.engine.language.pure.modelManager.sdlc.workspace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.function.Function;
import javax.security.auth.Subject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.finos.legend.engine.identity.extensions.pac4j.Pac4jUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetadataServerPrivateAccessTokenConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.WorkspaceSDLC;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.KerberosUtils;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

public class WorkspaceSDLCLoader
{
    private static final TypeReference<List<SDLCProjectDependency>> SDLC_PROJECT_DEPENDENCY_TYPE = new TypeReference<List<SDLCProjectDependency>>()
    {
    };

    private final ServerConnectionConfiguration sdlcServerConnectionConfig;
    private final ObjectMapper mapper;
    private ModelManager modelManager;

    public WorkspaceSDLCLoader(ServerConnectionConfiguration sdlcServerConnectionConfig)
    {
        this.sdlcServerConnectionConfig = sdlcServerConnectionConfig;
        this.mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    }

    public PureModelContextData loadWorkspace(Identity identity, WorkspaceSDLC sdlc, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        return this.doAs(identity, () ->
        {
            String url = sdlcServerConnectionConfig.getBaseUrl() + "/api/projects/" + sdlc.project + (sdlc.isGroupWorkspace ? "/groupWorkspaces/" : "/workspaces/") + sdlc.getWorkspace() + "/pureModelContextData";
            return SDLCLoader.loadMetadataFromHTTPURL(identity, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, url, httpClientProvider, x -> prepareHttpRequest(identity, x));
        });
    }

    public void setModelManager(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    public PureModelContextData getSDLCDependenciesPMCD(Identity identity, String clientVersion, WorkspaceSDLC sdlc, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        return this.doAs(identity, () ->
        {
            CloseableHttpClient httpclient;

            if (httpClientProvider != null)
            {
                httpclient = httpClientProvider.apply(identity);
            }
            else
            {
                httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
            }

            try (
                    CloseableHttpClient client = httpclient;
                    Scope scope = GlobalTracer.get().buildSpan("Load project upstream dependencies").startActive(true)
            )
            {
                String url = String.format("%s/api/projects/%s/%s/%s/revisions/HEAD/upstreamProjects",
                        sdlcServerConnectionConfig.getBaseUrl(),
                        sdlc.project,
                        sdlc.isGroupWorkspace ? "groupWorkspaces" : "workspaces",
                        sdlc.getWorkspace());

                HttpGet httpRequest = this.prepareHttpRequest(identity, url);
                HttpEntity entity = SDLCLoader.execHttpRequest(scope.span(), client, httpRequest);

                try (InputStream content = entity.getContent())
                {
                    List<SDLCProjectDependency> dependencies = mapper.readValue(content, SDLC_PROJECT_DEPENDENCY_TYPE);

                    PureModelContextData.Builder builder = PureModelContextData.newBuilder();

                    dependencies.forEach(dependency ->
                    {
                        builder.addPureModelContextData(this.loadDependencyData(identity, clientVersion, dependency));
                    });

                    builder.removeDuplicates();
                    return builder.build();
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    private PureModelContextData doAs(Identity identity, PrivilegedAction<PureModelContextData> action)
    {
        Subject kerberosCredential = KerberosUtils.getSubjectFromIdentity(identity);
        return kerberosCredential == null ? action.run() : Subject.doAs(kerberosCredential, action);
    }

    private HttpGet prepareHttpRequest(Identity identity, String url)
    {
        HttpGet httpRequest = null;

        if (this.sdlcServerConnectionConfig.pac4j != null && this.sdlcServerConnectionConfig.pac4j instanceof MetadataServerPrivateAccessTokenConfiguration)
        {
            String patHeaderName = ((MetadataServerPrivateAccessTokenConfiguration) this.sdlcServerConnectionConfig.pac4j).accessTokenHeaderName;
            //MutableList<GitlabPersonalAccessTokenProfile> patProfiles = pm.selectInstancesOf(GitlabPersonalAccessTokenProfile.class);
            if (identity != null)
            {
                httpRequest = new HttpGet(String.format("%s?client_name=%s", url, Pac4jUtils.getProfilesFromIdentity(identity).get(0).getClientName()));
                //httpRequest.addHeader(new BasicHeader(patHeaderName, patProfiles.getFirst().getPersonalAccessToken()));
            }
        }

        if (httpRequest == null)
        {
            httpRequest = new HttpGet(url);
        }

        return httpRequest;
    }

    private PureModelContextData loadDependencyData(Identity profiles, String clientVersion, SDLCProjectDependency dependency)
    {
        PureModelContextPointer pointer = new PureModelContextPointer();
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = dependency.getGroupId();
        sdlcInfo.artifactId = dependency.getArtifactId();
        sdlcInfo.version = dependency.getVersionId();
        pointer.sdlcInfo = sdlcInfo;
        pointer.serializer = new Protocol("pure", clientVersion);
        return this.modelManager.loadData(pointer, clientVersion, profiles);
    }

    private static class SDLCProjectDependency
    {
        public String projectId;
        public String versionId;

        public String getGroupId()
        {
            return projectId.split(":")[0];
        }

        public String getArtifactId()
        {
            return projectId.split(":")[1];
        }

        public String getVersionId()
        {
            return versionId;
        }
    }
}
