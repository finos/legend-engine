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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.SortedSets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.application.query.model.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;

import javax.lang.model.SourceVersion;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;


public class RedisQueryStoreManager implements QueryStoreManager
{
    private static final String COLLECTION_QUERY = "query";
    private static final String COLLECTION_QUERY_EVENT = "query-event";

    private static final Pattern VALID_ARTIFACT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*+(-[a-z][a-z0-9_]*+)*+$");

    private static final int MAX_NUMBER_OF_QUERIES = 100;
    private static final int MAX_NUMBER_OF_EVENTS = 1000;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // NOTE: these are non-compilable profile and tag that we come up with for query
    // so that it records the dataSpace it is created from
    private static final String QUERY_PROFILE_PATH = "meta::pure::profiles::query";
    private static final String QUERY_PROFILE_TAG_DATA_SPACE = "dataSpace";
    private static final int GET_QUERIES_LIMIT = 50;

    private final UnifiedJedis redisClient;
    private final JsonSetParams jsonSetOnlyIfNotExistParam = JsonSetParams.jsonSetParams().nx();

    private static final String REDIS_JSON_ROOT = "$";
    private static final String REDIS_QUERY_FIELD_MOD_PREFIX = "@";
    private static final String REDIS_QUERY_WILDCARD = "*";
    private static final String REDIS_KEY_DELIMITER = ":";
    private static final String REDIS_QUERY_INDEX_SUFFIX = REDIS_KEY_DELIMITER + "index";
    private static final Pattern REDIS_QUERY_SPECIAL_CHARACTERS_PATTERN = Pattern.compile("[-:.]");

    private static final String ARTIFACT_ID = "artifactId";
    private static final String ARTIFACT_ID_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + ARTIFACT_ID;
    private static final String GROUP_ID = "groupId";
    private static final String GROUP_ID_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + GROUP_ID;
    private static final String VERSION_ID = "versionId";
    private static final String VERSION_ID_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + VERSION_ID;
    private static final String EVENT_TYPE = "eventType";
    private static final String EVENT_TYPE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + EVENT_TYPE;
    private static final String ID = "id";
    private static final String ID_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + ID;
    private static final String NAME = "name";
    private static final String NAME_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + NAME;
    private static final String OWNER = "owner";
    private static final String OWNER_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + OWNER;
    private static final String QUERY_ID = "queryId";
    private static final String QUERY_ID_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + QUERY_ID;
    private static final String TIMESTAMP = "timestamp";
    private static final String TIMESTAMP_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + TIMESTAMP;
    public static final String CREATED = "createdAt";
    public static final String CREATED_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + CREATED;
    public static final String UPDATED = "lastUpdatedAt";
    public static final String UPDATED_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + UPDATED;
    private static final String STEREOTYPE_PROFILE = "stereotypes_profile";
    private static final String STEREOTYPE_PROFILE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + STEREOTYPE_PROFILE;
    private static final String STEREOTYPE_VALUE = "stereotypes_value";
    private static final String STEREOTYPE_VALUE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + STEREOTYPE_VALUE;
    private static final String TAGGED_VALUES_VALUE = "taggedValues_value";
    private static final String TAGGED_VALUES_VALUE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + TAGGED_VALUES_VALUE;
    private static final String TAGGED_VALUES_TAG_PROFILE = "taggedValues_tag_profile";
    private static final String TAGGED_VALUES_TAG_PROFILE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + TAGGED_VALUES_TAG_PROFILE;
    private static final String TAGGED_VALUES_TAG_VALUE = "taggedValues_tag_value";
    private static final String TAGGED_VALUES_TAG_VALUE_TAG = REDIS_QUERY_FIELD_MOD_PREFIX + TAGGED_VALUES_TAG_VALUE;

    public RedisQueryStoreManager(UnifiedJedis redisClient)
    {
        this.redisClient = redisClient;
        buildIndexes(redisClient);
    }

