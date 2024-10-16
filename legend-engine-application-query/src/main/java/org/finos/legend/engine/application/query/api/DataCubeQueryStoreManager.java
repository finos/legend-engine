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
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.factory.SortedSets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.application.query.model.DataCubeQuery;
import org.finos.legend.engine.application.query.model.QuerySearchSortBy;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.application.query.model.QuerySearchTermSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.vault.Vault;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DataCubeQueryStoreManager
{
    private static final int MAX_NUMBER_OF_QUERIES = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    private static final List<String> LIGHT_QUERY_PROJECTION = Arrays.asList("id", "name", "createdAt", "lastUpdatedAt", "lastOpenAt");
    private static final int GET_QUERIES_LIMIT = 50;

    private final MongoClient mongoClient;

    public DataCubeQueryStoreManager(MongoClient mongoClient)
    {
        this.mongoClient = mongoClient;
    }

    private MongoDatabase getDataCubeQueryDatabase()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.database"))
        {
            return this.mongoClient.getDatabase(Vault.INSTANCE.getValue("query.mongo.database"));
        }
        throw new RuntimeException("DataCube Query MongoDB database has not been configured properly");
    }

    private MongoCollection<Document> getQueryCollection()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.collection.dataCube"))
        {
            return this.getDataCubeQueryDatabase().getCollection(Vault.INSTANCE.getValue("query.mongo.collection.dataCube"));
        }
        throw new RuntimeException("DataCube Query MongoDB collection has not been configured properly");
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

    private DataCubeQuery documentToQuery(Document document)
    {
        return this.documentToClass(document, DataCubeQuery.class);
    }

    private Document queryToDocument(DataCubeQuery query) throws JsonProcessingException
    {
        return Document.parse(objectMapper.writeValueAsString(query));
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

    private static void validateNonNullQueryField(Object fieldValue, String message)
    {
        validate(fieldValue != null, message);
    }

    public static void validateQuery(DataCubeQuery query)
    {
        validateNonEmptyQueryField(query.id, "Query ID is missing or empty");
        validateNonEmptyQueryField(query.name, "Query name is missing or empty");
        validateNonNullQueryField(query.query, "Query is missing");
        validateNonNullQueryField(query.source, "Query source is missing");
    }

    public List<DataCubeQuery> searchQueries(QuerySearchSpecification searchSpecification, String currentUser)
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
                filters.add(filter);
            }
            else
            {
                Bson idFilter = Filters.eq("id", querySearchTermSpecification.searchTerm);
                Bson nameFilter = Filters.regex("name", Pattern.quote(querySearchTermSpecification.searchTerm), "i");
                Bson filter = Filters.or(idFilter, nameFilter);
                filters.add(filter);
            }
        }

        List<DataCubeQuery> queries = new ArrayList<>();
        List<Bson> aggregateLists = new ArrayList<>();
        aggregateLists.add(Aggregates.match(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters)));
        if (searchSpecification.sortByOption != null)
        {
            aggregateLists.add(Aggregates.sort(Sorts.descending(getSortByField(searchSpecification.sortByOption))));
        }
        aggregateLists.add(Aggregates.project(Projections.include(LIGHT_QUERY_PROJECTION)));
        aggregateLists.add(Aggregates.limit(Math.min(MAX_NUMBER_OF_QUERIES, searchSpecification.limit == null ? Integer.MAX_VALUE : searchSpecification.limit)));
        AggregateIterable<Document> documents = this.getQueryCollection()
                .aggregate(aggregateLists);

        for (Document doc : documents)
        {
            queries.add(documentToQuery(doc));
        }
        return queries;
    }

    public String getSortByField(QuerySearchSortBy sortBy)
    {
        switch (sortBy)
        {
            case SORT_BY_CREATE:
            {
                return "createdAt";
            }
            case SORT_BY_VIEW:
            {
                return "lastOpenAt";
            }
            case SORT_BY_UPDATE:
            {
                return "lastUpdatedAt";
            }
            default:
            {
                throw new EngineException("Unknown sort-by value", EngineErrorType.COMPILATION);
            }
        }
    }

    public List<DataCubeQuery> getQueries(List<String> queryIds)
    {
        if (queryIds.size() > GET_QUERIES_LIMIT)
        {
            throw new ApplicationQueryException("Can't fetch more than " + GET_QUERIES_LIMIT + " queries", Response.Status.BAD_REQUEST);
        }
        MutableList<DataCubeQuery> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.in("id", queryIds)).limit(GET_QUERIES_LIMIT), this::documentToQuery).toList();
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

    public DataCubeQuery getQuery(String queryId) throws JsonProcessingException
    {
        List<DataCubeQuery> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.isEmpty())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        DataCubeQuery query = matchingQueries.get(0);
        query.lastOpenAt = Instant.now().toEpochMilli();
        this.getQueryCollection().updateOne(
                Filters.eq("id", queryId),
                Updates.set("lastOpenAt", Instant.now().toEpochMilli())
        );
        return query;
    }


    public DataCubeQuery createQuery(DataCubeQuery query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);
        // TODO: store ownership information

        List<DataCubeQuery> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", query.id)), this::documentToQuery).toList();
        if (!matchingQueries.isEmpty())
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "' already existed", Response.Status.BAD_REQUEST);
        }
        query.createdAt = Instant.now().toEpochMilli();
        query.lastUpdatedAt = query.createdAt;
        query.lastOpenAt = query.createdAt;
        this.getQueryCollection().insertOne(queryToDocument(query));
        return query;
    }

    public DataCubeQuery updateQuery(String queryId, DataCubeQuery query, String currentUser) throws JsonProcessingException
    {
        validateQuery(query);

        List<DataCubeQuery> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
        if (!queryId.equals(query.id))
        {
            throw new ApplicationQueryException("Updating query ID is not supported", Response.Status.BAD_REQUEST);
        }
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.isEmpty())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        DataCubeQuery currentQuery = matchingQueries.get(0);

        // TODO: check ownership
        query.createdAt = currentQuery.createdAt;
        query.lastUpdatedAt = Instant.now().toEpochMilli();
        query.lastOpenAt = Instant.now().toEpochMilli();
        this.getQueryCollection().findOneAndReplace(Filters.eq("id", queryId), queryToDocument(query));
        return query;
    }

    public void deleteQuery(String queryId, String currentUser) throws JsonProcessingException
    {
        List<DataCubeQuery> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", queryId)), this::documentToQuery).toList();
        if (matchingQueries.size() > 1)
        {
            throw new IllegalStateException("Found multiple queries with ID '" + queryId + "'");
        }
        else if (matchingQueries.isEmpty())
        {
            throw new ApplicationQueryException("Can't find query with ID '" + queryId + "'", Response.Status.NOT_FOUND);
        }
        DataCubeQuery currentQuery = matchingQueries.get(0);

        // TODO: check ownership
        this.getQueryCollection().findOneAndDelete(Filters.eq("id", queryId));
    }
}
