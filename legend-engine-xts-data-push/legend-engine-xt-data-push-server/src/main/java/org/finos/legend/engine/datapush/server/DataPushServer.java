// Copyright 2023 Goldman Sachs
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

import io.dropwizard.setup.Bootstrap;
import org.finos.legend.engine.datapush.server.config.DataPushServerConfiguration;
import org.finos.legend.engine.server.support.server.config.BaseServerConfiguration;
import org.finos.legend.server.pac4j.LegendPac4jBundle;

public class DataPushServer extends BaseDataPushServer
{
    @Override
    protected ServerPlatformInfo newServerPlatformInfo()
    {
        return new ServerPlatformInfo(null, null, null);
    }

    @Override
    public void initialize(Bootstrap<DataPushServerConfiguration> bootstrap)
    {
        super.initialize(bootstrap);

//        bootstrap.addBundle(new ConnectionFactoryBundle<>(DataPushServerConfiguration::getConnectionFactoryConfiguration));
        bootstrap.addBundle(new LegendPac4jBundle<>(BaseServerConfiguration::getPac4jConfiguration));
    }

    public static void main(String... args) throws Exception
    {
        new DataPushServer().run(args);
    }
}
