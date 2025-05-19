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

package org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SnowflakeM2MUdfContent extends FunctionActivatorDeploymentContent
{
    @Deprecated
    public MutableList<String> sqlExpressions = Lists.mutable.empty();
    public String createStatement;
    public String grantStatement;
    public String udfName;
    public String description;
    public String creationTime;
    public String permissionScope;
    public String deploymentSchema;
    public MutableList<String> usedTables = Lists.mutable.empty();

    public SnowflakeM2MUdfContent()
    {
        //Empty constructor for Jackson
    }

    public SnowflakeM2MUdfContent(String udfName, MutableList<String> sqlExpressions, AlloySDLC sdlc)
    {
        this.udfName = udfName;
        this.sqlExpressions = sqlExpressions;
        this.creationTime = convertToValidDate(new Date());

    }

    public SnowflakeM2MUdfContent(String udfName, MutableList<String> sqlExpressions, String description, String ownership, AlloySDLC sdlc)
    {
        this(udfName, sqlExpressions, sdlc);
        this.description = description;
        this.ownership = ownership;

    }

    public SnowflakeM2MUdfContent(String udfName, String createStatement, String grantStatement, String permissionScope, String description, String deploymentSchema, String ownership, MutableList<String> usedTables)
    {
        this.udfName = udfName;
        this.createStatement = createStatement;
        this.grantStatement = grantStatement;
        this.permissionScope = permissionScope;
        this.description = description;
        this.deploymentSchema = deploymentSchema;
        this.ownership = ownership;
        this.creationTime = convertToValidDate(new Date());;
        this.usedTables = usedTables;
    }


    public static String convertToValidDate(Date date)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    public void addGrantStatement(String grant)
    {
        this.sqlExpressions.add(grant);
    }

}