    public void buildIndexes(UnifiedJedis redisClient)
    {
        Schema querySchema = new Schema()
                .addSortableTagField("$." + ID, false).as(ID)
                .addSortableTagField("$." + GROUP_ID, false).as(GROUP_ID)
                .addSortableTagField("$." + ARTIFACT_ID, false).as(ARTIFACT_ID)
                .addSortableTagField("$." + VERSION_ID, false).as(VERSION_ID)
                .addSortableTagField("$." + NAME, false).as(NAME)
                .addSortableTagField("$." + OWNER, false).as(OWNER)
                .addSortableNumericField("$." + CREATED).as(CREATED)
                .addSortableNumericField("$." + UPDATED).as(UPDATED)
                .addSortableTagField("$." + StringUtils.replace("stereotypes.*.profile", "_", "."), false).as(STEREOTYPE_PROFILE)
                .addSortableTagField("$." + StringUtils.replace("stereotypes.*.value", "_", "."), false).as(STEREOTYPE_VALUE)
                .addSortableTagField("$." + StringUtils.replace("taggedValues.*.value", "_", "."), false).as(TAGGED_VALUES_VALUE)
                .addSortableTagField("$." + StringUtils.replace("taggedValues.*.tag.profile", "_", "."), false).as(TAGGED_VALUES_TAG_PROFILE)
                .addSortableTagField("$." + StringUtils.replace("taggedValues.*.tag.value", "_", "."), false).as(TAGGED_VALUES_TAG_VALUE);

        buildIndex(redisClient, COLLECTION_QUERY, querySchema);

        Schema queryEventSchema = new Schema()
                .addSortableTagField("$." + QUERY_ID, false).as(QUERY_ID)
                .addSortableTagField("$." + EVENT_TYPE, false).as(EVENT_TYPE)
                .addSortableNumericField("$." + TIMESTAMP).as(TIMESTAMP);

        buildIndex(redisClient, COLLECTION_QUERY_EVENT, queryEventSchema);
    }

    private static Query documentToQuery(Document document)
    {
        return convertPropertiesMapToQuery(convertDocumentToPropertiesMap(document));
    }

    private static QueryEvent documentToQueryEvent(Document document)
    {
        return convertPropertiesMapToQueryEvent(convertDocumentToPropertiesMap(document));
    }

    private static String getStringValue(Object value)
    {
        return value instanceof byte[] ? new String((byte[]) value, StandardCharsets.UTF_8) : String.valueOf(value);
    }

