// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.jdbc;

import org.finos.legend.engine.postgres.PostgresServer;
import org.finos.legend.engine.postgres.handler.jdbc.JDBCSessionFactory;
import org.junit.Assert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class PostgresTestClient
{

    public static void main(String[] args) throws Exception
    {


        //jdbc:postgresql://localhost:5432/postgres"
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:9998/postgres", "dummy", "dummy");
        //Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");

        PreparedStatement statement = connection.prepareStatement("select * from  public.demo where name = ?");
        statement.setObject(1, "bob");
        ResultSet resultSet = statement.executeQuery();


    }
}

