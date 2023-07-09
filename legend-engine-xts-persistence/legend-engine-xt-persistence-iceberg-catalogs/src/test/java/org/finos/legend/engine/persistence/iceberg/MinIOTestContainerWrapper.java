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

import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

public class MinIOTestContainerWrapper extends GenericContainer<MinIOTestContainerWrapper>
{
    private static final int DEFAULT_PORT = 9000;
    private final String ROOT_USER = "minioadmin";
    private final String ROOT_USER_PASSWORD = "minioadmin";
    private GenericContainer container;

    public MinIOTestContainerWrapper()
    {
        container = new GenericContainer<>("minio/minio")
                .withExposedPorts(DEFAULT_PORT)
                .withEnv("MINIO_ROOT_USER", ROOT_USER)
                .withEnv("MINIO_ROOT_PASSWORD", ROOT_USER_PASSWORD)
                .withCommand("server", "/data")
/*                .withLogConsumer(new Consumer<OutputFrame>()
                {
                    @Override
                    public void accept(OutputFrame outputFrame)
                    {
                        System.out.println(outputFrame.getUtf8String());
                    }
                })*/
                .withStartupTimeout(Duration.ofSeconds(60));
    }

    public void start()
    {
        this.container.start();
    }

    public void stop()
    {
        this.container.stop();
    }

    public int getPort()
    {
        return DEFAULT_PORT;
    }

    public String getAccessKeyId()
    {
        return ROOT_USER;
    }

    public String getSecretAccessKey()
    {
        return ROOT_USER_PASSWORD;
    }

    public String getUrl()
    {
        Integer port = container.getMappedPort(DEFAULT_PORT);
        String host = container.getHost();
        return String.format("http://%s:%d", host, port);
    }
}
