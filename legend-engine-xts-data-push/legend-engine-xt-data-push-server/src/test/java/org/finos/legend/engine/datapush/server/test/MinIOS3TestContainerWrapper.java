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

package org.finos.legend.engine.datapush.server.test;

import org.junit.Assert;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class MinIOS3TestContainerWrapper
{
    private Network network;
    private GenericContainer<?> minioContainer;

    private MinIOS3TestContainerWrapper()
    {
    }

    public static MinIOS3TestContainerWrapper build()
    {
        return new MinIOS3TestContainerWrapper();
    }

    public void start() throws Exception
    {
        Assert.assertTrue("Docker environment not properly setup", DockerClientFactory.instance().isDockerAvailable());

        this.network = Network.newNetwork();
        this.initMinio();
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

        this.minioContainer = new GenericContainer<>("minio/minio:RELEASE.2023-08-09T23-30-22Z")
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

    public void stop() throws Exception
    {
        try (
                Network ignored = this.network;
                AutoCloseable ignored1 = this.minioContainer;
        )
        {
            // done
        }
    }
}