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

package org.finos.legend.engine.postgres.handler.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.finos.legend.engine.postgres.Session;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.postgres.auth.User;
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
      throws Exception
  {
    return new Session(new SessionHandler()
    {
      @Override
      public PostgresPreparedStatement prepareStatement(String query) throws SQLException
      {
        return new JDBCPostgresPreparedStatement(getConnection().prepareStatement(query));
      }

      @Override
      public PostgresStatement createStatement() throws SQLException
      {
        return new JDBCPostgresStatement(getConnection().createStatement());
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

  private static class JDBCPostgresStatement implements PostgresStatement
  {

    private Statement postgresStatement;

    public JDBCPostgresStatement(Statement postgresStatement)
    {
      this.postgresStatement = postgresStatement;
    }

    @Override
    public boolean execute(String query) throws Exception
    {
      return postgresStatement.execute(query);
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
      return new JDBCPostgresResultSet(postgresStatement.getResultSet());
    }
  }

  private static class JDBCPostgresPreparedStatement implements PostgresPreparedStatement
  {

    private PreparedStatement preparedStatement;

    public JDBCPostgresPreparedStatement(PreparedStatement preparedStatement)
    {
      this.preparedStatement = preparedStatement;
    }

    @Override
    public void setObject(int i, Object o) throws Exception
    {
      preparedStatement.setObject(i, o);
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
      return new JDBCPostgresResultSetMetaData(preparedStatement.getMetaData());
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws Exception
    {
      return preparedStatement.getParameterMetaData();
    }

    @Override
    public void close() throws Exception
    {
      preparedStatement.close();
    }

    @Override
    public void setMaxRows(int maxRows) throws Exception
    {
      preparedStatement.setMaxRows(maxRows);
    }

    @Override
    public boolean execute() throws Exception
    {
      return preparedStatement.execute();
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
      return new JDBCPostgresResultSet(preparedStatement.getResultSet());
    }
  }

  private static class JDBCPostgresResultSet implements PostgresResultSet
  {

    private ResultSet resultSet;

    public JDBCPostgresResultSet(ResultSet resultSet)
    {
      this.resultSet = resultSet;
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
      return new JDBCPostgresResultSetMetaData(resultSet.getMetaData());
    }

    @Override
    public Object getObject(int i) throws Exception
    {
      return resultSet.getObject(i);
    }

    @Override
    public boolean next() throws Exception
    {
      return resultSet.next();
    }
  }

  private static class JDBCPostgresResultSetMetaData implements PostgresResultSetMetaData
  {

    private ResultSetMetaData resultSetMetaData;


    public JDBCPostgresResultSetMetaData(ResultSetMetaData resultSetMetaData)
    {
      this.resultSetMetaData = resultSetMetaData;
    }

    @Override
    public int getColumnCount() throws Exception
    {
      return resultSetMetaData.getColumnCount();
    }

    @Override
    public String getColumnName(int i) throws Exception
    {
      return resultSetMetaData.getColumnName(i);
    }

    @Override
    public int getColumnType(int i) throws Exception
    {
      return resultSetMetaData.getColumnType(i);
    }

    @Override
    public int getScale(int i) throws Exception
    {
      return resultSetMetaData.getScale(i);
    }
  }


  public static void main(String[] args) throws Exception
  {
    JDBCSessionFactory sessionFactory = new JDBCSessionFactory(
        "jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
    Session session = sessionFactory.createSession(null, null);
    session.executeSimple("select * from public.demo");
  }
}   
