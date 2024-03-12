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

package org.finos.legend.engine.language.pure.modelManager.sdlc.alloy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

public class AlloySDLCLoader
{
    private final MetaDataServerConfiguration metaDataServerConfiguration;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();

    public AlloySDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    public class ProjectVersion
    {
        private final String groupId;
        private final String artifactId;
        private final String versionId;

        public ProjectVersion(String groupId, String artifactId, String versionId)
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.versionId = versionId;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public String getVersionId()
        {
            return versionId;
        }
    }

    public PureModelContextData loadAlloyProject(Identity identity, AlloySDLC alloySDLC, String clientVersion, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        return SDLCLoader.loadMetadataFromHTTPURL(identity, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, getMetaDataApiUrl(identity, alloySDLC, clientVersion), httpClientProvider);
    }

    public PureModelContextData loadAlloyProjects(Identity identity, List<AlloySDLC> alloySDLC, String clientVersion, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        return SDLCLoader.loadMetadataFromHTTPURL(identity, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, getMetaDataApiUrl(identity, alloySDLC, clientVersion), httpClientProvider, (url) ->
        {
            try
            {
                HttpPost httpPost = new HttpPost(url);
                List<ProjectVersion> projectVersions = alloySDLC.stream().map(sdlc -> new ProjectVersion(sdlc.groupId, sdlc.artifactId, sdlc.version)).collect(Collectors.toList());
                httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(projectVersions)));
                httpPost.setHeader(new BasicHeader(CONTENT_TYPE, APPLICATION_JSON));
                return httpPost;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public String getMetaDataApiUrl(Identity identity, AlloySDLC alloySDLC, String clientVersion)
    {
        Assert.assertTrue(alloySDLC.project == null, () -> "Accessing metadata services using project id was demised.  Please update AlloySDLC to provide group and artifact IDs");
        Assert.assertTrue(alloySDLC.groupId != null && alloySDLC.artifactId != null, () -> "AlloySDLC info must contain and group and artifact IDs to access metadata services");
        return (isLatestRevision(alloySDLC)) ?
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/projects/" + alloySDLC.groupId + "/" + alloySDLC.artifactId + "/revisions/latest/pureModelContextData?clientVersion=" + clientVersion :
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/projects/" + alloySDLC.groupId + "/" + alloySDLC.artifactId + "/versions/" + alloySDLC.version + "/pureModelContextData?clientVersion=" + clientVersion;
    }

    public String getMetaDataApiUrl(Identity identity, List<AlloySDLC> alloySDLC, String clientVersion)
    {
        return metaDataServerConfiguration.getAlloy().getBaseUrl() + "/projects/dependencies/pureModelContextData?clientVersion=" + clientVersion;
    }

    public List<String> checkAllPathsExist(PureModelContextData data, AlloySDLC alloySDLC)
    {
        List<String> pathsFromPointer = alloySDLC.packageableElementPointers.stream().map(s -> s.path).collect(Collectors.toList());
        List<String> entities = data.getElements().stream().map(s -> s.getPath()).collect(Collectors.toList());

        pathsFromPointer.removeAll(entities);
        return pathsFromPointer;
    }

    public boolean isLatestRevision(AlloySDLC alloySDLC)
    {
        return alloySDLC.version == null || alloySDLC.version.equals("none") || alloySDLC.version.equals("master-SNAPSHOT");
    }

    public PureModelContext getCacheKey(PureModelContext context)
    {
        return context;
    }
}