    private static QueryEvent createEvent(String queryId, QueryEvent.QueryEventType eventType)
    {
        QueryEvent event = new QueryEvent();
        event.queryId = queryId;
        event.timestamp = Instant.now().toEpochMilli();
        event.eventType = eventType;
        return event;
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

    public List<Query> searchQueries(QuerySearchSpecification searchSpecification, String currentUser)
    {
        StringBuffer redisQuery = new StringBuffer();

        List<String> searchSpecificationNameMatchAndOwnerList = new ArrayList<>();

        if (searchSpecification.searchTerm != null)
        {
            if (searchSpecification.exactMatchName != null && searchSpecification.exactMatchName)
            {
                appendQueryTagEqualCondition(redisQuery, NAME_TAG, searchSpecification.searchTerm);
            }
            else if (searchSpecification.showCurrentUserQueriesOnly != null && searchSpecification.showCurrentUserQueriesOnly)
            {
                AggregationBuilder aggregation = new AggregationBuilder(REDIS_QUERY_WILDCARD)
                        .load(ID_TAG, NAME_TAG, OWNER_TAG)
                        .filter("!exists(" + OWNER_TAG + ") || " + OWNER_TAG + " == '" + currentUser + "'");

                AggregationResult resultSet = redisClient.ftAggregate(COLLECTION_QUERY + REDIS_QUERY_INDEX_SUFFIX, aggregation);
                if (resultSet != null)
                {
                    for (Map<String, Object> propertiesMap : resultSet.getResults())
                    {
                        Pattern pattern = Pattern.compile(Pattern.quote(searchSpecification.searchTerm), Pattern.CASE_INSENSITIVE);
                        String queryId = getStringValue(propertiesMap.get(ID));
                        if (pattern.matcher(getStringValue(propertiesMap.get(NAME))).find() || searchSpecification.searchTerm.equals(queryId))
                        {
                            searchSpecificationNameMatchAndOwnerList.add(queryId);
                        }
                    }
                }
            }
            else
            {
                SearchResult resultSet = redisClient.ftSearch(COLLECTION_QUERY + REDIS_QUERY_INDEX_SUFFIX,
                        new redis.clients.jedis.search.Query(REDIS_QUERY_WILDCARD).returnFields(ID, NAME));

                if (resultSet != null)
                {
                    Pattern pattern = Pattern.compile(Pattern.quote(searchSpecification.searchTerm), Pattern.CASE_INSENSITIVE);
                    for (Document document : resultSet.getDocuments())
                    {
                        String queryId = getStringValue(document.get(ID));
                        if (pattern.matcher(getStringValue(document.get(NAME))).find() || searchSpecification.searchTerm.equals(queryId))
                        {
                            searchSpecificationNameMatchAndOwnerList.add(queryId);
                        }
                    }
                }
            }
        }

        if (searchSpecification.showCurrentUserQueriesOnly != null && searchSpecification.showCurrentUserQueriesOnly &&
            searchSpecificationNameMatchAndOwnerList.isEmpty())
        {
            AggregationBuilder aggregation = new AggregationBuilder(REDIS_QUERY_WILDCARD)
                    .load(ID_TAG, OWNER_TAG)
                    .filter("!exists(" + OWNER_TAG + ") || " + OWNER_TAG + " == '" + currentUser + "'");

            AggregationResult resultSet = redisClient.ftAggregate(COLLECTION_QUERY + REDIS_QUERY_INDEX_SUFFIX, aggregation);
            if (resultSet != null)
            {
                for (Map<String, Object> propertiesMap : resultSet.getResults())
                {
                    searchSpecificationNameMatchAndOwnerList.add(getStringValue(propertiesMap.get(ID)));
                }
            }
        }

        if (!searchSpecificationNameMatchAndOwnerList.isEmpty())
        {
            appendQueryTagInCondition(redisQuery, ID_TAG, searchSpecificationNameMatchAndOwnerList);
        }

        if (searchSpecification.projectCoordinates != null && !searchSpecification.projectCoordinates.isEmpty())
        {
            redisQuery.append("(");
            for (QueryProjectCoordinates projectCoordinate : searchSpecification.projectCoordinates)
            {
                redisQuery.append(" ( ");
                appendQueryTagEqualCondition(redisQuery, GROUP_ID_TAG, projectCoordinate.groupId);
                appendQueryTagEqualCondition(redisQuery, ARTIFACT_ID_TAG, projectCoordinate.artifactId);

                if (projectCoordinate.version != null)
                {
                    appendQueryTagEqualCondition(redisQuery, VERSION_ID_TAG, projectCoordinate.version);
                }
                redisQuery.append(") |");
            }
            redisQuery.setLength(redisQuery.length() - 1); // Removes the "|" after the last iteration
            redisQuery.append(") ");
        }

        if (searchSpecification.taggedValues != null && !searchSpecification.taggedValues.isEmpty())
        {
            redisQuery.append("("); // Open Scope
            for (TaggedValue taggedValue : searchSpecification.taggedValues)
            {
                redisQuery.append(" ( "); // Open Scope
                appendQueryTagEqualCondition(redisQuery, TAGGED_VALUES_TAG_PROFILE_TAG, taggedValue.tag.profile);
                appendQueryTagEqualCondition(redisQuery, TAGGED_VALUES_TAG_VALUE_TAG, taggedValue.tag.value);
                appendQueryTagEqualCondition(redisQuery, TAGGED_VALUES_VALUE_TAG, taggedValue.value);
                redisQuery.append(") "); //Close Scope

                if (searchSpecification.combineTaggedValuesCondition == null || !searchSpecification.combineTaggedValuesCondition)
                {
                    redisQuery.append("|"); // OR Condition
                }
            }
            if (searchSpecification.combineTaggedValuesCondition == null || !searchSpecification.combineTaggedValuesCondition)
            {
                redisQuery.setLength(redisQuery.length() - 1); // Removes the OR Condition after the last iteration
            }
            redisQuery.append(") "); //Close Scope
        }

        if (searchSpecification.stereotypes != null && !searchSpecification.stereotypes.isEmpty())
        {
            redisQuery.append("("); // Open Scope
            for (StereotypePtr stereotype : searchSpecification.stereotypes)
            {
                redisQuery.append(" ( "); // Open Scope
                appendQueryTagEqualCondition(redisQuery, STEREOTYPE_PROFILE_TAG, stereotype.profile);
                appendQueryTagEqualCondition(redisQuery, STEREOTYPE_VALUE_TAG, stereotype.value);
                redisQuery.append(") |"); //Close Scope
            }
            redisQuery.setLength(redisQuery.length() - 1); // Removes the OR Condition after the last iteration
            redisQuery.append(") "); //Close Scope
        }

        int limit = Math.min(MAX_NUMBER_OF_QUERIES, searchSpecification.limit == null ? Integer.MAX_VALUE : searchSpecification.limit);

        AggregationBuilder aggregation = new AggregationBuilder(redisQuery.length() == 0 ? REDIS_QUERY_WILDCARD : redisQuery.toString())
                .load(ID_TAG, NAME_TAG, VERSION_ID_TAG, GROUP_ID_TAG, ARTIFACT_ID_TAG, OWNER_TAG, CREATED_TAG, UPDATED_TAG)
                //.apply(OWNER_TAG + " == '" + currentUser + "'", "isCurrentUser") //TODO in conflict with ownership NULL logic
                //.sortByDesc("@isCurrentUser")
                .limit(limit);

        AggregationResult resultSet = redisClient.ftAggregate(COLLECTION_QUERY + REDIS_QUERY_INDEX_SUFFIX, aggregation);

        Map<Integer, Query> sortedMap = new TreeMap();
        int nonOwnersCounter = 0;
        int ownersCounter = limit + 1;

        List<Map<String, Object>> results = resultSet.getResults();
        if (results != null && !results.isEmpty())
        {
            for (Map<String, Object> result : results)
            {
                if (result != null)
                {
                    //result.remove("isCurrentUser"); //TODO in conflict with ownership NULL logic

                    Query query = convertPropertiesMapToQuery(result);
                    if (currentUser.equals(query.owner))
                    {
                        sortedMap.put(ownersCounter++, query);
                    }
                    else
                    {
                        sortedMap.put(nonOwnersCounter++, query);
                    }
                }
            }
        }
        return new ArrayList<>(sortedMap.values());
    }

    public List<Query> getQueries(List<String> queryIds)
    {
        if (queryIds.size() > GET_QUERIES_LIMIT)
        {
            throw new ApplicationQueryException("Can't fetch more than " + GET_QUERIES_LIMIT + " queries", Response.Status.BAD_REQUEST);
        }

        redis.clients.jedis.search.Query redisQuery = new redis.clients.jedis.search.Query(appendQueryTagInCondition(new StringBuffer(), ID_TAG, queryIds).toString());
        redisQuery.limit(0, GET_QUERIES_LIMIT);

        MutableList<Query> matchingQueries = LazyIterate.collect(find(COLLECTION_QUERY, redisQuery),
                RedisQueryStoreManager::documentToQuery).toList();

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
            throw new ApplicationQueryException(notFoundQueries.makeString("Can't find queries for the following ID(s):\\n", "\\n", ""), Response.Status.NOT_FOUND);
        }
        return matchingQueries;
    }

