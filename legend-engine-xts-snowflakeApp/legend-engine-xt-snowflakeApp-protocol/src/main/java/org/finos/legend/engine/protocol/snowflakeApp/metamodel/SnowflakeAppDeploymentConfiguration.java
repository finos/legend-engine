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

package org.finos.legend.engine.protocol.snowflakeApp.metamodel;

import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;

public class SnowflakeAppDeploymentConfiguration extends DeploymentConfiguration
{
    public ConnectionPointer activationConnection;

//    public String applicationName;

    public SnowflakeAppDeploymentConfiguration()
    {
    }

    public SnowflakeAppDeploymentConfiguration(ConnectionPointer cp)
    {
        activationConnection = cp;
    }
}
