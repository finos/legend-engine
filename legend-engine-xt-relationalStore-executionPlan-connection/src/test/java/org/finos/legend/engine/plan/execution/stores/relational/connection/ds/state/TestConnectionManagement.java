//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecificationRuntime;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Assert;
import org.junit.Before;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;

import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.TestLocalH2ConcurrentConnectionAcquisition.plainTextCredentialSupplier;
import static org.junit.Assert.assertNotNull;

public abstract class TestConnectionManagement
{
    protected FakeClock clock;
    protected long startTime;
    protected ConnectionStateManager connectionStateManager;

    @Before
    public void setup() throws Exception
    {
        resetSingleton();

        this.startTime = System.currentTimeMillis();
        this.clock = new FakeClock(startTime);
    }

    private void resetSingleton() throws Exception
    {
        Field field = ConnectionStateManager.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }

    Connection requestConnection(Identity identity, DataSourceSpecificationRuntime dataSourceSpecificationRuntime)
    {
        return dataSourceSpecificationRuntime.getConnectionUsingIdentity(identity, plainTextCredentialSupplier());
    }

    Connection requestConnection(String user, DataSourceSpecificationRuntime dataSourceSpecificationRuntime)
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting(user);
        return requestConnection(identity, dataSourceSpecificationRuntime);
    }

    LocalH2DataSourceSpecificationRuntime buildLocalDataSourceSpecification(List<String> initSQLS)
    {
        return new LocalH2DataSourceSpecificationRuntime(
                initSQLS,
                new H2Manager(),
                new TestDatabaseAuthenticationStrategyRuntime());
    }


    void assertPoolExists(boolean shouldExist, String user, ConnectionKey key)
    {
        if (shouldExist)
        {
            Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting(user);
            String poolName = connectionStateManager.poolNameFor(identity, key);
            DataSourceWithStatistics dataSourceSpecification = connectionStateManager.getDataSourceByPoolName(poolName);
            Assert.assertEquals("State mismatch for pool=" + poolName, shouldExist, dataSourceSpecification != null);
        }
    }

    void assertPoolStateExists(String... poolNames)
    {
        for (String poolName : poolNames)
        {
            assertNotNull("State not found for pool=" + poolName, connectionStateManager.getConnectionStateManagerPOJO(poolName));
        }
    }
}
