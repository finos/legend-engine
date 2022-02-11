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
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.*;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.execute.model.PlansResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.query.graphQL.api.execute.model.QueryClassMapping;
import org.finos.legend.engine.query.graphQL.api.execute.model.error.GraphQLErrorMain;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
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
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "GraphQL - Execution")
@Path("graphQL/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLExecute extends GraphQL
{
    private final PlanExecutor planExecutor;

    public GraphQLExecute(ModelManager modelManager, PlanExecutor planExecutor)
    {
        super(modelManager);
        this.planExecutor = planExecutor;
    }

    @POST
    @ApiOperation(value = "Generate plans from a GraphQL query in the context of a Mapping and a Runtime.")
    @Path("generatePlans")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generatePlans(@Context HttpServletRequest request, QueryClassMapping query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            PureModel pureModel = loadModel(profiles, request);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(query._class);
            Mapping mapping = pureModel.getMapping(query.mapping);
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(GraphQLExecute.class.getClassLoader().getResourceAsStream("exampleRuntime.json"), org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime.class), pureModel.getContext());

            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_Document queryDoc = toPureModel(document, pureModel);
            if (isQueryIntrospection(findQuery(document)))
            {
                return Response.ok("").type(MediaType.TEXT_HTML_TYPE).build();
            }
            else
            {
                RichIterable<? extends Pair<? extends String, ? extends Root_meta_pure_executionPlan_ExecutionPlan>> purePlans = core_external_query_graphql_transformation.Root_meta_external_query_graphQL_transformation_queryToPure_getPlansFromGraphQL_Class_1__Mapping_1__Runtime_1__Document_1__RouterExtension_MANY__Pair_MANY_(_class, mapping, runtime, queryDoc, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
                Collection<PlansResult.PlanUnit> plans = Iterate.collect(purePlans, p -> {
                            Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._second(), "ID", pureModel, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()));
                            try
                            {
                                return new PlansResult.PlanUnit(p._first(),
                                                                ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(PlanGenerator.serializeToJSON(nPlan, "vX_X_X", pureModel, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers), ExecutionPlan.class),
                                                                core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Boolean_1__RouterExtension_MANY__String_1_(nPlan, true, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport())
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
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a Mapping and a Runtime.")
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            PureModel pureModel = loadModel(profiles, request);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass("demo::Query");

            Document document = GraphQLGrammarParser.newInstance().parseDocument(query.query);
            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_Document queryDoc = toPureModel(document, pureModel);
            if (isQueryIntrospection(findQuery(document)))
            {
                return Response.ok("{" +
                        "  \"data\":" + core_external_query_graphql_introspection_transformation.Root_meta_external_query_graphQL_introspection_graphQLIntrospectionQuery_Class_1__Document_1__String_1_(_class, queryDoc, pureModel.getExecutionSupport()) +
                        "}").type(MediaType.TEXT_HTML_TYPE).build();
            }
            else
            {
                Mapping mapping = pureModel.getMapping("demo::Mapping");
                org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = pureModel.getRuntime("demo::Runtime");
                RichIterable<? extends Pair<? extends String, ? extends Root_meta_pure_executionPlan_ExecutionPlan>> purePlans = core_external_query_graphql_transformation.Root_meta_external_query_graphQL_transformation_queryToPure_getPlansFromGraphQL_Class_1__Mapping_1__Runtime_1__Document_1__RouterExtension_MANY__Pair_MANY_(_class, mapping, runtime, queryDoc, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
                Collection<org.eclipse.collections.api.tuple.Pair<String, SingleExecutionPlan>> plans = Iterate.collect(purePlans, p -> {
                            Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._second(), "ID", pureModel, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()));
                            return Tuples.pair(p._first(), PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(nPlan, "vX_X_X", pureModel, core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers)));
                        }
                );

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

                                plans.forEach(p -> {
                                    JsonStreamingResult result = null;
                                    try
                                    {
                                        generator.writeFieldName(p.getOne());
                                        result = (JsonStreamingResult) planExecutor.execute(p.getTwo());
                                        result.getJsonStream().accept(generator);
                                    }
                                    catch (IOException e)
                                    {
                                        throw new RuntimeException(e);
                                    }
                                    finally
                                    {
                                        result.close();
                                    }
                                });
                                generator.writeEndObject();
                                generator.writeEndObject();
                            }
                        }).build();
            }
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
