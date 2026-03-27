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
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.SortedSets;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.application.query.model.*;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.mongo.util.StoredVersionedAssetFetchOptions;

import javax.lang.model.SourceVersion;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryStoreManager
{
    private static final Pattern VALID_ARTIFACT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*+(-[a-z][a-z0-9_]*+)*+$");

    private static final int MAX_NUMBER_OF_QUERIES = 100;
    private static final int MAX_NUMBER_OF_EVENTS = 1000;
    private static final int QUERY_BATCH_SIZE = 1000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    // NOTE: these are non-compilable profile and tag that we come up with for query
    // so that it records the dataSpace it is created from
    private static final String QUERY_PROFILE_PATH = "meta::pure::profiles::query";
    private static final String QUERY_PROFILE_TAG_DATA_SPACE = "dataSpace";
    private static final List<String> EXCLUDED_PROJECTION_FIELDS = Arrays.asList("audit.validUntil", "audit.version", "content", "executionContext", "taggedValues", "stereotypes", "defaultParameterValues", "gridConfig");
    private static final int GET_QUERIES_LIMIT = 50;

    private final MongoClient mongoClient;
    private ApplicationQueryDao queryDao;

    public QueryStoreManager(MongoClient mongoClient)
    {
        this.mongoClient = mongoClient;
    }

    private MongoDatabase getQueryDatabase()
    {
        return this.mongoClient.getDatabase(getQueryDatabaseName());
    }

    private String getQueryDatabaseName()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.database"))
        {
            return Vault.INSTANCE.getValue("query.mongo.database");
        }
        throw new RuntimeException("Query MongoDB database has not been configured properly");
    }

    private String getQueryCollectionName()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.collection.query"))
        {
            return Vault.INSTANCE.getValue("query.mongo.collection.query");
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

    private ApplicationQueryDao getQueryDao()
    {
        if (this.queryDao == null)
        {
            this.queryDao = new ApplicationQueryDao(mongoClient, getQueryDatabaseName(), getQueryCollectionName());
        }
        return this.queryDao;
    }

    private Query convertFromStoredQuery(ApplicationStoredQuery storedQuery)
    {
        return QueryModelConverter.toQuery(storedQuery);
    }

    private ApplicationStoredQuery convertToStoredQuery(Query query)
    {
        return QueryModelConverter.toStoredQuery(query);
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
        return Document.parse(objectMapper.writeValueAsString(event));
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
        else if (query.executionContext instanceof DataProductModelAccessExecutionContext)
        {
            DataProductModelAccessExecutionContext dataProductModelAccessExecutionContext = (DataProductModelAccessExecutionContext) query.executionContext;
            validateNonEmptyQueryField(dataProductModelAccessExecutionContext.dataProductPath, "Query data product execution context dataProduct path is missing or empty");
            validateNonEmptyQueryField(dataProductModelAccessExecutionContext.accessPointGroupId, "Query data product model access execution context accessPointGroupId is missing or empty");
        }
        else if (query.executionContext instanceof DataProductNativeExecutionContext)
        {
            DataProductNativeExecutionContext dataProductNativeExecutionContext = (DataProductNativeExecutionContext) query.executionContext;
            validateNonEmptyQueryField(dataProductNativeExecutionContext.dataProductPath, "Query data product execution context dataProduct path is missing or empty");
            validateNonEmptyQueryField(dataProductNativeExecutionContext.executionKey, "Query data product native execution context executionKey is missing or empty");
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
                    filter = Filters.or(filter, Filters.eq("audit.createdBy", querySearchTermSpecification.searchTerm));
                }
                filters.add(filter);
            }
            else
            {
                Bson idFilter = Filters.eq("id", querySearchTermSpecification.searchTerm);
                Bson nameFilter = Filters.regex("name", Pattern.quote(querySearchTermSpecification.searchTerm), "i");
                Bson filter = Filters.or(idFilter, nameFilter);
                if (querySearchTermSpecification.includeOwner != null && querySearchTermSpecification.includeOwner)
                {
                    filter = Filters.or(idFilter, nameFilter, Filters.regex("audit.createdBy", Pattern.quote(querySearchTermSpecification.searchTerm), "i"));
                }
                filters.add(filter);
            }
        }
        if (searchSpecification.showCurrentUserQueriesOnly != null && searchSpecification.showCurrentUserQueriesOnly)
        {
            filters.add(Filters.in("audit.createdBy", currentUser, null));
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
            List<Bson> taggedValueFilters = ListIterate.collect(searchSpecification.taggedValues, taggedValue ->
                    Filters.and(
                            Filters.eq("taggedValues.tag.profile", taggedValue.tag.profile),
                            Filters.eq("taggedValues.tag.value", taggedValue.tag.value),
                            Filters.eq("taggedValues.value", taggedValue.value)
                    )
            );
            Bson taggedValuesFilter = searchSpecification.combineTaggedValuesCondition != null && searchSpecification.combineTaggedValuesCondition
                    ? Filters.and(taggedValueFilters)
                    : Filters.or(taggedValueFilters);

            List<String> dataspaceTaggedValues = searchSpecification.taggedValues != null
                    ? searchSpecification.taggedValues.stream()
                    .filter(taggedValue -> QUERY_PROFILE_PATH.equals(taggedValue.tag.profile) &&
                            QUERY_PROFILE_TAG_DATA_SPACE.equals(taggedValue.tag.value))
                    .map(taggedValue -> taggedValue.value)
                    .collect(Collectors.toList())
                    : new ArrayList<>();

            if (!dataspaceTaggedValues.isEmpty())
            {
                Bson executionContextFilter = Filters.and(
                        Filters.eq("executionContext._type", "dataSpaceExecutionContext"),
                        Filters.in("executionContext.dataSpacePath", dataspaceTaggedValues)
                );
                filters.add(Filters.or(taggedValuesFilter, executionContextFilter));
            }
            else
            {
                filters.add(taggedValuesFilter);
            }
        }
        if (searchSpecification.stereotypes != null && !searchSpecification.stereotypes.isEmpty())
        {
            filters.add(Filters.or(
                    ListIterate.collect(searchSpecification.stereotypes, stereotype ->
                            Filters.and(Filters.eq("stereotypes.profile", stereotype.profile), Filters.eq("stereotypes.value", stereotype.value)))));
        }

        StoredVersionedAssetFetchOptions.StoredAssetFetchOptionsBuilder builder = StoredVersionedAssetFetchOptions.builder();
        if (searchSpecification.sortByOption != null)
        {
            builder.sortDesc(getSortByField(searchSpecification.sortByOption));
        }
        builder.withExcludeFields(EXCLUDED_PROJECTION_FIELDS);
        if (searchSpecification.limit != null && searchSpecification.limit <= 0)
        {
            throw new ApplicationQueryException("Limit should be greater than 0", Response.Status.BAD_REQUEST);
        }
        builder.withLimit(Math.min(MAX_NUMBER_OF_QUERIES, searchSpecification.limit == null ? Integer.MAX_VALUE : searchSpecification.limit));

        return getQueryDao().find(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters), false, builder.build())
                .map(this::convertFromStoredQuery)
                .sorted(Comparator.comparing(query -> query.owner != null && query.owner.equals(currentUser) ? 0 : 1))
                .collect(Collectors.toList());
    }

    public String getSortByField(QuerySearchSortBy sortBy)
    {
        switch (sortBy)
        {
            case SORT_BY_CREATE:
                return "audit.createdAt";
            case SORT_BY_VIEW:
                return "lastOpenAt";
            case SORT_BY_UPDATE:
                return "audit.updatedAt";
            default:
                throw new EngineException("Unknown sort-by value", EngineErrorType.COMPILATION);
        }
    }

    public List<Query> getQueries(List<String> queryIds)
    {
        if (queryIds.size() > GET_QUERIES_LIMIT)
        {
            throw new ApplicationQueryException("Can't fetch more than " + GET_QUERIES_LIMIT + " queries", Response.Status.BAD_REQUEST);
        }
        List<Query> matchingQueries = getQueryDao().find(Maps.fixedSize.of("id", queryIds), false, true, StoredVersionedAssetFetchOptions.builder().withLimit(GET_QUERIES_LIMIT).build())
                .map(this::convertFromStoredQuery).collect(Collectors.toList());
        // validate
        MutableSortedSet<String> notFoundQueries = SortedSets.mutable.empty();
        MutableSortedSet<String> duplicatedQueries = SortedSets.mutable.empty();
        queryIds.forEach(queryId ->
        {
            long count = matchingQueries.stream().filter(query -> queryId.equals(query.id)).count();
            if (count > 1)
            {
                duplicatedQueries.add(queryId);
            }
            else if (count == 0)
            {
                notFoundQueries.add(queryId);
            }
        });
        if (!duplicatedQueries.isEmpty())
        {
            throw new IllegalStateException(duplicatedQueries.makeString("Found multiple queries with duplicated ID for the following ID(s):\\n", "\\n", ""));
        }
        if (!notFoundQueries.isEmpty())
        {
            throw new ApplicationQueryException(notFoundQueries.makeString("Can't find queries for the following ID(s):\\n", "\\n", ""), Response.Status.INTERNAL_SERVER_ERROR);
        }
        return matchingQueries;
    }

    public List<Query> getAllQueries(int from, int to)
    {
        if (to - from > QUERY_BATCH_SIZE)
        {
            throw new ApplicationQueryException("Can't fetch more than " + QUERY_BATCH_SIZE + " queries at a time", Response.Status.BAD_REQUEST);
        }
        else if (from < 0 || to < from)
        {
            throw new ApplicationQueryException("Invalid pagination range", Response.Status.BAD_REQUEST);
        }
        else if (from == to)
        {
            return new ArrayList<>();
        }
        return getQueryDao().getAll(StoredVersionedAssetFetchOptions.builder()
                .sortAsc("id")
                .withSkip(from)
                .withLimit(to - from)
                .build()).map(this::convertFromStoredQuery).collect(Collectors.toList());
    }

    public Query getQuery(String queryId)
    {
        Optional<ApplicationStoredQuery> matchingQuery = getQueryDao().get(queryId);
        if (!matchingQuery.isPresent())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        ApplicationStoredQuery storedQuery = matchingQuery.get();

        storedQuery.lastOpenAt = Instant.now().toEpochMilli();
        getQueryDao().update(queryId, storedQuery, null, false);
        return this.convertFromStoredQuery(storedQuery);
    }

    public Query createQuery(Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        query.owner = currentUser;

        Optional<ApplicationStoredQuery> existingQuery = getQueryDao().get(query.id);
        if (existingQuery.isPresent())
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "' already existed", Response.Status.BAD_REQUEST);
        }

        ApplicationStoredQuery createdQuery = getQueryDao().create(convertToStoredQuery(query), currentUser);
        query = this.convertFromStoredQuery(createdQuery);

        QueryEvent createdEvent = createEvent(query.id, QueryEvent.QueryEventType.CREATED);
        createdEvent.timestamp = query.createdAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(createdEvent));
        return query;
    }

    public Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        if (!queryId.equals(query.id))
        {
            throw new ApplicationQueryException("Updating query ID is not supported", Response.Status.BAD_REQUEST);
        }
        Optional<ApplicationStoredQuery> existingQuery = getQueryDao().get(queryId);
        if (!existingQuery.isPresent())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        ApplicationStoredQuery currentStoredQuery = existingQuery.get();

        // Make sure only the owner can update the query
        // NOTE: if the query is created by an anonymous user previously, set the current user as the owner;
        // we handle this case on the database update itself
        if (currentStoredQuery.getAudit().getCreatedBy() != null && !currentStoredQuery.getAudit().getCreatedBy().equals(currentUser))
        {
            throw new ApplicationQueryException("Only owner can update the query", Response.Status.FORBIDDEN);
        }
        ApplicationStoredQuery storedQuery = convertToStoredQuery(query);

        ApplicationStoredQuery updatedQuery = getQueryDao().update(queryId, storedQuery, currentUser);
        query = this.convertFromStoredQuery(updatedQuery);

        QueryEvent updatedEvent = createEvent(query.id, QueryEvent.QueryEventType.UPDATED);
        updatedEvent.timestamp = query.lastUpdatedAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(updatedEvent));
        return query;
    }

    public Query patchQuery(String queryId, Query updatedQuery, String currentUser) throws JsonProcessingException
    {
        Query currentQuery = this.getQuery(queryId);
        // Make sure only the owner can update the query
        // NOTE: if the query is created by an anonymous user previously, set the current user as the owner;
        // we handle this case on the database update itself
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
        ApplicationStoredQuery storedQuery = getQueryDao().update(queryId, convertToStoredQuery(currentQuery), currentUser);
        currentQuery = convertFromStoredQuery(storedQuery);

        QueryEvent updatedEvent = createEvent(queryId, QueryEvent.QueryEventType.UPDATED);
        updatedEvent.timestamp = currentQuery.lastUpdatedAt;
        this.getQueryEventCollection().insertOne(queryEventToDocument(updatedEvent));
        return currentQuery;
    }

    public void deleteQuery(String queryId, String currentUser) throws JsonProcessingException
    {
        Optional<ApplicationStoredQuery> existingQuery = getQueryDao().get(queryId);
        if (!existingQuery.isPresent())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        if (existingQuery.get().getAudit().getCreatedBy() != null && !existingQuery.get().getAudit().getCreatedBy().equals(currentUser))
        {
            throw new ApplicationQueryException("Only owner can delete the query", Response.Status.FORBIDDEN);
        }
        getQueryDao().delete(queryId, currentUser);
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

    public QueryStoreStats getQueryStoreStats()
    {
        Long count = this.getQueryDatabase().getCollection(getQueryCollectionName()).countDocuments();
        QueryStoreStats storeStats = new QueryStoreStats();
        storeStats.setQueryCount(count);
        Bson dataSpaceFilter = Filters.and(
                Filters.eq("taggedValues.tag.profile", QUERY_PROFILE_PATH),
                Filters.eq("taggedValues.tag.value", QUERY_PROFILE_TAG_DATA_SPACE));
        storeStats.setQueryCreatedFromDataSpaceCount(this.getQueryDatabase().getCollection(getQueryCollectionName())
                .countDocuments(dataSpaceFilter));
        return storeStats;
    }
}
