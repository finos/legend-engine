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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;
import javax.security.auth.Subject;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.SpannerTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.SpannerTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Spanner extends RelationalConnectionTest
{

    public static final String TEST_QUERY = "select 1";

    public static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";

    protected ConnectionManagerSelector connectionManagerSelector;

    public void verifyGcpTestSetup()
    {
        String googleApplicationCredentials = System.getenv(GOOGLE_APPLICATION_CREDENTIALS);
        if (googleApplicationCredentials == null || googleApplicationCredentials.trim().isEmpty())
        {
            fail(String.format("Tests cannot be run. GCP env variable %s has not been set", GOOGLE_APPLICATION_CREDENTIALS));
        }
    }

    @BeforeClass
    public static void setupTest()
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }

    @Before
    public void setup()
    {
        SpannerTestDatabaseAuthenticationFlowProvider flowProvider = new SpannerTestDatabaseAuthenticationFlowProvider();
        SpannerTestDatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration =
            new SpannerTestDatabaseAuthenticationFlowProviderConfiguration();
        flowProvider.configure(flowProviderConfiguration);
        assertGCPADCFlowIsAvailable(flowProvider);
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections
            .emptyList(), Optional.of(flowProvider));
    }

    protected void assertGCPADCFlowIsAvailable(SpannerTestDatabaseAuthenticationFlowProvider flowProvider)
    {
        verifyGcpTestSetup();
        DatasourceSpecification datasourceSpecification = new SpannerDatasourceSpecification();
        GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy = new GCPApplicationDefaultCredentialsAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, DatabaseType.Spanner);
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue(relationalDatabaseConnection.type + " GCP adc flow does not exist ", flow.isPresent());
    }
    
    @Test
    public void testSpannerGCPADCConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.SpannerWithGCPADCSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, 5, TEST_QUERY);
    }

    @Test
    public void testSpannerGCPADCConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.SpannerWithGCPADCSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>) null, systemUnderTest);
        testConnection(connection, 5, TEST_QUERY);
    }

    private RelationalDatabaseConnection SpannerWithGCPADCSpec() throws IOException
    {
        DatasourceSpecification datasourceSpecification = getSpannerDatasourceSpecification();
        GCPApplicationDefaultCredentialsAuthenticationStrategy authSpec = new GCPApplicationDefaultCredentialsAuthenticationStrategy();
        return new RelationalDatabaseConnection(datasourceSpecification, authSpec, DatabaseType.Spanner);
    }

    private DatasourceSpecification getSpannerDatasourceSpecification() throws IOException
    {
        File config =
            FileUtils.getFile("src/test/resources/org/finos/legend/engine/server/test/spannerDataSourceConfiguration.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(config, SpannerDatasourceSpecification.class);
    }
}
