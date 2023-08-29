// Copyright 2023 Goldman Sachs
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

package org.finos.legend.tableformat.iceberg.testsupport;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.aws.s3.S3FileIO;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.jdbc.JdbcCatalog;
import org.junit.Assert;
import org.junit.runner.Description;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.FailureDetectingExternalResource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.TrinoContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

public class IceboxTrino extends FailureDetectingExternalResource implements AutoCloseable
{
    private static final String LOCALSTACK_DOCKER_IMAGE = "localstack/localstack:2.2.0";
    private static final String TRINO_DOCKER_IMAGE = "trinodb/trino:422";
    private static final String POSTGRES_DOCKER_IMAGE = "postgres:12.15";

    private Network network;
    private LocalStackContainer localstack;
    private PostgreSQLContainer<?> postgres;
    private TrinoContainer trino;
    private JdbcCatalog catalog;
    private S3Client s3;
    private Path trinoFile;

    public IceboxTrino()
    {

    }

    public String getBucketName()
    {
        return "iceberg";
    }

    public String getBucketLocation()
    {
        return "s3://" + this.getBucketName() + "/";
    }

    public TrinoContainer getTrino()
    {
        return this.trino;
    }

    public Catalog getCatalog()
    {
        return this.catalog;
    }

    public S3Client getS3()
    {
        return this.s3;
    }

    public void start() throws Exception
    {
        Assert.assertTrue("Docker environment not properly setup", DockerClientFactory.instance().isDockerAvailable());

        this.trinoFile = Files.createTempFile("trino_catalog", ".properties");
        this.network = Network.newNetwork();
        this.initLocalStack();
        this.intPostgres();

        Startables.deepStart(this.localstack, this.postgres).join();

        this.initS3();
        this.initIcebergCatalog();

        this.initTrino();
        this.trino.start();
    }

    @Override
    protected void starting(Description description)
    {
        try
        {
            this.start();
        }
        catch (Exception e)
        {
            throw new RuntimeException(description.toString() + " - failed to start", e);
        }
    }

    @Override
    public void close() throws Exception
    {
        try (
                Network ignored = this.network;
                LocalStackContainer ignored1 = this.localstack;
                PostgreSQLContainer<?> ignored2 = this.postgres;
                TrinoContainer ignored3 = this.trino;
                JdbcCatalog ignored4 = this.catalog;
                S3Client ignored5 = this.s3
        )
        {
            Files.deleteIfExists(this.trinoFile);
        }
    }

    private void initLocalStack()
    {
        this.localstack = new LocalStackContainer(DockerImageName.parse(LOCALSTACK_DOCKER_IMAGE))
                .withServices(LocalStackContainer.Service.S3)
                .withNetwork(this.network)
                .withNetworkAliases("s3_aws");
    }

    private void initTrino() throws Exception
    {
        this.prepareTrinoIcebergCatalog();

        this.trino = new TrinoContainer(TRINO_DOCKER_IMAGE)
                .withCopyFileToContainer(MountableFile.forHostPath(this.trinoFile, 0100666), "/etc/trino/catalog/iceberg.properties")
                .withNetwork(this.network)
                .withNetworkAliases("trino");
    }

    public String getTrinoConnectorName()
    {
        return this.catalog.name();
    }

    private void prepareTrinoIcebergCatalog() throws Exception
    {
        try (
                OutputStream os = Files.newOutputStream(this.trinoFile)
        )
        {
            Properties props = new Properties();

            props.setProperty("connector.name", getTrinoConnectorName());

            props.setProperty("fs.native-s3.enabled", "true");

            props.setProperty("s3.aws-secret-key", this.localstack.getSecretKey());
            props.setProperty("s3.aws-access-key", this.localstack.getAccessKey());
            props.setProperty("s3.region", this.localstack.getRegion());
            props.setProperty("s3.endpoint", "http://" + this.localstack.getContainerInfo().getNetworkSettings().getNetworks().entrySet().iterator().next().getValue().getIpAddress() + ":4566/");

            props.setProperty("iceberg.catalog.type", "jdbc");

            props.setProperty("iceberg.jdbc-catalog.catalog-name", this.catalog.name());
            props.setProperty("iceberg.jdbc-catalog.connection-url", "jdbc:postgresql://postgres:5432/test?loggerLevel=OFF");
            props.setProperty("iceberg.jdbc-catalog.default-warehouse-dir", getBucketLocation());
            props.setProperty("iceberg.jdbc-catalog.connection-password", this.postgres.getPassword());
            props.setProperty("iceberg.jdbc-catalog.connection-user", this.postgres.getUsername());
            props.setProperty("iceberg.jdbc-catalog.driver-class", "org.postgresql.Driver");

            props.store(os, "Iceberg Trino Properties");
        }
    }

    private void intPostgres()
    {
        this.postgres = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE)
                .withNetwork(this.network)
                .withNetworkAliases("postgres");
    }

    private void initS3()
    {
        this.s3 = S3Client
                .builder()
                .endpointOverride(this.localstack.getEndpoint())
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(this.localstack.getAccessKey(), this.localstack.getSecretKey())
                        )
                )
                .region(Region.of(this.localstack.getRegion()))
                .build();

        this.s3.createBucket(CreateBucketRequest.builder().bucket(getBucketName()).build());
    }

    private void initIcebergCatalog() throws Exception
    {
        Class.forName(this.postgres.getDriverClassName()); // ensure JDBC driver is at runtime classpath
        Map<String, String> properties = new HashMap<>();
        properties.put(CatalogProperties.CATALOG_IMPL, JdbcCatalog.class.getName());
        properties.put(CatalogProperties.URI, this.postgres.getJdbcUrl());
        properties.put(JdbcCatalog.PROPERTY_PREFIX + "user", this.postgres.getUsername());
        properties.put(JdbcCatalog.PROPERTY_PREFIX + "password", this.postgres.getPassword());
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, getBucketLocation());

        this.catalog = new JdbcCatalog(x -> new S3FileIO(() -> this.s3), null, true);
        this.catalog.initialize(getBucketName(), properties);
    }
}