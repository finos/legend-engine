//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.*;

public class TestSnowflakeManager
{
    @Test
    public void testCreateLocalDataSourceSpecification_systemPropertyNotConfigured()
    {
        try
        {
            SnowflakeManager snowflakeManager = new SnowflakeManager();
            snowflakeManager.getLocalDataSourceSpecification();
            fail("Failed to throw exception");
        }
        catch (UnsupportedOperationException e)
        {
            assertEquals("Cannot create a local Snowflake datasource specification. System property snowflakeLocalDSSpecFilePath has not been set.", e.getMessage());
        }
    }

    @Test
    public void testCreateLocalDataSourceSpecification_localFileDoesNotExist()
    {
        System.setProperty("snowflakeLocalDSSpecFilePath", "/doesnotexist");
        try
        {
            this.testCreateLocalDataSourceSpecification_localFileDoesNotExistImpl();
        }
        finally
        {
            System.clearProperty("snowflakeLocalDSSpecFilePath");
        }
    }

    private void testCreateLocalDataSourceSpecification_localFileDoesNotExistImpl()
    {
        try
        {
            SnowflakeManager snowflakeManager = new SnowflakeManager();
            snowflakeManager.getLocalDataSourceSpecification();
            fail("Failed to throw exception");
        }
        catch (UnsupportedOperationException e)
        {
            String message = e.getMessage();
            assertTrue(message.startsWith("Cannot create a local Snowflake datasource specification. Failed to read file /doesnotexist"));
        }
    }

    @Test
    public void testCreateLocalDataSourceSpecification_localFileDoesExist() throws IOException
    {
        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        snowflakeLocalDataSourceSpecFileProperties.setProperty("accountName", "accountNameValue");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("region", "us-east-2");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("warehouse", "warehouse1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("databaseName", "database1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("cloudType", "aws");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("role", "role1");

        snowflakeLocalDataSourceSpecFileProperties.setProperty("privateKeyVaultReference", "ref1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("passphraseVaultReference", "phrase1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("publicUserName", "user1");
        ;

        Path tempDirectory = Files.createTempDirectory("temp");
        Path localProperties = tempDirectory.resolve("local.properties");
        snowflakeLocalDataSourceSpecFileProperties.store(new FileOutputStream(new File(localProperties.toAbsolutePath().toString())), "");

        System.setProperty("snowflakeLocalDSSpecFilePath", localProperties.toAbsolutePath().toString());
        try
        {
            this.testCreateLocalDataSourceSpecification_localFileDoesExistImpl();
        }
        finally
        {
            System.clearProperty("snowflakeLocalDSSpecFilePath");
        }
    }

    private void testCreateLocalDataSourceSpecification_localFileDoesExistImpl()
    {
        SnowflakeManager snowflakeManager = new SnowflakeManager();
        DataSourceSpecification localDataSourceSpecification = snowflakeManager.getLocalDataSourceSpecification();
        assertTrue(localDataSourceSpecification instanceof SnowflakeDataSourceSpecification);
    }
}
