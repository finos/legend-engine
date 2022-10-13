/*
 *  Copyright 2022 Goldman Sachs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.finos.legend.engine.api.analytics;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.api.analytics.model.LineageAnalyticsInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_analytics_lineage_ReportLineage;
import org.finos.legend.pure.generated.Root_meta_analytics_lineage_flow_Flow;
import org.finos.legend.pure.generated.Root_meta_analytics_lineage_graph_Graph;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_lineage_scanProperties_PropertyPathNode;
import org.finos.legend.pure.generated.Root_meta_pure_lineage_scanProperties_propertyTree_PropertyPathTree;
import org.finos.legend.pure.generated.Root_meta_pure_lineage_scanRelations_RelationTree;
import org.finos.legend.pure.generated.core_analytics_lineage_fullAnalytics;
import org.finos.legend.pure.generated.core_analytics_lineage_graph;
import org.finos.legend.pure.generated.core_pure_lineage_scanProperties;
import org.finos.legend.pure.generated.core_relational_relational_extensions_extension;
import org.finos.legend.pure.generated.core_relational_relational_lineage_scanRelations_scanRelations;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/lineage")
public class LineageAnalytics
{

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final ModelManager modelManager;

    public LineageAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("model/propertyPathTree")
    @ApiOperation(value = "Analyze the function to get property path tree")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response functionTree(LineageAnalyticsInput lineageAnalyticsInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_FUNCTION_TREE_START).toString());
        try (Scope scope = GlobalTracer.get().buildSpan("Lineage: functionTree").startActive(true))
        {
            PureModel pureModel = this.modelManager.loadModel(lineageAnalyticsInput.model, lineageAnalyticsInput.clientVersion, profiles, null);
            LambdaFunction function = HelperValueSpecificationBuilder.buildLambda(lineageAnalyticsInput.function.body, lineageAnalyticsInput.function.parameters, pureModel.getContext());
            org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends Root_meta_pure_lineage_scanProperties_PropertyPathNode>> res = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_scanProperties_ValueSpecification_1__List_1__Function_MANY__Map_$0_1$__Res_$0_1$_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification) function._expressionSequence().getFirst(), new Root_meta_pure_functions_collection_List_Impl(""), Lists.fixedSize.empty(), new PureMap(Maps.fixedSize.empty()), pureModel.getExecutionSupport())._result();
            Root_meta_pure_lineage_scanProperties_propertyTree_PropertyPathTree propertyTree = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_propertyTree_buildPropertyTree_List_MANY__PropertyPathTree_1_(res, pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_FUNCTION_TREE_STOP, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(propertyTree, pureModel.getExecutionSupport()));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.LINEAGE_FUNCTION_TREE_ERROR, profiles);
        }
    }


    @POST
    @Path("model/class")
    @ApiOperation(value = "Analyze the function to get referenced model classes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response classLineage(LineageAnalyticsInput lineageAnalyticsInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_CLASS_LINEAGE_START).toString());

        try (Scope scope = GlobalTracer.get().buildSpan("Lineage: classLineage").startActive(true))
        {
            PureModel pureModel = this.modelManager.loadModel(lineageAnalyticsInput.model, lineageAnalyticsInput.clientVersion, profiles, null);
            LambdaFunction function = HelperValueSpecificationBuilder.buildLambda(lineageAnalyticsInput.function.body, lineageAnalyticsInput.function.parameters, pureModel.getContext());
            org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends Root_meta_pure_lineage_scanProperties_PropertyPathNode>> res = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_scanProperties_ValueSpecification_1__List_1__Function_MANY__Map_$0_1$__Res_$0_1$_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification) function._expressionSequence().getFirst(), new Root_meta_pure_functions_collection_List_Impl(""), Lists.fixedSize.empty(), new PureMap(Maps.fixedSize.empty()), pureModel.getExecutionSupport())._result();
            Root_meta_pure_lineage_scanProperties_propertyTree_PropertyPathTree propertyTree = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_propertyTree_buildPropertyTree_List_MANY__PropertyPathTree_1_(res, pureModel.getExecutionSupport());
            Root_meta_analytics_lineage_flow_Flow flow = core_analytics_lineage_fullAnalytics.Root_meta_analytics_lineage_flowClass_toFlowClass_FunctionDefinition_1__PropertyPathTree_1__Flow_1_(function, propertyTree, pureModel.getExecutionSupport());
            Root_meta_analytics_lineage_graph_Graph graph = core_analytics_lineage_graph.Root_meta_analytics_lineage_graph_toGraph_Flow_1__Graph_1_(flow, pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_CLASS_LINEAGE_STOP, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(graph, pureModel.getExecutionSupport()));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.LINEAGE_CLASS_LINEAGE_ERROR, profiles);
        }
    }

    @POST
    @Path("store/relational/database")
    @ApiOperation(value = "Analyze the function and mapping to get referenced databases and tables")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response databaseLineage(LineageAnalyticsInput lineageAnalyticsInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_DATABASE_LINEAGE_START).toString());

        if (lineageAnalyticsInput.mapping == null)
        {
            throw new RuntimeException("Unable to find a mapping in your request");
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Lineage: databaseLineage").startActive(true))
        {
            PureModel pureModel = this.modelManager.loadModel(lineageAnalyticsInput.model, lineageAnalyticsInput.clientVersion, profiles, null);
            LambdaFunction function = HelperValueSpecificationBuilder.buildLambda(lineageAnalyticsInput.function.body, lineageAnalyticsInput.function.parameters, pureModel.getContext());
            org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends Root_meta_pure_lineage_scanProperties_PropertyPathNode>> res = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_scanProperties_ValueSpecification_1__List_1__Function_MANY__Map_$0_1$__Res_$0_1$_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification) function._expressionSequence().getFirst(), new Root_meta_pure_functions_collection_List_Impl(""), Lists.fixedSize.empty(), new PureMap(Maps.fixedSize.empty()), pureModel.getExecutionSupport())._result();
            Root_meta_pure_lineage_scanProperties_propertyTree_PropertyPathTree propertyTree = core_pure_lineage_scanProperties.Root_meta_pure_lineage_scanProperties_propertyTree_buildPropertyTree_List_MANY__PropertyPathTree_1_(res, pureModel.getExecutionSupport());
            Root_meta_analytics_lineage_flow_Flow flow = core_analytics_lineage_fullAnalytics.Root_meta_analytics_lineage_flowDatabase_toFlowDatabase_FunctionDefinition_1__Mapping_MANY__PropertyPathTree_1__Flow_1_(function, Lists.fixedSize.of(pureModel.getMapping(lineageAnalyticsInput.mapping)), propertyTree, pureModel.getExecutionSupport());
            Root_meta_analytics_lineage_graph_Graph graph = core_analytics_lineage_graph.Root_meta_analytics_lineage_graph_toGraph_Flow_1__Graph_1_(flow, pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_DATABASE_LINEAGE_STOP, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(graph, pureModel.getExecutionSupport()));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.LINEAGE_DATABASE_LINEAGE_ERROR, profiles);
        }
    }

    @POST
    @Path("store/relational/reportColumn")
    @ApiOperation(value = "Analyze the function and mapping to get referenced database columns for projected columns")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reportLineage(LineageAnalyticsInput lineageAnalyticsInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_REPORT_LINEAGE_START).toString());

        if (lineageAnalyticsInput.mapping == null)
        {
            throw new RuntimeException("Unable to find a mapping in your request");
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Lineage: reportLineage").startActive(true))
        {
            PureModel pureModel = this.modelManager.loadModel(lineageAnalyticsInput.model, lineageAnalyticsInput.clientVersion, profiles, null);
            LambdaFunction function = HelperValueSpecificationBuilder.buildLambda(lineageAnalyticsInput.function.body, lineageAnalyticsInput.function.parameters, pureModel.getContext());
            Root_meta_analytics_lineage_ReportLineage reportLineage = core_analytics_lineage_fullAnalytics.Root_meta_analytics_lineage_buildReportLineage_ValueSpecification_1__Mapping_1__ReportLineage_1_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification) function._expressionSequence().getFirst(), pureModel.getMapping(lineageAnalyticsInput.mapping), pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_REPORT_LINEAGE_STOP, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(reportLineage, pureModel.getExecutionSupport()));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.LINEAGE_REPORT_LINEAGE_ERROR, profiles);
        }
    }

    @POST
    @Path("store/relational/relationTree")
    @ApiOperation(value = "Analyze the function and mapping to get relation join tree")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response relationTree(LineageAnalyticsInput lineageAnalyticsInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_RELATION_TREE_START).toString());

        if (lineageAnalyticsInput.mapping == null)
        {
            throw new RuntimeException("Unable to find a mapping in your request");
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Lineage: relationTree").startActive(true))
        {
            PureModel pureModel = this.modelManager.loadModel(lineageAnalyticsInput.model, lineageAnalyticsInput.clientVersion, profiles, null);
            LambdaFunction function = HelperValueSpecificationBuilder.buildLambda(lineageAnalyticsInput.function.body, lineageAnalyticsInput.function.parameters, pureModel.getContext());
            Root_meta_pure_lineage_scanRelations_RelationTree relationTree = core_relational_relational_lineage_scanRelations_scanRelations.Root_meta_pure_lineage_scanRelations_scanRelations_FunctionDefinition_1__Mapping_1__Extension_MANY__RelationTree_1_(function, pureModel.getMapping(lineageAnalyticsInput.mapping), CompiledSupport.toPureCollection(core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport())), pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(profiles, LoggingEventType.LINEAGE_RELATION_TREE_STOP, System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(relationTree, pureModel.getExecutionSupport()));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.LINEAGE_RELATION_TREE_ERROR, profiles);
        }
    }
}
