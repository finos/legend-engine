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
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfContent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestSnowflakeM2MUdfStageDeployer
{
    private final Path jarTarget = Paths.get(SnowflakeM2MUdfStageDeployer.TEMP_DIR, SnowflakeM2MUdfStageDeployer.EXECUTION_JAR_FILENAME);
    private final Path planTarget = Paths.get(SnowflakeM2MUdfStageDeployer.TEMP_DIR, SnowflakeM2MUdfStageDeployer.EXECUTION_PLAN_FILENAME);

    @After
    public void cleanup() throws Exception
    {
        Files.deleteIfExists(jarTarget);
        Files.deleteIfExists(planTarget);
    }


    @Test
    public void testDownloadFileFetchesFromProvidedUrl() throws Exception
    {
        // Simulate "area" with a local file served over a file:// URL, keyed by engine version.
        File source = File.createTempFile("source-jar", ".jar");
        byte[] content = "jar-bytes-for-version-1.2.3".getBytes(StandardCharsets.UTF_8);
        Files.write(source.toPath(), content);

        Function<String, String> provider = version ->
        {
            Assert.assertEquals("1.2.3", version);
            return source.toURI().toString();
        };

        SnowflakeM2MUdfStageDeployer deployer = new SnowflakeM2MUdfStageDeployer(provider);
        Path downloaded = deployer.downloadFile("1.2.3");

        Assert.assertEquals(jarTarget, downloaded);
        Assert.assertTrue(Files.exists(jarTarget));
        Assert.assertArrayEquals(content, Files.readAllBytes(jarTarget));

        Files.deleteIfExists(source.toPath());
    }

    @Test
    public void testWriteStringToFile() throws Exception
    {
        String plan = "{\"_type\":\"simple\",\"rootExecutionNode\":{}}";
        SnowflakeM2MUdfStageDeployer deployer = new SnowflakeM2MUdfStageDeployer(v -> null);

        Path written = deployer.writeStringToFile(plan);

        Assert.assertEquals(planTarget, written);
        Assert.assertTrue(Files.exists(planTarget));
        Assert.assertEquals(plan, new String(Files.readAllBytes(planTarget), StandardCharsets.UTF_8));
    }

    @Test
    public void testDeleteFile() throws Exception
    {
        Files.write(jarTarget, "x".getBytes(StandardCharsets.UTF_8));
        Files.write(planTarget, "y".getBytes(StandardCharsets.UTF_8));

        SnowflakeM2MUdfStageDeployer deployer = new SnowflakeM2MUdfStageDeployer(v -> null);
        deployer.deleteFile(SnowflakeM2MUdfStageDeployer.EXECUTION_JAR_FILENAME);
        deployer.deleteFile(SnowflakeM2MUdfStageDeployer.EXECUTION_PLAN_FILENAME);

        Assert.assertFalse(Files.exists(jarTarget));
        Assert.assertFalse(Files.exists(planTarget));
    }

    @Test
    public void testPutJarAndExecutionPlanToStage() throws Exception
    {
        File source = File.createTempFile("source-jar", ".jar");
        Files.write(source.toPath(), "jar".getBytes(StandardCharsets.UTF_8));

        List<String> executedSql = new ArrayList<>();
        Connection connection = recordingConnection(executedSql);

        List<String> sqlCommands = new ArrayList<>();
        sqlCommands.add("PUT jar");
        sqlCommands.add("PUT plan");
        sqlCommands.add("CREATE FUNCTION foo");

        SnowflakeM2MUdfContent content = new SnowflakeM2MUdfContent();
        content.engineVersion = "1.2.3";
        content.executionPlan = "{\"plan\":true}";
        content.sqlCommands = sqlCommands;

        SnowflakeM2MUdfStageDeployer deployer = new SnowflakeM2MUdfStageDeployer(v -> source.toURI().toString());
        deployer.putJarAndExecutionPlanToStage(connection, content);

        Assert.assertEquals(sqlCommands, executedSql);

        // Temp files are cleaned up in the finally block.
        Assert.assertFalse(Files.exists(jarTarget));
        Assert.assertFalse(Files.exists(planTarget));

        Files.deleteIfExists(source.toPath());
    }

    /**
     * Dependency-free JDBC stub (dynamic proxy) that records every SQL string
     * passed to {@link Statement#execute(String)}.
     */
    private static Connection recordingConnection(List<String> executedSql)
    {
        InvocationHandler statementHandler = (proxy, method, args) ->
        {
            if ("execute".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof String)
            {
                executedSql.add((String) args[0]);
                return false;
            }
            return defaultValue(method.getReturnType());
        };
        Statement statement = (Statement) Proxy.newProxyInstance(
                TestSnowflakeM2MUdfStageDeployer.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                statementHandler);

        InvocationHandler connectionHandler = (proxy, method, args) ->
        {
            if ("createStatement".equals(method.getName()))
            {
                return statement;
            }
            return defaultValue(method.getReturnType());
        };
        return (Connection) Proxy.newProxyInstance(
                TestSnowflakeM2MUdfStageDeployer.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connectionHandler);
    }

    private static Object defaultValue(Class<?> returnType) throws SQLException
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == void.class)
        {
            return null;
        }
        return 0;
    }
}

