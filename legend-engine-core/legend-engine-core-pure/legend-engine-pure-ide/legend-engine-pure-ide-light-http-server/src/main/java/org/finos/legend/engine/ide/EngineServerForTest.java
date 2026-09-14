//  Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.ide;

import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.server.Server;
import org.finos.legend.engine.server.test.shared.MetadataTestServerResource;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Standalone backend for the "run tests through the real engine" dev loop (Option A of the LSP
 * dev-loop strategy). It boots the same server stack that {@code PureIDELight.withAlloyServerSupport()}
 * starts in-process (H2 + a local test metadata server + the full {@link Server}), but:
 * <ul>
 *   <li>on FIXED, predictable ports (so a separate JVM — the Pure LSP bridge — can point its
 *       {@code -Dlegend.test.server.*} system properties at it), and</li>
 *   <li>as a long-lived process that blocks forever (no Pure IDE HTTP server).</li>
 * </ul>
 *
 * Once this is running, start the Pure LSP with:
 * <pre>
 *   -Dlegend.test.server.host=127.0.0.1
 *   -Dlegend.test.server.port=9095
 *   -Dlegend.test.clientVersion=vX_X_X
 *   -Dlegend.test.serverVersion=v1
 *   -Dlegend.test.serializationKind=json
 *   -Dlegend.test.h2.port=9092
 * </pre>
 * and any Pure {@code meta::pure::router::execute(...)} (e.g. via {@code go()}) will route through
 * {@code mayExecuteLegendTest} to real plan-generation + execution on this server, instead of the
 * interpreted engine.
 *
 * Ports can be overridden with system properties {@code engine.test.server.port} /
 * {@code legend.test.h2.port}.
 */
public class EngineServerForTest
{
    public static final int DEFAULT_SERVER_PORT = 9095;
    public static final int DEFAULT_H2_PORT = 9092;
    private static final String SERVER_CONFIG = "org/finos/legend/engine/server/test/userTestConfig.json";

    public static void main(String[] args) throws Exception
    {
        System.setProperty("user.timezone", "GMT");

        int serverPort = Integer.getInteger("engine.test.server.port", DEFAULT_SERVER_PORT);
        int h2Port = Integer.getInteger("legend.test.h2.port", DEFAULT_H2_PORT);

        StartedEngine started = start(serverPort, h2Port, "vX_X_X");

        System.out.println("[engine-server-for-test] READY");
        System.out.println("[engine-server-for-test] server.host=127.0.0.1 server.port=" + started.serverPort + " h2.port=" + started.h2Port);
        System.out.println("[engine-server-for-test] Start the Pure LSP with: -Dlegend.test.server.host=127.0.0.1 -Dlegend.test.server.port=" + started.serverPort
                + " -Dlegend.test.clientVersion=vX_X_X -Dlegend.test.serverVersion=v1 -Dlegend.test.serializationKind=json -Dlegend.test.h2.port=" + started.h2Port);

        Thread.currentThread().join();
    }

    /**
     * Boot H2 + metadata + full engine {@link Server} in-process on the given (fixed) ports and
     * return once all three are listening. Callers that want the JVM to stay up must arrange that
     * themselves (Dropwizard's Jetty threads normally suffice; {@link #main} additionally blocks).
     */
    public static StartedEngine start(int serverPort, int h2Port, String clientVersion) throws Exception
    {
        PureWithEngineHelper.initClientVersionIfNotAlreadySet(clientVersion);

        System.setProperty("legend.test.h2.port", String.valueOf(h2Port));
        H2TestServerResource h2 = new H2TestServerResource();
        h2.start();
        System.out.println("[engine-server-for-test] H2 started on port " + h2.getPort());

        MetadataTestServerResource metadata = new MetadataTestServerResource();
        metadata.start();
        System.out.println("[engine-server-for-test] metadata server started");

        System.setProperty("dw.server.connector.port", String.valueOf(serverPort));
        String configFile = extractConfigToTempFile();
        Server<?> server = new Server<>();
        server.run("server", configFile);

        System.setProperty("alloy.test.server.host", "127.0.0.1");
        System.setProperty("alloy.test.server.port", String.valueOf(serverPort));
        System.setProperty("legend.test.server.host", "127.0.0.1");
        System.setProperty("legend.test.server.port", String.valueOf(serverPort));

        return new StartedEngine(serverPort, h2.getPort());
    }

    public static final class StartedEngine
    {
        public final int serverPort;
        public final int h2Port;

        StartedEngine(int serverPort, int h2Port)
        {
            this.serverPort = serverPort;
            this.h2Port = h2Port;
        }
    }

    private static String extractConfigToTempFile() throws Exception
    {
        try (InputStream in = Objects.requireNonNull(
                EngineServerForTest.class.getClassLoader().getResourceAsStream(SERVER_CONFIG),
                "Could not find server config on classpath: " + SERVER_CONFIG))
        {
            File tmp = File.createTempFile("engineServerForTestConfig", ".json");
            tmp.deleteOnExit();
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tmp.getAbsolutePath();
        }
    }
}
