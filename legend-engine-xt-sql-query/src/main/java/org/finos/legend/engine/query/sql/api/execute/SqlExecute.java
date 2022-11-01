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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
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
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Identifier;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Node;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Table;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_query_sql_binding_fromPure_fromPure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
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
    private final MetaDataServerConfiguration metadataserver;
    private final DeploymentMode deploymentMode;


    public SqlExecute(ModelManager modelManager, PlanExecutor planExecutor,
                      Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions,
                      MutableList<PlanTransformer> transformers, MetaDataServerConfiguration metadataserver,
                      DeploymentMode deploymentMode)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.metadataserver = metadataserver;
        this.deploymentMode = deploymentMode;
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a Mapping and a Runtime.")
    @Path("test2/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeService(@Context HttpServletRequest request, @PathParam("projectId") String projectId, String serviceId, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo) throws Exception
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        PureModelContextData pureModelContextData = loadModelContextData(profiles, request, projectId);
        Service service = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                .collect(e -> (Service) e)
                .detect(s -> s.pattern.equals(serviceId));
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, PureClientVersions.production, profiles, "");
        PureSingleExecution singleExecution = (PureSingleExecution) service.execution;
        Mapping mapping = pureModel.getMapping(singleExecution.mapping);
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(singleExecution.runtime, pureModel.getContext());
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(singleExecution.func.body, singleExecution.func.parameters, pureModel.getContext());
        String clientVersion = PureClientVersions.production;
        ExecutionContext context = new BaseExecutionContext();
        SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlanWithTrace(lambda, mapping, runtime,
                HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext()),
                pureModel, clientVersion, PlanPlatform.JAVA, profiles,
                extensions.apply(pureModel), transformers);
        long start = System.currentTimeMillis();
        return this.execImpl(planExecutor, profiles, request.getRemoteUser(), SerializationFormat.defaultFormat, start, singleExecutionPlan);
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a Mapping and a Runtime.")
    @Path("test3/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, @PathParam("projectId") String projectId, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo) throws Exception
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseStatement(sql);

        PureModel emptyPureModel = new PureModel(PureModelContextData.newBuilder().build(), profiles, deploymentMode);
        Root_meta_external_query_sql_metamodel_Node query = new Translator().translate(node, emptyPureModel);

        RichIterable<? extends Root_meta_external_query_sql_metamodel_Table> tables = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getTables_Node_1__Table_MANY_(query, emptyPureModel.getExecutionSupport());
        Root_meta_external_query_sql_metamodel_Identifier serviceId = tables.collect(Root_meta_external_query_sql_metamodel_Table::_name)
                .collect(RichIterable::getLast)
                .getOnly();

        PureModelContextData pureModelContextData = loadModelContextData(profiles, request, projectId);
        Service service = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                .collect(e -> (Service) e)
                .detect(s -> s.pattern.equals(serviceId._value()));

        String clientVersion = PureClientVersions.production;
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, clientVersion, profiles, "");

        PureSingleExecution singleExecution = (PureSingleExecution) service.execution;
        Mapping mapping = pureModel.getMapping(singleExecution.mapping);
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(singleExecution.runtime, pureModel.getContext());
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(singleExecution.func.body, singleExecution.func.parameters, pureModel.getContext());

        ExecutionContext context = new BaseExecutionContext();
        core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getPlansFromSQL_FunctionDefinition_1__Mapping_1__Runtime_1__Node_1__Extension_MANY__ExecutionPlan_1_(lambda, mapping, runtime, query, extensions.apply(pureModel), pureModel.getExecutionSupport());

        SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlanWithTrace(lambda, mapping, runtime,
                HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext()),
                pureModel, clientVersion, PlanPlatform.JAVA, profiles,
                extensions.apply(pureModel), transformers);
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
            if (metadataserver == null || metadataserver.getSdlc() == null)
            {
                throw new EngineException("Please specify the metadataserver.sdlc information in the server configuration");
            }
            HttpGet req = new HttpGet("http://" + metadataserver.getSdlc().host + ":" + metadataserver.getSdlc().port + "/api/projects/" + project + "/pureModelContextData");
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
