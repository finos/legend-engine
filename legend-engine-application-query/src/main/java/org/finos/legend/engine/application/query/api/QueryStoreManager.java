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
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.factory.SortedSets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.application.query.model.*;
import org.finos.legend.engine.shared.core.vault.Vault;

import javax.lang.model.SourceVersion;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class QueryStoreManager
{
    private static final Pattern VALID_ARTIFACT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*+(-[a-z][a-z0-9_]*+)*+$");

    private static final int MAX_NUMBER_OF_QUERIES = 100;
    private static final int MAX_NUMBER_OF_EVENTS = 1000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    // NOTE: these are non-compilable profile and tag that we come up with for query
    // so that it records the dataSpace it is created from
    private static final String QUERY_PROFILE_PATH = "meta::pure::profiles::query";
    private static final String QUERY_PROFILE_TAG_DATA_SPACE = "dataSpace";
    private static final List<String> LIGHT_QUERY_PROJECTION = Arrays.asList("id", "name", "versionId", "originalVersionId", "groupId", "artifactId", "owner", "createdAt", "lastUpdatedAt");
    private static final int GET_QUERIES_LIMIT = 50;
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

    private <T> T documentToClass(Document document, Class<T> _class)
    {
        try
        {
            return this.objectMapper.convertValue(document, _class);
        }
        catch (Exception e)
        {
            throw new ApplicationQueryException("Unable to deserialize document to class '" + _class.getName() + "':" + e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    private Query documentToQuery(Document document)
    {
        return this.documentToClass(document, Query.class);
    }

    private Document queryToDocument(Query query) throws JsonProcessingException
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

    private Document queryEventToDocument(QueryEvent event) throws JsonProcessingException
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
        if (query.executionContext instanceof QueryExplicitExecutionContext)
        {
            QueryExplicitExecutionContext queryExplicitExecutionContext = (QueryExplicitExecutionContext) query.executionContext;
            validateNonEmptyQueryField(queryExplicitExecutionContext.mapping, "Query mapping is missing or empty");
            validateNonEmptyQueryField(queryExplicitExecutionContext.runtime, "Query runtime is missing or empty");
        }
        else if (query.executionContext instanceof QueryDataSpaceExecutionContext)
        {
            QueryDataSpaceExecutionContext queryDataSpaceExecutionContext = (QueryDataSpaceExecutionContext) query.executionContext;
            validateNonEmptyQueryField(queryDataSpaceExecutionContext.dataSpacePath, "Query data Space execution context dataSpace path is missing or empty");
        }
        else
        {
            validateNonEmptyQueryField(query.mapping, "Query mapping is missing or empty");
            validateNonEmptyQueryField(query.runtime, "Query runtime is missing or empty");
        }
        validateNonEmptyQueryField(query.content, "Query content is missing or empty");
        validate(SourceVersion.isName(query.groupId), "Query project group ID is invalid");
        validate(VALID_ARTIFACT_ID_PATTERN.matcher(query.artifactId).matches(), "Query project artifact ID is invalid");
        // TODO: we can potentially create a pattern check for version
    }

    public List<Query> searchQueries(QuerySearchSpecification searchSpecification, String currentUser)
    {
        List<Bson> filters = new ArrayList<>();
        if (searchSpecification.searchTermSpecification != null)
        {
            QuerySearchTermSpecification querySearchTermSpecification = searchSpecification.searchTermSpecification;
            if (querySearchTermSpecification.searchTerm == null)
            {
                throw new ApplicationQueryException("Query search spec expecting a search term", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (querySearchTermSpecification.exactMatchName != null && querySearchTermSpecification.exactMatchName)
            {
                Bson filter = Filters.eq("name", querySearchTermSpecification.searchTerm);
                if (querySearchTermSpecification.includeOwner != null && querySearchTermSpecification.includeOwner)
                {
                    filter = Filters.or(filter,Filters.eq("owner", querySearchTermSpecification.searchTerm));
                }
                filters.add(filter);
            }
            else
            {
                Bson idFilter  = Filters.eq("id", querySearchTermSpecification.searchTerm);
                Bson nameFilter = Filters.regex("name", Pattern.quote(querySearchTermSpecification.searchTerm), "i");
                Bson filter = Filters.or(idFilter, nameFilter);
                if (querySearchTermSpecification.includeOwner != null && querySearchTermSpecification.includeOwner)
                {
                    filter = Filters.or(idFilter, nameFilter, Filters.regex("owner", Pattern.quote(querySearchTermSpecification.searchTerm), "i"));
                }
                filters.add(filter);
            }
        }
        if (searchSpecification.showCurrentUserQueriesOnly != null && searchSpecification.showCurrentUserQueriesOnly)
        {
            filters.add(Filters.in("owner", currentUser, null));
        }
        if (searchSpecification.projectCoordinates != null && !searchSpecification.projectCoordinates.isEmpty())
        {
            filters.add(Filters.or(
                    ListIterate.collect(searchSpecification.projectCoordinates, projectCoordinate ->
                            projectCoordinate.version != null
                                    ? Filters.and(
                                    Filters.eq("groupId", projectCoordinate.groupId),
                                    Filters.eq("artifactId", projectCoordinate.artifactId),
                                    Filters.eq("versionId", projectCoordinate.version)
                            ) : Filters.and(
                                    Filters.eq("groupId", projectCoordinate.groupId),
                                    Filters.eq("artifactId", projectCoordinate.artifactId)
                            )
                    )));
        }
        if (searchSpecification.taggedValues != null && !searchSpecification.taggedValues.isEmpty())
        {
            List taggedValueFilters = ListIterate.collect(searchSpecification.taggedValues, taggedValue ->
                    Filters.and(Filters.eq("taggedValues.tag.profile", taggedValue.tag.profile), Filters.eq("taggedValues.tag.value", taggedValue.tag.value), Filters.eq("taggedValues.value", taggedValue.value)));
            filters.add(searchSpecification.combineTaggedValuesCondition != null && searchSpecification.combineTaggedValuesCondition ? Filters.and(taggedValueFilters) : Filters.or(taggedValueFilters));
        }
        if (searchSpecification.stereotypes != null && !searchSpecification.stereotypes.isEmpty())
        {
            filters.add(Filters.or(
                    ListIterate.collect(searchSpecification.stereotypes, stereotype ->
                            Filters.and(Filters.eq("stereotypes.profile", stereotype.profile), Filters.eq("stereotypes.value", stereotype.value)))));
        }

        List<Query> queries = new ArrayList<>();
        AggregateIterable<Document> documents = this.getQueryCollection()
                .aggregate(Arrays.asList(
                        Aggregates.addFields(new Field("isCurrentUser", new Document("$eq", Arrays.asList("$owner", currentUser)))),
                        Aggregates.match(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters)),
                        Aggregates.sort(Sorts.descending("isCurrentUser")),
                        Aggregates.project(Projections.include(LIGHT_QUERY_PROJECTION)),
                        Aggregates.limit(Math.min(MAX_NUMBER_OF_QUERIES, searchSpecification.limit == null ? Integer.MAX_VALUE : searchSpecification.limit))));
        for (Document doc : documents)
        {
            queries.add(documentToQuery(doc));
        }
        return queries;
    }

    public List<Query> getQueries(List<String> queryIds)
    {
        if (queryIds.size() > GET_QUERIES_LIMIT)
        {
            throw new ApplicationQueryException("Can't fetch more than " + GET_QUERIES_LIMIT + " queries", Response.Status.BAD_REQUEST);
        }
        MutableList<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.in("id", queryIds)).limit(GET_QUERIES_LIMIT), this::documentToQuery).toList();
        // validate
        MutableSortedSet<String> notFoundQueries = SortedSets.mutable.empty();
        MutableSortedSet<String> duplicatedQueries = SortedSets.mutable.empty();
        queryIds.forEach(queryId ->
        {
            int count = matchingQueries.count(query -> queryId.equals(query.id));
            if (count > 1)
            {
                duplicatedQueries.add(queryId);
            }
            else if (count == 0)
            {
                notFoundQueries.add(queryId);
            }
        });
        if (duplicatedQueries.size() != 0)
        {
            throw new IllegalStateException(duplicatedQueries.makeString("Found multiple queries with duplicated ID for the following ID(s):\\n", "\\n", ""));
        }
        if (notFoundQueries.size() != 0)
        {
            throw new ApplicationQueryException(notFoundQueries.makeString("Can't find queries for the following ID(s):\\n", "\\n", ""), Response.Status.INTERNAL_SERVER_ERROR);
        }
        return matchingQueries;
    }

    public Query getQuery(String queryId) throws JsonProcessingException
    {
        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.size() == 0)
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        Query query = matchingQueries.get(0);
        query.lastOpenAt = Instant.now().toEpochMilli();
        this.getQueryCollection().findOneAndReplace(Filters.eq("id", queryId), queryToDocument(query));
        return query;
    }

    public QueryStoreStats getQueryStoreStats() throws JsonProcessingException
    {
        Long count = this.getQueryCollection().countDocuments();
        QueryStoreStats storeStats = new QueryStoreStats();
        storeStats.setQueryCount(count);
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.and(Filters.eq("taggedValues.tag.profile", QUERY_PROFILE_PATH), Filters.eq("taggedValues.tag.value", QUERY_PROFILE_TAG_DATA_SPACE)));
        storeStats.setQueryCreatedFromDataSpaceCount(this.getQueryCollection()
                .countDocuments(Filters.and(filters)));
        return storeStats;
    }


    public Query createQuery(Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        // Force the current user as owner regardless of user input
        query.owner = currentUser;

        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", query.id)), this::documentToQuery).toList();
        if (matchingQueries.size() >= 1)
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "' already existed", Response.Status.BAD_REQUEST);
        }
        query.createdAt = Instant.now().toEpochMilli();
        query.lastUpdatedAt = query.createdAt;
        query.lastOpenAt = query.createdAt;
        this.getQueryCollection().insertOne(queryToDocument(query));
        QueryEvent createdEvent = createEvent(query.id, QueryEvent.QueryEventType.CREATED);
        createdEvent.timestamp = query.createdAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(createdEvent));
        return query;
    }

    public Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
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
        query.createdAt = currentQuery.createdAt;
        query.lastUpdatedAt = Instant.now().toEpochMilli();
        query.lastOpenAt = Instant.now().toEpochMilli();
        query.originalVersionId = currentQuery.originalVersionId;
        this.getQueryCollection().findOneAndReplace(Filters.eq("id", queryId), queryToDocument(query));
        QueryEvent updatedEvent = createEvent(query.id, QueryEvent.QueryEventType.UPDATED);
        updatedEvent.timestamp = query.lastUpdatedAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(updatedEvent));
        return query;
    }

    public Query patchQuery(String queryId, Query updatedQuery, String currentUser) throws JsonProcessingException
    {
        Query currentQuery = this.getQuery(queryId);
        // Make sure only the owner can update the query
        // NOTE: if the query is created by an anonymous user previously, set the current user as the owner
        if (currentQuery.owner != null && !currentQuery.owner.equals(currentUser))
        {
            throw new ApplicationQueryException("Only owner can update the query", Response.Status.FORBIDDEN);
        }

        Class<? extends Query> queryClass = currentQuery.getClass();
        for (java.lang.reflect.Field field : queryClass.getDeclaredFields())
        {
            try
            {
                field.setAccessible(true);
                Object updatedValue = field.get(updatedQuery);
                if (updatedValue != null)
                {
                    field.set(currentQuery, updatedValue);
                }
            }
            catch (IllegalAccessException e)
            {
                throw new ApplicationQueryException("Can't modify query field" + field.getName(), Response.Status.BAD_REQUEST);
            }
        }
        currentQuery.owner = currentUser;
        currentQuery.lastUpdatedAt = Instant.now().toEpochMilli();
        currentQuery.lastOpenAt = Instant.now().toEpochMilli();
        this.getQueryCollection().findOneAndReplace(Filters.eq("id", queryId), queryToDocument(currentQuery));
        QueryEvent updatedEvent = createEvent(queryId, QueryEvent.QueryEventType.UPDATED);
        updatedEvent.timestamp = currentQuery.lastUpdatedAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(updatedEvent));
        return currentQuery;
    }

    public void deleteQuery(String queryId, String currentUser) throws JsonProcessingException
    {
        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
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
