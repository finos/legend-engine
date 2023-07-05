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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class TestPoolName extends TestConnectionManagement
{
    @Before
    public void setup() throws Exception
    {
        super.setup();

        this.connectionStateManager = ConnectionStateManager.getInstance();
        ConnectionStateManager.setInstanceForTesting(connectionStateManager);
    }

    /*
        Connection state was keyed by a triplet : identity name + db key + db auth key
        This is not sufficient to uniquely identify requests.
        Consider the following use case :
            Request1 : identity = alice (Kerberos), db key = database1, db auth key = oauth
            Request2 : identity = alice (OAuth),    db key = database1, db auth key = oauth

       In this example, even though both the requests have the same identity, the db credential has to be created with the appropriate incoming credential.
       For e.g. Request2 cannot simply use the Kerberos credential from Request1. That credential could have expired and the associated connection state might be in the process of being cleaned up.
     */
    @Test
    public void testPoolNameIncludesCredentialType()
    {
        Identity user1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("pool1");
        Identity user2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("pool2");
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Collections.singletonList("DROP TABLE IF EXISTS T1;"));

        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        assertTrue(pool1.matches("DBPool_LocalH2_port:\\d{5}_sqlCS:3263863932_type:TestDB_pool1_org\\.finos\\.legend\\.engine\\.shared\\.core\\.identity\\.credential\\.AnonymousCredential"));

        String pool2 = connectionStateManager.poolNameFor(user2, ds1.getConnectionKey());
        assertTrue(pool2.matches("DBPool_LocalH2_port:\\d{5}_sqlCS:3263863932_type:TestDB_pool2_org\\.finos\\.legend\\.engine\\.shared\\.core\\.identity\\.credential\\.AnonymousCredential"));
    }
}
