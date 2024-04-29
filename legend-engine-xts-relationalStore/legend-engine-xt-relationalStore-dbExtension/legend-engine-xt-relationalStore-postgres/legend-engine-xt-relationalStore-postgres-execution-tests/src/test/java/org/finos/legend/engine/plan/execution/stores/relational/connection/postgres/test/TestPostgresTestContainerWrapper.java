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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assume.assumeTrue;

public class TestPostgresTestContainerWrapper
{
    private static PostgresTestContainerWrapper postgresTestContainerWrapper;

    @BeforeClass
    public static void setupClass() throws Exception
    {
        try
        {
            postgresTestContainerWrapper = PostgresTestContainerWrapper.build();
            postgresTestContainerWrapper.postgreSQLContainer.start();
        }
        catch (Exception e)
        {
            assumeTrue("Cannot start PostgreSQLContainer", false);
        }
    }

    @AfterClass
    public static void shutdownClass()
    {
        if (postgresTestContainerWrapper != null)
        {
            postgresTestContainerWrapper.postgreSQLContainer.stop();
        }
    }

    @Test
    public void testDbConnection() throws Exception
    {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(
                postgresTestContainerWrapper.getJdbcUrl(),
                postgresTestContainerWrapper.getUser(),
                postgresTestContainerWrapper.getPassword()
        );

        Statement stmt = conn.createStatement();
        MutableList<String> setupSqls = Lists.mutable.with("drop table if exists PERSON;",
                "create table PERSON(fullName VARCHAR(200));",
                "insert into PERSON (fullName) values ('Mickey Mouse');");
        for (String sql : setupSqls)
        {
            stmt.executeUpdate(sql);
        }
        stmt.close();
        conn.close();
    }

}
