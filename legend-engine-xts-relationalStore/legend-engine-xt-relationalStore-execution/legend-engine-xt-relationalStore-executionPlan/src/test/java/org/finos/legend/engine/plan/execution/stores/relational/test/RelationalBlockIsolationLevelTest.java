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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnection;
import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnectionContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.TransactionIsolationLevel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

public class RelationalBlockIsolationLevelTest
{
    @Test
    public void testSetIsolationLevel() throws SQLException
    {
        // Setup
        Connection mockConnection = Mockito.mock(Connection.class);
        BlockConnection blockConnection = new BlockConnection(mockConnection);
        BlockConnectionContext context = new BlockConnectionContext();
        
        // Add the block connection to the context using reflection
        java.lang.reflect.Method setBlockConnectionMethod;
        try
        {
            setBlockConnectionMethod = BlockConnectionContext.class.getDeclaredMethod("setBlockConnection", 
                org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector.class,
                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection.class,
                BlockConnection.class);
            setBlockConnectionMethod.setAccessible(true);
            setBlockConnectionMethod.invoke(context, null, null, blockConnection);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        // Test
        context.setIsolationLevel(TransactionIsolationLevel.SERIALIZABLE);
        
        // Verify
        Mockito.verify(mockConnection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    }
}
