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

package org.finos.legend.engine.server.test.shared;

import org.finos.legend.engine.server.Server;

public class ServersState
{
    private final Server server;
    private final TestMetaDataServer metadataServer;
    private final org.h2.tools.Server H2Server;

    public ServersState(Server server, TestMetaDataServer metadataServer, org.h2.tools.Server h2Server)
    {
        this.server = server;
        this.metadataServer = metadataServer;
        this.H2Server = h2Server;
    }

    public void shutDown()
    {
        try
        {
            this.server.shutDown();
            this.metadataServer.shutDown();
            if (this.H2Server != null)
            {
                this.H2Server.shutdown();
                this.H2Server.stop();
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
