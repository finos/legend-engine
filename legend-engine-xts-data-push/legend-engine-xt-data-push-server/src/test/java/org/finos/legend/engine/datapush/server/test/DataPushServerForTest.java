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

package org.finos.legend.engine.datapush.server.test;

import io.dropwizard.setup.Environment;
import org.finos.legend.engine.datapush.server.BaseDataPushServer;
import org.finos.legend.engine.datapush.server.config.DataPushServerConfiguration;
import org.finos.legend.engine.datapush.server.resources.DataPushTestResource;
import org.finos.legend.engine.server.support.server.BaseServer;

public class DataPushServerForTest extends BaseDataPushServer
{
    public DataPushServerForTest()
    {
    }

    @Override
    protected void configureServerExtension(DataPushServerConfiguration configuration, Environment environment)
    {
        environment.jersey().register(new DataPushTestResource());
    }

    @Override
    protected BaseServer.ServerPlatformInfo newServerPlatformInfo()
    {
        return new ServerPlatformInfo(null, null, null);
    }

    public static void main(String... args) throws Exception
    {
        new DataPushServerForTest().run(args);
    }
}