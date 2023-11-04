// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.shared.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetadataServerPrivateAccessTokenConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.query.sql.providers.shared.utils.TraceUtils;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.server.pac4j.gitlab.GitlabPersonalAccessTokenProfile;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Optional;

public class ProjectCoordinateLoader
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private final ModelManager modelManager;
    private final ServerConnectionConfiguration sdlcServerConfig;
    private final Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider;

    public ProjectCoordinateLoader(ModelManager modelManager, ServerConnectionConfiguration sdlcServerConfig)
    {
        this(modelManager, sdlcServerConfig, profiles -> (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore()));
    }

    public ProjectCoordinateLoader(ModelManager modelManager, ServerConnectionConfiguration sdlcServerConfig, Function<MutableList<CommonProfile>, CloseableHttpClient> httpClientProvider)
    {
        this.modelManager = modelManager;
        this.sdlcServerConfig = sdlcServerConfig;
        this.httpClientProvider = httpClientProvider;
    }

    public ProjectResolvedContext resolve(ProjectCoordinateWrapper projectCoordinateWrapper, MutableList<CommonProfile> profiles)
    {
        return resolve(projectCoordinateWrapper, true, profiles);
    }

    public ProjectResolvedContext resolve(ProjectCoordinateWrapper projectCoordinateWrapper, boolean required, MutableList<CommonProfile> profiles)
    {
        Optional<String> coordinates = projectCoordinateWrapper.getCoordinates();
        if (coordinates.isPresent())
        {
            PureModelContextPointer pointer = pointerFromCoordinates(coordinates.get());

            PureModelContextData pmcd = modelManager.loadData(pointer, PureClientVersions.production, profiles);

            return new ProjectResolvedContext(pointer, pmcd);
        }
        Optional<String> project = projectCoordinateWrapper.getProject();
        if (project.isPresent())
        {
            Optional<String> workspace = projectCoordinateWrapper.getWorkspace();
            Optional<String> groupWorkspace = projectCoordinateWrapper.getGroupWorkspace();
            String workspaceId = workspace.orElseGet(groupWorkspace::get);
            boolean isGroup = groupWorkspace.isPresent();
            String projectId = project.get();

            PureModelContextData pmcd = loadProjectPureModelContextData(projectId, workspaceId, isGroup, profiles);

            return new ProjectResolvedContext(pmcd, pmcd);
        }

        if (required)
        {
            throw new EngineException("project/workspace or coordinates must be supplied");
        }

        return null;
    }

    private PureModelContextPointer pointerFromCoordinates(String coordinates)
    {
        AlloySDLC sdlc = new AlloySDLC();
        enrichCoordinates(sdlc, coordinates);
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlc;
        return pointer;
    }

    private void enrichCoordinates(AlloySDLC alloySDLC, String coordinates)
    {
        String[] parts = coordinates.split(":");
        if (parts.length != 3)
        {
            throw new IllegalArgumentException("Invalid coordinates on service " + coordinates);
        }

        alloySDLC.groupId = parts[0];
        alloySDLC.artifactId = parts[1];
        alloySDLC.version = parts[2];
    }

    private PureModelContextData loadProjectPureModelContextData(String project, String workspace, boolean isGroup, MutableList<CommonProfile> profiles)
    {
        return doAs(ProfileManagerHelper.extractSubject(profiles), () ->
        {
            String url = String.format("%s/api/projects/%s/%s/%s/pureModelContextData", sdlcServerConfig.getBaseUrl(), project, isGroup ? "groupWorkspaces" : "workspaces", workspace);
            PureModelContextData projectPMCD = SDLCLoader.loadMetadataFromHTTPURL(profiles, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, url, httpClientProvider, (String _url) ->
            {
                HttpGet httpRequest = new HttpGet(_url);
                if (this.sdlcServerConfig.pac4j != null && this.sdlcServerConfig.pac4j instanceof MetadataServerPrivateAccessTokenConfiguration)
                {
                    String patHeaderName = ((MetadataServerPrivateAccessTokenConfiguration) this.sdlcServerConfig.pac4j).accessTokenHeaderName;
                    MutableList<GitlabPersonalAccessTokenProfile> patProfiles = profiles.selectInstancesOf(GitlabPersonalAccessTokenProfile.class);
                    if (patProfiles.getFirst() != null)
                    {
                        httpRequest = new HttpGet(String.format("%s?client_name=%s", _url, patProfiles.getFirst().getClientName()));
                        httpRequest.addHeader(new BasicHeader(patHeaderName, patProfiles.getFirst().getPersonalAccessToken()));
                    }
                }
                return httpRequest;
            });
            PureModelContextData dependencyPMCD = getSDLCDependenciesPMCD(project, workspace, isGroup, profiles);

            return projectPMCD.combine(dependencyPMCD);
        });
    }

    private PureModelContextData getSDLCDependenciesPMCD(String project, String workspace, boolean isGroup, MutableList<CommonProfile> profiles)
    {
        return TraceUtils.trace("Get SDLC Dependencies", span ->
        {

            span.setTag("project", project);
            span.setTag("workspace", workspace);
            span.setTag("group", isGroup);

            if (sdlcServerConfig == null)
            {
                throw new EngineException("SDLC Server configuration must be supplied");
            }
            try (CloseableHttpClient client = this.httpClientProvider.apply(profiles))
            {
                String url = String.format("%s/api/projects/%s/%s/%s/revisions/HEAD/upstreamProjects",
                        sdlcServerConfig.getBaseUrl(),
                        project,
                        isGroup ? "groupWorkspaces" : "workspaces",
                        workspace);

                HttpGet httpRequest = new HttpGet(url);
                String patHeaderName = ((MetadataServerPrivateAccessTokenConfiguration) this.sdlcServerConfig.pac4j).accessTokenHeaderName;
                MutableList<GitlabPersonalAccessTokenProfile> patProfiles = profiles.selectInstancesOf(GitlabPersonalAccessTokenProfile.class);
                if (patProfiles.getFirst() != null)
                {
                    httpRequest = new HttpGet(String.format("%s?client_name=%s", url, patProfiles.getFirst().getClientName()));
                    httpRequest.addHeader(new BasicHeader(patHeaderName, patProfiles.getFirst().getPersonalAccessToken()));
                }

                try (CloseableHttpResponse response = client.execute(httpRequest))
                {
                    StatusLine status = response.getStatusLine();
                    if (!Response.Status.Family.familyOf(status.getStatusCode()).equals(Response.Status.Family.SUCCESSFUL))
                    {
                        throw new RuntimeException(String.format("Status Code: %s\nReason: %s\nMessage: %s",
                                status.getStatusCode(), status.getReasonPhrase(), "Error fetching from " + url));
                    }

                    List<SDLCProjectDependency> dependencies = OBJECT_MAPPER.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<SDLCProjectDependency>>()
                    {
                    });
                    PureModelContextData.Builder builder = PureModelContextData.newBuilder();
                    dependencies.forEach(dependency ->
                    {
                        try
                        {
                            builder.addPureModelContextData(loadProjectData(profiles, dependency.getGroupId(), dependency.getArtifactId(), dependency.versionId));
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
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

    private PureModelContextData loadProjectData(MutableList<CommonProfile> profiles, String groupId, String artifactId, String versionId)
    {
        return TraceUtils.trace("Loading Project Data", span ->
        {

            span.setTag("groupId", groupId);
            span.setTag("artifactId", artifactId);
            span.setTag("versionId", versionId);

            Subject subject = ProfileManagerHelper.extractSubject(profiles);
            PureModelContextPointer pointer = new PureModelContextPointer();
            AlloySDLC sdlcInfo = new AlloySDLC();
            sdlcInfo.groupId = groupId;
            sdlcInfo.artifactId = artifactId;
            sdlcInfo.version = versionId;
            pointer.sdlcInfo = sdlcInfo;

            return doAs(subject, () -> this.modelManager.loadData(pointer, PureClientVersions.production, profiles));
        });
    }

    private <T> T doAs(Subject subject, PrivilegedAction<T> action)
    {
        return subject != null ? Subject.doAs(subject, action) : action.run();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SDLCProjectDependency
    {
        private final String projectId;
        private final String versionId;

        public SDLCProjectDependency(@JsonProperty("projectId") String projectId, @JsonProperty("versionId") String versionId)
        {
            this.projectId = projectId;
            this.versionId = versionId;
        }

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