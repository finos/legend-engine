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

package org.finos.legend.engine.plan.execution.stores.relational.connection.memsql.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class TestMemSQLContainerWrapper
{
    private MemSQLContainerWrapper memSQLContainerWrapper;

    private void startMemSQLContainer()
    {
        assumeTrue("Cannot start MemSQL Container, skipping test.", DockerClientFactory.instance().isDockerAvailable());
        this.memSQLContainerWrapper = MemSQLContainerWrapper.build("SINGLESTORE_INTEGRATION_LICENSE_KEY");
        this.memSQLContainerWrapper.start();
    }

    @Before
    public void setup()
    {
        this.startMemSQLContainer();
    }

    @After
    public void cleanup()
    {
        if (this.memSQLContainerWrapper != null)
        {
            this.memSQLContainerWrapper.stop();
        }
    }

    @Test
    public void testDirectJDBCConnection()
    {
        try (
                Connection connection = this.memSQLContainerWrapper.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select current_user();"))
        {
            resultSet.next();
            String currentUser = resultSet.getString(1);
            assertEquals("root@%", currentUser);
        }
        catch (Exception e)
        {
            fail("Unable to connect to MemSQL test container!");
        }
    }
}
