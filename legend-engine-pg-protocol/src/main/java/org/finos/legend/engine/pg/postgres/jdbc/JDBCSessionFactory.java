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

package org.finos.legend.engine.pg.postgres.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.finos.legend.engine.Session;
import org.finos.legend.engine.SessionHandler;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.jetbrains.annotations.Nullable;

public class JDBCSessionFactory implements SessionsFactory
{

  private Connection connection;
  private final String connectionString;
  private final String user;
  private final String password;

  public JDBCSessionFactory(String connectionString, String user, String password)
  {
    this.connectionString = connectionString;
    this.user = user;
    this.password = password;
  }

  @Override
  public Session createSession(@Nullable String defaultSchema, User authenticatedUser)
  {
    return new Session(new SessionHandler()
    {
      @Override
      public PreparedStatement prepareStatement(String query) throws SQLException
      {
        return getConnection().prepareStatement(query);
      }

      @Override
      public Statement createStatement() throws SQLException
      {
        return getConnection().createStatement();
      }
    });
  }


  private Connection getConnection() throws SQLException
  {
    if (connection == null)
    {
      this.connection = DriverManager.getConnection(connectionString, user, password);
    }
    return connection;
  }

  public static void main(String[] args)
  {
    JDBCSessionFactory sessionFactory = new JDBCSessionFactory(
        "jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
    Session session = sessionFactory.createSession(null, null);
    session.executeSimple("select * from public.demo");
  }
}   
