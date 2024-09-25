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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.generation.dataquality.DataQualityLambdaGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.request.RequestContextHelper;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecuteNodeParameterTransformationHelper;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.dataquality.model.DataQualityExecuteInput;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "DataQuality - Execution")
@Path("pure/v1/dataquality")
@Produces(MediaType.APPLICATION_JSON)
public class DataQualityExecute
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataQualityExecute.class);
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final ModelManager modelManager;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions;
    private final Iterable<? extends PlanTransformer> transformers;
    private final PlanExecutor planExecutor;
    private final DataQualityPlanLoader dataQualityPlanLoader;


    public DataQualityExecute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, Iterable<? extends PlanTransformer> transformers, MetaDataServerConfiguration metaDataServerConfiguration, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        this.modelManager = modelManager;
        this.extensions = extensions;
        this.transformers = transformers;
        this.planExecutor = planExecutor;
        this.dataQualityPlanLoader = new DataQualityPlanLoader(metaDataServerConfiguration.sdlc, httpClientProvider);
        MetricsHandler.createMetrics(this.getClass());

    }

    @POST
    @Path("generatePlan")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    //@Prometheus(name = "generate plan")
    public Response generatePlan(DataQualityExecuteTrialInput dataQualityExecuteInput, @ApiParam(hidden = true) @Pac4JProfileManager() ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_GENERATE_PLAN_START).toString());
        try (Scope scope = GlobalTracer.get().buildSpan("DataQuality: planGeneration").startActive(true))
        {
            // 1. load pure model from PureModelContext
            PureModel pureModel = this.modelManager.loadModel(dataQualityExecuteInput.model, dataQualityExecuteInput.clientVersion, identity, null);
            // 2. call DQ PURE func to generate lambda
            LambdaFunction dqLambdaFunction = DataQualityLambdaGenerator.generateLambda(pureModel, dataQualityExecuteInput.packagePath);
            // 3. Generate Plan from the lambda generated in the previous step
            SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(dqLambdaFunction, null, null, null, pureModel, dataQualityExecuteInput.clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers);// since lambda has from function we dont need mapping, runtime and context
            LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_GENERATE_PLAN_END, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(identity.getName(), singleExecutionPlan, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_PLAN_ERROR, identity.getName());
        }
    }

    @POST
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    //@Prometheus(name = "generate plan")
    public Response execute(@Context HttpServletRequest request, DataQualityExecuteTrialInput dataQualityExecuteInput, @ApiParam(hidden = true) @Pac4JProfileManager() ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_PLAN_EXECUTION_START).toString());
        try (Scope scope = GlobalTracer.get().buildSpan("DataQuality: executeTrial").startActive(true))
        {
            // 1. load pure model from PureModelContext
            PureModel pureModel = this.modelManager.loadModel(dataQualityExecuteInput.model, dataQualityExecuteInput.clientVersion, identity, null);
            // 2. call DQ PURE func to generate lambda
            LambdaFunction dqLambdaFunction = DataQualityLambdaGenerator.generateLambdaForTrial(pureModel, dataQualityExecuteInput.packagePath, dataQualityExecuteInput.queryLimit);
            // 3. Generate Plan from the lambda generated in the previous step
            SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(dqLambdaFunction, null, null, null, pureModel, dataQualityExecuteInput.clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers);// since lambda has from function we dont need mapping, runtime and context
            // 4. execute plan
            Map<String, Object> lambdaParameterMap = dataQualityExecuteInput.lambdaParameterValues != null ? dataQualityExecuteInput.lambdaParameterValues.stream().collect(Collectors.<ParameterValue, String, Object>toMap(p -> p.name, p -> p.value.accept(new PrimitiveValueSpecificationToObjectVisitor()))) : Maps.mutable.empty();
            Response response = executePlan(request, identity, start, singleExecutionPlan, lambdaParameterMap);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            }
            LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_PLAN_EXECUTION_END, System.currentTimeMillis() - start).toString());
            return response;
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, identity.getName());
        }
    }

    @POST
    @Path("lambda")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambda(@Context HttpServletRequest request, DataQualityExecuteTrialInput dataQualityExecuteInput, @ApiParam(hidden = true) @Pac4JProfileManager() ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("DataQuality: lambda").startActive(true))
        {
            // 1. load pure model from PureModelContext
            PureModel pureModel = this.modelManager.loadModel(dataQualityExecuteInput.model, dataQualityExecuteInput.clientVersion, identity, null);
            // 2. call DQ PURE func to generate lambda
            LambdaFunction dqLambdaFunction = DataQualityLambdaGenerator.generateLambdaForTrial(pureModel, dataQualityExecuteInput.packagePath, dataQualityExecuteInput.queryLimit);
            Lambda lambda = DataQualityLambdaGenerator.transformLambda(dqLambdaFunction, pureModel, this.extensions);
            return ManageConstantResult.manageResult(identity.getName(), lambda, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, identity.getName());
        }
    }


    @POST
    @Path("executeArtifacts")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    //@Prometheus(name = "generate plan")
    public Response execute(@Context HttpServletRequest request, DataQualityExecuteInput dataQualityParameterValue, @ApiParam(hidden = true) @Pac4JProfileManager() ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_ARTIFACT_PLAN_EXECUTION_START).toString());
        try (Scope scope = GlobalTracer.get().buildSpan("DataQuality: execute").startActive(true))
        {
            // 1. fetch plan from artifacts
            SingleExecutionPlan singleExecutionPlan = this.dataQualityPlanLoader.fetchPlanFromSDLC(identity, dataQualityParameterValue);

             // 2. execute plan
            Map<String, Object> lambdaParameterMap = dataQualityParameterValue.lambdaParameterValues != null ? dataQualityParameterValue.lambdaParameterValues.stream().collect(Collectors.<ParameterValue, String, Object>toMap(p -> p.name, p -> p.value.accept(new PrimitiveValueSpecificationToObjectVisitor()))) : Maps.mutable.empty();
            Response response = executePlan(request, identity, start, singleExecutionPlan, lambdaParameterMap);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            }
            LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_ARTIFACT_PLAN_EXECUTION_END, System.currentTimeMillis() - start).toString());
            return response;
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, identity.getName());
        }
    }

    private Response executePlan(HttpServletRequest request, Identity identity, long start, SingleExecutionPlan singleExecutionPlan, Map<String, Object> lambdaParameterMap)
    {
        MutableMap<String, Result> parametersToConstantResult = Maps.mutable.empty();
        ExecuteNodeParameterTransformationHelper.buildParameterToConstantResult(singleExecutionPlan, lambdaParameterMap, parametersToConstantResult);
        Result result = planExecutor.execute(singleExecutionPlan, parametersToConstantResult, request.getRemoteUser(), identity, null, RequestContextHelper.RequestContext(request));
        return this.wrapInResponse(identity, SerializationFormat.DEFAULT, start, result);
    }


    private Response wrapInResponse(Identity identity, SerializationFormat format, long start, Result result)
    {
        LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
        MetricsHandler.observe("execute", start, System.currentTimeMillis());
        try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return ResultManager.manageResult(identity.getName(), result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }

    @POST
    @Path("propertyPathTree")
    @ApiOperation(value = "Analyze the DataQuality tree to generate property path tree for given constraints")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response generatePropertyPathTree(DataQualityExecuteTrialInput input,
                                             @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_PROPERTY_PATH_TREE_START).toString());
        try
        {
            PureModel pureModel = this.modelManager.loadModel(input.model, input.clientVersion, identity, null);
            PackageableElement packageableElement = pureModel.getPackageableElement(input.packagePath);
            if (!DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement))
            {
                return ExceptionTool.exceptionManager("Invalid Element", LoggingEventType.MODEL_RESOLVE_ERROR, identity.getName());
            }
            RootGraphFetchTree rootGraphFetchTree = DataQualityPropertyPathTreeGenerator.getPropertyPathTree(((Root_meta_external_dataquality_DataQuality)packageableElement)._validationTree(), input.clientVersion, pureModel, this.extensions.apply(pureModel));
            LOGGER.info(new LogInfo(identity.getName(), DataQualityExecutionLoggingEventType.DATAQUALITY_PROPERTY_PATH_TREE_END).toString());
            return ManageConstantResult.manageResult(identity.getName(), rootGraphFetchTree);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.MODEL_RESOLVE_ERROR, identity.getName());
        }

    }

}
