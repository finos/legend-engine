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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.application.query.model.Query;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Api(tags = "Application - Query")
@Path("pure/v1/query")
@Produces(MediaType.APPLICATION_JSON)
public class QueryAPI
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    private final MongoDatabase mongoDatabase;
    private final String mongoCollection;

    public QueryAPI(MongoDatabase mongoDatabase, String mongoCollection)
    {
        this.mongoDatabase = mongoDatabase;
        this.mongoCollection = mongoCollection;
    }

    private static Query documentToQuery(Document document)
    {
        Query query = new Query();
        query.id = document.getString("id");
        query.name = document.getString("name");
        query.projectId = document.getString("projectId");
        query.projectId = document.getString("groupId");
        query.projectId = document.getString("artifactId");
        query.versionId = document.getString("versionId");
        query.mapping = document.getString("mapping");
        query.runtime = document.getString("runtime");
        query.content = document.getString("content");
        query.owner = document.getString("owner");
        return query;
    }

    private static Document queryToDocument(Query query) throws JsonProcessingException
    {
        return Document.parse(objectMapper.writeValueAsString(query));
    }

    private static String getCurrentUser(ProfileManager<CommonProfile> profileManager)
    {
        CommonProfile profile = profileManager.get(true).orElse(null);
        return profile != null ? profile.getId() : null;
    }

    @GET
    @ApiOperation(value = "Get all queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueries(@QueryParam("search") @ApiParam("The search string") String search,
                               @QueryParam("limit") @ApiParam("Limit the number of queries returned") int limit,
                               @QueryParam("showCurrentUserQueriesOnly") @ApiParam("Limit to queries which belong to the current user") boolean showCurrentUserQueriesOnly,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            MongoCollection<Document> queryCollection = this.mongoDatabase.getCollection(this.mongoCollection, Document.class);
            List<Bson> filters = new ArrayList<>();
            if (showCurrentUserQueriesOnly)
            {
                // NOTE: every user is considered owner of the queries created by unknown user
                filters.add(Filters.in("owner", getCurrentUser(profileManager), null));
            }
            if (search != null)
            {
                filters.add(Filters.regex("name", Pattern.quote(search), "i"));
            }
            List<Query> queries = LazyIterate.collect(queryCollection
                .find(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters))
                .projection(Projections.include("id", "name", "projectId", "versionId"))
                .limit(limit), QueryAPI::documentToQuery).toList();
            return Response.ok(queries).build();
        }
        catch (Exception e)
        {
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
            MongoCollection<Document> queryCollection = this.mongoDatabase.getCollection(this.mongoCollection, Document.class);
            List<Query> matchingQueries = LazyIterate.collect(queryCollection.find(Filters.eq("id", queryId)), QueryAPI::documentToQuery).toList();
            if (matchingQueries.size() > 1)
            {
                throw new IllegalStateException("Found multiple query with ID '" + queryId + "'");
            }
            else if (matchingQueries.size() == 0)
            {
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Can't find query with ID '" + queryId + "'\"}").build();
            }
            return Response.ok(matchingQueries.get(0)).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_ERROR, null);
        }
    }

    @POST
    @ApiOperation(value = "Create a new query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createQuery(Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Create Query").startActive(true))
        {
            // Force the current user as owner regardless of user input
            query.owner = getCurrentUser(profileManager);

            MongoCollection<Document> queryCollection = this.mongoDatabase.getCollection(this.mongoCollection, Document.class);
            List<Query> matchingQueries = LazyIterate.collect(queryCollection.find(Filters.eq("id", query.id)), QueryAPI::documentToQuery).toList();
            if (matchingQueries.size() >= 1)
            {
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Query with ID '" + query.id + "' already exists\"}").build();
            }
            queryCollection.insertOne(queryToDocument(query));
            return Response.ok().entity(query).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, profiles);
        }
    }

    @PUT
    @Path("{queryId}")
    @ApiOperation(value = "Update query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateQuery(@PathParam("queryId") String queryId, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Update Query").startActive(true))
        {
            MongoCollection<Document> queryCollection = this.mongoDatabase.getCollection(this.mongoCollection, Document.class);
            List<Query> matchingQueries = LazyIterate.collect(queryCollection.find(Filters.eq("id", queryId)), QueryAPI::documentToQuery).toList();
            if (!queryId.equals(query.id))
            {
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Updating query ID is not supported\"}").build();
            }
            if (matchingQueries.size() > 1)
            {
                throw new IllegalStateException("Found multiple query with ID '" + queryId + "'");
            }
            else if (matchingQueries.size() == 0)
            {
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Can't find query with ID '" + queryId + "'\"}").build();
            }
            Query currentQuery = matchingQueries.get(0);

            // Make sure only the owner can update the query
            // NOTE: if the query is created by an anonymous user previously, set the current user as the owner
            String currentUser = getCurrentUser(profileManager);
            if (currentQuery.owner != null && !currentQuery.owner.equals(currentUser))
            {
                return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Only owner can update the query\"}").build();
            }
            query.owner = currentUser;
            queryCollection.findOneAndUpdate(Filters.eq("id", queryId), queryToDocument(query));
            return Response.ok().entity(query).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, profiles);
        }
    }

    @DELETE
    @Path("{queryId}")
    @ApiOperation(value = "Delete the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteQuery(@PathParam("queryId") String queryId, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Delete Query").startActive(true))
        {
            MongoCollection<Document> queryCollection = this.mongoDatabase.getCollection(this.mongoCollection, Document.class);
            List<Query> matchingQueries = LazyIterate.collect(queryCollection.find(Filters.eq("id", queryId)), QueryAPI::documentToQuery).toList();
            if (matchingQueries.size() > 1)
            {
                throw new IllegalStateException("Found multiple query with ID '" + queryId + "'");
            }
            else if (matchingQueries.size() == 0)
            {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            Query currentQuery = matchingQueries.get(0);

            // Make sure only the owner can delete the query
            String currentUser = getCurrentUser(profileManager);
            if (currentQuery.owner != null && !currentQuery.owner.equals(currentUser))
            {
                return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":\"Only owner can delete the query\"}").build();
            }

            queryCollection.findOneAndDelete(Filters.eq("id", queryId));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, profiles);
        }
    }
}
