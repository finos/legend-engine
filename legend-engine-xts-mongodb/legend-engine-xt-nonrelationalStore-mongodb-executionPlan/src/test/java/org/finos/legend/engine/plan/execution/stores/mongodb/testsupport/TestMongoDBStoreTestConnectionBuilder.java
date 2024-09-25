// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.mongodb.testsupport;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.data.MongoDBCollectionData;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.data.MongoDBStoreEmbeddedData;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBDatasourceSpecification;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBURL;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TestMongoDBStoreTestConnectionBuilder
{

    public static final String LEGEND_DB = "legend_db";
    public static final String TEST_COLLECTION = "test_collection";

    private static MongoDBCollectionData getNameCollectionData()
    {
        MongoDBCollectionData nameCollectionData = new MongoDBCollectionData();
        nameCollectionData.collectionName = TEST_COLLECTION;
        String item1 = "{ \"name\": \"hugo\", \"firm\": \"abc1 corp\" }";
        String item2 = "{ \"name\": \"renu\", \"firm\": \"abc1 corp\" }";
        String item3 = "{ \"name\": \"theo\", \"firm\": \"abc2 corp\" }";
        nameCollectionData.documents = Lists.mutable.of(item1, item2, item3);
        return nameCollectionData;
    }

    @Test
    @Ignore
    public void testInMemoryMongoDB() throws IOException
    {
        MongoDBStoreTestConnectionFactory testConnectionFactory = new MongoDBStoreTestConnectionFactory();

        MongoDBConnection connection = new MongoDBConnection();

        MongoDBDatasourceSpecification dataSourceSpec = new MongoDBDatasourceSpecification();
        dataSourceSpec.databaseName = LEGEND_DB;
        MongoDBURL mongoDBURL = new MongoDBURL();
        mongoDBURL.baseUrl = "http://baseUrl"; // Not used for test connection creation.
        mongoDBURL.port = -99L;   // Not used for test connection creation.
        dataSourceSpec.serverURLs = Lists.mutable.of(mongoDBURL);
        connection.dataSourceSpecification = dataSourceSpec;

        AuthenticationSpecification authenticationSpec = new UserPasswordAuthenticationSpecification();
        connection.authenticationSpecification = authenticationSpec;
        connection.debug = true;

        MongoDBStoreEmbeddedData embeddedData = new MongoDBStoreEmbeddedData();
        embeddedData.databaseName = LEGEND_DB;
        embeddedData.testData = Lists.mutable.of(getNameCollectionData());

        Optional<Pair<Connection, List<Closeable>>> result = testConnectionFactory.tryBuildTestConnection(connection, Lists.mutable.of(embeddedData));
        if (result.isPresent())
        {
            MongoDBConnection conn = (MongoDBConnection) result.get().getOne();
            String hostName = conn.dataSourceSpecification.serverURLs.get(0).baseUrl;
            long hostPort = conn.dataSourceSpecification.serverURLs.get(0).port;
            MongoClient testDBClient = MongoClients.create("mongodb://" + hostName + ":" + Long.toString(hostPort));
            MongoDatabase mongoDatabase = testDBClient.getDatabase(LEGEND_DB);
            MongoCollection<Document> collection = mongoDatabase.getCollection(TEST_COLLECTION);
            Document getAll = new Document();
            FindIterable<Document> cursor = collection.find(getAll);
            try (MongoCursor<Document> iterator = cursor.cursor())
            {
                long count = 0;
                while (iterator.hasNext())
                {
                    String jsonString  = iterator.next().toString();
                    count++;
                }
                Assert.assertEquals("Should have three documents", 3L, count);
            }
            result.get().getTwo().get(0).close();
        }
        else
        {
            Assert.fail("Failed to create InMemory MongoDB instance");
        }


    }

}
