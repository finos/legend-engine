// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.integration;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.NoCredentials;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.Database;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.DatabaseAdminClient;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.DatabaseId;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.Dialect;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.InstanceConfigId;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.InstanceId;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.InstanceInfo;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.Spanner;
import org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.SpannerOptions;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SpannerTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final int DEFAULT_STARTUP_ATTEMPTS = 3;

    public SpannerEmulatorContainer spannerContainerEmulator = new SpannerEmulatorContainer(
            DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "gcr.io") + "/cloud-spanner-emulator/emulator")
            .asCompatibleSubstituteFor("gcr.io/cloud-spanner-emulator/emulator")
    ).withStartupAttempts(DEFAULT_STARTUP_ATTEMPTS).withImagePullPolicy(PullPolicy.ageBased(Duration.of(7L, ChronoUnit.DAYS)));

    private Spanner spanner;
    private final String PCT_SPANNER_PROJECT_ID = "legend-spanner-pct-testing";
    private final String PCT_SPANNER_INSTANCE_ID = "pct-test-instance";
    private final String PCT_SPANNER_DB = "pct-test-db";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Spanner");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Spanner;
    }

    @Override
    public void setup()
    {
        this.startSpannerContainer();
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {

        if (!spannerContainerEmulator.isRunning())
        {
            // Start the container is the function is called from within the IDE
            this.setup();
        }
        SpannerDatasourceSpecification spannerDatasourceSpecification = new SpannerDatasourceSpecification();
        spannerDatasourceSpecification.projectId = PCT_SPANNER_PROJECT_ID;
        spannerDatasourceSpecification.instanceId = PCT_SPANNER_INSTANCE_ID;
        spannerDatasourceSpecification.databaseId = PCT_SPANNER_DB;
        spannerDatasourceSpecification.proxyHost = spannerContainerEmulator.getEmulatorGrpcEndpoint().split(":")[0];
        spannerDatasourceSpecification.proxyPort = Integer.valueOf(spannerContainerEmulator.getEmulatorGrpcEndpoint().split(":")[1]);
        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(spannerDatasourceSpecification, new TestDatabaseAuthenticationStrategy(), DatabaseType.Spanner);
        conn.type = DatabaseType.Spanner;           // for compatibility with legacy DatabaseConnection
        conn.element = null;
        return conn;
    }

    @Override
    public void cleanup() throws Exception
    {
        spanner.close();
        spannerContainerEmulator.stop();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }

    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    private void startSpannerContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Spanner ");
        long start = System.currentTimeMillis();
        spannerContainerEmulator.start();

        this.spanner = SpannerOptions.newBuilder()
                .setEmulatorHost(spannerContainerEmulator.getEmulatorGrpcEndpoint())
                .setCredentials(NoCredentials.getInstance())
                .setProjectId(PCT_SPANNER_PROJECT_ID)
                .build().getService();

        InstanceInfo instanceInfo = InstanceInfo.newBuilder(InstanceId.of(PCT_SPANNER_PROJECT_ID, PCT_SPANNER_INSTANCE_ID))
                        .setDisplayName("PCT test instance")
                        .setNodeCount(1)
                        .setInstanceConfigId(InstanceConfigId.of(PCT_SPANNER_PROJECT_ID, "emulator-config"))
                        .build();

        DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();
        Database databaseInfo = dbAdminClient.newDatabaseBuilder(DatabaseId.of(PCT_SPANNER_PROJECT_ID, PCT_SPANNER_INSTANCE_ID, PCT_SPANNER_DB))
                .setDialect(Dialect.POSTGRESQL)
                .build();

        try
        {
            spanner.getInstanceAdminClient().createInstance(instanceInfo).get();

            dbAdminClient.createDatabase(databaseInfo, Lists.mutable.empty()).get();

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Spanner on endpoint:" + spannerContainerEmulator.getEmulatorGrpcEndpoint()  + " , time taken(ms):" + (end - start));
    }
}
