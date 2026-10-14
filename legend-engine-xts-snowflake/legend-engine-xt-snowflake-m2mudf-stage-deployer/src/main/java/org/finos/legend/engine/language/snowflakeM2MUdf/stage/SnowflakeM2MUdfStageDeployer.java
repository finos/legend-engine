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

package org.finos.legend.engine.language.snowflakeM2MUdf.stage;

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.M2MUdfContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SnowflakeM2MUdfStageDeployer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeM2MUdfStageDeployer.class);

    public static final String EXECUTION_JAR_FILENAME  = "legend-engine-xt-snowflake-m2mudf-plan-executor.jar";
    public static final String EXECUTION_PLAN_FILENAME = "executionPlan.json";
    public static final String TEMP_DIR                = System.getProperty("java.io.tmpdir");

    private final Function<String, String> engineDownloadUrlProvider;

    public SnowflakeM2MUdfStageDeployer(Function<String, String> engineDownloadUrlProvider)
    {
        this.engineDownloadUrlProvider = engineDownloadUrlProvider;
    }

    public void putJarAndExecutionPlanToStage(Connection jdbcConnection, M2MUdfContent content) throws SQLException
    {
        try
        {
            downloadFile(content.getEngineVersion());
            writeStringToFile(content.getExecutionPlan());
            int i = 0;
            for (String sql : content.getSqlCommands())
            {
                try (Statement statement = jdbcConnection.createStatement())
                {
                    statement.execute(sql);
                }
                i++;
                LOGGER.info("{} sql execution successful", i);
            }
        }
        finally
        {
            deleteFile(EXECUTION_JAR_FILENAME);
            deleteFile(EXECUTION_PLAN_FILENAME);
        }
    }

    protected Path downloadFile(String engineVersion)
    {
        String url = this.engineDownloadUrlProvider.apply(engineVersion);
        Path target = Paths.get(TEMP_DIR, EXECUTION_JAR_FILENAME);
        try (InputStream in = new URL(url).openStream())
        {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
        catch (Exception e)
        {
            LOGGER.info("Not able to download the plan-executor jar for engine version {}", engineVersion);
            throw new RuntimeException(e);
        }
    }

    protected Path writeStringToFile(String executionPlanJson)
    {
        Path target = Paths.get(TEMP_DIR, EXECUTION_PLAN_FILENAME);
        try
        {
            Files.write(target, executionPlanJson.getBytes());
            return target;
        }
        catch (Exception e)
        {
            LOGGER.info("Not able to materialise execution plan JSON to a temp file");
            throw new RuntimeException(e);
        }
    }

    protected void deleteFile(String fileName)
    {
        try
        {
            Files.deleteIfExists(Paths.get(TEMP_DIR, fileName));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}

