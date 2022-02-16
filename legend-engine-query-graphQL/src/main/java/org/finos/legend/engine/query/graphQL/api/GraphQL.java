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

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.Translator;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.pac4j.core.profile.CommonProfile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public abstract class GraphQL
{
    protected ModelManager modelManager;

    public GraphQL(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    protected static org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_Document toPureModel(Document document, PureModel pureModel)
    {
        return new Translator().translate(document, pureModel);
    }

    protected PureModel loadModel(MutableList<CommonProfile> profiles, HttpServletRequest request, String project, String branch) throws IOException
    {
        CookieStore cookieStore = new BasicCookieStore();
        ArrayIterate.forEach(request.getCookies(), c -> cookieStore.addCookie(new MyCookie(c)));
        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            HttpGet req = new HttpGet("http://localhost:7070/api/projects/" + project + "/workspaces/" + branch + "/pureModelContextData");
            try (CloseableHttpResponse res = client.execute(req))
            {
                PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(EntityUtils.toString(res.getEntity()), PureModelContextData.class);
                PureModel pureModel = this.modelManager.loadModel(pureModelContextData, PureClientVersions.production, profiles, "");
                return pureModel;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
