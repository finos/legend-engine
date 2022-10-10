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
import java.util.Optional;
import javax.security.auth.Subject;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Spanner extends ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_GoogleCloud
{

    public static final String TEST_QUERY = "select 1";
    public static final String SPANNER_PROJECT_ID = "SPANNER_PROJECT_ID";
    public static final String SPANNER_INSTANCE_ID = "SPANNER_INSTANCE_ID";
    public static final String SPANNER_DATABASE_ID = "SPANNER_DATABASE_ID";

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

    private RelationalDatabaseConnection SpannerWithGCPADCSpec()
    {
        SpannerDatasourceSpecification datasourceSpecification = getSpannerDatasourceSpecification();
        GCPApplicationDefaultCredentialsAuthenticationStrategy authSpec = new GCPApplicationDefaultCredentialsAuthenticationStrategy();
        return new RelationalDatabaseConnection(datasourceSpecification, authSpec, DatabaseType.Spanner);
    }

    private SpannerDatasourceSpecification getSpannerDatasourceSpecification()
    {
        SpannerDatasourceSpecification datasourceSpecification = new SpannerDatasourceSpecification();
        datasourceSpecification.projectId = Optional.ofNullable(System.getenv(SPANNER_PROJECT_ID)).orElse("legend-integration-testing");
        datasourceSpecification.instanceId = Optional.ofNullable(System.getenv(SPANNER_INSTANCE_ID)).orElse("test-instance");
        datasourceSpecification.databaseId = Optional.ofNullable(System.getenv(SPANNER_DATABASE_ID)).orElse("test-db");
        return datasourceSpecification;
    }
}
