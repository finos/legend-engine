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
import org.apache.arrow.memory.RootAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeephavenTestContainer
{
    public static final Logger LOGGER = LoggerFactory.getLogger(DeephavenTestContainer.class);
    private static final String DEEPHAVEN_VERSION_TAG = "0.37.4";
    private static final int PORT = 10_000;
    private static final String PSK = "myStaticPSK";
    private static final String APP_DIR = "/app.d";
    private static final String START_OPTS = "-Xmx4g -Dauthentication.psk=" + PSK + " -Ddeephaven.application.dir=" + APP_DIR;
    private static final String SCRIPT_RESOURCE = "testDataSetup.py";

    private static final ClientChannelFactory CLIENT_CHANNEL_FACTORY = ClientChannelFactoryDefaulter.builder()
            .userAgent(BarrageSessionFactoryConfig.userAgent(Collections.singletonList("deephaven-barrage-examples")))
            .build();

    public static final BufferAllocator bufferAllocator = new RootAllocator();

    public static GenericContainer<?> deephavenContainer;
    public static BarrageSession deephavenSession;

    public static boolean startDeephaven(String versionTag)
    {
        if (deephavenContainer != null && deephavenContainer.isRunning())
        {
            stopDeephaven();
        }
        try
        {
            Path tempDir = createTempAppDirectory();
            DockerImageName imageName = DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "ghcr.io") + "/deephaven/server:" + versionTag)
                    .asCompatibleSubstituteFor("ghcr.io/deephaven/server:" + versionTag);

            deephavenContainer = new GenericContainer<>(imageName)
                    .withExposedPorts(PORT)
                    .withPrivilegedMode(true)
                    .withCopyFileToContainer(MountableFile.forHostPath(tempDir), APP_DIR)
                    .withCopyFileToContainer(MountableFile.forClasspathResource(SCRIPT_RESOURCE),APP_DIR + "/testDataSetup.py")
                    .withEnv("START_OPTS", START_OPTS)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("Deephaven"))
                    .waitingFor(Wait.forHttp("/").forPort(PORT).forStatusCode(200).withStartupTimeout(java.time.Duration.ofMinutes(2)));

            deephavenContainer.start();
            return true;
        }
        catch (Exception e)
        {
            stopDeephaven();
            return false;
        }
    }

    private static GenericContainer<?> startDeephavenContainerForPCT(String versionTag)
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

    private static BarrageSession buildSession(GenericContainer<?> container)
    {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        final BarrageSessionFactoryConfig.Factory factory = BarrageSessionFactoryConfig.builder()
                .clientConfig(ClientConfig.builder().target(DeephavenTarget.builder().isSecure(false).host(container.getHost()).port(container.getMappedPort(PORT)).build()).build())
                .clientChannelFactory(CLIENT_CHANNEL_FACTORY)
                .allocator(bufferAllocator)
                .scheduler(scheduler)
                .build()
                .factory();

        deephavenSession = factory.newBarrageSession(sessionConfig());
        return deephavenSession;
    }

    private static SessionConfig sessionConfig()
    {
        final SessionConfig.Builder builder = SessionConfig.builder();
        builder.authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + PSK);
        return builder.build();
    }

    public static boolean startDeephavenForPCT(String versionTag)
    {
//        if (deephavenContainer != null && deephavenContainer.isRunning())
//        {
//            stopDeephaven();
//        }

        try
        {
            deephavenContainer = startDeephavenContainerForPCT(versionTag);
            deephavenSession = buildSession(deephavenContainer);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to start Deephaven for PCT", e);
            stopDeephaven();
            return false;
        }
    }

    private static Path createTempAppDirectory()
    {
        try
        {
            Path tempDir = Files.createTempDirectory("deephaven-app");
            String appConfig = "type=script\n" +
                                "scriptType=python\n" +
                                "enabled=true\n" +
                                "id=test-app\n" +
                                "name=Test App\n" +
                                "file_0=./testDataSetup.py";
            Files.write(tempDir.resolve("test.app"), appConfig.getBytes(StandardCharsets.UTF_8));
            return tempDir;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create temp app directory", e);
        }
    }

    public static boolean stopDeephaven()
    {
        // Close the session first if it exists
        if (deephavenSession != null)
        {
            try
            {
                deephavenSession.close();
            }
            catch (Exception e)
            {
                LOGGER.warn("Error closing Deephaven session", e);
            }
            finally
            {
                deephavenSession = null;
            }
        }

        // Then stop the container
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