    public Query getQuery(String queryId)
    {
        StringBuffer query = appendQueryTagEqualCondition(new StringBuffer(), ID_TAG, queryId);

        List<Query> matchingQueries = LazyIterate.collect(find(COLLECTION_QUERY, new redis.clients.jedis.search.Query(query.toString())),
                RedisQueryStoreManager::documentToQuery).toList();

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

    public QueryStoreStats getQueryStoreStats()
    {
        Long count = count(COLLECTION_QUERY, new redis.clients.jedis.search.Query(REDIS_QUERY_WILDCARD));
        QueryStoreStats storeStats = new QueryStoreStats();
        storeStats.setQueryCount(count);

        StringBuffer query = appendQueryTagEqualCondition(new StringBuffer(), TAGGED_VALUES_TAG_PROFILE_TAG, QUERY_PROFILE_PATH);
        appendQueryTagEqualCondition(query, TAGGED_VALUES_TAG_VALUE_TAG, QUERY_PROFILE_TAG_DATA_SPACE);
        storeStats.setQueryCreatedFromDataSpaceCount(count(COLLECTION_QUERY, new redis.clients.jedis.search.Query(query.toString())));

        return storeStats;
    }

    public Query createQuery(Query query, String currentUser)
    {
        validateQuery(query);

        // Force the current user as owner regardless of user input
        query.owner = currentUser;

        StringBuffer redisQuery = appendQueryTagEqualCondition(new StringBuffer(), ID_TAG, query.id);

        List<Query> matchingQueries = LazyIterate.collect(find(COLLECTION_QUERY, new redis.clients.jedis.search.Query(redisQuery.toString())),
                RedisQueryStoreManager::documentToQuery).toList();

        if (matchingQueries.size() >= 1)
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "' already existed", Response.Status.BAD_REQUEST);
        }

