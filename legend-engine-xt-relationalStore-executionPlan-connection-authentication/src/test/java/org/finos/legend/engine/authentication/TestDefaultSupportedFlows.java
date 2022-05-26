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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestDefaultSupportedFlows
{
    private static LegendDefaultDatabaseAuthenticationFlowProvider DEFAULT_PROVIDER = new LegendDefaultDatabaseAuthenticationFlowProvider();
    private final RelationalDatabaseConnection relationalDatabaseConnection;

    @Before
    public void configureDefaultProvider(){
        DEFAULT_PROVIDER.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                { DatabaseType.H2, StaticDatasourceSpecification.class, TestDatabaseAuthenticationStrategy.class },
                { DatabaseType.Snowflake, SnowflakeDatasourceSpecification.class, SnowflakePublicAuthenticationStrategy.class },
                { DatabaseType.BigQuery, BigQueryDatasourceSpecification.class, GCPApplicationDefaultCredentialsAuthenticationStrategy.class },
                { DatabaseType.BigQuery, BigQueryDatasourceSpecification.class, GCPWorkloadIdentityFederationAuthenticationStrategy.class},
                { DatabaseType.SqlServer, StaticDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class },
                { DatabaseType.Databricks, DatabricksDatasourceSpecification.class, ApiTokenAuthenticationStrategy.class },
                { DatabaseType.Redshift, RedshiftDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class }
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