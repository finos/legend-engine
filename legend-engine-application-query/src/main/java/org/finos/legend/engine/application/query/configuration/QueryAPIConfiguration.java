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

package org.finos.legend.engine.application.query.configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.finos.legend.engine.shared.core.vault.Vault;

public class QueryAPIConfiguration
{
    public static MongoClient getMongoDBClient()
    {
        String mongoUrl = Vault.INSTANCE.hasValue("query.mongo.url") ? Vault.INSTANCE.getValue("query.mongo.url") : null;
        Integer mongoPort = Vault.INSTANCE.hasValue("query.mongo.port") ? Integer.parseInt(Vault.INSTANCE.getValue("query.mongo.port")) : null;

        String mongoUsername = Vault.INSTANCE.hasValue("query.mongo.username") ? Vault.INSTANCE.getValue("query.mongo.username") : null;
        String mongoPassword = Vault.INSTANCE.hasValue("query.mongo.password") ? Vault.INSTANCE.getValue("query.mongo.password") : null;
        String mongoCredentialSource = Vault.INSTANCE.hasValue("query.mongo.credentialSource") ? Vault.INSTANCE.getValue("query.mongo.credentialSource") : null;

        MongoCredential credential = null;
        if (mongoUsername != null && mongoPassword != null)
        {
            credential = MongoCredential.createPlainCredential(mongoUsername, mongoCredentialSource != null ? mongoCredentialSource : "$external", mongoPassword.toCharArray());
        }

        MongoClient mongoClient = new MongoClient();
        if (mongoUrl != null && mongoPort != null)
        {
            if (credential != null)
            {
                mongoClient = new MongoClient(new ServerAddress(mongoUrl, mongoPort), credential, new MongoClientOptions.Builder().build());
            }
            else
            {
                mongoClient = new MongoClient(mongoUrl, mongoPort);
            }
        }

        return mongoClient;
    }
}
