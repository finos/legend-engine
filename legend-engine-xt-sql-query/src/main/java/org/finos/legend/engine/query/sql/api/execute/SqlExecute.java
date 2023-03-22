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
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.query.sql.api.sources.TableSourceExtractor;
import org.finos.legend.engine.query.sql.api.sources.TableSource;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.engine.protocol.sql.metamodel.Translator;
import org.finos.legend.engine.query.sql.api.sources.*;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_Node;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_transformation_queryToPure_SQLSource;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_query_sql_binding_fromPure_fromPure;
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
import java.util.List;

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
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions;
    private final MutableList<PlanTransformer> transformers;
    private final MutableMap<Object, SQLSourceProvider> providers;

    public SqlExecute(ModelManager modelManager, PlanExecutor planExecutor,
                      Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions,
                      List<SQLSourceProvider> providers,
                      MutableList<PlanTransformer> transformers)
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
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SingleExecutionPlan singleExecutionPlan = generateQueryPlan(sql, profiles);
        long start = System.currentTimeMillis();
        return this.execImpl(planExecutor, profiles, request.getRemoteUser(), SerializationFormat.defaultFormat, start, singleExecutionPlan);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using protocol model")
    @Path("executeQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query, Maps.mutable.of());
        SingleExecutionPlan singleExecutionPlan = generateQueryPlan(query, context, profiles);
        long start = System.currentTimeMillis();
        return this.execImpl(planExecutor, profiles, request.getRemoteUser(), SerializationFormat.defaultFormat, start, singleExecutionPlan);
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using sql string")
    @Path("generatePlanQueryString")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generatePlanWith(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SingleExecutionPlan singleExecutionPlan = generateQueryPlan(sql, profiles);
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(singleExecutionPlan).build();
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using protocol model")
    @Path("generatePlanQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generatePlan(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query, Maps.mutable.of());
        SingleExecutionPlan singleExecutionPlan = generateQueryPlan(query, context, profiles);
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(singleExecutionPlan).build();
    }

    private SingleExecutionPlan generateQueryPlan(String sql, MutableList<CommonProfile> profiles)
    {
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Statement statement = parser.parseStatement(sql);
        SQLContext context = new SQLContext(statement, Maps.mutable.of());
        return generateQueryPlan(statement, context, profiles);
    }


    private SingleExecutionPlan generateQueryPlan(Statement statement, SQLContext context, MutableList<CommonProfile> profiles)
    {
        List<TableSource> tables = new TableSourceExtractor().visit(statement);

        MutableMultimap<String, TableSource> grouped = Iterate.groupBy(tables, TableSource::getType);

        boolean schemasValid = Iterate.allSatisfy(grouped.keySet(), providers::containsKey);

        if (!schemasValid)
        {
            throw new IllegalArgumentException("Unsupported schema types " + String.join(", ", grouped.keySet().select(k -> !providers.containsKey(k))));
        }

        RichIterable<SQLSourceResolvedContext> resolved = grouped.keySet().collect(k -> resolve(grouped.get(k), context, providers.get(k), profiles));

        boolean allCompatiblePointers = resolved.allSatisfy(p -> p.getPureModelContext() instanceof PureModelContextPointer && resolved.allSatisfy(p2 -> ((PureModelContextPointer) p.getPureModelContext()).safeEqual(p.getPureModelContext(), p2.getPureModelContext())));

        PureModelContext pureModelContext;

        //this means all pointers are from same source, so we can combine to utilise model cache.
        if (allCompatiblePointers)
        {
            pureModelContext = resolved.collectIf(p -> p.getPureModelContext() instanceof PureModelContextPointer, p -> ((PureModelContextPointer) p.getPureModelContext()))
                    .injectInto(null, (d, e) -> e.combine((PureModelContextPointer) d));
        }
        else
        {
            pureModelContext = resolved.injectInto(PureModelContextData.newPureModelContextData(), (p, p2) -> p.combine(modelManager.loadData(p2.getPureModelContext(), PureClientVersions.production, profiles)));
        }

        PureModel pureModel = modelManager.loadModel(pureModelContext, PureClientVersions.production, profiles, "");

        RichIterable<SQLSource> sources = resolved.flatCollect(SQLSourceResolvedContext::getSources);

        Root_meta_external_query_sql_metamodel_Node query = new Translator().translate(statement, pureModel);

        RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource> compiledSources = new SQLSourceTranslator().translate(sources, pureModel);
        Root_meta_pure_executionPlan_ExecutionPlan plan = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_getPlansFromSQL_SQLSource_MANY__Node_1__Extension_MANY__ExecutionPlan_1_(compiledSources, query, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
        SingleExecutionPlan singleExecutionPlan = transformExecutionPlan(plan, pureModel, PureClientVersions.production, profiles, routerExtensions.apply(pureModel), transformers);

        return singleExecutionPlan;
    }

    private SQLSourceResolvedContext resolve(MutableCollection<TableSource> tables, SQLContext context, SQLSourceProvider extension, MutableList<CommonProfile> profiles)
    {
        return extension.resolve(tables.toList(), context, profiles);
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
