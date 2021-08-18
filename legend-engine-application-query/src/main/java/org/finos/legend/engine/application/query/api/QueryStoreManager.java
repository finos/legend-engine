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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.shared.core.vault.Vault;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class QueryStoreManager
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Document EMPTY_FILTER = Document.parse("{}");

    private final MongoClient mongoClient;

    public QueryStoreManager(MongoClient mongoClient)
    {
        this.mongoClient = mongoClient;
    }

    private MongoCollection<Document> getQueryCollection()
    {
        if (Vault.INSTANCE.hasValue("query.mongo.database") && Vault.INSTANCE.hasValue("query.mongo.collection"))
        {
            return this.mongoClient.getDatabase(Vault.INSTANCE.getValue("query.mongo.database")).getCollection(Vault.INSTANCE.getValue("query.mongo.collection"));
        }
        throw new RuntimeException("Query MongoDB database and collection have not been configured properly");
    }

    static Query documentToQuery(Document document)
    {
        Query query = new Query();
        query.id = document.getString("id");
        query.name = document.getString("name");
        query.groupId = document.getString("groupId");
        query.artifactId = document.getString("artifactId");
        query.versionId = document.getString("versionId");
        query.mapping = document.getString("mapping");
        query.runtime = document.getString("runtime");
        query.content = document.getString("content");
        query.owner = document.getString("owner");
        return query;
    }

    static Document queryToDocument(Query query) throws JsonProcessingException
    {
        return Document.parse(objectMapper.writeValueAsString(query));
    }

    public List<Query> getQueries(String search, Integer limit, boolean showCurrentUserQueriesOnly, String currentUser)
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
        return LazyIterate.collect(this.getQueryCollection()
            .find(filters.isEmpty() ? EMPTY_FILTER : Filters.and(filters))
            .projection(Projections.include("id", "name", "versionId", "groupId", "artifactId", "owner"))
            .limit(limit == null ? 0 : limit), QueryStoreManager::documentToQuery).toList();
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
        // Force the current user as owner regardless of user input
        query.owner = currentUser;

        List<Query> matchingQueries = LazyIterate.collect(this.getQueryCollection().find(Filters.eq("id", query.id)), QueryStoreManager::documentToQuery).toList();
        if (matchingQueries.size() >= 1)
        {
            throw new ApplicationQueryException("Query with ID '" + query.id + "'", Response.Status.BAD_REQUEST);
        }
        this.getQueryCollection().insertOne(queryToDocument(query));
        return query;
    }

    public Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException
    {
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
        return query;
    }

    public void deleteQuery(String queryId, String currentUser)
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
    }
}
