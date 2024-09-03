// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb;

import org.apache.iceberg.catalog.Namespace;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.IcebergDuckDBPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.authentication.DuckDBS3AuthenticationStrategy;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.tableformat.iceberg.testsupport.IceboxSpark;

public class DuckDBS3TestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private final IceboxSpark iceboxSpark = new IceboxSpark();
    private final InMemoryVaultForTesting vaultImplementation = new InMemoryVaultForTesting();

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
        this.iceboxSpark.close();
    }

    @Override
    public void start() throws Exception
    {
        this.iceboxSpark.start();
        this.setup();
        this.createIcebergState();
    }

    private void createIcebergState() throws Exception
    {
        // create namespace (spark won't create it)
        this.iceboxSpark.createNamespace("nyc");

        String createTable = "CREATE TABLE demo.nyc.taxis " +
                "(" +
                "  vendor_id bigint," +
                "  trip_id bigint," +
                "  trip_distance float," +
                "  fare_amount double," +
                "  store_and_fwd_flag string" +
                ") PARTITIONED BY (vendor_id)" +
                "LOCATION '" + this.iceboxSpark.getBucketLocation() + "wh/nyc/taxis/';";
        this.iceboxSpark.runSparkQL(createTable);

        String insertSql = "INSERT INTO demo.nyc.taxis VALUES" +
                "(1, 1000371, 1.8, 15.32, 'N'), " +
                "(2, 1000372, 2.5, 22.15, 'N'), " +
                "(2, 1000373, 0.9, 9.01, 'N'), " +
                "(1, 1000374, 8.4, 42.13, 'Y');";
        this.iceboxSpark.runSparkQL(insertSql);

        this.iceboxSpark.writeHintFile("nyc", "taxis");

        this.iceboxSpark.printBucketObjects();
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.DuckDB;
    }

    @Override
    public void setup()
    {
        this.vaultImplementation.setValue("secretAccessKeyVaultReference", IceboxSpark.S3_SECRET_KEY);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        DuckDBDatasourceSpecification duckDBDataSourceSpecification = new DuckDBDatasourceSpecification();
        duckDBDataSourceSpecification.path = "";

        DuckDBS3AuthenticationStrategy s3 = new DuckDBS3AuthenticationStrategy();
        s3.region = IceboxSpark.S3_REGION;
        s3.accessKeyId = IceboxSpark.S3_ACCESS_KEY;
        s3.secretAccessKeyVaultReference = "secretAccessKeyVaultReference";
        s3.endpoint = this.iceboxSpark.getS3EndPoint();

        IcebergDuckDBPostProcessor icebergDuckDBPostProcessor = new IcebergDuckDBPostProcessor();
        // todo relation accessor does not support schema yet,  hence /nyc/ directory
        icebergDuckDBPostProcessor.rootPath = this.iceboxSpark.getBucketLocation() + "wh/nyc/";
//        icebergDuckDBPostProcessor.allowMovedPath = true;

        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(duckDBDataSourceSpecification, s3, DatabaseType.DuckDB);
        relationalDatabaseConnection.postProcessors = Lists.mutable.with(
                icebergDuckDBPostProcessor
        );

        return relationalDatabaseConnection;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
    }
}
