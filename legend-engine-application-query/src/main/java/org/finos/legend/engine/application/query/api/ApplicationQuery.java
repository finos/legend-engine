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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.application.query.model.CubeSearchSpecification;
import org.finos.legend.engine.application.query.model.DataCubeQuery;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.application.query.model.StoredCube;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Tag(name = "Application - Query")
@Path("pure/v1/query")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationQuery
{
    private final QueryStoreManager queryStoreManager;
    private final DataCubeQueryStoreManager dataCubeQueryStoreManager;
    private final CubeStoreManager cubeStoreManager;

    public ApplicationQuery(MongoClient mongoClient)
    {
        this.queryStoreManager = new QueryStoreManager(mongoClient);
        this.dataCubeQueryStoreManager = new DataCubeQueryStoreManager(mongoClient);
        this.cubeStoreManager = new CubeStoreManager(mongoClient);
    }

    private static String getCurrentUser(ProfileManager profileManager)
    {
        UserProfile profile = profileManager.getProfile().orElse(null);
        return profile != null ? profile.getId() : null;
    }

    @POST
    @Path("search")
    @Operation(summary = "Search queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchQueries(QuerySearchSpecification searchSpecification, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        try
        {
            return Response.ok().entity(this.queryStoreManager.searchQueries(searchSpecification, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.SEARCH_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("batch")
    @Operation(summary = "Get the queries with specified IDs")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueries(@QueryParam("queryIds") @Parameter(description = "The list of query IDs to fetch (must contain no more than 50 items)") List<String> queryIds)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQueries(queryIds)).build();
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
    @Operation(summary = "Get the query with specified ID")
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

    @GET
    @Path("{queryId}/history")
    @Operation(summary = "Get all previous versions of the query with the specified ID, or a specific version when the 'version' query parameter is provided")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryHistory(@PathParam("queryId") String queryId,
                                    @QueryParam("version") @Parameter(description = "Optional specific version to retrieve; if omitted, all versions are returned") Integer version)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQueryHistory(queryId, version)).build();
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

    @GET
    @Path("allQueries")
    @Operation(summary = "Get all queries within the specified index range [from, to)")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAllQueries(@QueryParam("from") int from, @QueryParam("to") int to)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getAllQueries(from, to)).build();
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


    @POST
    @Operation(summary = "Create a new query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createQuery(Query query, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
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
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("{queryId}")
    @Operation(summary = "Update query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateQuery(@PathParam("queryId") String queryId, Query query, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
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
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("{queryId}/patchQuery")
    @Operation(summary = "Patch Query - update selected query fields")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response patchQuery(@PathParam("queryId") String queryId, Query query, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Patch Query - update selected query fields").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.patchQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @DELETE
    @Path("{queryId}")
    @Operation(summary = "Delete the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteQuery(@PathParam("queryId") String queryId, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
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
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, identity.getName());
        }
    }

    @GET
    @Path("events")
    @Operation(summary = "Get query events")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryEvents(@QueryParam("queryId") @Parameter(description = "The query ID the event is associated with") String queryId,
                                   @QueryParam("eventType") @Parameter(description = "The type of event") QueryEvent.QueryEventType eventType,
                                   @QueryParam("since") @Parameter(description = "Lower limit on the UNIX timestamp for the event creation time") Long since,
                                   @QueryParam("until") @Parameter(description = "Upper limit on the UNIX timestamp for the event creation time") Long until,
                                   @QueryParam("limit") @Parameter(description = "Limit the number of events returned") Integer limit,
                                   @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
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

    @GET
    @Path("/stats")
    @Operation(summary = "Get query store statistics")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryStoreStats()
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQueryStoreStats()).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_STATS_ERROR, null);
        }
    }

    // --------------------------------------------- DataCube Query ---------------------------------------------

    @POST
    @Path("dataCube/search")
    @Operation(summary = "Search DataCube queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchDataCubeQueries(QuerySearchSpecification searchSpecification, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.searchQueries(searchSpecification, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.SEARCH_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("dataCube/batch")
    @Operation(summary = "Get the DataCube queries with specified IDs")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueries(@QueryParam("queryIds") @Parameter(description = "The list of query IDs to fetch (must contain no more than 50 items)") List<String> queryIds)
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQueries(queryIds)).build();
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
    @Path("dataCube/{queryId}")
    @Operation(summary = "Get the DataCube query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQuery(@PathParam("queryId") String queryId)
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQuery(queryId)).build();
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
    @Path("dataCube")
    @Operation(summary = "Create a new DataCube query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createDataCubeQuery(DataCubeQuery query, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.createQuery(query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("dataCube/{queryId}")
    @Operation(summary = "Update DataCube query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateDataCubeQuery(@PathParam("queryId") String queryId, DataCubeQuery query, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.updateQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @DELETE
    @Path("dataCube/{queryId}")
    @Operation(summary = "Delete the DataCube query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteDataCubeQuery(@PathParam("queryId") String queryId, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            this.dataCubeQueryStoreManager.deleteQuery(queryId, getCurrentUser(profileManager));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, identity.getName());
        }
    }

    @GET
    @Path("dataCube/events")
    @Operation(summary = "Get DataCube query events")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueryEvents(@QueryParam("queryId") @Parameter(description = "The query ID the event is associated with") String queryId,
                                           @QueryParam("eventType") @Parameter(description = "The type of event") QueryEvent.QueryEventType eventType,
                                           @QueryParam("since") @Parameter(description = "Lower limit on the UNIX timestamp for the event creation time") Long since,
                                           @QueryParam("until") @Parameter(description = "Upper limit on the UNIX timestamp for the event creation time") Long until,
                                           @QueryParam("limit") @Parameter(description = "Limit the number of events returned") Integer limit,
                                           @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.getQueryEvents(queryId, eventType, since, until, limit)).build();
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

    @GET
    @Path("dataCube/stats")
    @Operation(summary = "Get DataCube query store statistics")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueryStoreStats()
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQueryStoreStats()).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_STATS_ERROR, null);
        }
    }

    // --------------------------------------------- Legend Cube ---------------------------------------------

    @POST
    @Path("cube/search")
    @Operation(summary = "Search saved Legend Cubes, without their content")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchCubes(CubeSearchSpecification searchSpecification)
    {
        try
        {
            return Response.ok().entity(this.cubeStoreManager.searchCubes(searchSpecification)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.SEARCH_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("cube/{cubeId}")
    @Operation(summary = "Get the latest version of a saved Legend Cube")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getCube(@PathParam("cubeId") String cubeId)
    {
        try
        {
            return Response.ok(this.cubeStoreManager.getCube(cubeId)).build();
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

    @GET
    @Path("cube/{cubeId}/version/{version}")
    @Operation(summary = "Get a saved version of a Legend Cube")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getCubeVersion(@PathParam("cubeId") String cubeId, @PathParam("version") int version)
    {
        try
        {
            return Response.ok(this.cubeStoreManager.getCubeVersion(cubeId, version)).build();
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
    @Path("cube")
    @Operation(summary = "Save a new Legend Cube; the store makes its id, and the caller is its first owner")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createCube(StoredCube cube, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.cubeStoreManager.createCube(cube, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("cube/{cubeId}")
    @Operation(summary = "Save a Legend Cube's content over the version read (409 when it has changed since); owners only")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateCube(@PathParam("cubeId") String cubeId, StoredCube cube, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.cubeStoreManager.updateCube(cubeId, cube, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @PATCH
    @Path("cube/{cubeId}")
    @Operation(summary = "Change a Legend Cube's name, owners or tags over the version read; owners only")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response patchCube(@PathParam("cubeId") String cubeId, Map<String, Object> patch, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.cubeStoreManager.patchCube(cubeId, patch, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @DELETE
    @Path("cube/{cubeId}")
    @Operation(summary = "Delete a Legend Cube at the version read: softly, or for good with its versions; owners only")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteCube(@PathParam("cubeId") String cubeId, @QueryParam("version") @Parameter(description = "The version that was read") Integer version, @QueryParam("permanent") @DefaultValue("false") @Parameter(description = "Whether to remove the cube and its versions for good") boolean permanent, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            this.cubeStoreManager.deleteCube(cubeId, version, permanent, getCurrentUser(profileManager));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, identity.getName());
        }
    }
}
