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

package org.finos.legend.engine.datapush.server;

import io.dropwizard.setup.Environment;
import org.finos.legend.engine.datapush.server.config.DataPushServerConfiguration;
import org.finos.legend.engine.datapush.server.resources.DataPushResource;
import org.finos.legend.engine.server.support.server.BaseServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class BaseDataPushServer extends BaseServer<DataPushServerConfiguration>
{
    protected ServerInfo serverInfo;

    private static String getLocalHostName() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostName();
    }

    @Override
    protected void configureServerCore(DataPushServerConfiguration configuration, Environment environment)
    {
        environment.jersey().register(DataPushResource.class);
    }

    @Override
    protected void configureServerExtension(DataPushServerConfiguration configuration, Environment environment)
    {
        super.configureServerExtension(configuration, environment);
    }

    public static final class ServerInfo
    {
        private final String hostName;
        private final String initTime;
        private final ServerPlatformInfo serverPlatformInfo;

        private ServerInfo(String hostName, String initTime, ServerPlatformInfo serverPlatformInfo)
        {
            this.hostName = hostName;
            this.initTime = initTime;
            this.serverPlatformInfo = (serverPlatformInfo == null) ? new ServerPlatformInfo(null, null, null) : serverPlatformInfo;
        }

        public String getHostName()
        {
            return this.hostName;
        }

        public String getInitTime()
        {
            return this.initTime;
        }

        public ServerPlatformInfo getPlatform()
        {
            return this.serverPlatformInfo;
        }
    }
}
