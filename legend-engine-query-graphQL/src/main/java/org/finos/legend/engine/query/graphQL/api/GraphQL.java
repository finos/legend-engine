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

    protected PureModel loadModel(MutableList<CommonProfile> profiles, HttpServletRequest request) throws IOException
    {
        CookieStore cookieStore = new BasicCookieStore();
        ArrayIterate.forEach(request.getCookies(), c -> cookieStore.addCookie(new MyCookie(c)));
        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            HttpGet req = new HttpGet("http://localhost:7070/api/projects/PROD-24018957/workspaces/gql/pureModelContextData");
            try (CloseableHttpResponse res = client.execute(req))
            {
                PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(EntityUtils.toString(res.getEntity()), PureModelContextData.class);
                PureModel pureModel = this.modelManager.loadModel(pureModelContextData, "vX_X_X", profiles, "");
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
