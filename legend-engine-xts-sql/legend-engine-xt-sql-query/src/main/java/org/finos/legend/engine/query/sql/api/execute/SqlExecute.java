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

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.sql.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.engine.query.sql.api.sources.SQLContext;
import org.finos.legend.engine.query.sql.api.sources.SQLSource;
import org.finos.legend.engine.query.sql.api.sources.SQLSourceProvider;
import org.finos.legend.engine.query.sql.api.sources.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.api.sources.SQLSourceTranslator;
import org.finos.legend.engine.query.sql.api.sources.TableSource;
import org.finos.legend.engine.query.sql.api.sources.TableSourceExtractor;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_Schema;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Node;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Query;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_transformation_queryToPure_PlanGenerationResult;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_transformation_queryToPure_SQLSource;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_json_toJSON;
import org.finos.legend.pure.generated.core_external_query_sql_binding_fromPure_fromPure;
import org.finos.legend.pure.generated.core_pure_tools_tools_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.generation.PlanGenerator.transformExecutionPlan;

@Api(tags = "SQL - Execution")
@Path("sql/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class SqlExecute
{

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SqlExecute.class);
    private static final SQLGrammarParser parser = SQLGrammarParser.newInstance();
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions;
    private final Iterable<? extends PlanTransformer> transformers;
    private final MutableMap<Object, SQLSourceProvider> providers;

    public SqlExecute(ModelManager modelManager, PlanExecutor planExecutor,
                      Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions,
                      List<SQLSourceProvider> providers,
                      Iterable<? extends PlanTransformer> transformers)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.routerExtensions = routerExtensions;
        this.transformers = transformers;
        this.providers = ListIterate.groupByUniqueKey(providers, SQLSourceProvider::getType);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using sql string")
    @Path("executeQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response executeSql(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = (Query) parser.parseStatement(sql);
        return executeSql(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using protocol model")
    @Path("executeQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query, Maps.mutable.of());
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());

        Result result = execute(query, request.getRemoteUser(), context, profiles);

        MetricsHandler.observe("execute", start, System.currentTimeMillis());
        try (Scope ignored = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(profiles, result, SerializationFormat.defaultFormat, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using sql string")
    @Path("generateLambdaString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Lambda generateLambda(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = (Query) parser.parseStatement(sql);
        return generateLambda(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using protocol model")
    @Path("generateLambda")
    @Consumes({MediaType.APPLICATION_JSON})
    public Lambda generateLambda(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query, Maps.mutable.of());
        return lambda(query, context, profiles);
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using sql string")
    @Path("generatePlanQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response generatePlan(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = (Query) parser.parseStatement(sql);
        return generatePlan(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using protocol model")
    @Path("generatePlanQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generatePlan(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query, Maps.mutable.of());
        SingleExecutionPlan singleExecutionPlan = plan(query, context, profiles);
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(singleExecutionPlan).build();
    }

    @POST
    @ApiOperation(value = "Get schema for a SQL query in the context of a Mapping and a Runtime from a SDLC project")
    @Path("getSchemaFromQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response getSchema(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = (Query) parser.parseStatement(sql);
        return getSchema(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Get schema for a SQL query in the context of a Mapping and a Runtime from a SDLC project")
    @Path("getSchemaFromQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getSchema(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        String schema = schema(query, profiles);
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(schema).build();
    }

    private Lambda lambda(Query query, SQLContext context, MutableList<CommonProfile> profiles)
    {
        return process(query,
                (transformedContext, pureModel, sources) -> transformLambda(transformedContext.lambda(pureModel.getExecutionSupport()), pureModel),
                (sources, extensions, pureModel) -> core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_rootContext_SQLSource_MANY__Extension_MANY__SqlTransformContext_1_(sources, extensions, pureModel.getExecutionSupport())._scopeWithFrom(false),
                context, profiles);
    }

    private String schema(Query query, MutableList<CommonProfile> profiles)
    {
        SQLContext context = new SQLContext(query, Maps.mutable.of());
        return process(query, (t, pm, sources) ->
        {
            Root_meta_external_query_sql_Schema schema = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getSchema_SqlTransformContext_1__Schema_1_(t, pm.getExecutionSupport());
            return serializeToJSON(schema, pm, false);
        }, context, profiles);
    }

    private Root_meta_external_query_sql_transformation_queryToPure_PlanGenerationResult planResult(Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext transformedContext, PureModel pureModel, RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource> sources)
    {
        return core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getPlanResult_SqlTransformContext_1__SQLSource_MANY__Extension_MANY__PlanGenerationResult_1_(transformedContext, sources, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
    }

    private SingleExecutionPlan plan(Query query, SQLContext context, MutableList<CommonProfile> profiles)
    {
        return process(query, (transformedContext, pureModel, sources) -> transformExecutionPlan(planResult(transformedContext, pureModel, sources)._plan(), pureModel, PureClientVersions.production, profiles, routerExtensions.apply(pureModel), transformers), context, profiles);
    }

    private Result execute(Query query, String user, SQLContext context, MutableList<CommonProfile> profiles)
    {
        return process(query, (transformedContext, pureModel, sources) ->
        {
            Root_meta_external_query_sql_transformation_queryToPure_PlanGenerationResult plans = planResult(transformedContext, pureModel, sources);

            Map<String, Result> arguments = UnifiedMap.newMapWith(IterableIterate.collectIf(plans._arguments(), p -> p._value() != null || p._plan() != null, p ->
            {
                Result result;

                if (p._value() != null)
                {
                    Object value = p._value() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List
                            ? ((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List) p._value())._values()
                            : p._value();
                    result = new ConstantResult(value);
                }
                else
                {
                    Root_meta_pure_executionPlan_ExecutionPlan l = PlanPlatform.JAVA.bindPlan(p._plan(), null, pureModel, routerExtensions.apply(pureModel));
                    SingleExecutionPlan m = transformExecutionPlan(l, pureModel, PureClientVersions.production, profiles, routerExtensions.apply(pureModel), transformers);
                    result = planExecutor.execute(m, Maps.mutable.empty(), "pentej", profiles);
                }

                return Tuples.pair(p._name(), result);
            }));

            Root_meta_pure_executionPlan_ExecutionPlan plan = plans._plan();
            plan = PlanPlatform.JAVA.bindPlan(plan, null, pureModel, routerExtensions.apply(pureModel));
            SingleExecutionPlan transformedPlan = transformExecutionPlan(plan, pureModel, PureClientVersions.production, profiles, routerExtensions.apply(pureModel), transformers);

            return planExecutor.execute(transformedPlan, arguments, user, profiles);
        }, context, profiles);
    }

    private <T> T process(Query query, Function3<Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext, PureModel, RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource>, T> func, SQLContext context, MutableList<CommonProfile> profiles)
    {
        return process(query, func, (sources, extensions, pureModel) -> core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_rootContext_SQLSource_MANY__Extension_MANY__SqlTransformContext_1_(sources, extensions, pureModel.getExecutionSupport()), context, profiles);
    }

    private <T> T process(Query query, 
                          Function3<Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext, PureModel, RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource>, T> func, 
                          Function3<RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource>, RichIterable<? extends Root_meta_pure_extension_Extension>, PureModel, Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext> transformContextFunc, 
                          SQLContext context,
                          MutableList<CommonProfile> profiles)
    {
        Pair<RichIterable<SQLSource>, PureModelContext> sqlSourcesAndPureModel = getSourcesAndModel(query, context, profiles);
        RichIterable<SQLSource> sources = sqlSourcesAndPureModel.getOne();
        PureModelContext pureModelContext = sqlSourcesAndPureModel.getTwo();

        PureModel pureModel = modelManager.loadModel(pureModelContext, PureClientVersions.production, profiles, "");

        Root_meta_external_query_sql_metamodel_Query compiledQuery = new ProtocolToMetamodelTranslator().translate(query, pureModel);

        RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource> compiledSources = new SQLSourceTranslator().translate(sources, pureModel);
        LOGGER.info("{}", new LogInfo(profiles, LoggingEventType.GENERATE_PLAN_START));

        Root_meta_external_query_sql_transformation_queryToPure_SqlTransformContext transformedContext = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_processRootQuery_Query_1__SqlTransformContext_1__SqlTransformContext_1_(
                compiledQuery, transformContextFunc.value(compiledSources, routerExtensions.apply(pureModel), pureModel), pureModel.getExecutionSupport());

        return func.value(transformedContext, pureModel, compiledSources);
    }

    private Pair<RichIterable<SQLSource>, PureModelContext> getSourcesAndModel(Query query, SQLContext context, MutableList<CommonProfile> profiles)
    {
        Set<TableSource> tables = new TableSourceExtractor().visit(query);

        MutableMultimap<String, TableSource> grouped = Iterate.groupBy(tables, TableSource::getType);

        boolean schemasValid = Iterate.allSatisfy(grouped.keySet(), providers::containsKey);

        if (!schemasValid)
        {
            throw new IllegalArgumentException("Unsupported schema types " + String.join(", ", grouped.keySet().select(k -> !providers.containsKey(k))));
        }

        RichIterable<SQLSourceResolvedContext> resolved = grouped.keySet().collect(k -> resolve(grouped.get(k), context, providers.get(k), profiles));

        MutableList<PureModelContext> allContexts = IterableIterate.flatCollect(resolved, SQLSourceResolvedContext::getPureModelContexts);
        boolean allCompatiblePointers = allContexts.allSatisfy(p -> p instanceof PureModelContextPointer && allContexts.allSatisfy(p2 -> ((PureModelContextPointer) p).safeEqual(p, p2)));

        PureModelContext pureModelContext;

        //this means all pointers are from same source, so we can combine to utilise model cache.
        if (allCompatiblePointers)
        {
            pureModelContext = allContexts.collectIf(p -> p instanceof PureModelContextPointer, p -> ((PureModelContextPointer) p))
                    .injectInto(null, (d, e) -> e.combine((PureModelContextPointer) d));
        }
        else
        {
            pureModelContext = resolved.injectInto(PureModelContextData.newPureModelContextData(), (p, p2) -> PureModelContextData.combine(p, PureModelContextData.newPureModelContextData(), ListIterate.collect(p2.getPureModelContexts(), c -> modelManager.loadData(c, PureClientVersions.production, profiles)).toArray(new PureModelContextData[]{})));
        }
        RichIterable<SQLSource> sources = resolved.flatCollect(SQLSourceResolvedContext::getSources);
        return Tuples.pair(sources, pureModelContext);
    }


    private SQLSourceResolvedContext resolve(MutableCollection<TableSource> tables, SQLContext context, SQLSourceProvider extension, MutableList<CommonProfile> profiles)
    {
        return extension.resolve(tables.toList(), context, profiles);
    }

    static String serializeToJSON(Object pureObject, PureModel pureModel, Boolean alloyJSON)
    {
        return alloyJSON
                ? org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(pureObject, pureModel.getExecutionSupport())
                : core_external_format_json_toJSON.Root_meta_json_toJSON_Any_MANY__Integer_$0_1$__Config_1__String_1_(
                        Lists.mutable.with(pureObject),
                        1000L,
                        core_external_format_json_toJSON.Root_meta_json_config_Boolean_1__Boolean_1__Boolean_1__Boolean_1__Config_1_(true, false, false, false, pureModel.getExecutionSupport()),
                        pureModel.getExecutionSupport()
        );
    }

    private Lambda transformLambda(LambdaFunction<?> lambda, PureModel pureModel)
    {
        Object protocol = transformToVersionedModel(lambda,  PureClientVersions.production, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
        return transform(protocol, Lambda.class, pureModel);
    }

    private <T> T transform(Object object, java.lang.Class<T> clazz, PureModel pureModel)
    {
        String json = serializeToJSON(object, pureModel, true);
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(json, clazz);
        }
        catch (Exception e)
        {
            throw new EngineException(e.getMessage());
        }
    }

    public Object transformToVersionedModel(FunctionDefinition<?> lambda, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        try
        {
            Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_valueSpecification");
            Method method = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_transformLambda_FunctionDefinition_1__Extension_MANY__Lambda_1_", FunctionDefinition.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            return method.invoke(null, lambda, extensions, executionSupport);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}