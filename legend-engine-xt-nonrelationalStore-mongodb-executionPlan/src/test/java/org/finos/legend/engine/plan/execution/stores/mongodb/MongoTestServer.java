// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assume.assumeTrue;

public class MongoTestServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTestServer.class);
    public MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4.1"));

    public static final String DB_ROOT_USERNAME = "sa";
    public static final String DB_ROOT_PASSWORD = "sa";
    public static final String DB_AUTH_SOURCE = "admin"; // authentication DB, typically "admin" with mongo
    // mongodb.com/docs/manual/reference/connection-string/#mongodb-urioption-urioption.authSource

    private static final int MONGO_PORT = 27017;

    private static final String DB_USER_DATABASE = "userDatabase";
    private static final String DB_USER_DB_PERSON_COLLECTION = "person";

    public void run()
    {
        Assume.assumeTrue("A Docker client must be running for this integration test.", DockerClientFactory.instance().isDockerAvailable());
        this.startMongoDbContainer();

        MongoClient client = this.mongoClientForRootAdminWithStaticUserNamePassword();

        MongoDatabase userDatabase = client.getDatabase(DB_USER_DATABASE);
        userDatabase.createCollection(DB_USER_DB_PERSON_COLLECTION);

        String createPersonCollectionDocumentsCommand = loadFromFile("/mongoData/person.json");

        Document bsonCmd = Document.parse(createPersonCollectionDocumentsCommand);
        userDatabase.runCommand(bsonCmd);
    }

    public Integer getRunningPort()
    {
        return this.mongoDBContainer.getMappedPort(MONGO_PORT);
    }

    public boolean isRunning()
    {
        return this.mongoDBContainer.isRunning();
    }

    private void startMongoDbContainer()
    {
        try
        {
            List<String> list = new ArrayList<>();
            list.add("MONGO_INITDB_ROOT_USERNAME=" + DB_ROOT_USERNAME);
            list.add("MONGO_INITDB_ROOT_PASSWORD=" + DB_ROOT_PASSWORD);
            list.add("MONGO_INITDB_DATABASE=" + "admin");
            mongoDBContainer.setEnv(list);
            mongoDBContainer.withExposedPorts(MONGO_PORT);
            mongoDBContainer.start();

            LOGGER.info("Started MongoDB with port: " + this.mongoDBContainer.getMappedPort(MONGO_PORT));

        }
        catch (Throwable ex)
        {
            assumeTrue("Cannot start MongoDBContainer", false);
        }
    }

    private MongoClient mongoClientForRootAdminWithStaticUserNamePassword()
    {
        String connectionURL = "mongodb://" + DB_ROOT_USERNAME + ":" +
                DB_ROOT_PASSWORD + "@localhost:" + this.getRunningPort() + "/admin";
        MongoClient mongoClient = MongoClients.create(connectionURL);

        return mongoClient;
    }

    private String loadFromFile(String resourceName)
    {
        URL resource = MongoTestServer.class.getResource(resourceName);
        try
        {
            return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8).trim();
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}
