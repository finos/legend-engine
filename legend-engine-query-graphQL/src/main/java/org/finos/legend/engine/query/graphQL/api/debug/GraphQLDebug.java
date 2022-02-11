package org.finos.legend.engine.query.graphQL.api.debug;

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
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.debug.model.GraphFetchResult;
import org.finos.legend.engine.query.graphQL.api.debug.model.QueryAndClass;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.lang.reflect.Method;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;
import static org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_;

@Api(tags = "GraphQL - Debug")
@Path("graphQL/v1/debug")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLDebug extends GraphQL
{
    public GraphQLDebug(ModelManager modelManager)
    {
        super(modelManager);
    }

    @POST
    @ApiOperation(value = "Parse a GraphQL query in the context of a model and translate to GraphFetch")
    @Path("generateGraphFetch")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateGraphFetch(@Context HttpServletRequest request, QueryAndClass query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Generate Graph Fetch").startActive(true))
        {
            PureModel pureModel = loadModel(profiles, request);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(query._class);
            Root_meta_external_query_graphQL_transformation_queryToPure_GraphFetchResult graphFetch = buildGraphFetch(_class, toPureModel(GraphQLGrammarParser.newInstance().parseDocument(query.query), pureModel), pureModel);

            // Serialize the tree to production protocol
            String version = PureClientVersions.production;
            Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_valueSpecification");
            Method graphFetchProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_valueSpecification_transformGraphFetchTree_GraphFetchTree_1__String_MANY__Map_1__RouterExtension_MANY__GraphFetchTree_1_", GraphFetchTree.class, RichIterable.class, PureMap.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            Object res = graphFetchProtocolMethod.invoke(null, graphFetch._graphFetchTree(), Lists.mutable.empty(), new PureMap(Maps.mutable.empty()), core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
            String asJSON = Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(res, pureModel.getExecutionSupport());
            RootGraphFetchTree protocolSerializedTree = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(asJSON, RootGraphFetchTree.class);

            return Response.ok(new GraphFetchResult(
                    protocolSerializedTree,
                    graphFetch._explodedDomain().collect(c -> {
                        ValueSpecification val = null;
                        try
                        {
                            Method functionProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_valueSpecification_transformFunctionBody_FunctionDefinition_1__RouterExtension_MANY__ValueSpecification_MANY_", FunctionDefinition.class, RichIterable.class, ExecutionSupport.class);
                            Object res2 = functionProtocolMethod.invoke(null, c._second(), core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
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
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    private static Root_meta_external_query_graphQL_transformation_queryToPure_GraphFetchResult buildGraphFetch(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_Document document, PureModel pureModel)
    {
        return core_external_query_graphql_transformation.Root_meta_external_query_graphQL_transformation_queryToPure_getGraphFetchFromGraphQL_Class_1__Document_1__GraphFetchResult_1_(_class, document, pureModel.getExecutionSupport());
    }

    @POST
    @ApiOperation(value = "Generate a Pure Instance builder from a GraphQL document serialized as JSON")
    @Path("generatePureInstanceBuilder")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
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
