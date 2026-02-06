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

package org.finos.legend.engine.plan.execution.stores.deephaven.test;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.BarrageSessionFactoryConfig;
import io.deephaven.client.impl.ClientChannelFactory;
import io.deephaven.client.impl.ClientChannelFactoryDefaulter;
import io.deephaven.client.impl.ClientConfig;
import io.deephaven.client.impl.SessionConfig;
import io.deephaven.uri.DeephavenTarget;
import org.apache.arrow.memory.BufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeephavenTestContainer
{
    public static final Logger LOGGER = LoggerFactory.getLogger(DeephavenTestContainer.class);
    private static final int PORT = 10_000;
    private static final String PSK = "myStaticPSK";

    private static final ClientChannelFactory CLIENT_CHANNEL_FACTORY = ClientChannelFactoryDefaulter.builder()
            .userAgent(BarrageSessionFactoryConfig.userAgent(Collections.singletonList("deephaven-barrage-examples")))
            .build();

    public static GenericContainer<?> deephavenContainer;

    private static GenericContainer<?> startDeephavenContainer(String versionTag)
    {
        DockerImageName imageName = DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "ghcr.io") + "/deephaven/server:" + versionTag)
                .asCompatibleSubstituteFor("ghcr.io/deephaven/server:" + versionTag);

        deephavenContainer = new GenericContainer<>(imageName)
                .withExposedPorts(PORT)
                .withPrivilegedMode(true)
                .withEnv("START_OPTS", "-Xmx4g -Dauthentication.psk=" + PSK)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("Deephaven"))
                .waitingFor(Wait.forHttp("/").forPort(PORT).forStatusCode(200).withStartupTimeout(java.time.Duration.ofMinutes(2)));

        deephavenContainer.start();
        return deephavenContainer;
    }

    public static BarrageSession buildSession(GenericContainer<?> container, BufferAllocator bufferAllocator)
    {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        final BarrageSessionFactoryConfig.Factory factory = BarrageSessionFactoryConfig.builder()
                .clientConfig(ClientConfig.builder().target(DeephavenTarget.builder().isSecure(false).host(container.getHost()).port(container.getMappedPort(PORT)).build()).build())
                .clientChannelFactory(CLIENT_CHANNEL_FACTORY)
                .allocator(bufferAllocator)
                .scheduler(scheduler)
                .build()
                .factory();

        return factory.newBarrageSession(sessionConfig());
    }

    private static SessionConfig sessionConfig()
    {
        final SessionConfig.Builder builder = SessionConfig.builder();
        builder.authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + PSK);
        return builder.build();
    }

    public static boolean startDeephaven(String versionTag)
    {
        try
        {
            if (deephavenContainer == null || !deephavenContainer.isRunning())
            {
                deephavenContainer = startDeephavenContainer(versionTag);
            }
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to start Deephaven container", e);
            stopDeephaven();
            return false;
        }
    }

    public static boolean stopDeephaven()
    {
        if (deephavenContainer != null && deephavenContainer.isRunning())
        {
            try
            {
                deephavenContainer.stop();
                return true;
            }
            catch (Exception e)
            {
                LOGGER.warn("Error stopping Deephaven container", e);
                return false;
            }
            finally
            {
                deephavenContainer = null;
            }
        }
        return true;
    }

    public boolean isRunning()
    {
        return deephavenContainer.isRunning();
    }

    public int getMappedPortTest()
    {
        if (deephavenContainer != null && deephavenContainer.isRunning())
        {
            return deephavenContainer.getMappedPort(PORT);
        }
        throw new IllegalStateException("Container is not running");
    }

    public String getHost()
    {
        if (deephavenContainer != null)
        {
            return deephavenContainer.getHost();
        }
        throw new IllegalStateException("Container is not running");
    }

    public String getLogs()
    {
        if (deephavenContainer != null)
        {
            return deephavenContainer.getLogs();
        }
        throw new IllegalStateException("Container is not running");
    }

    public static String getPsk()
    {
        return PSK;
    }
}
