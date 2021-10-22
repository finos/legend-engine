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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import javax.security.auth.Subject;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderSelector;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.pac4j.core.profile.CommonProfile;
import static org.junit.Assert.assertTrue;

public class TestConnectionAcquisitionWithFlowProvider_Local extends DbSpecificTests
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ConnectionManagerSelector connectionManagerSelector;

    @Before
    public void setup()
    {
        installFlowProvider();
        assertStaticH2FlowIsAvailable();

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), new RelationalExecutorInfo());
    }

    private void installFlowProvider()
    {
        DatabaseAuthenticationFlowProviderSelector.enableLegendDefaultFlowProvider();
        boolean flowProviderPresent = DatabaseAuthenticationFlowProviderSelector.getProvider().isPresent();
        assertTrue("Flow provider is not available", flowProviderPresent);
    }

    public void assertStaticH2FlowIsAvailable()
    {
        StaticDatasourceSpecification staticDatasourceSpecification = new StaticDatasourceSpecification();
        TestDatabaseAuthenticationStrategy testDatabaseAuthenticationStrategy = new TestDatabaseAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(staticDatasourceSpecification, testDatabaseAuthenticationStrategy, DatabaseType.H2);
        relationalDatabaseConnection.type = DatabaseType.H2;

        DatabaseAuthenticationFlowProvider flowProvider = DatabaseAuthenticationFlowProviderSelector.getProvider().get();
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("static h2 flow does not exist ", flow.isPresent());
    }

    @After
    public void cleanup()
    {
        DatabaseAuthenticationFlowProviderSelector.disableFlowProvider();
    }

    @Test
    public void testLocalTestConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.localH2WithUserPasswordSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject)null, systemUnderTest);
        testConnection(connection, "SELECT * FROM PersonTable");
    }

    @Test
    public void testLocalTestConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.localH2WithUserPasswordSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>)null, systemUnderTest);
        testConnection(connection, "SELECT * FROM PersonTable");
    }

    private RelationalDatabaseConnection localH2WithUserPasswordSpec() throws Exception
    {
        MutableList<String> setupSqls = Lists.mutable.with("drop table if exists PersonTable;",
                "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                "drop table if exists FirmTable;",
                "create table FirmTable(id INT, legalName VARCHAR(200));",
                "insert into FirmTable (id, legalName) values (1, 'firm')");

        LocalH2DatasourceSpecification localH2DatasourceSpec = new LocalH2DatasourceSpecification(null, setupSqls);
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(localH2DatasourceSpec, testDatabaseAuthSpec, DatabaseType.H2);
    }

    @Override
    protected Subject getSubject()
    {
        return null;
    }
}