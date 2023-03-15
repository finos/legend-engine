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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MongoDBExecutor
{

    private final CredentialProviderProvider credentialProviderProvider;
    private final static int DEFAULT_BATCH_SIZE = 10;

    public MongoDBExecutor(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public List<Document> getPipelineFromDbCommand(String dbCommand) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(dbCommand);
        ArrayNode pipelineString = (ArrayNode) actualObj.get("pipeline");
        List<Document> pipeline = new ArrayList<>();

        pipelineString.forEach(node ->
        {
            String nodeStr = node.toString();
            pipeline.add(Document.parse(nodeStr));
        });

        return pipeline;
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

            // Loading with no iterator
            Document dbResult = mongoDatabase.runCommand(bsonCmd);

            // using Collection and Iterator
            List<String> result = new ArrayList<>();
            List<Document> pipelineDoc = getPipelineFromDbCommand(dbCommand);
            try (MongoCursor<Document> cursor = mongoDatabase.getCollection("person").
                    aggregate(pipelineDoc)
                    .batchSize(DEFAULT_BATCH_SIZE).iterator())
            {
                while (cursor.hasNext())
                {
                    result.add(cursor.next().toJson());
                }
            }

            // return results as InputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(result);
            oos.flush();
            byte[] bytes = baos.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(bytes);

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

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder().
                with(systemPropertiesCredentialVault).with(propertiesFileCredentialVault).build();


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
