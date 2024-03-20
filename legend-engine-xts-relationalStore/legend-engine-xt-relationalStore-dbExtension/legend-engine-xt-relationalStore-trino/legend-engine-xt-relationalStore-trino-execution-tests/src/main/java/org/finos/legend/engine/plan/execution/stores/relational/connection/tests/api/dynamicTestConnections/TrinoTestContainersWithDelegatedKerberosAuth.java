// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.testcontainers.containers.TrinoContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class TrinoTestContainersWithDelegatedKerberosAuth
        implements DynamicTestConnection
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public String type()
    {
        return "Test_Connection_Delegated_Kerberos";
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Trino;
    }

    public TrinoContainer trinoContainer = new TrinoContainer(DockerImageName.parse("trinodb/trino"))
            .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(10)));

    @Override
    public void setup()
    {
        this.startTrinoContainer();
    }

    private void startTrinoContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Trino ");

        long start = System.currentTimeMillis();
        this.trinoContainer.start();
        String containerHost = this.trinoContainer.getHost();
        int containerPort = this.trinoContainer.getMappedPort(8080);
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Trino on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        TrinoDatasourceSpecification trinoDatasourceSpecification = new TrinoDatasourceSpecification();
        trinoDatasourceSpecification.host = this.trinoContainer.getHost();
        trinoDatasourceSpecification.port = this.trinoContainer.getMappedPort(8080);
        trinoDatasourceSpecification.clientTags = "cg:vega";

        DelegatedKerberosAuthenticationStrategy authSpec = new DelegatedKerberosAuthenticationStrategy();

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(trinoDatasourceSpecification, authSpec, DatabaseType.Trino);
        conn.type = DatabaseType.Trino;         // for compatibility with legacy DatabaseConnection
        conn.element = "";                          // placeholder , will be set by pure tests
        return conn;
    }

    @Override
    public void cleanup()
    {
        this.trinoContainer.stop();
    }
}
