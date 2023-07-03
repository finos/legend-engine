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
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.ExecutableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationType;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.DirectiveDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InterfaceTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ScalarTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.Type;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeSystemDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.UnionTypeDefinition;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLDevCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLProdCacheKey;
import org.finos.legend.engine.query.graphQL.api.execute.model.PlansResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.query.graphQL.api.execute.model.error.GraphQLErrorMain;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_graphFetch;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_introspection_query;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_print;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import static org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecutionHelper.argumentValueToObject;
import static org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecutionHelper.extractFieldByName;
import static org.finos.legend.engine.query.graphQL.api.execute.model.GraphQLCachableVisitorHelper.createCachableGraphQLQuery;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "GraphQL - Execution")
@Path("graphQL/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLExecute extends GraphQL
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final PlanExecutor planExecutor;
    private final Iterable<? extends PlanTransformer> transformers;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final GraphQLPlanCache graphQLPlanCache;

    public GraphQLExecute(ModelManager modelManager, PlanExecutor planExecutor, MetaDataServerConfiguration metadataserver, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Iterable<? extends PlanTransformer> transformers, GraphQLPlanCache planCache)
    {
        super(modelManager, metadataserver);
        this.planExecutor = planExecutor;
        this.transformers = transformers;
        this.extensionsFunc = extensionsFunc;
        this.graphQLPlanCache = planCache;

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
        Root_meta_pure_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(GraphQLExecute.class.getClassLoader().getResourceAsStream("exampleRuntime.json"), org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime.class), pureModel.getContext());

        Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);
        if (isQueryIntrospection(findQuery(document)))
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
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            return generateQueryPlans(queryClassPath, mappingPath, query, loadSDLCProjectModel(profiles, request, projectId, workspaceId, isGroupWorkspace));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a mapping and a runtime")
    @Path("generatePlans/prod/{groupId}/{artifactId}/{versionId}/query/{queryClassPath}/mapping/{mappingPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlansProd(@Context HttpServletRequest request, @PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("versionId") String versionId, @PathParam("queryClassPath") String queryClassPath, @PathParam("mappingPath") String mappingPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            return generateQueryPlans(queryClassPath, mappingPath, query, loadProjectModel(profiles, groupId, artifactId, versionId));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    private Response executeGraphQLQuery(String queryClassPath, String mappingPath, String runtimePath, Document document, GraphQLCacheKey graphQLCacheKey, MutableList<CommonProfile> profiles, Callable<PureModel> modelLoader)
    {
        List<SerializedNamedPlans> planWithSerialized;
        OperationDefinition graphQLQuery = findQuery(document);

        try
        {
            if (isQueryIntrospection(graphQLQuery))
            {
                PureModel pureModel = modelLoader.call();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
                org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);

                return Response.ok("{" +
                        "  \"data\":" + core_external_query_graphql_transformation_transformation_introspection_query.Root_meta_external_query_graphQL_transformation_introspection_graphQLIntrospectionQuery_Class_1__Document_1__String_1_(_class, queryDoc, pureModel.getExecutionSupport()) +
                        "}").type(MediaType.TEXT_HTML_TYPE).build();
            }
            else
            {
                if (graphQLPlanCache != null)
                {
                    planWithSerialized = graphQLPlanCache.getIfPresent(graphQLCacheKey);
                    if (planWithSerialized == null) //cache miss, generate the plan and add to the cache
                    {
                        LOGGER.debug(new LogInfo(profiles, LoggingEventType.GRAPHQL_EXECUTE, "Cache miss. Generating new plan").toString());
                        PureModel pureModel = modelLoader.call();
                        planWithSerialized = buildPlanWithParameter(queryClassPath, mappingPath, runtimePath, document, graphQLQuery, pureModel, graphQLCacheKey);
                        graphQLPlanCache.put(graphQLCacheKey, planWithSerialized);
                    }
                    else
                    {
                        LOGGER.debug(new LogInfo(profiles, LoggingEventType.GRAPHQL_EXECUTE, "Cache hit. Using previously cached plan").toString());
                    }
                }
                else   //no cache so we generate the plan
                {
                    PureModel pureModel = modelLoader.call();
                    planWithSerialized = buildPlanWithParameter(queryClassPath, mappingPath, runtimePath, document, graphQLQuery, pureModel, graphQLCacheKey);

                }
            }
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
        List<SerializedNamedPlans> finalPlanWithSerialized = planWithSerialized;
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

                        finalPlanWithSerialized.forEach(p ->
                        {
                            JsonStreamingResult result = null;
                            try
                            {
                                Map<String, Result> parameterMap = new HashMap<>();
                                extractFieldByName(graphQLQuery, p.propertyName).arguments.stream().forEach(a -> parameterMap.put(a.name, new ConstantResult(argumentValueToObject(a.value))));

                                generator.writeFieldName(p.propertyName);
                                result = (JsonStreamingResult) planExecutor.execute(p.serializedPlan, parameterMap, null, profiles);
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
                        generator.writeEndObject();
                    }
                }).build();
    }

    private List<SerializedNamedPlans> buildPlanWithParameter(String queryClassPath, String mappingPath, String runtimePath, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        Mapping mapping = pureModel.getMapping(mappingPath);
        Root_meta_pure_runtime_Runtime runtime = pureModel.getRuntime(runtimePath);
        org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = toPureModel(document, pureModel);
        RichIterable<? extends Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan> purePlans = core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_graphQLExecutableToPlansWithParameters_Class_1__Document_1__Mapping_1__Runtime_1__Extension_MANY__NamedExecutionPlan_MANY_(_class, queryDoc, mapping, runtime, extensions, pureModel.getExecutionSupport());
        List<SerializedNamedPlans> plans = purePlans.toList().stream().map(p ->
        {
            Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._plan(), "ID", pureModel, extensions);
            SerializedNamedPlans serializedPlans = new SerializedNamedPlans();
            serializedPlans.propertyName = p._name();
            serializedPlans.serializedPlan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(nPlan, PureClientVersions.production, pureModel, extensions, this.transformers));
            return serializedPlans;

        }).collect(Collectors.toList());


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
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            Document cachableGraphQLQuery = createCachableGraphQLQuery(document);
            GraphQLDevCacheKey key = new GraphQLDevCacheKey(projectId, workspaceId, queryClassPath, mappingPath, runtimePath, objectMapper.writeValueAsString(cachableGraphQLQuery));
            return this.executeGraphQLQuery(queryClassPath, mappingPath, runtimePath, document, key, profiles, () -> loadSDLCProjectModel(profiles, request, projectId, workspaceId, isGroupWorkspace));
        }
        catch (Exception ex)
        {
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
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            GraphQLProdCacheKey key = new GraphQLProdCacheKey(groupId, artifactId, versionId, mappingPath, runtimePath, queryClassPath, objectMapper.writeValueAsString(createCachableGraphQLQuery(document)));

            return this.executeGraphQLQuery(queryClassPath, mappingPath, runtimePath, document, key, profiles, () -> loadProjectModel(profiles, groupId, artifactId, versionId));
        }
        catch (Exception ex)
        {
            return Response.ok(new GraphQLErrorMain(ex.getMessage())).build();
        }
    }

    private boolean isQueryIntrospection(OperationDefinition operationDefinition)
    {
        List<Selection> selections = operationDefinition.selectionSet;
        return !selections.isEmpty() && selections.get(0) instanceof Field && ((Field) selections.get(0)).name.equals("__schema");
    }


    private OperationDefinition findQuery(Document document)
    {
        Collection<Definition> res = Iterate.select(document.definitions, d -> d.accept(new DefinitionVisitor<Boolean>()
                                                                                        {

                                                                                            @Override
                                                                                            public Boolean visit(DirectiveDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(EnumTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(ExecutableDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(FragmentDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(InterfaceTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(ObjectTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(InputObjectTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(OperationDefinition val)
                                                                                            {
                                                                                                return val.type == OperationType.query;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(ScalarTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(SchemaDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(Type val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(TypeSystemDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }

                                                                                            @Override
                                                                                            public Boolean visit(UnionTypeDefinition val)
                                                                                            {
                                                                                                return false;
                                                                                            }
                                                                                        }
        ));

        if (res.isEmpty())
        {
            throw new RuntimeException("Please provide a query");
        }
        else if (res.size() > 1)
        {
            throw new RuntimeException("Found more than one query");
        }
        else
        {
            return (OperationDefinition) res.iterator().next();
        }
    }


}