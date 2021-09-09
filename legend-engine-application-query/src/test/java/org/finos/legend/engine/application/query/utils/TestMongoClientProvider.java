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

package org.finos.legend.engine.application.query.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import java.net.InetSocketAddress;

public class TestMongoClientProvider
{
    private final MongoServer mongoServer;
    public final MongoClient mongoClient;

    public TestMongoClientProvider()
    {
        this.mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress socketAddress = mongoServer.bind();
        this.mongoClient = MongoClients.create("mongodb://" + socketAddress.getHostName() + ":" + socketAddress.getPort());
    }

    public void cleanUp()
    {
        this.mongoClient.close();
        this.mongoServer.shutdown();
    }
}
