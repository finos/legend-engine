// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.mongodb.test.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.mongodb.test.MongoTestContainer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;

import java.util.Map;
import java.util.Optional;

public class MongoDBCommands
{

    public static final String DB_ROOT_USERNAME = "sa";
    public static final String DB_ROOT_PASSWORD = "sa";
    public static final Map<String, GenericContainer<MongoDBContainer>> CONTAINERS = Maps.mutable.empty();
    public static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final String DB_USER_DATABASE = "userDatabase";
    private static final String DB_USER_DB_PERSON_COLLECTION = "person";
    private static final int MONGO_PORT = 27017;
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBCommands.class);
    public static String START_SERVER_FUNCTION = "startMongoDBTestServer_String_1__URL_1_";
    public static String STOP_SERVER_FUNCTION = "stopMongoDBTestServer_String_1__Nil_0_";
    public static String REQUEST_SERVER_FUNCTION = "requestMongoDBTestServer_String_1__String_1__String_1_";

    public static Root_meta_pure_functions_io_http_URL startServer(String imageTag)
    {
        LOGGER.debug("Starting MongoDB docker image + " + imageTag);

        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("mongoDBUrl");
        GenericContainer<MongoDBContainer> container = CONTAINERS.computeIfAbsent(imageTag, MongoDBCommands::createContainer);
        url._host(container.getHost());
        url._port(container.getMappedPort(MONGO_PORT));
        url._path("/");

        return url;
    }

    public static void stopServer(String imageTag)
    {
        LOGGER.debug("Stopping MongoDB docker image + " + imageTag);
        Optional.ofNullable(CONTAINERS.remove(imageTag)).ifPresent(GenericContainer::stop);
    }

    public static String request(String imageTag, String json)
    {

        LOGGER.debug("Performing MongoDB request for image: " + imageTag);
        Integer mongoPort = getPortForRunningContainerImage(imageTag);
        String host = getHostForRunningContainerImage(imageTag);

        MongoClient adminClient = mongoClientForRootAdminWithStaticUserNamePassword(host, mongoPort);

        MongoDatabase userDatabase = adminClient.getDatabase(DB_USER_DATABASE);

        Document bsonCmd = Document.parse(json);
        Document document = userDatabase.runCommand(bsonCmd);

        String jsonResult = document.toJson();
        LOGGER.debug("MongoDB request result:\n" + jsonResult);

        return jsonResult;
    }

    private static Integer getPortForRunningContainerImage(String imageTag)
    {
        return CONTAINERS.get(imageTag).getMappedPort(MONGO_PORT);
    }

    private static String getHostForRunningContainerImage(String imageTag)
    {
        return CONTAINERS.get(imageTag).getHost();
    }

    private static GenericContainer<MongoDBContainer> createContainer(String imageTag)
    {
        long start = System.currentTimeMillis();
        MongoTestContainer mongoTestContainer = new MongoTestContainer();
        mongoTestContainer.run();
        GenericContainer<MongoDBContainer> container = mongoTestContainer.getContainer();

        LOGGER.info("MongoDB Test cluster for version {} running on {}.  Took {}ms to start.", imageTag, container.getHost(), System.currentTimeMillis() - start);
        return container;
    }

    private static MongoClient mongoClientForRootAdminWithStaticUserNamePassword(String hostName, Integer port)
    {
        String connectionURL = "mongodb://" + DB_ROOT_USERNAME + ":" +
                DB_ROOT_PASSWORD + "@" + hostName + ":" + port + "/admin";
        return MongoClients.create(connectionURL);
    }

}
