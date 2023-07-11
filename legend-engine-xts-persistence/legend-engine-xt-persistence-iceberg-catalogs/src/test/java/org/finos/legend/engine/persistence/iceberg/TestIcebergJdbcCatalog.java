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

package org.finos.legend.engine.persistence.iceberg;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.aws.AwsProperties;
import org.apache.iceberg.aws.s3.S3FileIO;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.NoSuchNamespaceException;
import org.apache.iceberg.jdbc.JdbcCatalog;
import org.apache.iceberg.types.Types;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.LegendPostgresTestConnectionProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class TestIcebergJdbcCatalog
{
    private static LegendPostgresTestConnectionProvider connectionProvider;

    private static Catalog icebergCatalog;
    private static MinIOTestContainerWrapper minIOTestContainerWrapper;
    private static String S3_ICEBERG_BUCKET_NAME = "iceberg";
    private static S3Client s3Client;

    @BeforeClass
    public static void setup() throws Exception
    {
        if (!DockerClientFactory.instance().isDockerAvailable())
        {
            assumeTrue("Skipping test. Docker not available", false);
        }

        System.setProperty("aws.region", "US_WEST_1");

        startPostgres();
        startMiniIO();
        initializeIcebergCatalog();
    }


    private static void initializeIcebergCatalog()
    {
        MutableMap<String, String> jdbcProperties = Maps.mutable
                .with(CatalogProperties.CATALOG_IMPL, JdbcCatalog.class.getCanonicalName())
                .withKeyValue(CatalogProperties.URI, connectionProvider.getUrl())
                .withKeyValue(JdbcCatalog.PROPERTY_PREFIX + "user", connectionProvider.getUser())
                .withKeyValue(JdbcCatalog.PROPERTY_PREFIX + "password", connectionProvider.getPassword());

        MutableMap<String, String> s3Properties = Maps.mutable
                .with(CatalogProperties.WAREHOUSE_LOCATION, "s3://" + S3_ICEBERG_BUCKET_NAME)
                .withKeyValue(CatalogProperties.FILE_IO_IMPL, S3FileIO.class.getCanonicalName())
                .withKeyValue(AwsProperties.S3FILEIO_ENDPOINT, minIOTestContainerWrapper.getUrl())
                .withKeyValue(AwsProperties.S3FILEIO_PATH_STYLE_ACCESS, "true")
                .withKeyValue(AwsProperties.S3FILEIO_ACCESS_KEY_ID, minIOTestContainerWrapper.getAccessKeyId())
                .withKeyValue(AwsProperties.S3FILEIO_SECRET_ACCESS_KEY, minIOTestContainerWrapper.getSecretAccessKey());

        icebergCatalog = new IcebergJdbcCatalogBuilder()
                .jdbcProperties(jdbcProperties)
                .s3Properties(s3Properties)
                .hadoopConfiguration(new Configuration())
                .build("test_postgres_catalog");
    }

    private static void startMiniIO()
    {
        minIOTestContainerWrapper = new MinIOTestContainerWrapper();
        minIOTestContainerWrapper.start();

        AwsCredentials credentials = AwsBasicCredentials.create(minIOTestContainerWrapper.getAccessKeyId(), minIOTestContainerWrapper.getSecretAccessKey());
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(minIOTestContainerWrapper.getUrl()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .forcePathStyle(true)
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(S3_ICEBERG_BUCKET_NAME).build());
    }

    private static void startPostgres() throws Exception
    {
        connectionProvider = new LegendPostgresTestConnectionProvider();
        connectionProvider.initializeForTest();
    }

    @AfterClass
    public static void shutdown() throws Exception
    {
        System.clearProperty("aws.region");

        if (connectionProvider == null)
        {
            return;
        }
        connectionProvider.shutdownForTest();

        if (minIOTestContainerWrapper == null)
        {
            return;
        }
        minIOTestContainerWrapper.stop();
    }

    @Test
    public void testCatalog() throws Exception
    {
        String icebergCatalogName = icebergCatalog.name();

        String icebergNamespaceName = "random-new-namespace" + System.currentTimeMillis();
        Namespace namespace = Namespace.of(icebergNamespaceName);

        this.testNamespaceThatDoesNotExist(icebergCatalog, namespace);

        String icebergTableName = "random-new-table1-" + System.currentTimeMillis();
        this.createTableInIcebergCatalog(namespace, icebergTableName);

        String icebergTableLocationInS3 = this.assertTableExistsInUnderlyingPostgres(icebergCatalogName, icebergNamespaceName, icebergTableName);
        this.assertTableExistsInS3(S3_ICEBERG_BUCKET_NAME, icebergTableLocationInS3);
    }

    private void testNamespaceThatDoesNotExist(Catalog jdbcCatalog, Namespace namespaceName)
    {
        try
        {
            List<TableIdentifier> tables = jdbcCatalog.listTables(namespaceName);
            assertEquals(0, tables.size());
            Assert.fail("Failed to get " + NoSuchNamespaceException.class);
        }
        catch (NoSuchNamespaceException e)
        {
            // ignore
        }
    }

    public Table createTableInIcebergCatalog(Namespace namespace, String tableName)
    {
        TableIdentifier name = TableIdentifier.of(namespace, tableName);
        Schema schema = new Schema(
                Types.NestedField.required(1, "level", Types.StringType.get()),
                Types.NestedField.required(2, "event_time", Types.TimestampType.withZone()),
                Types.NestedField.required(3, "message", Types.StringType.get()),
                Types.NestedField.optional(4, "call_stack", Types.ListType.ofRequired(5, Types.StringType.get()))
        );
        PartitionSpec spec = PartitionSpec.builderFor(schema)
                .hour("event_time")
                .identity("level")
                .build();

        Table table = icebergCatalog.createTable(name, schema, spec);
        return table;
    }

    public String assertTableExistsInUnderlyingPostgres(String cName, String nName, String tName) throws Exception
    {
        try (Connection connection = connectionProvider.getConnection())
        {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from iceberg_tables");
            while (resultSet.next())
            {
                String catalogName = resultSet.getString(1);
                String namespaceName = resultSet.getString(2);
                String tableName = resultSet.getString(3);
                String metadataLocation = resultSet.getString(3);

                if (catalogName.equals(cName) && namespaceName.equals(nName) && tableName.equals(tName))
                {
                    return metadataLocation;
                }
            }
        }
        fail("Fail to locate table in postgres catalog");
        return null;
    }

    public void assertTableExistsInS3(String bucketName, String tableBucketLocation)
    {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        for (S3Object s3Object : listObjectsV2Response.contents())
        {
            String key = s3Object.key();
            if (key.contains(tableBucketLocation))
            {
                return;
            }
        }
        fail("Failed to find table in S3 : " + tableBucketLocation);
    }
}
