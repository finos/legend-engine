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

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.finos.legend.engine.ide.lsp.handlers.DiagnosticsHandler;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Language Server Protocol implementation for the Pure language.
 */
public class PureLanguageServer implements LanguageServer, LanguageClientAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureLanguageServer.class);

    private LanguageClient client;
    private LSPSession session;
    private PureTextDocumentService textDocumentService;
    private PureWorkspaceService workspaceService;
    private DiagnosticsHandler diagnosticsHandler;
    private int errorCode = 1;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        LOGGER.info("Initializing Pure Language Server");

        // Get workspace root
        Path workspaceRoot = null;
        if (params.getRootUri() != null)
        {
            try
            {
                workspaceRoot = Paths.get(new URI(params.getRootUri()));
                LOGGER.info("Workspace root: {}", workspaceRoot);
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to parse workspace root URI: {}", params.getRootUri(), e);
            }
        }
        else if (params.getRootPath() != null)
        {
            workspaceRoot = Paths.get(params.getRootPath());
            LOGGER.info("Workspace root (from path): {}", workspaceRoot);
        }

        // Initialize session
        session = new LSPSession(workspaceRoot);
        diagnosticsHandler = new DiagnosticsHandler(session);

        // Initialize services
        textDocumentService = new PureTextDocumentService(session, diagnosticsHandler);
        workspaceService = new PureWorkspaceService(session);

        // Build server capabilities
        ServerCapabilities capabilities = new ServerCapabilities();

        // Text document sync - full sync for simplicity
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        // Completion
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(false);
        completionOptions.setTriggerCharacters(java.util.Arrays.asList("::", ".", "$", "@"));
        capabilities.setCompletionProvider(completionOptions);

        // Go to definition
        capabilities.setDefinitionProvider(true);

        // Hover
        capabilities.setHoverProvider(true);

        // Find references
        capabilities.setReferencesProvider(true);

        // Document symbols
        capabilities.setDocumentSymbolProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        LOGGER.info("Pure Language Server initialized");

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params)
    {
        LOGGER.info("Pure Language Server fully initialized");
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        LOGGER.info("Shutting down Pure Language Server");
        errorCode = 0;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit()
    {
        LOGGER.info("Exiting Pure Language Server");
        System.exit(errorCode);
    }

    @Override
    public TextDocumentService getTextDocumentService()
    {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService()
    {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client)
    {
        this.client = client;
        if (diagnosticsHandler != null)
        {
            diagnosticsHandler.setClient(client);
        }
        LOGGER.info("Connected to language client");
    }

    public LanguageClient getClient()
    {
        return client;
    }

    public LSPSession getSession()
    {
        return session;
    }
}
