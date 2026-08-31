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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.finos.legend.pure.lsp.LegendPureLspServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IDE-runnable entry point for the Legend Pure LSP server, alongside {@link PureIDELight}.
 * <p>
 * {@code LegendPureLspServer.main()} (in legend-pure-lsp-server) is designed to be launched as a bare
 * {@code java -cp <computed-classpath> ...} subprocess, because on its own it has no engine-scale
 * classpath - the pure-lsp-server dev-loop skill computes one via Maven and passes it in via
 * {@code --classpath-file}. This class needs none of that: it runs inside this module, which (like
 * {@link PureIDELight}) already carries the full engine-scale classpath as ordinary Maven dependencies,
 * so it only needs a config JSON telling it which repos to source-root, which port to listen on, and
 * which Pure/backend-Server options to set.
 * <p>
 * The payoff is that this becomes a plain "Run"/"Debug" configuration in any IDE (main class + one
 * program arg, the config path) - no skill, no computed classpath file, full user control over every
 * option in {@link LegendLspServerConfiguration}. Running it under "Debug" means Java breakpoints set
 * directly in the LSP server's own code (e.g. {@code LegendPureSession}, {@code LegendDebugSession},
 * {@code DebugService}) simply work under the IDE's normal debugger, with no DAP/socket-forwarding
 * layer involved - point a real LSP client (e.g. the Legend Pure IntelliJ plugin, configured to CONNECT
 * to this port rather than spawn its own process) at the port from {@link LegendLspServerConfiguration#port},
 * then drive it and step through the server-side Java directly.
 */
public class LegendLspServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendLspServer.class);

    // Matches PureIDELight's own convention of defaulting to the source-tree path (read as a plain
    // file relative to the process's working directory, i.e. the repo root) rather than a packaged
    // classpath resource - this module's resource-copy build step is overridden for web-content only.
    private static final String DEFAULT_CONFIG_RESOURCE =
            "legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/legendLspServerConfig.json";

    public static void main(String[] args) throws Exception
    {
        System.setProperty("user.timezone", "GMT");

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_RESOURCE;
        LegendLspServerConfiguration config = loadConfig(configPath);
        if (config.port <= 0)
        {
            throw new IllegalArgumentException("Config field 'port' must be a positive port number (got " + config.port + "). "
                    + "This entry point only supports socket mode - see LegendLspServerConfiguration#port.");
        }

        applySystemProperties(config);

        List<Path> repoRoots = config.repoRoots.stream().map(Paths::get).collect(Collectors.toList());
        Set<String> classpathRepositoryNames = new LinkedHashSet<>(config.classpathRepositoryNames);

        LegendPureLspServer server = new LegendPureLspServer();
        LOGGER.info("Legend LSP server: pre-configuring workspace from {}", repoRoots);
        server.preconfigureAndWarm(repoRoots, classpathRepositoryNames);

        LOGGER.info("Legend LSP server: warm and listening on 127.0.0.1:{}", config.port);
        LegendPureLspServer.runSocketMode(server, config.port);
    }

    private static LegendLspServerConfiguration loadConfig(String path) throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(path);
        if (file.isFile())
        {
            LOGGER.info("Legend LSP server: loading config from file {}", file.getAbsolutePath());
            return mapper.readValue(file, LegendLspServerConfiguration.class);
        }
        try (InputStream resource = LegendLspServer.class.getClassLoader().getResourceAsStream(path))
        {
            if (resource == null)
            {
                throw new FileNotFoundException("Legend LSP server config not found (not a file on disk, not a classpath resource): " + path);
            }
            LOGGER.info("Legend LSP server: loading config from classpath resource {}", path);
            return mapper.readValue(resource, LegendLspServerConfiguration.class);
        }
    }

    private static void applySystemProperties(LegendLspServerConfiguration config)
    {
        for (Map.Entry<String, Boolean> entry : config.pureOptions.entrySet())
        {
            System.setProperty("pure.options." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        LegendLspServerConfiguration.BackendServerConfiguration backendServer = config.backendServer;
        if (backendServer != null)
        {
            setIfPresent("legend.test.server.host", backendServer.host);
            setIfPresent("legend.test.server.port", backendServer.port);
            setIfPresent("legend.test.clientVersion", backendServer.clientVersion);
            setIfPresent("legend.test.serverVersion", backendServer.serverVersion);
            setIfPresent("legend.test.serializationKind", backendServer.serializationKind);
            setIfPresent("legend.test.h2.port", backendServer.h2Port);
        }

        for (Map.Entry<String, String> entry : config.additionalSystemProperties.entrySet())
        {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static void setIfPresent(String key, String value)
    {
        if (value != null)
        {
            System.setProperty(key, value);
        }
    }
}
