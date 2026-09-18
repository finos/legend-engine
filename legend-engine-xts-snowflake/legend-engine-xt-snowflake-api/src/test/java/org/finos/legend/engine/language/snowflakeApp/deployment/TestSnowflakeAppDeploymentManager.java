// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.snowflakeApp.deployment;

import org.junit.Assert;
import org.junit.Test;

public class TestSnowflakeAppDeploymentManager
{
    private static final String DEPLOY_STUB = "/schemas/LEGEND_NATIVE_APPS/user-function/%S()";

    @Test
    public void testEnrichDeploymentLocationAppendsStubWhenLocationIsResolved()
    {
        String enriched = SnowflakeAppDeploymentManager.enrichDeploymentLocation("https://app.region.privatelink.snowflakecomputing.com/region/account/data/databases/DB", DEPLOY_STUB, "MYAPP");
        Assert.assertEquals("https://app.region.privatelink.snowflakecomputing.com/region/account/data/databases/DB/schemas/LEGEND_NATIVE_APPS/user-function/MYAPP()", enriched);
    }

    @Test
    public void testEnrichDeploymentLocationLeavesEmptyLocationUnchanged()
    {
        Assert.assertEquals("", SnowflakeAppDeploymentManager.enrichDeploymentLocation("", DEPLOY_STUB, "MYAPP"));
    }

    @Test
    public void testEnrichDeploymentLocationLeavesNullLocationUnchanged()
    {
        Assert.assertNull(SnowflakeAppDeploymentManager.enrichDeploymentLocation(null, DEPLOY_STUB, "MYAPP"));
    }
}
