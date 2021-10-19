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

import java.util.Arrays;
import java.util.Collection;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestDefaultSupportedFlows
{
    private static LegendDefaultDatabaseAuthenticationFlowProvider DEFAULT_PROVIDER = new LegendDefaultDatabaseAuthenticationFlowProvider();
    private final RelationalDatabaseConnection relationalDatabaseConnection;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                { DatabaseType.H2, LocalH2DatasourceSpecification.class, TestDatabaseAuthenticationStrategy.class },
                { DatabaseType.H2, LocalH2DatasourceSpecification.class, DefaultH2AuthenticationStrategy.class },
                { DatabaseType.H2, StaticDatasourceSpecification.class, TestDatabaseAuthenticationStrategy.class },
                { DatabaseType.Snowflake, SnowflakeDatasourceSpecification.class, SnowflakePublicAuthenticationStrategy.class },
                { DatabaseType.BigQuery, BigQueryDatasourceSpecification.class, GCPApplicationDefaultCredentialsAuthenticationStrategy.class },
        });
    }

    public TestDefaultSupportedFlows(DatabaseType databaseType, Class<? extends DatasourceSpecification> dsClass, Class<? extends AuthenticationStrategy> authClass) throws Exception
    {
        DatasourceSpecification datasourceSpecification = dsClass.getDeclaredConstructor().newInstance();
        AuthenticationStrategy authenticationStrategy = authClass.getDeclaredConstructor().newInstance();
        this.relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, databaseType);
    }

    @Test
    public void test()
    {
        DEFAULT_PROVIDER.lookupFlowOrThrow(this.relationalDatabaseConnection);
    }
}