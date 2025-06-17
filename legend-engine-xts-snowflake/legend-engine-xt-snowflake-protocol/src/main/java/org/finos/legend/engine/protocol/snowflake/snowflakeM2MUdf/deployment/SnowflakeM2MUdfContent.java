// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment;

import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.deployment.SnowflakeAppContent;

import java.util.Date;
import java.util.List;

public class SnowflakeM2MUdfContent extends FunctionActivatorDeploymentContent
{
    public String executionPlan;
    public List<String> sqlCommands;
    public String engineVersion;
    public String udfName;
    public String description;
    public String creationTime;
    public String databaseName;
    public String deploymentSchema;
    public String deploymentStage;

    public SnowflakeM2MUdfContent()
    {
        //Empty constructor for Jackson
    }

    public SnowflakeM2MUdfContent(String udfName, String executionPlan, List<String> sqlCommands, String engineVersion, String description, String databaseName, String deploymentSchema, String deploymentStage, String ownership)
    {
        this.udfName = udfName;
        this.executionPlan = executionPlan;
        this.sqlCommands = sqlCommands;
        this.engineVersion = engineVersion;
        this.description = description;
        this.databaseName = databaseName;
        this.deploymentSchema = deploymentSchema;
        this.deploymentStage = deploymentStage;
        this.ownership = ownership;
        this.creationTime = convertToValidDate(new Date());;
    }

    public static String convertToValidDate(Date date)
    {
        return SnowflakeAppContent.convertToValidDate(date);
    }

}

