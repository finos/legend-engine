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

package org.finos.legend.engine.query.graphQL.api.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.debug.model.GraphFetchResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_transformation_queryToPure_GraphFetchResult;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_graphFetch;
import org.finos.legend.pure.generated.core_pure_protocol_generation_builder_generation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.function.Function;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;
import static org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_;

@Api(tags = "GraphQL - Debug")
@Path("graphQL/v1/debug")
public class GraphQLDebug extends GraphQL
{
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc;

    public GraphQLDebug(ModelManager modelManager, MetaDataServerConfiguration metadataserver, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc)
    {
        super(modelManager, metadataserver);
        this.extensionsFunc = extensionsFunc;
    }

    private Response generateGraphFetch(String queryClassPath, Query query, PureModel pureModel) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, JsonProcessingException
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
        Root_meta_external_query_graphQL_transformation_queryToPure_GraphFetchResult graphFetch = buildGraphFetch(_class, toPureModel(GraphQLGrammarParser.newInstance().parseDocument(query.query), pureModel), pureModel);

        // Serialize the tree to production protocol
        String version = PureClientVersions.production;
        Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_valueSpecification");
        Method graphFetchProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_valueSpecification_transformGraphFetchTree_GraphFetchTree_1__String_MANY__Map_1__Extension_MANY__GraphFetchTree_1_", GraphFetchTree.class, RichIterable.class, PureMap.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
        Object res = graphFetchProtocolMethod.invoke(null, graphFetch._graphFetchTree(), Lists.mutable.empty(), new PureMap(Maps.mutable.empty()), extensions, pureModel.getExecutionSupport());
        String asJSON = Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(res, pureModel.getExecutionSupport());
        RootGraphFetchTree protocolSerializedTree = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(asJSON, RootGraphFetchTree.class);

        return Response.ok(new GraphFetchResult(
                protocolSerializedTree,
                graphFetch._explodedDomain().collect(c ->
                {
                    ValueSpecification val = null;
                    try
                    {
                        Method functionProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_valueSpecification_transformFunctionBody_FunctionDefinition_1__Extension_MANY__ValueSpecification_MANY_", FunctionDefinition.class, RichIterable.class, ExecutionSupport.class);
                        Object res2 = functionProtocolMethod.invoke(null, c._second(), extensions, pureModel.getExecutionSupport());
                        String asJSON2 = Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(res2, pureModel.getExecutionSupport());
                        val = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(asJSON2, ValueSpecification.class);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                    return new GraphFetchResult.DomainUnit(c._first(), val);
                }).toList()
        )).build();
    }

    @POST
    @ApiOperation(value = "Generate Pure graphFetch(s) from a graphQL query using metadata from SDLC project")
    @Path("generateGraphFetch/dev/{projectId}/{workspaceId}/query/{queryClassPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateGraphFetchDev(@Context HttpServletRequest request, @PathParam("workspaceId") String workspaceId, @PathParam("projectId") String projectId, @PathParam("queryClassPath") String queryClassPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Generate Graph Fetch").startActive(true))
        {
            return this.generateGraphFetch(queryClassPath, query, loadSDLCProjectModel(profiles, request, projectId, workspaceId, false));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    @POST
    @ApiOperation(value = "Generate Pure graphFetch(s) from a graphQL query")
    @Path("generateGraphFetch/prod/{groupId}/{artifactId}/{versionId}/query/{queryClassPath}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateGraphFetchProd(@Context HttpServletRequest request, @PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @PathParam("versionId") String versionId, @PathParam("queryClassPath") String queryClassPath, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Generate Graph Fetch").startActive(true))
        {
            return this.generateGraphFetch(queryClassPath, query, loadProjectModel(profiles, request, groupId, artifactId, versionId));
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }


    private static Root_meta_external_query_graphQL_transformation_queryToPure_GraphFetchResult buildGraphFetch(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document document, PureModel pureModel)
    {
        return core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_getGraphFetchFromGraphQL_Class_1__Document_1__GraphFetchResult_1_(_class, document, pureModel.getExecutionSupport());
    }

    @POST
    @ApiOperation(value = "Generate a Pure Instance builder from a GraphQL document serialized as JSON")
    @Path("generatePureInstanceBuilder")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response generatePureInstanceBuilder(@Context HttpServletRequest request, String json, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Generate Pure Instance Builder").startActive(true))
        {
            Document document = new ObjectMapper().readValue(json, ExecutableDocument.class);
            PureModel pureModel = new PureModel(PureModelContextData.newBuilder().build(), Lists.mutable.empty(), DeploymentMode.TEST);
            return Response.ok(buildPureInstanceGeneration(toPureModel(document, pureModel), pureModel)).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    private static String buildPureInstanceGeneration(Any pureInstance, PureModel pureModel)
    {
        return core_pure_protocol_generation_builder_generation.Root_meta_protocols_generation_builder_builderGeneration_Any_1__String_1_(pureInstance, pureModel.getExecutionSupport());
    }
}
