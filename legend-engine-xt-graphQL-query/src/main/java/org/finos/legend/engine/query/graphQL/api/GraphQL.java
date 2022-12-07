// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.Translator;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public abstract class GraphQL
{
    protected ModelManager modelManager;
    protected MetaDataServerConfiguration metadataserver;

    public GraphQL(ModelManager modelManager, MetaDataServerConfiguration metadataserver)
    {
        this.modelManager = modelManager;
        this.metadataserver = metadataserver;
    }

    protected static org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document toPureModel(Document document, PureModel pureModel)
    {
        return new Translator().translate(document, pureModel);
    }

    protected PureModel loadSDLCProjectModel(MutableList<CommonProfile> profiles, HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace) throws PrivilegedActionException
    {
        Subject subject = ProfileManagerHelper.extractSubject(profiles);
        return subject == null ?
                getSDLCProjectPureModel(profiles, request, projectId, workspaceId, isGroupWorkspace) :
                Subject.doAs(subject, (PrivilegedExceptionAction<PureModel>) () -> getSDLCProjectPureModel(profiles, request, projectId, workspaceId, isGroupWorkspace));
    }

    private PureModel getSDLCProjectPureModel(MutableList<CommonProfile> profiles, HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace)
    {
        CookieStore cookieStore = new BasicCookieStore();
        ArrayIterate.forEach(request.getCookies(), c -> cookieStore.addCookie(new MyCookie(c)));


        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            if (metadataserver == null || metadataserver.getSdlc() == null)
            {
                throw new EngineException("Please specify the metadataserver.sdlc information in the server configuration");
            }
            HttpGet req = new HttpGet("http://" + metadataserver.getSdlc().host + ":" + metadataserver.getSdlc().port + "/api/projects/" + projectId + (isGroupWorkspace ? "/groupWorkspaces/" : "/workspaces/") + workspaceId + "/pureModelContextData");
            try (CloseableHttpResponse res = client.execute(req))
            {
                ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                PureModelContextData pureModelContextData = mapper.readValue(res.getEntity().getContent(), PureModelContextData.class);
                return this.modelManager.loadModel(pureModelContextData, PureClientVersions.production, profiles, "");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected PureModel loadProjectModel(MutableList<CommonProfile> profiles, HttpServletRequest request, String groupId, String artifactId, String versionId) throws PrivilegedActionException
    {
        Subject subject = ProfileManagerHelper.extractSubject(profiles);
        PureModelContextPointer pointer = new PureModelContextPointer();
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = groupId;
        sdlcInfo.artifactId = artifactId;
        sdlcInfo.version = versionId;
        pointer.sdlcInfo = sdlcInfo;
        return subject == null ?
                this.modelManager.loadModel(pointer, PureClientVersions.production, profiles, "") :
                Subject.doAs(subject, (PrivilegedExceptionAction<PureModel>) () -> this.modelManager.loadModel(pointer, PureClientVersions.production, profiles, ""));
    }
}
