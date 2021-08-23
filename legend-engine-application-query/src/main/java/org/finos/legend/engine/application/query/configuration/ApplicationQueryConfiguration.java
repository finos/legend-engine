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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.finos.legend.engine.shared.core.vault.Vault;

public class ApplicationQueryConfiguration
{
    public static MongoClient getMongoClient()
    {
        String mongoConnectionString = Vault.INSTANCE.hasValue("query.mongo.connectionString") ? Vault.INSTANCE.getValue("query.mongo.connectionString") : null;
        if (mongoConnectionString != null)
        {
            return MongoClients.create(mongoConnectionString);
        }
        return MongoClients.create();
    }
}
