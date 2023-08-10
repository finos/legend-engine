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

package org.finos.legend.engine.shared.core.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;

public class DeploymentStateAndVersions
{
    public static DeploymentVersionInfo sdlc;
    public static String sdlcJSON = "{}";
    public static DeploymentMode DEPLOYMENT_MODE;
    public static String SERVER_PROTOCOL_VERSION = "v1";

    static
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource("legendExecutionVersion.json");
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                sdlcJSON = scanner.hasNext() ? scanner.next() : "{}";
                sdlc = new ObjectMapper().readValue(sdlcJSON, DeploymentVersionInfo.class);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
