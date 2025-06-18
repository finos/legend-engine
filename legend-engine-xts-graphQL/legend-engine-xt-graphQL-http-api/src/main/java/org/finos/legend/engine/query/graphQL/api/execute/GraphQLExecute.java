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

package org.finos.legend.engine.query.graphQL.api.execute;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLDevCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLProdDataspaceCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLProdMappingRuntimeCacheKey;
import org.finos.legend.engine.query.graphQL.api.execute.directives.IGraphQLDirectiveExtension;
import org.finos.legend.engine.query.graphQL.api.execute.model.PlansResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.query.graphQL.api.execute.model.error.GraphQLErrorMain;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_graphFetch;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_introspection_query;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_print;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.finos.legend.engine.query.graphQL.api.execute.model.GraphQLCachableVisitorHelper.createCachableGraphQLQuery;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlDevError;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlDevExecution;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdError;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdErrorWithDataSpace;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdExecution;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdExecutionWithDataSpace;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "GraphQL - Execution")
@Path("graphQL/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLExecute extends GraphQL
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GraphQLExecute.class);
    private final PlanExecutor planExecutor;
    private final Iterable<? extends PlanTransformer> transformers;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final GraphQLPlanCache graphQLPlanCache;
    private final List<IGraphQLDirectiveExtension> graphQLExecuteExtensions = Lists.mutable.empty();

    public GraphQLExecute(ModelManager modelManager, PlanExecutor planExecutor, MetaDataServerConfiguration metadataserver, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Iterable<? extends PlanTransformer> transformers, GraphQLPlanCache planCache)
    {
        super(modelManager, metadataserver);
        this.planExecutor = planExecutor;
        this.transformers = transformers;
        this.extensionsFunc = extensionsFunc;
        this.graphQLPlanCache = planCache;
        for (IGraphQLDirectiveExtension graphQLExecuteExtension : ServiceLoader.load(IGraphQLDirectiveExtension.class))
        {
            this.graphQLExecuteExtensions.add(graphQLExecuteExtension);
        }
    }

    public GraphQLExecute(ModelManager modelManager, PlanExecutor planExecutor, MetaDataServerConfiguration metadataserver, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Iterable<? extends PlanTransformer> transformers)
    {
        this(modelManager, planExecutor, metadataserver, extensionsFunc, transformers, null);
    }

    private Response generateQueryPlans(String queryClassPath, String mappingPath, Query query, PureModel pureModel) throws IOException
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        Mapping mapping = pureModel.getMapping(mappingPath);
        Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(GraphQLExecute.class.getClassLoader().getResourceAsStream("exampleRuntime.json"), org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime.class), pureModel.getContext());

        Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);
        if (isQueryIntrospection(GraphQLExecutionHelper.findQuery(document)))
        {
            return Response.ok("").type(MediaType.TEXT_HTML_TYPE).build();
        }
        else
        {
            RichIterable<? extends Pair<? extends String, ? extends Root_meta_pure_executionPlan_ExecutionPlan>> purePlans = core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_getPlansFromGraphQL_Class_1__Mapping_1__Runtime_1__Document_1__Extension_MANY__Pair_MANY_(_class, mapping, runtime, queryDoc, extensions, pureModel.getExecutionSupport());
            Collection<PlansResult.PlanUnit> plans = Iterate.collect(purePlans, p ->
                    {
                        Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._second(), "ID", pureModel, extensions);
                        try
                        {
                            return new PlansResult.PlanUnit(p._first(),
                                    ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(PlanGenerator.serializeToJSON(nPlan, PureClientVersions.production, pureModel, extensions, this.transformers), ExecutionPlan.class),
                                    core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Boolean_1__Extension_MANY__String_1_(nPlan, true, extensions, pureModel.getExecutionSupport())
                            );
                        }
                        catch (JsonProcessingException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
            );
            return Response.ok(new PlansResult(plans)).build();
        }
    }

    @Deprecated
    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a mapping and a runtime from a SDLC project", notes = "DEPRECATED: use the generatePlans APIs that include a 'workspace' or 'groupWorkspace' path param")
    @Path("generatePlans/dev/{projectId}/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlansDev(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.generatePlansDevWithUserWorkspace(request, projectId, workspaceId, queryClassPath, mappingPath, query, pm);
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a mapping and a runtime from a SDLC project (user workspace)")
    @Path("generatePlans/dev/{projectId}/workspace/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlansDevWithUserWorkspace(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.generatePlansDevWithWorkspaceImpl(request, projectId, workspaceId, false, queryClassPath, mappingPath, query, pm);
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a mapping and a runtime from a SDLC project (group workspace)")
    @Path("generatePlans/dev/{projectId}/groupWorkspace/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlansDevWithGroupWorkspace(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.generatePlansDevWithWorkspaceImpl(request, projectId, workspaceId, true, queryClassPath, mappingPath, query, pm);
    }

    private Response generatePlansDevWithWorkspaceImpl(HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace, String queryClassPath, String mappingPath, Query query, ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            return generateQueryPlans(queryClassPath, mappingPath, query, loadSDLCProjectModel(identity, request, projectId, workspaceId, isGroupWorkspace));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
        }
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a mapping and a runtime")
    @Path("generatePlans/prod/{groupId}/{artifactId}/{versionId}/query/{queryClassPath}/mapping/{mappingPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlansProd(@Context HttpServletRequest request, @PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("versionId") String versionId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            return generateQueryPlans(queryClassPath, mappingPath, query, loadProjectModel(identity, groupId, artifactId, versionId));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
        }
    }

    private Response executeIntrospection(String queryClassPath, Document document, PureModel pureModel)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);

        return Response.ok("{" +
                "  \"data\":" + core_external_query_graphql_transformation_transformation_introspection_query.Root_meta_external_query_graphQL_transformation_introspection_graphQLIntrospectionQuery_Class_1__Document_1__String_1_(_class, queryDoc, pureModel.getExecutionSupport()) +
                "}").type(MediaType.TEXT_HTML_TYPE).build();
    }

    private Response executeGraphQLQuery(Document document, GraphQLCacheKey graphQLCacheKey, Identity identity, Callable<PureModel> modelLoader) throws Exception
    {
        List<SerializedNamedPlans> planWithSerialized;
        OperationDefinition graphQLQuery = GraphQLExecutionHelper.findQuery(document);
        PureModel pureModel = null;
        try
        {
            if (isQueryIntrospection(graphQLQuery))
            {
                pureModel = modelLoader.call();
                return executeIntrospection(graphQLCacheKey.getQueryClassPath(), document, pureModel);
            }
            else
            {
                if (graphQLPlanCache != null)
                {
                    planWithSerialized = graphQLPlanCache.getIfPresent(graphQLCacheKey);
                    if (planWithSerialized == null) //cache miss, generate the plan and add to the cache
                    {
                        LOGGER.debug(new LogInfo(identity.getName(), LoggingEventType.GRAPHQL_EXECUTE, "Cache miss. Generating new plan").toString());
                        pureModel = modelLoader.call();
                        planWithSerialized = getSerializedNamedPlans(document, graphQLCacheKey, graphQLQuery, pureModel);
                        graphQLPlanCache.put(graphQLCacheKey, planWithSerialized);
                    }
                    else
                    {
                        LOGGER.debug(new LogInfo(identity.getName(), LoggingEventType.GRAPHQL_EXECUTE, "Cache hit. Using previously cached plan").toString());
                    }
                }
                else   //no cache so we generate the plan
                {
                    pureModel = modelLoader.call();
                    planWithSerialized = getSerializedNamedPlans(document, graphQLCacheKey, graphQLQuery, pureModel);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_ERROR, e).toString());
            Span activeSpan = GlobalTracer.get().activeSpan();
            if (activeSpan != null)
            {
                Tags.ERROR.set(activeSpan, true);
                activeSpan.setTag("error.message", e.getMessage());
            }
            throw e;
        }
        return execute(identity, planWithSerialized, graphQLQuery);
    }

    private List<SerializedNamedPlans> getSerializedNamedPlans(Document document, GraphQLCacheKey graphQLCacheKey, OperationDefinition graphQLQuery, PureModel pureModel)
    {
        List<SerializedNamedPlans> planWithSerialized;
        if (graphQLCacheKey instanceof GraphQLDevCacheKey)
        {
            GraphQLDevCacheKey key = (GraphQLDevCacheKey) graphQLCacheKey;
            planWithSerialized = buildPlanWithParameter(key.getQueryClassPath(), key.getMappingPath(), key.getRuntimePath(), document, graphQLQuery, pureModel, graphQLCacheKey);
        }
        else if (graphQLCacheKey instanceof GraphQLProdMappingRuntimeCacheKey)
        {
            GraphQLProdMappingRuntimeCacheKey key = (GraphQLProdMappingRuntimeCacheKey) graphQLCacheKey;
            planWithSerialized = buildPlanWithParameter(key.getQueryClassPath(), key.getMappingPath(), key.getRuntimePath(), document, graphQLQuery, pureModel, graphQLCacheKey);
        }
        else if (graphQLCacheKey instanceof GraphQLProdDataspaceCacheKey)
        {
            GraphQLProdDataspaceCacheKey key = (GraphQLProdDataspaceCacheKey) graphQLCacheKey;
            planWithSerialized = buildPlanWithParameterUsingDataspace(key.getQueryClassPath(), key.getDataspacePath(), key.getExecutionContext(), document, graphQLQuery, pureModel, graphQLCacheKey);
        }
        else
        {
            throw new UnsupportedOperationException("Invalid graphql cache key");
        }
        return planWithSerialized;
    }

    private Response execute(Identity identity, List<SerializedNamedPlans> planWithSerialized, OperationDefinition graphQLQuery)
    {
        return Response.ok(
                (StreamingOutput) outputStream ->
                {
                    try (JsonGenerator generator = new JsonFactory().createGenerator(outputStream)
                            .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
                            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);)
                    {
                        generator.writeStartObject();
                        generator.setCodec(new ObjectMapper());
                        generator.writeFieldName("data");
                        generator.writeStartObject();

                        planWithSerialized.stream().filter(serializedNamedPlans -> GraphQLExecutionHelper.isARootField(serializedNamedPlans.propertyName, graphQLQuery)).forEach(p ->
                        {
                            JsonStreamingResult result = null;
                            try
                            {
                                Map<String, Result> parameterMap = GraphQLExecutionHelper.getParameterMap(graphQLQuery, p.propertyName);

                                generator.writeFieldName(p.propertyName);
                                result = (JsonStreamingResult) planExecutor.execute(p.serializedPlan, parameterMap, null, identity);
                                result.getJsonStream().accept(generator);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                            finally
                            {
                                if (result != null)
                                {
                                    result.close();
                                }
                            }
                        });
                        generator.writeEndObject();
                        Map<String, ?> extensions = this.computeExtensionsField(graphQLQuery, planWithSerialized, identity);
                        if (!extensions.isEmpty())
                        {
                            generator.writeFieldName("extensions");
                            generator.writeObject(extensions);
                        }
                        generator.writeEndObject();
                    }
                }).build();
    }

    private Map<String, ?> computeExtensionsField(OperationDefinition query, List<SerializedNamedPlans> serializedNamedPlans, Identity identity)
    {
        Map<String, Map<String, Object>> m = new HashMap<>();
        List<Directive> directives = GraphQLExecutionHelper.findDirectives(query);
        String rootFieldName = ((Field) query.selectionSet.get(0)).name; // assuming there's only one field in the selection set
        if (!directives.isEmpty())
        {
            m.put(rootFieldName, new HashMap<>());
            directives.forEach(directive ->
            {
                SingleExecutionPlan plan = serializedNamedPlans.stream().filter(serializedNamedPlan -> serializedNamedPlan.propertyName.equals(GraphQLExecutionHelper.getPlanNameForDirective(rootFieldName, directive))).findFirst().get().serializedPlan;
                Map<String, Result> parameterMap = GraphQLExecutionHelper.getParameterMap(query, rootFieldName);
                Object object = getExtensionForDirective(directive).executeDirective(directive, plan, planExecutor, parameterMap, identity);
                m.get(rootFieldName).put(directive.name, object);
            });
        }
        return m;
    }

    IGraphQLDirectiveExtension getExtensionForDirective(Directive directive)
    {
        List<IGraphQLDirectiveExtension> modifiedListOfExtensions = graphQLExecuteExtensions.stream().filter(
                graphQLExecuteExtension -> graphQLExecuteExtension.getSupportedDirectives().contains(directive.name)
        ).collect(Collectors.toList());
        if (modifiedListOfExtensions.size() == 0)
        {
            throw new RuntimeException("No extensions found for " + directive.name);
        }
        else if (modifiedListOfExtensions.size() > 1)
        {
            throw new RuntimeException("Too many extensions found for " + directive.name);
        }
        return modifiedListOfExtensions.get(0);
    }

    private List<SerializedNamedPlans> buildExtensionsPlanWithParameter(String rootFieldName, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Mapping mapping, Root_meta_core_runtime_Runtime runtime, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey)
    {
        List<Directive> directives = GraphQLExecutionHelper.findDirectives(query);
        List<SerializedNamedPlans> serializedNamedPlans = Lists.mutable.empty();
        directives.forEach(directive ->
        {
            SingleExecutionPlan plan = (SingleExecutionPlan) getExtensionForDirective(directive).planDirective(
                    document,
                    pureModel,
                    _class,
                    mapping,
                    runtime,
                    this.extensionsFunc.apply(pureModel),
                    this.transformers
            );
            serializedNamedPlans.add(new SerializedNamedPlans(GraphQLExecutionHelper.getPlanNameForDirective(rootFieldName, directive), plan));
        });
        return serializedNamedPlans;
    }

    private Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext getDataspaceExecutionContext(String dataspacePath, String executionContext, PureModel pureModel)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(dataspacePath);
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace, () -> "Can't find data space '" + dataspacePath + "'");
        if (executionContext.equals("defaultExecutionContext"))
        {
            return ((Root_meta_pure_metamodel_dataSpace_DataSpace) packageableElement)._executionContexts().select(dataSpaceExecutionContext -> dataSpaceExecutionContext._name().equals(((Root_meta_pure_metamodel_dataSpace_DataSpace) packageableElement)._defaultExecutionContext()._name())).toList().get(0);
        }
        else
        {
            try
            {
                return ((Root_meta_pure_metamodel_dataSpace_DataSpace) packageableElement)._executionContexts().select(dataSpaceExecutionContext -> dataSpaceExecutionContext._name().equals(executionContext)).toList().get(0);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Invalid execution context " + executionContext, e);
            }
        }
    }

    private List<SerializedNamedPlans> buildPlanWithParameterUsingDataspace(String queryClassPath, String dataspacePath, String executionContext, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);

        Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext executionContextPureElement = getDataspaceExecutionContext(dataspacePath, executionContext, pureModel);
        Mapping mapping = executionContextPureElement._mapping();
        Root_meta_core_runtime_Runtime runtime = executionContextPureElement._defaultRuntime()._runtimeValue();
        return getSerializedNamedPlans(pureModel, extensions, _class, mapping, runtime, document, query, queryDoc, graphQLCacheKey);
    }

    private List<SerializedNamedPlans> buildPlanWithParameter(String queryClassPath, String mappingPath, String runtimePath, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);

        Mapping mapping = pureModel.getMapping(mappingPath);
        Root_meta_core_runtime_Runtime runtime = pureModel.getRuntime(runtimePath);
        return getSerializedNamedPlans(pureModel, extensions, _class, mapping, runtime, document, query, queryDoc, graphQLCacheKey);
    }

    private List<SerializedNamedPlans> getSerializedNamedPlans(
            PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions,
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class,
            Mapping mapping,
            Root_meta_core_runtime_Runtime runtime,
            Document document,
            OperationDefinition query,
            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc,
            GraphQLCacheKey graphQLCacheKey
    )
    {
        RichIterable<? extends Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan> purePlans = core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_graphQLExecutableToPlansWithParameters_Class_1__Document_1__Mapping_1__Runtime_1__Extension_MANY__NamedExecutionPlan_MANY_(_class, queryDoc, mapping, runtime, extensions, pureModel.getExecutionSupport());
        List<SerializedNamedPlans> plans = purePlans.toList().stream().map(p ->
        {
            Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._plan(), "ID", pureModel, extensions);
            SerializedNamedPlans serializedPlans = new SerializedNamedPlans();
            serializedPlans.propertyName = p._name();
            serializedPlans.serializedPlan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(nPlan, PureClientVersions.production, pureModel, extensions, this.transformers));
            return serializedPlans;
        }).collect(Collectors.toList());
        List<List<SerializedNamedPlans>> extensionPlans = plans.stream().map(plan ->
                buildExtensionsPlanWithParameter(plan.propertyName, _class, mapping, runtime, document, query, pureModel, graphQLCacheKey)
        ).collect(Collectors.toList());
        extensionPlans.forEach(plans::addAll);
        return plans;
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a mapping and a runtime from a SDLC project", notes = "DEPRECATED: use the execute APIs that include a 'workspace' or 'groupWorkspace' path param")
    @Path("execute/dev/{projectId}/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}/runtime/{runtimePath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executeDev(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, @PathParam("runtimePath") String runtimePath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.executeDevWithUserWorkspace(request, projectId, workspaceId, queryClassPath, mappingPath, runtimePath, query, pm);
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a mapping and a runtime from a SDLC project (user workspace)")
    @Path("execute/dev/{projectId}/workspace/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}/runtime/{runtimePath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executeDevWithUserWorkspace(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, @PathParam("runtimePath") String runtimePath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.executeDevImpl(request, projectId, workspaceId, false, queryClassPath, mappingPath, runtimePath, query, pm);
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a mapping and a runtime from a SDLC project (group workspace)")
    @Path("execute/dev/{projectId}/groupWorkspace/{workspaceId}/query/{queryClassPath}/mapping/{mappingPath}/runtime/{runtimePath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executeDevWithGroupWorkspace(@Context HttpServletRequest request, @PathParam("projectId") String projectId, @PathParam("workspaceId") String workspaceId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, @PathParam("runtimePath") String runtimePath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return this.executeDevImpl(request, projectId, workspaceId, true, queryClassPath, mappingPath, runtimePath, query, pm);
    }

    private Response executeDevImpl(HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace, String queryClassPath, String mappingPath, String runtimePath, Query query, ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            long startTime = System.currentTimeMillis();
            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            Document cachableGraphQLQuery = createCachableGraphQLQuery(document);
            GraphQLDevCacheKey key = new GraphQLDevCacheKey(projectId, workspaceId, queryClassPath, mappingPath, runtimePath, objectMapper.writeValueAsString(cachableGraphQLQuery));

            Response response = this.executeGraphQLQuery(document, key, identity, () -> loadSDLCProjectModel(identity, request, projectId, workspaceId, isGroupWorkspace));
            observeGraphqlDevExecution(projectId, workspaceId, queryClassPath, mappingPath, runtimePath, startTime, System.currentTimeMillis());
            return response;
        }
        catch (Exception ex)
        {
            observeGraphqlDevError(ex, projectId, workspaceId, queryClassPath, mappingPath, runtimePath);
            return Response.ok(new GraphQLErrorMain(ex.getMessage())).build();
        }
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a mapping and a runtime")
    @Path("execute/prod/{groupId}/{artifactId}/{versionId}/query/{queryClassPath}/mapping/{mappingPath}/runtime/{runtimePath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executeProd(@Context HttpServletRequest request, @PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("versionId") String versionId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, @PathParam("runtimePath") String runtimePath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            long startTime = System.currentTimeMillis();
            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            GraphQLProdMappingRuntimeCacheKey key = new GraphQLProdMappingRuntimeCacheKey(groupId, artifactId, versionId, mappingPath, runtimePath, queryClassPath, objectMapper.writeValueAsString(createCachableGraphQLQuery(document)));

            Response response = this.executeGraphQLQuery(document, key, identity, () -> loadProjectModel(identity, groupId, artifactId, versionId));
            observeGraphqlProdExecution(groupId, artifactId, versionId, queryClassPath, mappingPath, runtimePath, startTime, System.currentTimeMillis());
            return response;
        }
        catch (Exception ex)
        {
            observeGraphqlProdError(ex, groupId, artifactId, versionId, queryClassPath, mappingPath, runtimePath);
            return Response.ok(new GraphQLErrorMain(ex.getMessage())).build();
        }
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a mapping and a runtime")
    @Path("execute/prod/{groupId}/{artifactId}/{versionId}/query/{queryClassPath}/dataspace/{dataspacePath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executeProdWithDataspace(@Context HttpServletRequest request, @PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("versionId") String versionId, @PathParam("dataspacePath") String dataspacePath, @QueryParam("executionContext") @DefaultValue("defaultExecutionContext") String executionContext, @PathParam("queryClassPath") String queryClassPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            long startTime = System.currentTimeMillis();
            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            GraphQLProdDataspaceCacheKey key = new GraphQLProdDataspaceCacheKey(groupId, artifactId, versionId, dataspacePath, executionContext, queryClassPath, objectMapper.writeValueAsString(createCachableGraphQLQuery(document)));

            Response response = this.executeGraphQLQuery(document, key, identity, () -> loadProjectModel(identity, groupId, artifactId, versionId));
            observeGraphqlProdExecutionWithDataSpace(groupId, artifactId, versionId, queryClassPath, dataspacePath, startTime, System.currentTimeMillis());
            return response;
        }
        catch (Exception ex)
        {
            observeGraphqlProdErrorWithDataSpace(ex, groupId, artifactId, versionId, queryClassPath, dataspacePath);
            return Response.ok(new GraphQLErrorMain(ex.getMessage())).build();
        }
    }

    private boolean isQueryIntrospection(OperationDefinition operationDefinition)
    {
        List<Selection> selections = operationDefinition.selectionSet;
        return !selections.isEmpty() && selections.get(0) instanceof Field && ((Field) selections.get(0)).name.equals("__schema");
    }


}
