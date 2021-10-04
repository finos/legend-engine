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

package org.finos.legend.engine.application.query.api;

import com.mongodb.client.MongoClient;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QueryProjectCoordinates;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

@Api(tags = "Application - Query")
@Path("pure/v1/query")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationQuery
{
    private final QueryStoreManager queryStoreManager;

    public ApplicationQuery(MongoClient mongoClient)
    {
        this.queryStoreManager = new QueryStoreManager(mongoClient);
    }

    private static String getCurrentUser(ProfileManager<CommonProfile> profileManager)
    {
        CommonProfile profile = profileManager.get(true).orElse(null);
        return profile != null ? profile.getId() : null;
    }

    @GET
    @ApiOperation(value = "Get queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueries(@QueryParam("search") @ApiParam("The search string") String search,
                               @QueryParam("projectCoordinates") @ApiParam("The list of projects the queries are associated with") List<String> projectCoordinates,
                               @QueryParam("limit") @ApiParam("Limit the number of queries returned") Integer limit,
                               @QueryParam("showCurrentUserQueriesOnly") @ApiParam("Limit to queries which belong to the current user") boolean showCurrentUserQueriesOnly,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            List<QueryProjectCoordinates> coordinates = ListIterate.distinct(projectCoordinates).collect((projectCoordinate) ->
            {
                QueryProjectCoordinates coordinate = new QueryProjectCoordinates();
                String[] gaCoordinate = projectCoordinate.split(":");
                if (gaCoordinate.length != 2)
                {
                    return null;
                }
                coordinate.groupId = gaCoordinate[0];
                coordinate.artifactId = gaCoordinate[1];
                return coordinate;
            }).select(Objects::nonNull);
            return Response.ok().entity(this.queryStoreManager.getQueries(search, coordinates, limit, showCurrentUserQueriesOnly, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("{queryId}")
    @ApiOperation(value = "Get the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQuery(@PathParam("queryId") String queryId)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQuery(queryId)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_ERROR, null);
        }
    }

    @POST
    @ApiOperation(value = "Create a new query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createQuery(Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Create Query").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.createQuery(query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, ProfileManagerHelper.extractProfiles(profileManager));
        }
    }

    @PUT
    @Path("{queryId}")
    @ApiOperation(value = "Update query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateQuery(@PathParam("queryId") String queryId, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Update Query").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.updateQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, ProfileManagerHelper.extractProfiles(profileManager));
        }
    }

    @DELETE
    @Path("{queryId}")
    @ApiOperation(value = "Delete the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteQuery(@PathParam("queryId") String queryId, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Delete Query").startActive(true))
        {
            this.queryStoreManager.deleteQuery(queryId, getCurrentUser(profileManager));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, ProfileManagerHelper.extractProfiles(profileManager));
        }
    }

    @GET
    @Path("events")
    @ApiOperation(value = "Get query events")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryEvents(@QueryParam("queryId") @ApiParam("The query ID the event is associated with") String queryId,
                                   @QueryParam("eventType") @ApiParam("The type of event") QueryEvent.QueryEventType eventType,
                                   @QueryParam("since") @ApiParam("Lower limit on the UNIX timestamp for the event creation time") Long since,
                                   @QueryParam("until") @ApiParam("Upper limit on the UNIX timestamp for the event creation time") Long until,
                                   @QueryParam("limit") @ApiParam("Limit the number of events returned") Integer limit,
                                   @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            return Response.ok().entity(this.queryStoreManager.getQueryEvents(queryId, eventType, since, until, limit)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_EVENTS_ERROR, null);
        }
    }
}
