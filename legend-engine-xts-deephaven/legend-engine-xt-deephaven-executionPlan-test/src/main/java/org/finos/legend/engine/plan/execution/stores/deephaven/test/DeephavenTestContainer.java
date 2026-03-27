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

import io.deephaven.client.impl.BarrageSessionFactoryConfig;
import io.deephaven.client.impl.ClientChannelFactory;
import io.deephaven.client.impl.ClientChannelFactoryDefaulter;
import io.deephaven.client.impl.ClientConfig;
import io.deephaven.client.impl.SessionConfig;
import io.deephaven.uri.DeephavenTarget;
import org.apache.arrow.memory.BufferAllocator;
import org.finos.legend.engine.plan.execution.stores.deephaven.connection.DeephavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DeephavenTestContainer
{
    public static final Logger LOGGER = LoggerFactory.getLogger(DeephavenTestContainer.class);
    private static final int PORT = 10_000;
    private static final String PSK = "myStaticPSK";
    private static final String APP_DIR = "/app.d";

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

    public static DeephavenSession buildSession(GenericContainer<?> container, BufferAllocator bufferAllocator)
    {
        ClientConfig clientConfig = ClientConfig.builder()
                .target(DeephavenTarget.builder().isSecure(false).host(container.getHost()).port(container.getMappedPort(PORT)).build())
                .build();
        SessionConfig sessionConfig = SessionConfig.builder()
                .authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + PSK)
                .build();
        return new DeephavenSession(clientConfig, sessionConfig, CLIENT_CHANNEL_FACTORY, bufferAllocator);
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

    public static boolean startDeephavenWithGeneratedApp(String versionTag, String appConfigContent, String javaSource, String appFileName)
    {
        if (deephavenContainer != null && deephavenContainer.isRunning())
        {
            stopDeephaven();
        }
        try
        {
            Path tempDir = Files.createTempDirectory("deephaven-app");

            Path appFilePath = tempDir.resolve(appFileName + ".app");
            Files.write(appFilePath, appConfigContent.getBytes(StandardCharsets.UTF_8));

            String className = extractClassName(javaSource);
            String packageName = extractPackageName(javaSource);

            Path jarPath = JavaSourceCompiler.compileToJar(javaSource, className, packageName, tempDir);
            String containerJarPath = "/opt/deephaven/generated/" + jarPath.getFileName().toString();

            System.out.println("[DeephavenTestContainer] Wrote .app file: " + appFilePath);
            System.out.println("[DeephavenTestContainer] Compiled JAR: " + jarPath);
            System.out.println("[DeephavenTestContainer] Container JAR path: " + containerJarPath);

            DockerImageName imageName = DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", "ghcr.io") + "/deephaven/server:" + versionTag)
                    .asCompatibleSubstituteFor("ghcr.io/deephaven/server:" + versionTag);

            deephavenContainer = new GenericContainer<>(imageName)
                    .withExposedPorts(PORT)
                    .withPrivilegedMode(true)
                    .withCopyFileToContainer(MountableFile.forHostPath(appFilePath), APP_DIR + "/" + appFileName + ".app")
                    .withCopyFileToContainer(MountableFile.forHostPath(jarPath), containerJarPath)
                    .withEnv("START_OPTS", "-Xmx4g -Dauthentication.psk=" + PSK + " -Ddeephaven.application.dir=" + APP_DIR)
                    .withEnv("EXTRA_CLASSPATH", containerJarPath)
                    .withLogConsumer(outputFrame -> System.out.print("[Deephaven] " + outputFrame.getUtf8String()))
                    .waitingFor(Wait.forHttp("/").forPort(PORT).forStatusCode(200).withStartupTimeout(java.time.Duration.ofMinutes(2)));

            deephavenContainer.start();
            System.out.println("[DeephavenTestContainer] Container started successfully on port " + deephavenContainer.getMappedPort(PORT));
            return true;
        }
        catch (Exception e)
        {
            System.err.println("[DeephavenTestContainer] Failed to start Deephaven container with generated app:");
            e.printStackTrace(System.err);
            if (deephavenContainer != null)
            {
                try
                {
                    System.err.println("[DeephavenTestContainer] === Container logs ===");
                    System.err.println(deephavenContainer.getLogs());
                    System.err.println("[DeephavenTestContainer] === End container logs ===");
                }
                catch (Exception logEx)
                {
                    System.err.println("[DeephavenTestContainer] Could not retrieve container logs: " + logEx.getMessage());
                }
            }
            LOGGER.error("Failed to start Deephaven container with generated app", e);
            stopDeephaven();
            return false;
        }
    }

    private static String extractClassName(String javaSource)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)").matcher(javaSource);
        if (m.find())
        {
            return m.group(1);
        }
        throw new IllegalArgumentException("Could not extract class name from Java source");
    }

    private static String extractPackageName(String javaSource)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("package\\s+([\\w.]+)\\s*;").matcher(javaSource);
        if (m.find())
        {
            return m.group(1);
        }
        return "";
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
