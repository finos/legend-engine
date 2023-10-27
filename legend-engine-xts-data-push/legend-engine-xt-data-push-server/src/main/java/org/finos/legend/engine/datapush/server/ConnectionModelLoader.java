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

package org.finos.legend.engine.datapush.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

public class ConnectionModelLoader
{
    private final MetaDataServerConfiguration metaDataServerConfiguration;

    public ConnectionModelLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    public Connection getConnectionFromSDLCWorkspace(HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace, String connectionPath)
    {
        // NOTE: this flow is really meant only for development, here we have to
        CookieStore cookieStore = new BasicCookieStore();
        ArrayIterate.forEach(request.getCookies(), c -> cookieStore.addCookie(new DEV_ONLY__HttpClientBuilderCookie(c)));

        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            if (metaDataServerConfiguration == null || metaDataServerConfiguration.getSdlc() == null)
            {
                throw new EngineException("Please specify the metadataserver.sdlc information in the server configuration");
            }
            HttpGet req = new HttpGet("http://" + metaDataServerConfiguration.getSdlc().host + ":" + metaDataServerConfiguration.getSdlc().port + "/api/projects/" + projectId + (isGroupWorkspace ? "/groupWorkspaces/" : "/workspaces/") + workspaceId + "/pureModelContextData");
            try (CloseableHttpResponse res = client.execute(req))
            {
                ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                PureModelContextData pureModelContextData = mapper.readValue(res.getEntity().getContent(), PureModelContextData.class);
                return ListIterate.select(pureModelContextData.getElements(), element -> element.getPath().equals(connectionPath)).selectInstancesOf(Connection.class).getAny();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnectionFromProject(List<CommonProfile> profiles, String groupId, String artifactId, String versionId, String connectionPath)
    {
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = groupId;
        sdlcInfo.artifactId = artifactId;
        sdlcInfo.version = versionId;
        Assert.assertTrue(sdlcInfo.project == null, () -> "Accessing metadata services using project id was demised.  Please update AlloySDLC to provide group and artifact IDs");
        Assert.assertTrue(sdlcInfo.groupId != null && sdlcInfo.artifactId != null, () -> "AlloySDLC info must contain and group and artifact IDs to access metadata services");
        PureModelContextData pureModelContextData = SDLCLoader.loadMetadataFromHTTPURL(Lists.mutable.withAll(profiles), LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, (isLatestRevision(sdlcInfo)) ?
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/projects/" + sdlcInfo.groupId + "/" + sdlcInfo.artifactId + "/revisions/latest/pureModelContextData" :
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/projects/" + sdlcInfo.groupId + "/" + sdlcInfo.artifactId + "/versions/" + sdlcInfo.version + "/pureModelContextData");
        return ListIterate.select(pureModelContextData.getElements(), element -> element.getPath().equals(connectionPath)).selectInstancesOf(Connection.class).getAny();
    }

    private boolean isLatestRevision(AlloySDLC alloySDLC)
    {
        return alloySDLC.version == null || alloySDLC.version.equals("none") || alloySDLC.version.equals("master-SNAPSHOT");
    }

    private static class DEV_ONLY__HttpClientBuilderCookie implements Cookie
    {
        private final javax.servlet.http.Cookie cookie;

        public DEV_ONLY__HttpClientBuilderCookie(javax.servlet.http.Cookie cookie)
        {
            this.cookie = cookie;
        }

        @Override
        public String getName()
        {
            return this.cookie.getName();
        }

        @Override
        public String getValue()
        {
            return this.cookie.getValue();
        }

        @Override
        public String getComment()
        {
            return this.cookie.getComment();
        }

        @Override
        public String getCommentURL()
        {
            return "";
        }

        @Override
        public Date getExpiryDate()
        {
            if (this.cookie.getMaxAge() >= 0)
            {
                return new Date(System.currentTimeMillis() + this.cookie.getMaxAge() * 1000L);
            }
            throw new RuntimeException("");
        }

        @Override
        public boolean isPersistent()
        {
            return true;
        }

        @Override
        public String getDomain()
        {
            return "localhost";
        }

        @Override
        public String getPath()
        {
            return "/";
        }

        @Override
        public int[] getPorts()
        {
            return new int[]{};
        }

        @Override
        public boolean isSecure()
        {
            return false;
        }

        @Override
        public int getVersion()
        {
            return this.cookie.getVersion();
        }

        @Override
        public boolean isExpired(Date date)
        {
            return false;
        }
    }
}
