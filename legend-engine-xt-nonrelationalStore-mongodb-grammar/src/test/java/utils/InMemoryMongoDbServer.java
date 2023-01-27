// Copyright 2022 Goldman Sachs
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

package utils;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.slf4j.Logger;

public class InMemoryMongoDbServer
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(InMemoryMongoDbServer.class);
    protected static final String DEFAULT_MONGO_HOSTNAME = "localhost";
    protected static final int DEFAULT_MONGO_PORT = 27017;

    public static MongoServer startServer(int port)
    {
        LOGGER.debug("Starting setup of connection for local Mongo database server on port: " + port + " and address: " + DEFAULT_MONGO_HOSTNAME);
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind(DEFAULT_MONGO_HOSTNAME, port);
        LOGGER.debug("Completed setup for local Mongo database server on port: {}, and address: {} ", DEFAULT_MONGO_PORT, DEFAULT_MONGO_HOSTNAME);

        return mongoServer;
    }

    public static MongoServer startServer()
    {
        return startServer(DEFAULT_MONGO_PORT);
    }

    public static void main(String[] args)
    {
        try
        {
            startServer(Integer.parseInt(args[0]));
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException(e);
        }
    }

}