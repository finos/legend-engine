// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.lsp;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendWorkspaceService implements WorkspaceService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendWorkspaceService.class);
    static final String CMD_REINDEX = "legend.reindexWorkspace";

    private final LegendPureLspServer server;

    LegendWorkspaceService(LegendPureLspServer server)
    {
        this.server = server;
    }

    private static final int MAX_WORKSPACE_SYMBOLS = 500;

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LegendPureSession session = this.server.getSession();
            if (session == null || !session.isInitialized())
            {
                return Either.forLeft(Collections.emptyList());
            }

            // Use the pre-built index — no tree walking, no session lock needed
            List<SymbolInformation> symbols = this.server.getSymbolProvider().search(
                    this.server.getUriMapper(),
                    params.getQuery(),
                    MAX_WORKSPACE_SYMBOLS
            );
            return Either.<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>forLeft(symbols);
        });
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params)
    {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
    {
        // Run async to avoid blocking the LSP message thread
        CompletableFuture.runAsync(() -> handleFileChanges(params));
    }

    private void handleFileChanges(DidChangeWatchedFilesParams params)
    {
        LegendPureSession session = this.server.getSession();
        if (session == null || !session.isInitialized())
        {
            return;
        }

        FileChangeHandler handler = new FileChangeHandler(this.server.getUriMapper());
        List<LegendPureSession.FileChange> changes = handler.toFileChanges(params.getChanges());

        if (changes.isEmpty())
        {
            return;
        }

        LegendPureSession.CompileResult result = session.applyBulkChangesAndCompile(changes);

        if (result.isInternalError())
        {
            LOGGER.error("Internal error after file changes, triggering recovery", result.getError());
            this.server.triggerRecovery();
            return;
        }

        if (result.isSuccess())
        {
            for (LegendPureSession.FileChange change : changes)
            {
                String uri = this.server.getUriMapper().toUri(change.sourceId);
                if (uri != null)
                {
                    DiagnosticsPublisher.clear(this.server.getClient(), uri);
                }
            }
            // Rebuild symbol index after bulk changes
            this.server.getSymbolProvider().buildIndex(session.getPureRuntime());
        }
        else if (!result.isInternalError() && result.getError() != null)
        {
            // Publish diagnostics for compile errors from file watcher changes
            for (LegendPureSession.FileChange change : changes)
            {
                String uri = this.server.getUriMapper().toUri(change.sourceId);
                if (uri != null)
                {
                    DiagnosticsPublisher.publish(
                            this.server.getClient(), uri,
                            DiagnosticsPublisher.fromException(result.getError()));
                }
            }
        }
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params)
    {
        if (CMD_REINDEX.equals(params.getCommand()))
        {
            return CompletableFuture.supplyAsync(() ->
            {
                reindex();
                return null;
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    private void reindex()
    {
        LegendPureSession session = this.server.getSession();
        if (session == null)
        {
            return;
        }
        this.server.getClient().showMessage(new MessageParams(MessageType.Info, "Pure LSP: reindexing..."));
        try
        {
            this.server.getUriMapper().clear();
            session.reinitialize();
            // Rebuild symbol index
            this.server.getSymbolProvider().buildIndex(session.getPureRuntime());
            // Client listens for showMessage to trigger cache clear
            this.server.getClient().showMessage(new MessageParams(MessageType.Info, "Pure LSP: reindex complete"));
        }
        catch (Exception e)
        {
            LOGGER.error("Reindex failed", e);
            this.server.getClient().showMessage(new MessageParams(MessageType.Error, "Pure LSP reindex failed: " + e.getMessage()));
        }
    }
}
