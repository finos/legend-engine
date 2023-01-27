// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static utils.InMemoryMongoDbServer.DEFAULT_MONGO_HOSTNAME;
import static utils.InMemoryMongoDbServer.DEFAULT_MONGO_PORT;

public class MongoDbClient
{
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("MongoDbClient");

    private final MongoClient mongoClient;
    public static final String DEFAULT_DATABASE_NAME = "my_database";
    private static final String CONNECTION_STRING_TEMPLATE = "mongodb://%s:%s";

    public MongoDbClient()
    {
        this("mongodb://" + DEFAULT_MONGO_HOSTNAME + ":" + DEFAULT_MONGO_PORT);
    }

    public MongoDbClient(int port)
    {
        this(String.format(CONNECTION_STRING_TEMPLATE, DEFAULT_MONGO_HOSTNAME, port));
    }

    public MongoDbClient(String hostname, int port)
    {
        this(String.format(CONNECTION_STRING_TEMPLATE, hostname, port));
    }

    public MongoDbClient(String connectionString)
    {
        this.mongoClient = MongoClients.create(connectionString);
    }

    public MongoClient getMongoDBClient()
    {
        return this.mongoClient;
    }

    private String executeNativeQueryWithCursor(MongoDatabase database, String query) throws RuntimeException
    {
        List<String> res = new ArrayList<>();
        try
        {
            Document bsonCmd = Document.parse(query);

            // Execute the native query
            Document result = database.runCommand(bsonCmd);
            Document cursor = (Document) result.get("cursor");
            List<Document> docs = (List<Document>) cursor.get("firstBatch");


            docs.forEach(doc -> res.add(doc.toJson()));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to execute Mongo native query:\n" + query, e);
        }
        return "[" + String.join(",", res) + "]";

    }

    private MongoDatabase getDefaultDB()
    {
        return this.mongoClient.getDatabase(DEFAULT_DATABASE_NAME);
    }

    public String executeNativeQuery(String mongoQuery)
    {
        MongoDatabase database = this.getDefaultDB();

        return executeNativeQueryWithCursor(database, mongoQuery);
    }


    public void insertCollectionItemAsJsonString(String collectionName, String jsonString)
    {
        ArrayNode nodes;
        try
        {
            nodes = (ArrayNode) mapper.readTree(jsonString);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to parse input contents as JSON", e);
            throw new RuntimeException(e);
        }

        insertCollectionItemAsJsonNodes(collectionName, nodes);
    }

    private void insertCollectionItemAsJsonNodes(String collectionName, ArrayNode nodes)
    {
        MongoDatabase database = this.getMongoDBClient().getDatabase(DEFAULT_DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<Document> docs = new ArrayList<>();

        nodes.forEach(n ->
        {
            String json;
            try
            {
                json = mapper.writeValueAsString(n);
                docs.add(Document.parse(json));
            }
            catch (JsonProcessingException e)
            {
                LOGGER.error("Failed to parse item: ", e);
                throw new RuntimeException(e);
            }
        });

        collection.insertMany(docs);
    }

    public void shutDown()
    {
        this.mongoClient.close();
    }
}
