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

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.service.ServiceModeling;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.Translator;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Node;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_query_sql_binding_fromPure_fromPure;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.generation.PlanGenerator.transformExecutionPlan;

@Api(tags = "SQL - Execution")
@Path("sql/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class SqlExecute
{

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server - SQL");
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions;
    private final MutableList<PlanTransformer> transformers;
    private final MetaDataServerConfiguration metadataServer;
    private final ServiceModeling serviceModeling;


    public SqlExecute(ModelManager modelManager, PlanExecutor planExecutor,
                      Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions,
                      MutableList<PlanTransformer> transformers, MetaDataServerConfiguration metadataServer,
                      DeploymentMode deploymentMode)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.metadataServer = metadataServer;
        this.serviceModeling = new ServiceModeling(modelManager, deploymentMode);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query in the context of a Mapping and a Runtime from a SDLC project")
    @Path("execute/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, @PathParam("projectId") String projectId, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo) throws Exception
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseStatement(sql);

        PureModelContextData pureModelContextData = loadModelContextData(profiles, request, projectId);
        String clientVersion = PureClientVersions.production;
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, clientVersion, profiles, "");
        Root_meta_external_query_sql_metamodel_Node query = new Translator().translate(node, pureModel);

        MutableList<Root_meta_legend_service_metamodel_Service> services = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                .collect(e -> (Service) e)
                .collect(e -> serviceModeling.compileService(e, pureModel.getContext(e)))
                .toList();

        Root_meta_pure_executionPlan_ExecutionPlan plan = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getPlansFromSQL_Service_MANY__Node_1__Extension_MANY__ExecutionPlan_1_(services, query, extensions.apply(pureModel), pureModel.getExecutionSupport());
        SingleExecutionPlan singleExecutionPlan = transformExecutionPlan(plan, pureModel, clientVersion, profiles, extensions.apply(pureModel), transformers);
        long start = System.currentTimeMillis();
        return this.execImpl(planExecutor, profiles, request.getRemoteUser(), SerializationFormat.defaultFormat, start, singleExecutionPlan);
    }

    protected PureModelContextData loadModelContextData(MutableList<CommonProfile> profiles, HttpServletRequest request, String project) throws PrivilegedActionException
    {
        Subject subject = ProfileManagerHelper.extractSubject(profiles);
        return subject == null ?
                getPureModelContextData(request, project) :
                Subject.doAs(subject, (PrivilegedExceptionAction<PureModelContextData>) () -> getPureModelContextData(request, project));
    }

    private PureModelContextData getPureModelContextData(HttpServletRequest request, String project)
    {
        CookieStore cookieStore = new BasicCookieStore();
        ArrayIterate.forEach(request.getCookies(), c -> cookieStore.addCookie(new MyCookie(c)));


        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            if (metadataServer == null || metadataServer.getSdlc() == null)
            {
                throw new EngineException("Please specify the metadataServer.sdlc information in the server configuration");
            }
            HttpGet req = new HttpGet("http://" + metadataServer.getSdlc().host + ":" + metadataServer.getSdlc().port + "/api/projects/" + project + "/pureModelContextData");
            try (CloseableHttpResponse res = client.execute(req))
            {
                ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                return mapper.readValue(res.getEntity().getContent(), PureModelContextData.class);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Response execImpl(PlanExecutor planExecutor, MutableList<CommonProfile> pm, String user, SerializationFormat format, long start, SingleExecutionPlan plan)
    {
        Result result = planExecutor.execute(plan, Maps.mutable.empty(), user, pm);
        LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
        MetricsHandler.observe("execute", start, System.currentTimeMillis());
        try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(pm, result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }

}
