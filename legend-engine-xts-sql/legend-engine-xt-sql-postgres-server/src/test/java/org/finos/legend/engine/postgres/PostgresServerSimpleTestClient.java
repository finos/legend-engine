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

package org.finos.legend.engine.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class PostgresServerSimpleTestClient
{

    private static final Logger LOGGER = Logger.getLogger(PostgresServerSimpleTestClient.class.getName());


    public static void main(String[] args) throws Exception
    {

        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:9998/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {


            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next())
            {
                for (int i = 1; i < +columnCount; i++)
                {
                    System.out.println(resultSet.getMetaData().getColumnName(i) + " : " + resultSet.getObject(i));
                }
                System.out.println("\n");
            }

        }
    }


}