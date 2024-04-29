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

import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.junit.*;
import org.testcontainers.DockerClientFactory;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import java.sql.Connection;
import java.sql.Statement;

@Ignore
public class IceboxTrinoTest
{
    @ClassRule
    public static final IceboxTrino ICEBOX = new IceboxTrino();

    @Test
    public void testCatalogUpdatedThruTrino() throws Exception
    {
        Assume.assumeTrue(DockerClientFactory.instance().isDockerAvailable());

        Catalog catalog = ICEBOX.getCatalog();
        SupportsNamespaces supportsNamespaces = (SupportsNamespaces) catalog;

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(ICEBOX.getBucketName())
                .build();

        Assert.assertEquals("Bucket should be empty", 0, ICEBOX.getS3().listObjectsV2(listObjectsV2Request).contents().size());

        Namespace namespace = Namespace.of("example_s3_schema");
        TableIdentifier table1 = TableIdentifier.of(namespace, "sample_table_1");
        TableIdentifier table2 = TableIdentifier.of(namespace, "sample_table_2");

        Assert.assertFalse("Namespace should not exists", supportsNamespaces.namespaceExists(namespace));
        Assert.assertFalse("Table1 should not exist", catalog.tableExists(table1));
        Assert.assertFalse("Table2 should not exist", catalog.tableExists(table2));

        try (Connection connection = ICEBOX.getTrino().createConnection();
             Statement statement = connection.createStatement())
        {
            // create an Iceberg namespace / schema
            statement.execute("CREATE SCHEMA " + ICEBOX.getTrinoConnectorName() + "." + namespace + " WITH (location = '" + ICEBOX.getBucketLocation() + "example_s3_schema/')");

            // create an Iceberg table inside namespace
            statement.execute("CREATE TABLE " + ICEBOX.getTrinoConnectorName() + "." + table1 + " (c1 INTEGER)");
            statement.execute("insert into " + ICEBOX.getTrinoConnectorName() + "." + table1 + " values (1), (2), (3)");

            // create an Iceberg table inside namespace
            statement.execute("CREATE TABLE " + ICEBOX.getTrinoConnectorName() + "." + table2 + " (c1 VARCHAR(10))");
            statement.execute("insert into " + ICEBOX.getTrinoConnectorName() + "." + table2 + " values ('hello'), ('world'), ('!')");
        }

        Assert.assertTrue("Namespace should have been created", supportsNamespaces.namespaceExists(namespace));

        Assert.assertTrue("Table1 should have been created", catalog.tableExists(table1));
        Assert.assertTrue("Table2 should have been created", catalog.tableExists(table2));

        ListObjectsV2Response listObjectsV2 = ICEBOX.getS3().listObjectsV2(listObjectsV2Request);
        Assert.assertTrue("S3 iceberg content should exits", listObjectsV2.contents().size() > 0);

//        for(S3Object s3Object : listObjectsV2.contents())
//        {
//            System.out.println(s3Object.key() + " - " + s3Object.size());
//        }
//
//        Table table1LLoaded = catalog.loadTable(table1);
//        String tableS3Key = table1LLoaded.location().substring(ICEBOX.getBucketLocation().length());
//        String snapshotManifestS3Key = table1LLoaded.currentSnapshot().manifestListLocation().substring(ICEBOX.getBucketLocation().length());
    }
}
