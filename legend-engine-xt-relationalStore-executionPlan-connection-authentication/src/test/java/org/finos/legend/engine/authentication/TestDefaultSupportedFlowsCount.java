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

package org.finos.legend.engine.authentication;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestDefaultSupportedFlowsCount {
    LegendDefaultDatabaseAuthenticationFlowProvider defaultProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();

    @Before
    public void configureDefaultProvider(){
        defaultProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
    }

    @Test
    public void testCount() {
        assertEquals("mismatch in number of default authentication flows", TestDefaultSupportedFlows.data().size(), defaultProvider.count());
    }

    @Test
    public void h2FlowSupportedOnlyForStaticDatasource() {
        try
        {
            defaultProvider.registerFlow(new MyH2Flow());
            fail("Failed to throw");
        }
        catch (UnsupportedOperationException e)
        {
            assertEquals("Attempt to register a H2 flow with datasource spec 'LocalH2DatasourceSpecification'. Only 'StaticDatasourceSpecification' is supported for H2", e.getMessage());
        }
    }

    static class MyH2Flow implements DatabaseAuthenticationFlow<LocalH2DatasourceSpecification, TestDatabaseAuthenticationStrategy> {

        @Override
        public Class<LocalH2DatasourceSpecification> getDatasourceClass() {
            return LocalH2DatasourceSpecification.class;
        }

        @Override
        public Class<TestDatabaseAuthenticationStrategy> getAuthenticationStrategyClass() {
            return TestDatabaseAuthenticationStrategy.class;
        }

        @Override
        public DatabaseType getDatabaseType() {
            return DatabaseType.H2;
        }

        @Override
        public Credential makeCredential(Identity identity, LocalH2DatasourceSpecification datasourceSpecification, TestDatabaseAuthenticationStrategy authenticationStrategy) throws Exception {
            return null;
        }
    }
}