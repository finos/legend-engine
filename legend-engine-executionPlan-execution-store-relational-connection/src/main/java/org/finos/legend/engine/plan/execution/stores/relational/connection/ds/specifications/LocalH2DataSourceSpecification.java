// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.zaxxer.hikari.HikariDataSource;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

public class LocalH2DataSourceSpecification extends DataSourceSpecification
{
    static int port = DynamicPortGenerator.generatePort();
    static
    {
        try
        {
            // We can create only one instance of the server as the databases are anyway shared in the process (VM)
            // The important part is to have an empty name in 'this.buildDataSource("127.0.0.1", port, "", null)'
            // It ensure the database creation is 'private' to the connection.
            AlloyH2Server.startServer(port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public LocalH2DataSourceSpecification(LocalH2DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    public Connection getConnectionUsingIdentity(Identity identity, Optional<CredentialSupplier> databaseCredentialSupplier)
    {
        try
        {
            HikariDataSource dataSource = this.buildDataSource("127.0.0.1", port, "", identity);
            Connection connection = dataSource.getConnection();
            LocalH2DataSourceSpecificationKey _key = (LocalH2DataSourceSpecificationKey) this.datasourceKey;
            if (_key.getTestDataSetupSqls() != null && !_key.getTestDataSetupSqls().isEmpty())
            {
                for (String sql : _key.getTestDataSetupSqls())
                {
                    try (Statement statement = connection.createStatement())
                    {
                        statement.executeUpdate(sql);
                    }
                }
            }

            return new WrappedH2Connection(connection, dataSource);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static class WrappedH2Connection implements Connection
    {
        private Connection conn;
        private HikariDataSource dataSource;

        public WrappedH2Connection(Connection connection, HikariDataSource dataSource)
        {
            this.conn = connection;
            this.dataSource = dataSource;
        }

        @Override
        public Statement createStatement() throws SQLException
        {
            return this.conn.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException
        {
            return this.conn.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException
        {
            return this.conn.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException
        {
            return this.conn.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException
        {
            this.conn.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException
        {
            return this.conn.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException
        {
            this.conn.commit();
        }

        @Override
        public void rollback() throws SQLException
        {
            this.conn.rollback();
        }

        @Override
        public void close() throws SQLException
        {
            this.conn.close();
            this.dataSource.close();
        }

        @Override
        public boolean isClosed() throws SQLException
        {
            return this.conn.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException
        {
            return this.conn.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException
        {
            this.conn.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException
        {
            return this.conn.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException
        {
            this.conn.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException
        {
            return this.conn.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException
        {
            this.conn.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException
        {
            return this.conn.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException
        {
            return this.conn.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException
        {
            this.conn.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return this.conn.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
            return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException
        {
            return this.conn.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException
        {
            this.conn.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException
        {
            this.conn.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException
        {
            return this.conn.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException
        {
            return this.conn.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException
        {
            return this.conn.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException
        {
            this.conn.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException
        {
            this.conn.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return this.conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return this.conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
        {
            return this.conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
        {
            return this.conn.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
        {
            return this.conn.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
        {
            return this.conn.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException
        {
            return this.conn.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException
        {
            return this.conn.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException
        {
            return this.conn.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException
        {
            return this.conn.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException
        {
            return this.conn.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException
        {
            this.conn.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException
        {
            this.conn.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException
        {
            return this.conn.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException
        {
            return this.conn.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException
        {
            return this.conn.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException
        {
            return this.conn.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException
        {
            this.conn.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException
        {
            return this.conn.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException
        {
            this.conn.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
        {
            this.conn.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException
        {
            return this.conn.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException
        {
            return this.conn.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException
        {
            return this.conn.isWrapperFor(iface);
        }
    }
}