        query.createdAt = Instant.now().toEpochMilli();
        query.lastUpdatedAt = query.createdAt;

        upsert(createQueryKey(query), true, false, convertQueryToPropertiesMap(query));

        QueryEvent createdEvent = createEvent(query.id, QueryEvent.QueryEventType.CREATED);
        createdEvent.timestamp = query.createdAt;

        upsert(createQueryEventKey(createdEvent), true, false, convertQueryToPropertiesMap(createdEvent));
        return query;
    }

    public Query updateQuery(String queryId, Query query, String currentUser)
    {
        validateQuery(query);

        StringBuffer redisQuery = appendQueryTagEqualCondition(new StringBuffer(), ID_TAG, query.id);

        List<Query> matchingQueries = LazyIterate.collect(find(COLLECTION_QUERY, new redis.clients.jedis.search.Query(redisQuery.toString())),
                RedisQueryStoreManager::documentToQuery).toList();

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

        upsert(createQueryKey(query), false, false, convertQueryToPropertiesMap(query));

        QueryEvent updatedEvent = createEvent(query.id, QueryEvent.QueryEventType.UPDATED);
        updatedEvent.timestamp = query.lastUpdatedAt;

        upsert(createQueryEventKey(updatedEvent), true, false, convertQueryToPropertiesMap(updatedEvent));
        return query;
    }

    public void deleteQuery(String queryId, String currentUser)
    {
        StringBuffer redisQuery = appendQueryTagEqualCondition(new StringBuffer(), ID_TAG, queryId);

        List<Query> matchingQueries = LazyIterate.collect(find(COLLECTION_QUERY, new redis.clients.jedis.search.Query(redisQuery.toString())),
                RedisQueryStoreManager::documentToQuery).toList();

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

        deleteByKey(createQueryKey(currentQuery));

        QueryEvent deletedEvent = createEvent(queryId, QueryEvent.QueryEventType.DELETED);
        upsert(createQueryEventKey(deletedEvent), true, false, convertQueryToPropertiesMap(deletedEvent));
    }

    public List<QueryEvent> getQueryEvents(String queryId, QueryEvent.QueryEventType eventType, Long since, Long until, Integer limit)
    {
        StringBuffer redisQuery = new StringBuffer();

        if (queryId != null)
        {
            appendQueryTagEqualCondition(redisQuery, QUERY_ID_TAG, queryId);
        }
        if (eventType != null)
        {
            appendQueryTagEqualCondition(redisQuery, EVENT_TYPE_TAG, eventType.toString());
        }
        if (since != null)
        {
            appendQueryTagGreaterThanOrEqualToCondition(redisQuery, TIMESTAMP_TAG, since);
        }
        if (until != null)
        {
            appendQueryTagLessThanOrEqualToCondition(redisQuery, TIMESTAMP_TAG, until);
        }

        redis.clients.jedis.search.Query query = new redis.clients.jedis.search.Query(redisQuery.length() > 0 ? redisQuery.toString() : REDIS_QUERY_WILDCARD);
        query.limit(0, Math.min(MAX_NUMBER_OF_EVENTS, limit == null ? Integer.MAX_VALUE : limit));

        return LazyIterate.collect(find(COLLECTION_QUERY_EVENT, query), RedisQueryStoreManager::documentToQueryEvent).toList();
    }

    private StringBuffer appendQueryTagEqualCondition(StringBuffer query, String fieldModifier, Object fieldValue)
    {
        if (fieldValue instanceof String)
        {
            fieldValue = handleSpecialCharacters(String.valueOf(fieldValue));
        }
        return query.append(fieldModifier).append(":{ ").append(fieldValue).append(" } ");
    }

    private StringBuffer appendQueryTagInCondition(StringBuffer query, String fieldModifier, List<String> fieldValues)
    {
        if (fieldValues == null && fieldValues.isEmpty())
        {
            return query;
        }
        query.append(fieldModifier).append(":{ ");

        for (String fieldValue : fieldValues)
        {
            fieldValue = handleSpecialCharacters(fieldValue);

            query.append(fieldValue).append(" | ");
        }
        query.setLength(query.length() - 2);
        query.append("} ");

        return query;
    }

    protected StringBuffer appendQueryTagLessThanOrEqualToCondition(StringBuffer query, String fieldModifier, Object fieldValue)
    {
        if (fieldValue instanceof String)
        {
            fieldValue = handleSpecialCharacters(String.valueOf(fieldValue));
        }
        return query.append(fieldModifier).append(":[-inf ").append(fieldValue).append("] ");
    }

    protected StringBuffer appendQueryTagGreaterThanOrEqualToCondition(StringBuffer query, String fieldModifier, Object fieldValue)
    {
        if (fieldValue instanceof String)
        {
            fieldValue = handleSpecialCharacters(String.valueOf(fieldValue));
        }
        return query.append(fieldModifier).append(":[").append(fieldValue).append(" inf] ");
    }

    private void buildIndex(UnifiedJedis redisClient, String collectionName, Schema schema)
    {
        String indexName = collectionName + REDIS_QUERY_INDEX_SUFFIX;
        try
        {
            redisClient.ftInfo(indexName);
            return;
        }
        catch (Exception e)
        {
            // if index already exists this will consume the exception which is the expected behavior
        }

        // For SORTABLE fields, the default ordering is ASC if not specified otherwise
        // Uniqueness will need to be managed at the INSERT level using the NX option since Redis does not support unique indexes
        IndexDefinition def = new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{ collectionName + ":" });
        redisClient.ftCreate(indexName, redis.clients.jedis.search.IndexOptions.defaultOptions().setDefinition(def), schema);
    }

    private static Map<String, Object> convertDocumentToPropertiesMap(Document document)
    {
        if (document == null || !document.hasProperty(REDIS_JSON_ROOT))
        {
            return null;
        }

        String jsonString = String.valueOf(document.get(REDIS_JSON_ROOT));
        if (jsonString == null || jsonString.isEmpty())
        {
            return null;
        }

        try
        {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception e)
        {
            throw new ApplicationQueryException(String.format("error converting document (%s) reason: %s", document.getId(), e.getMessage()), Response.Status.BAD_REQUEST);
        }
    }

    private static Query convertPropertiesMapToQuery(Map<String, Object> propertiesMap)
    {
        Query query = new Query();

        query.id = propertiesMap.containsKey(ID) ? getStringValue(propertiesMap.get(ID)) : query.id;
        query.name = propertiesMap.containsKey(NAME) ? getStringValue(propertiesMap.get(NAME)) : query.name;
        query.description = propertiesMap.containsKey("description") ? getStringValue(propertiesMap.get("description")) : query.description;
        query.groupId = propertiesMap.containsKey(GROUP_ID) ? getStringValue(propertiesMap.get(GROUP_ID)) : query.groupId;
        query.artifactId = propertiesMap.containsKey(ARTIFACT_ID) ? getStringValue(propertiesMap.get(ARTIFACT_ID)) : query.artifactId;
        query.versionId = propertiesMap.containsKey(VERSION_ID) ? getStringValue(propertiesMap.get(VERSION_ID)) : query.versionId;
        query.mapping = propertiesMap.containsKey("mapping") ? getStringValue(propertiesMap.get("mapping")) : query.mapping;
        query.runtime = propertiesMap.containsKey("runtime") ? getStringValue(propertiesMap.get("runtime")) : query.runtime;
        query.content = propertiesMap.containsKey("content") ? getStringValue(propertiesMap.get("content")) : query.content;
        query.owner = propertiesMap.containsKey(OWNER) ? getStringValue(propertiesMap.get(OWNER)) : query.owner;
        query.createdAt = propertiesMap.containsKey(CREATED) ? Long.parseLong(getStringValue(propertiesMap.get(CREATED))) : query.createdAt;
        query.lastUpdatedAt = propertiesMap.containsKey(UPDATED) ? Long.parseLong(getStringValue(propertiesMap.get(UPDATED))) : query.lastUpdatedAt;

        if (propertiesMap.containsKey("taggedValues"))
        {
            List<Map<String, Object>> taggedValuesProperties = (List<Map<String, Object>>) propertiesMap.get("taggedValues");

            query.taggedValues = new ArrayList<>(taggedValuesProperties.size());

            for (Map<String, Object> taggedValueProperties : taggedValuesProperties)
            {
                TaggedValue taggedValue = new TaggedValue();
                taggedValue.value = getStringValue(taggedValueProperties.get("value"));

                if (taggedValueProperties.containsKey("tag"))
                {
                    Map<String, Object> tagProperties = (Map<String, Object>) taggedValueProperties.get("tag");

                    taggedValue.tag = new TagPtr();
                    taggedValue.tag.value = getStringValue(tagProperties.get("value"));
                    taggedValue.tag.profile = getStringValue(tagProperties.get("profile"));
                }

                query.taggedValues.add(taggedValue);
            }
        }

        if (propertiesMap.containsKey("stereotypes"))
        {
            List<Map<String, Object>> stereotypesProperties = (List<Map<String, Object>>) propertiesMap.get("stereotypes");

            query.stereotypes = new ArrayList<>(stereotypesProperties.size());

            for (Map<String, Object> stereotypeProperties : stereotypesProperties)
            {
                StereotypePtr stereotype = new StereotypePtr();
                stereotype.value = getStringValue(stereotypeProperties.get("value"));
                stereotype.profile = getStringValue(stereotypeProperties.get("profile"));

                query.stereotypes.add(stereotype);
            }
        }

        if (propertiesMap.containsKey("defaultParameterValues"))
        {
            List<Map<String, Object>> defaultParameterValuesProperties =
                    (List<Map<String, Object>>) propertiesMap.get("defaultParameterValues");

            query.defaultParameterValues = new ArrayList<>(defaultParameterValuesProperties.size());

            for (Map<String, Object> stereotypeProperties : defaultParameterValuesProperties)
            {
                QueryParameterValue queryParameterValue = new QueryParameterValue();
                queryParameterValue.name = getStringValue(stereotypeProperties.get("name"));
                queryParameterValue.content = getStringValue(stereotypeProperties.get("content"));

                query.defaultParameterValues.add(queryParameterValue);
            }
        }

        return query;
    }

    private static QueryEvent convertPropertiesMapToQueryEvent(Map<String, Object> propertiesMap)
    {
        QueryEvent event = new QueryEvent();

        event.queryId = getStringValue(propertiesMap.get(QUERY_ID));
        event.timestamp = Long.parseLong(getStringValue(propertiesMap.get(TIMESTAMP)));
        event.eventType = QueryEvent.QueryEventType.valueOf(getStringValue(propertiesMap.get(EVENT_TYPE)));
        return event;
    }

    private Map<String, Object> convertQueryToPropertiesMap(Object query)
    {
        try
        {
            return objectMapper.readValue(objectMapper.writeValueAsString(query), new TypeReference<Map<String, Object>>() {});
        }
        catch (JsonProcessingException e)
        {
            throw new ApplicationQueryException("Error serializing dataset to json", Response.Status.BAD_REQUEST);
        }
    }

    private long count(String collectionName, redis.clients.jedis.search.Query query)
    {
        query.limit(0,0);

        SearchResult countResult = redisClient.ftSearch(collectionName + REDIS_QUERY_INDEX_SUFFIX, query);
        return countResult != null ? countResult.getTotalResults() : 0;
    }

    private String createQueryKey(Query query)
    {
        return COLLECTION_QUERY + ":" + query.id;
    }

    private String createQueryEventKey(QueryEvent queryEvent)
    {
        return COLLECTION_QUERY_EVENT + ":" + queryEvent.queryId + ":" + queryEvent.timestamp;
    }

    private long deleteByKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        return redisClient.unlink(key);
    }

    private String handleSpecialCharacters(String input)
    {
        return REDIS_QUERY_SPECIAL_CHARACTERS_PATTERN.matcher(input).replaceAll("\\\\$0");
    }

    private  List<Document> find(String collectionName, redis.clients.jedis.search.Query query)
    {
        SearchResult searchResult = redisClient.ftSearch(collectionName + REDIS_QUERY_INDEX_SUFFIX, query);
        if (searchResult != null)
        {
            return searchResult.getDocuments();
        }
        return new ArrayList<>();
    }

    private  void upsert(String key, boolean isIndexUnique, boolean isIdRequired, Map<String, Object> propertiesMap)
    {
        String status;

        if (isIdRequired)
        {
            propertiesMap.putIfAbsent(ID, key);
        }

        if (isIndexUnique)
        {
            status = redisClient.jsonSet(key, Path.ROOT_PATH, propertiesMap, jsonSetOnlyIfNotExistParam);
        }
        else
        {
            status = redisClient.jsonSet(key, Path.ROOT_PATH, propertiesMap);
        }

        if (!"OK".equals(status))
        {
            throw new ApplicationQueryException("Error inserting dataset with key: '" + key + "' - ensure the key is unique",
                    Response.Status.NOT_MODIFIED);
        }
    }

}