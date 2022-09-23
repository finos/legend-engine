// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.jdbc;

import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class JdbcHelperTest
{
    public static final String TEST_SCHEMA = "TEST_SCHEMA";
    private static final String TEST_DATABASE = "TEST_DB";
    private static final String H2_JDBC_URL = "jdbc:h2:mem:" + TEST_DATABASE +
        ";DATABASE_TO_UPPER=false;mode=mysql;LOCK_TIMEOUT=10000";
    private static final String H2_USER_NAME = "sa";
    private static final String H2_PASSWORD = "";
    private static final String H2_CREATE_TEST_SCHEMA_SQL = "CREATE SCHEMA IF NOT EXISTS {TEST_SCHEMA_NAME} AUTHORIZATION {USER_NAME}"
        .replace("{TEST_SCHEMA_NAME}", TEST_SCHEMA)
        .replace("{USER_NAME}", H2_USER_NAME);

    @Test
    void prepare() throws Exception
    {
        JdbcHelper sink = JdbcHelper.of(H2Sink.createConnection(H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL));

        // Create table example
        List<String> list2 = new ArrayList<>();
        list2.add("CREATE TABLE PERSON(ID INT PRIMARY KEY, NAME VARCHAR(255), BIRTH DATETIME)");
        list2.add("INSERT INTO PERSON VALUES (1, 'A', '2020-01-01 00:00:00')");
        list2.add("INSERT INTO PERSON VALUES (2, 'B', '2021-01-01 00:00:00')");
        sink.executeStatements(list2);
    }
}
