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

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.nessie.NessieCatalog;
import org.junit.Assert;
import org.junit.runner.Description;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.FailureDetectingExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

public class IceboxSpark extends FailureDetectingExternalResource implements AutoCloseable
{
    private Network network;
    private GenericContainer<?> nessie;
    private GenericContainer<?> minio;
    private GenericContainer<?> sparkIceberg;
    private MinioClient minioClient;
    private NessieCatalog catalog;

    public IceboxSpark()
    {

    }

    public String getBucketName()
    {
        return "warehouse";
    }

    public String getBucketLocation()
    {
        return "s3://" + this.getBucketName() + "/";
    }

    public Catalog getCatalog()
    {
        return this.catalog;
    }

    public MinioClient getS3()
    {
        return this.minioClient;
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

    public void start() throws Exception
    {
        Assert.assertTrue("Docker environment not properly setup", DockerClientFactory.instance().isDockerAvailable());

        this.network = Network.newNetwork();
        this.initMinio();
        this.initNessie();
        this.initSpark();

        this.sparkIceberg.start();

        this.initS3();
        this.initIcebergCatalog();
    }

    private void initNessie()
    {
        this.nessie = new GenericContainer<>("projectnessie/nessie:0.67.0")
                .withNetwork(this.network)
                .withNetworkAliases("nessie")
                .withExposedPorts(19120);
    }

    private void initMinio()
    {
//        GenericContainer<?> mc = new GenericContainer<>("minio/mc:RELEASE.2023-08-08T17-23-59Z")
//                .withNetwork(this.network)
//                .withEnv("AWS_ACCESS_KEY_ID", "admin")
//                .withEnv("AWS_SECRET_ACCESS_KEY", "password")
//                .withEnv("AWS_REGION", "us-east-1")
//                .withCreateContainerCmdModifier(x -> x.withEntrypoint(
//                                "/bin/sh",
//                                "-c",
//                                "until (/usr/bin/mc config host add minio http://minio:9000 admin password) do echo '...waiting...' && sleep 1; done; " +
//                                        "/usr/bin/mc rm -r --force minio/" + this.getBucketName() + "; " +
//                                        "/usr/bin/mc mb minio/" + this.getBucketName() + "; " +
//                                        "/usr/bin/mc policy set public minio/" + this.getBucketName() + "; " +
//                                        "tail -f /dev/null"
//                        )
//                );

        this.minio = new GenericContainer<>("minio/minio:RELEASE.2023-08-09T23-30-22Z")
                .withNetwork(this.network)
                .withNetworkAliases("minio", "warehouse.minio")
                .withEnv("MINIO_ROOT_USER", "admin")
                .withEnv("MINIO_ROOT_PASSWORD", "password")
                .withEnv("MINIO_DOMAIN", "minio")
                .withExposedPorts(9000, 9001)
                .withCommand("server", "/data", "--console-address", ":9001")
//                .dependsOn(mc)
        ;

    }

    private void initSpark()
    {
        this.sparkIceberg = new GenericContainer<>("tabulario/spark-iceberg:3.4.1_1.3.1")
                .withNetwork(network)
                .withEnv("AWS_ACCESS_KEY_ID", "admin")
                .withEnv("AWS_SECRET_ACCESS_KEY", "password")
                .withEnv("AWS_REGION", "us-east-1")
                .withCopyFileToContainer(MountableFile.forClasspathResource("spark-defaults.conf"), "/opt/spark/conf/spark-defaults.conf")
                .withExposedPorts(8888, 8080, 10000)
                .dependsOn(this.minio, this.nessie)
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private void initS3() throws Exception
    {
        this.minioClient =
                MinioClient.builder()
                        .endpoint("http://" + minio.getHost() + ":" + minio.getMappedPort(9000))
                        .credentials("admin", "password")
                        .build();

        this.minioClient.makeBucket(MakeBucketArgs.builder().bucket(this.getBucketName()).build());
    }

    private void initIcebergCatalog() throws Exception
    {
        this.catalog = new NessieCatalog();
        Map<String, String> properties = new HashMap<>();
        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO");
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "s3a://" + this.getBucketName() + "/wh");
        properties.put("s3.endpoint", "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        properties.put("s3.access-key-id", "admin");
        properties.put("s3.secret-access-key", "password");
        properties.put("client.credentials-provider", "software.amazon.awssdk.auth.credentials.StaticCredentialsProvider");
        properties.put("client.region", "us-east-1");
        properties.put(CatalogProperties.URI, "http://" + nessie.getHost() + ":" + nessie.getMappedPort(19120) + "/api/v1");
        this.catalog.initialize("demo", properties);
    }

    @Override
    public void close() throws Exception
    {
        try (
                Network ignored = this.network;
                AutoCloseable ignored1 = this.minio;
                AutoCloseable ignored2 = this.nessie;
                AutoCloseable ignored3 = this.sparkIceberg
        )
        {
            // done
        }
    }

    public String runSparkQL(String sql) throws Exception
    {
        Container.ExecResult result = this.sparkIceberg.execInContainer("/opt/spark/bin/spark-sql", "-e", "\"" + sql + "\"");
        if (result.getExitCode() != 0)
        {
            throw new RuntimeException("Failed to execute sql: " + sql + ".  Err from process: " + result.getStderr());
        }
        return result.getStdout();
    }
}