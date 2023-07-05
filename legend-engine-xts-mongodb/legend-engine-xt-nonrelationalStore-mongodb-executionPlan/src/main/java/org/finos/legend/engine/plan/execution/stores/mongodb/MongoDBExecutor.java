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

package org.finos.legend.engine.plan.execution.stores.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.stores.mongodb.auth.MongoDBConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.mongodb.auth.MongoDBStoreConnectionProvider;
import org.finos.legend.engine.plan.execution.stores.mongodb.result.MongoDBResult;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;

import java.util.function.Supplier;

public class MongoDBExecutor
{

    private static final int DEFAULT_BATCH_SIZE = 10;
    private final CredentialProviderProvider credentialProviderProvider;

    public MongoDBExecutor(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public MongoDBResult executeMongoDBQuery(String dbCommand, MongoDBConnection dbConnection, Identity serviceIdentity)
    {
        try
        {
            MongoDBStoreConnectionProvider mongoDBConnectionProvider = getMongoDBConnectionProvider();
            MongoDBConnectionSpecification mongoDBConnectionSpec = new MongoDBConnectionSpecification(dbConnection.dataSourceSpecification);
            Supplier<MongoClient> mongoClientSupplier = mongoDBConnectionProvider.makeConnection(mongoDBConnectionSpec, dbConnection.authenticationSpecification, serviceIdentity);
            MongoClient mongoClient = mongoClientSupplier.get();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(dbConnection.dataSourceSpecification.databaseName);

            Document bsonCmd = Document.parse(dbCommand);


            MongoDBResult mongoDBResult;
            try
            {
                MongoCursor<Document> cursor = mongoDatabase.getCollection(bsonCmd.getString("aggregate"))
                        .aggregate(bsonCmd.getList("pipeline", Document.class))
                        .batchSize(DEFAULT_BATCH_SIZE).iterator();
                mongoDBResult = new MongoDBResult(mongoClient, cursor);
            }
            catch (Exception e)
            {
                throw new EngineException(
                        String.format("Failed to execute query : %s, database: %s", dbCommand.toString(), dbConnection.dataSourceSpecification.databaseName),
                        e,
                        ExceptionCategory.SERVER_EXECUTION_ERROR);
            }
            return mongoDBResult;
        }
        catch (Exception e)
        {
            throw new EngineException(
                    String.format("Failed to execute query : %s, database: %s", dbCommand.toString(), dbConnection.dataSourceSpecification.databaseName),
                    e,
                    ExceptionCategory.SERVER_EXECUTION_ERROR);
        }
    }

    private MongoDBStoreConnectionProvider getMongoDBConnectionProvider()
    {
        MongoDBStoreConnectionProvider connectionProvider = new MongoDBStoreConnectionProvider(this.credentialProviderProvider);
        return connectionProvider;
    }

}
