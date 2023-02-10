// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.eclipse.collections.impl.list.mutable.FastList;

import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class TestRelationStoreExecutableManager
{
    //Tests that a relational statement is added to the manager for an execution and then properly removed when the RelationalResult is closed
    @Test
    public void verifyRelationalResultCallsManagerAndClearsOnClose() throws SQLException
    {
        final String session = "testSession";
        Statement mockStatement = Mockito.mock(Statement.class);
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Connection mockConnection = Mockito.mock(Connection.class);
        ResultSetMetaData mockMetadata = Mockito.mock(ResultSetMetaData.class);
        RelationalExecutionNode mockExecutionNode = Mockito.mock(RelationalExecutionNode.class);
        DatabaseConnection mockDatabaseConnection = Mockito.mock(DatabaseConnection.class);

        mockExecutionNode.connection = mockDatabaseConnection;
        Mockito.when(mockConnection.createStatement()).thenReturn(mockStatement);
        Mockito.when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.getMetaData()).thenReturn(mockMetadata);
        Mockito.when(mockDatabaseConnection.accept(any())).thenReturn(false);
        StoreExecutableManager.INSTANCE.registerManager();
        RelationalResult result = new RelationalResult(FastList.newListWith(new RelationalExecutionActivity("TEST")), mockExecutionNode, FastList.newListWith(new SQLResultColumn("test", "INTEGER")), null, null, mockConnection, null, null, null, session);
        Assert.assertEquals(1, StoreExecutableManager.INSTANCE.getExecutables(session).size());
        result.close();
        Assert.assertTrue(StoreExecutableManager.INSTANCE.getExecutables(session).isEmpty());
        StoreExecutableManager.INSTANCE.reset();

    }

}
