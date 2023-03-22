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
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.PlatformCredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.stores.mongodb.auth.MongoDBConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.mongodb.auth.MongoDBStoreConnectionProvider;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

public class MongoDBExecutor
{

    private static final int DEFAULT_BATCH_SIZE = 10;
    private final CredentialProviderProvider credentialProviderProvider;

    public MongoDBExecutor(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public InputStreamResult executeMongoDBQuery(String dbCommand, MongoDBConnection dbConnection)
    {
        // Connection has datasource details & authentication.
        try
        {
            MongoDBStoreConnectionProvider mongoDBConnectionProvider = getMongoDBConnectionProvider();
            MongoDBConnectionSpecification mongoDBConnectionSpec = new MongoDBConnectionSpecification(dbConnection.dataSourceSpecification);
            Identity serviceIdentity = new Identity("serviceAccount", new AnonymousCredential());
            MongoClient mongoClient = mongoDBConnectionProvider.makeConnection(mongoDBConnectionSpec, dbConnection.authenticationSpecification, serviceIdentity);
            MongoDatabase mongoDatabase = mongoClient.getDatabase(dbConnection.dataSourceSpecification.databaseName);

            Document bsonCmd = Document.parse(dbCommand);


            // Loading with no iterator - TODO: Fix this up
            JSONArray jsonArray = new JSONArray();

            // using Collection and Iterator
            try (MongoCursor<Document> cursor = mongoDatabase.getCollection(bsonCmd.getString("aggregate"))
                    .aggregate(bsonCmd.getList("pipeline", Document.class))
                    .batchSize(DEFAULT_BATCH_SIZE).iterator())
            {
                while (cursor.hasNext())
                {
                    JSONObject jsonResult = new JSONObject(cursor.next().toJson());
                    jsonArray.put(jsonResult);
                    System.out.println("the mongo results are:");
                    System.out.println(jsonResult);
                }
            }

            InputStream inputStream = new ByteArrayInputStream(jsonArray.toString().getBytes());

            return new InputStreamResult(inputStream);

        }
        catch (Exception e)
        {
            throw new RuntimeException("error streaming MongoDB Results", e);
        }
    }

    private MongoDBStoreConnectionProvider getMongoDBConnectionProvider()
    {
        Properties properties = new Properties();
        properties.put("passwordRef1", "");
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);
        SystemPropertiesCredentialVault systemPropertiesCredentialVault = new SystemPropertiesCredentialVault();

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(systemPropertiesCredentialVault)
                .with(propertiesFileCredentialVault).build();


        // Setup CV Provider with just platform CV provider
        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                //.with(awsSecretsManagerVault)
                .build();

        // Looks like the link between the CV provider and the ProviderProvider
        IntermediationRuleProvider intermediationRuleProvider = IntermediationRuleProvider.builder()
                .with(new UserPasswordFromVaultRule(credentialVaultProvider))
                .build();

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(new UserPasswordCredentialProvider())
                .with(intermediationRuleProvider)
                .build();

        MongoDBStoreConnectionProvider connectionProvider = new MongoDBStoreConnectionProvider(credentialProviderProvider);
        return connectionProvider;
    }

}
