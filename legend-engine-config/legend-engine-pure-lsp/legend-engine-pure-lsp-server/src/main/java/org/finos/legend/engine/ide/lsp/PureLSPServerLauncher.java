// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.ide.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Main entry point for the Pure LSP Server.
 * Supports both stdio and socket transport modes.
 */
public class PureLSPServerLauncher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureLSPServerLauncher.class);

    public static void main(String[] args)
    {
        LOGGER.info("Starting Pure LSP Server");

        // Parse arguments
        TransportMode mode = TransportMode.STDIO;
        int port = 5007;

        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--stdio":
                    mode = TransportMode.STDIO;
                    break;
                case "--socket":
                    mode = TransportMode.SOCKET;
                    if (i + 1 < args.length)
                    {
                        try
                        {
                            port = Integer.parseInt(args[++i]);
                        }
                        catch (NumberFormatException e)
                        {
                            LOGGER.error("Invalid port number: {}", args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length)
                    {
                        try
                        {
                            port = Integer.parseInt(args[++i]);
                        }
                        catch (NumberFormatException e)
                        {
                            LOGGER.error("Invalid port number: {}", args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    LOGGER.warn("Unknown argument: {}", args[i]);
            }
        }

        try
        {
            switch (mode)
            {
                case STDIO:
                    launchStdio();
                    break;
                case SOCKET:
                    launchSocket(port);
                    break;
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to start LSP server", e);
            System.exit(1);
        }
    }

    /**
     * Launch the server using standard input/output for communication.
     * This is the default mode used by most LSP clients (including VSCode).
     */
    private static void launchStdio() throws InterruptedException, ExecutionException
    {
        LOGGER.info("Launching Pure LSP Server in stdio mode");

        PureLanguageServer server = new PureLanguageServer();

        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
            server,
            System.in,
            System.out
        );

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        Future<?> startListening = launcher.startListening();
        startListening.get();
    }

    /**
     * Launch the server listening on a TCP socket.
     * Useful for debugging and remote connections.
     */
    private static void launchSocket(int port) throws Exception
    {
        LOGGER.info("Launching Pure LSP Server on port {}", port);

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            LOGGER.info("Pure LSP Server listening on port {}", port);
            System.out.println("Pure LSP Server listening on port " + port);

            while (true)
            {
                Socket socket = serverSocket.accept();
                LOGGER.info("Client connected from {}", socket.getRemoteSocketAddress());

                // Handle each client in a new thread
                new Thread(() -> handleSocketClient(socket)).start();
            }
        }
    }

    private static void handleSocketClient(Socket socket)
    {
        try
        {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            PureLanguageServer server = new PureLanguageServer();

            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                server,
                in,
                out
            );

            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);

            Future<?> startListening = launcher.startListening();
            startListening.get();
        }
        catch (Exception e)
        {
            LOGGER.error("Error handling client connection", e);
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (Exception e)
            {
                // Ignore close errors
            }
        }
    }

    private static void printUsage()
    {
        System.out.println("Pure LSP Server - Language Server Protocol implementation for Pure language");
        System.out.println();
        System.out.println("Usage: java -jar legend-engine-pure-lsp-server.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --stdio           Use standard input/output for communication (default)");
        System.out.println("  --socket <port>   Listen on TCP socket at specified port");
        System.out.println("  --port <port>     Specify port number (used with --socket, default: 5007)");
        System.out.println("  -h, --help        Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar legend-engine-pure-lsp-server.jar --stdio");
        System.out.println("  java -jar legend-engine-pure-lsp-server.jar --socket 5007");
    }

    private enum TransportMode
    {
        STDIO,
        SOCKET
    }
}
