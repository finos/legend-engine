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

import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.junit.*;
import org.testcontainers.DockerClientFactory;

@Ignore
public class IceboxSparkTest
{
    @ClassRule
    public static final IceboxSpark ICEBOX = new IceboxSpark();

    @Test
    public void testCatalogUpdatedThruSpark() throws Exception
    {
        Assume.assumeTrue(DockerClientFactory.instance().isDockerAvailable());

        // create namespace (spark won't create it)
        Namespace namespace = ICEBOX.createNamespace("nyc");

        Catalog catalog = ICEBOX.getCatalog();

        TableIdentifier table1 = TableIdentifier.of(namespace, "taxis");
        Assert.assertFalse("Table1 should not exist", catalog.tableExists(table1));

        String createTable = "CREATE TABLE demo.nyc.taxis " +
                "(" +
                "  vendor_id bigint," +
                "  trip_id bigint," +
                "  trip_distance float," +
                "  fare_amount double," +
                "  store_and_fwd_flag string" +
                ") PARTITIONED BY (vendor_id);";
        ICEBOX.runSparkQL(createTable);

        Assert.assertTrue("Table1 should have been created", catalog.tableExists(table1));

        String insertSql = "INSERT INTO demo.nyc.taxis VALUES" +
                "(1, 1000371, 1.8, 15.32, 'N'), " +
                "(2, 1000372, 2.5, 22.15, 'N'), " +
                "(2, 1000373, 0.9, 9.01, 'N'), " +
                "(1, 1000374, 8.4, 42.13, 'Y');";
        ICEBOX.runSparkQL(insertSql);

        String selectSql = "SELECT * FROM demo.nyc.taxis order by trip_id;";
        String selectResult = ICEBOX.runSparkQL(selectSql);
        Assert.assertEquals("1\t1000371\t1.8\t15.32\tN\n"
                        + "2\t1000372\t2.5\t22.15\tN\n"
                        + "2\t1000373\t0.9\t9.01\tN\n"
                        + "1\t1000374\t8.4\t42.13\tY\n",
                selectResult);

//        String selectFilesSql = "SELECT * FROM demo.nyc.taxis.files;";
//        String select2Result = ICEBOX.runSparkQL(selectFilesSql);
//        System.out.println("SQL: " + selectFilesSql + "Result:");
//        System.out.println(select2Result);

        Iterable<Result<Item>> listObjects = ICEBOX.getS3().listObjects(ListObjectsArgs.builder().bucket(ICEBOX.getBucketName()).build());
        Assert.assertTrue("S3 iceberg content should exits", listObjects.iterator().hasNext());
    }
}
