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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Schema;
import org.finos.legend.engine.query.sql.api.SQLExecutor;
import org.finos.legend.engine.query.sql.providers.core.SQLContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;

@Api(tags = "SQL - Execution")
@Path("sql/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class SqlExecute
{
    private static final SQLGrammarParser PARSER = SQLGrammarParser.newInstance();

    private final SQLExecutor executor;

    @Deprecated
    public SqlExecute(ModelManager modelManager, PlanExecutor planExecutor,
                      Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions,
                      List<SQLSourceProvider> providers,
                      Iterable<? extends PlanTransformer> transformers)
    {
        this(new SQLExecutor(modelManager, planExecutor, routerExtensions, providers, transformers));
    }

    public SqlExecute(SQLExecutor executor)
    {
        this.executor = executor;
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using sql string")
    @Path("executeQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response executeSql(@Context HttpServletRequest request, String sql, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat")SerializationFormat format,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = parseSQL(sql);
        return executeSql(request, query, format, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using protocol model")
    @Path("executeQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response executeSql(@Context HttpServletRequest request, Query query, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        SQLContext context = new SQLContext(query);

        Result result = this.executor.execute(query, request.getRemoteUser(), context, profiles);

        try (Scope ignored = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(profiles, result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using sql string")
    @Path("generateLambdaString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Lambda generateLambda(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = parseSQL(sql);
        return generateLambda(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Execute a SQL query using protocol model")
    @Path("generateLambda")
    @Consumes({MediaType.APPLICATION_JSON})
    public Lambda generateLambda(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query);
        return executor.lambda(query, context, profiles);
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using sql string")
    @Path("generatePlanQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public ExecutionPlan generatePlan(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = parseSQL(sql);
        return generatePlan(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Generate plans for a SQL query using protocol model")
    @Path("generatePlanQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public ExecutionPlan generatePlan(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        SQLContext context = new SQLContext(query);
        return this.executor.plan(query, context, profiles);
    }

    @POST
    @ApiOperation(value = "Get schema for a SQL query")
    @Path("getSchemaFromQueryString")
    @Consumes({MediaType.TEXT_PLAIN})
    public Schema getSchema(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        Query query = parseSQL(sql);
        return getSchema(request, query, pm, uriInfo);
    }

    @POST
    @ApiOperation(value = "Get schema for a SQL query")
    @Path("getSchemaFromQuery")
    @Consumes({MediaType.APPLICATION_JSON})
    public Schema getSchema(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        return this.executor.schema(query, profiles);
    }

    @POST
    @ApiOperation(value = "Parse SQL to metamodel")
    @Path("parseToMetamodel")
    @Consumes({MediaType.TEXT_PLAIN})
    public Query parseToMetamodel(@Context HttpServletRequest request, String sql, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        return parseSQL(sql);
    }

    private Query parseSQL(String sql)
    {
        return (Query) PARSER.parseStatement(sql);
    }
}