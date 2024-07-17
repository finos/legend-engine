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

package org.finos.legend.engine.language.snowflakeApp.deployment;

public class SnowflakeGrantInfo
{
    public String privilege;
    public String objectType;
    public String objectName;
    public String granteeRole;
    public String grantee;

    public SnowflakeGrantInfo(String privilege, String objectType, String objectName, String granteeRole, String grantee)
    {
        this.privilege = privilege;
        this.objectType = objectType;
        this.objectName = objectName;
        this.granteeRole = granteeRole;
        this.grantee = grantee;
    }
}
