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

package org.finos.legend.engine.plan.execution.stores.mongodb.auth;

import com.mongodb.ServerAddress;
import org.finos.legend.connection.legacy.ConnectionSpecification;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBDatasourceSpecification;

import java.util.List;
import java.util.stream.Collectors;

public class MongoDBConnectionSpecification extends ConnectionSpecification
{

    private final MongoDBDatasourceSpecification mongoDBDataSourceSpec;

    public MongoDBConnectionSpecification(MongoDBDatasourceSpecification mongoDBDataSourceSpec)
    {
        this.mongoDBDataSourceSpec = mongoDBDataSourceSpec;
    }

    public List<ServerAddress> getServerAddresses()
    {
        return this.mongoDBDataSourceSpec.serverURLs.stream().map(x -> new ServerAddress(x.baseUrl, (int)(long)x.port)).collect(Collectors.toList());
    }

    public String getDatabaseName()
    {
        return mongoDBDataSourceSpec.databaseName;
    }
}
