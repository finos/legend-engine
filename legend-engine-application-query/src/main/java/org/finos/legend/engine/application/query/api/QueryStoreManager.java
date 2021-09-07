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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QueryProjectCoordinates;
import org.finos.legend.engine.shared.core.vault.Vault;

import javax.lang.model.SourceVersion;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class QueryStoreManager
{
    private static final Pattern VALID_ARTIFACT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*+(-[a-z][a-z0-9_]*+)*+$");

    private static final int MAX_NUMBER_OF_QUERIES = 100;
    private static final int MAX_NUMBER_OF_EVENTS = 1000;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    private final MongoClient mongoClient;

    public QueryStoreManager(MongoClient mongoClient)
    {
        this.mongoClient = mongoClient;
    }

    private MongoDatabase getQueryDatabase()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.database"))
        {
            return this.mongoClient.getDatabase(Vault.INSTANCE.getValue("query.mongo.database"));
        }
        throw new RuntimeException("Query MongoDB database has not been configured properly");
    }

    private MongoCollection<Document> getQueryCollection()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.collection.query"))
        {
            return this.getQueryDatabase().getCollection(Vault.INSTANCE.getValue("query.mongo.collection.query"));
        }
        throw new RuntimeException("Query MongoDB collection has not been configured properly");
    }

    private MongoCollection<Document> getQueryEventCollection()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.collection.queryEvent"))
        {
            return this.getQueryDatabase().getCollection(Vault.INSTANCE.getValue("query.mongo.collection.queryEvent"));
        }
        throw new RuntimeException("Query event MongoDB collection has not been configured properly");
    }

    private static Query documentToQuery(Document document)
    {
        Query query = new Query();
        query.id = document.getString("id");
        query.name = document.getString("name");
        query.description = document.getString("description");
        query.groupId = document.getString("groupId");
        query.artifactId = document.getString("artifactId");
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

    private static QueryEvent createEvent(String queryId, QueryEvent.QueryEventType eventType)
    {
        QueryEvent event = new QueryEvent();
        event.queryId = queryId;
        event.timestamp = Instant.now().toEpochMilli();
        event.eventType = eventType;
        return event;
    }

    private static QueryEvent documentToQueryEvent(Document document)
    {
        QueryEvent event = new QueryEvent();
        event.queryId = document.getString("queryId");
        try
        {
            event.timestamp = document.getLong("timestamp");
        }
        catch (ClassCastException e)
        {
            event.timestamp = Long.valueOf(document.getInteger("timestamp"));
        }
        event.eventType = QueryEvent.QueryEventType.valueOf(document.getString("eventType"));
        return event;
    }

    private static Document queryEventToDocument(QueryEvent event) throws JsonProcessingException
    {
        return Document.parse(objectMapper.writeValueAsString((event)));
    }

    private static void validate(boolean predicate, String message)
    {
        if (!predicate)
        {
            throw new ApplicationQueryException(message, Response.Status.BAD_REQUEST);
        }
    }

    private static void validateNonEmptyQueryField(String fieldValue, String message)
    {
        validate(fieldValue != null && !fieldValue.isEmpty(), message);
    }

    public static void validateQuery(Query query)
    {
        validateNonEmptyQueryField(query.id, "Query ID is missing or empty");
        validateNonEmptyQueryField(query.name, "Query name is missing or empty");
        validateNonEmptyQueryField(query.groupId, "Query project group ID is missing or empty");
        validateNonEmptyQueryField(query.artifactId, "Query project artifact ID is missing or empty");
        validateNonEmptyQueryField(query.versionId, "Query project version is missing or empty");
        validateNonEmptyQueryField(query.mapping, "Query mapping is missing or empty");
        validateNonEmptyQueryField(query.runtime, "Query runtime is missing or empty");
        validateNonEmptyQueryField(query.content, "Query content is missing or empty");

        validate(SourceVersion.isName(query.groupId), "Query project group ID is invalid");
        validate(VALID_ARTIFACT_ID_PATTERN.matcher(query.artifactId).matches(), "Query project artifact ID is invalid");
        // TODO: we can potentially create a pattern check for version
    }

    public List<Query> getQueries(String search, List<QueryProjectCoordinates> projectCoordinates, Integer limit, boolean showCurrentUserQueriesOnly, String currentUser)
    {
        List<Bson> filters = new ArrayList<>();
        if (showCurrentUserQueriesOnly)
        {
            // NOTE: every user is considered owner of the queries created by unknown user
            filters.add(Filters.in("owner", currentUser, null));
        }
        if (search != null)
        {
            filters.add(Filters.regex("name", Pattern.quote(search), "i"));
        }
        if (projectCoordinates != null && !projectCoordinates.isEmpty())
        {
            filters.add(Filters.or(
                ListIterate.collect(projectCoordinates, projectCoordinate ->
                    Filters.and(Filters.eq("groupId", projectCoordinate.groupId), Filters.eq("artifactId", projectCoordinate.artifactId)))));
        }
        return LazyIterate.collect(this.getQueryCollection()
            .find(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters))
            // NOTE: return a light version of the query to save bandwidth
            .projection(Projections.include("id", "name", "versionId", "groupId", "artifactId", "owner"))
            .limit(Math.min(MAX_NUMBER_OF_QUERIES, limit == null ? Integer.MAX_VALUE : limit)), QueryStoreManager::documentToQuery).toList();
    }

    public Query getQuery(String queryId)
    {
        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), QueryStoreManager::documentToQuery).toList();
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.size() == 0)
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        return matchingQueries.get(0);
    }

    public Query createQuery(Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        // Force the current user as owner regardless of user input
        query.owner = currentUser;

        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", query.id)), QueryStoreManager::documentToQuery).toList();
        if (matchingQueries.size() >= 1)
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "' already existed", Response.Status.BAD_REQUEST);
        }
        this.getQueryCollection().insertOne(queryToDocument(query));
        this.getQueryEventCollection().insertOne(queryEventToDocument(createEvent(query.id, QueryEvent.QueryEventType.CREATED)));
        return query;
    }

    public Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), QueryStoreManager::documentToQuery).toList();
        if (!queryId.equals(query.id))
        {
            throw new ApplicationQueryException("Updating query ID is not supported", Response.Status.BAD_REQUEST);
        }
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.size() == 0)
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        Query currentQuery = matchingQueries.get(0);

        // Make sure only the owner can update the query
        // NOTE: if the query is created by an anonymous user previously, set the current user as the owner
        if (currentQuery.owner != null && !currentQuery.owner.equals(currentUser))
        {
            throw new ApplicationQueryException("Only owner can update the query", Response.Status.FORBIDDEN);
        }
        query.owner = currentUser;
        this.getQueryCollection().findOneAndReplace(Filters.eq("id", queryId), queryToDocument(query));
        this.getQueryEventCollection().insertOne(queryEventToDocument(createEvent(query.id, QueryEvent.QueryEventType.UPDATED)));
        return query;
    }

    public void deleteQuery(String queryId, String currentUser) throws JsonProcessingException
    {
        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), QueryStoreManager::documentToQuery).toList();
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.size() == 0)
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        Query currentQuery = matchingQueries.get(0);

        // Make sure only the owner can delete the query
        if (currentQuery.owner != null && !currentQuery.owner.equals(currentUser))
        {
            throw new ApplicationQueryException("Only owner can delete the query", Response.Status.FORBIDDEN);
        }
        this.getQueryCollection().findOneAndDelete(Filters.eq("id", queryId));
        this.getQueryEventCollection().insertOne(queryEventToDocument(createEvent(queryId, QueryEvent.QueryEventType.DELETED)));
    }

    public List<QueryEvent> getQueryEvents(String queryId, QueryEvent.QueryEventType eventType, Long since, Long until, Integer limit)
    {
        List<Bson> filters = new ArrayList<>();
        if (queryId != null)
        {
            filters.add(Filters.eq("queryId", queryId));
        }
        if (eventType != null)
        {
            filters.add(Filters.eq("eventType", eventType.toString()));
        }
        if (since != null)
        {
            filters.add(Filters.gte("timestamp", since));
        }
        if (until != null)
        {
            filters.add(Filters.lte("timestamp", until));
        }
        return LazyIterate.collect(this.getQueryEventCollection()
            .find(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters))
            .limit(Math.min(MAX_NUMBER_OF_EVENTS, limit == null ? Integer.MAX_VALUE : limit)), QueryStoreManager::documentToQueryEvent).toList();
    }
}
