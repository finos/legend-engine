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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendTextDocumentService implements TextDocumentService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendTextDocumentService.class);
    private static final long DEBOUNCE_MS = 300;

    private final LegendPureLspServer server;
    private final Map<String, String> openDocuments = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingCompiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread t = new Thread(r, "lsp-compile-debounce");
        t.setDaemon(true);
        return t;
    });

    LegendTextDocumentService(LegendPureLspServer server)
    {
        this.server = server;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();
        // Never compile pure:// sources — they are read-only classpath sources
        // served by the virtual filesystem. Compiling them would create duplicate
        // in-memory sources conflicting with the storage-backed originals.
        if (uri.startsWith("pure://"))
        {
            return;
        }
        String content = params.getTextDocument().getText();
        this.openDocuments.put(uri, content);
        scheduleCompile(uri, content);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();
        if (uri.startsWith("pure://"))
        {
            return;
        }
        List<?> changes = params.getContentChanges();
        if (!changes.isEmpty())
        {
            String content = params.getContentChanges().get(changes.size() - 1).getText();
            this.openDocuments.put(uri, content);
            scheduleCompile(uri, content);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();
        this.openDocuments.remove(uri);
        cancelPending(uri);
        DiagnosticsPublisher.clear(this.server.getClient(), uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params)
    {
        // didChange already scheduled a compile; didSave is a no-op
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                LegendPureSession session = this.server.getSession();
                if (session == null || !session.isInitialized())
                {
                    return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.emptyList());
                }

                String uri = params.getTextDocument().getUri();
                int line = params.getPosition().getLine() + 1;
                int column = params.getPosition().getCharacter() + 1;

                String sourceId = this.server.getUriMapper().toSourceId(uri);
                String resolvedId = session.resolveSourceId(sourceId);
                if (resolvedId == null)
                {
                    return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.emptyList());
                }

                // Synchronize on session to avoid reading the graph while a compile mutates it
                Location location;
                synchronized (session)
                {
                    location = NavigationProvider.definition(
                            session.getPureRuntime(),
                            this.server.getUriMapper(),
                            resolvedId,
                            line,
                            column
                    );
                }

                if (location == null)
                {
                    return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.emptyList());
                }
                return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.singletonList(location));
            }
            catch (Exception e)
            {
                LOGGER.error("Error in go-to-definition", e);
                return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.emptyList());
            }
        });
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                LegendPureSession session = this.server.getSession();
                if (session == null || !session.isInitialized())
                {
                    return null;
                }

                String uri = params.getTextDocument().getUri();
                int line = params.getPosition().getLine() + 1;
                int column = params.getPosition().getCharacter() + 1;

                String sourceId = this.server.getUriMapper().toSourceId(uri);
                String resolvedId = session.resolveSourceId(sourceId);
                if (resolvedId == null)
                {
                    return null;
                }

                synchronized (session)
                {
                    return HoverProvider.hover(session.getPureRuntime(), resolvedId, line, column);
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error in hover", e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                LegendPureSession session = this.server.getSession();
                if (session == null || !session.isInitialized())
                {
                    return Collections.<Location>emptyList();
                }

                String uri = params.getTextDocument().getUri();
                int line = params.getPosition().getLine() + 1;
                int column = params.getPosition().getCharacter() + 1;
                boolean includeDeclaration = params.getContext().isIncludeDeclaration();

                String sourceId = this.server.getUriMapper().toSourceId(uri);
                String resolvedId = session.resolveSourceId(sourceId);
                if (resolvedId == null)
                {
                    return Collections.<Location>emptyList();
                }

                synchronized (session)
                {
                    return ReferencesProvider.references(
                            session.getPureRuntime(),
                            this.server.getUriMapper(),
                            resolvedId,
                            line,
                            column,
                            includeDeclaration
                    );
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error in find-references", e);
                return Collections.<Location>emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                LegendPureSession session = this.server.getSession();
                if (session == null || !session.isInitialized())
                {
                    return new SemanticTokens(Collections.emptyList());
                }

                String uri = params.getTextDocument().getUri();
                if (uri.startsWith("pure://"))
                {
                    // For virtual files, derive source ID from pure:// path
                    String sourceId = uri.substring("pure://".length());
                    synchronized (session)
                    {
                        List<Integer> data = SemanticTokensProvider.getTokens(
                                session.getPureRuntime(), sourceId);
                        return new SemanticTokens(data);
                    }
                }

                String sourceId = this.server.getUriMapper().toSourceId(uri);
                String resolvedId = session.resolveSourceId(sourceId);
                if (resolvedId == null)
                {
                    return new SemanticTokens(Collections.emptyList());
                }

                synchronized (session)
                {
                    List<Integer> data = SemanticTokensProvider.getTokens(
                            session.getPureRuntime(), resolvedId);
                    return new SemanticTokens(data);
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error in semantic tokens", e);
                return new SemanticTokens(Collections.emptyList());
            }
        });
    }

    /**
     * Per-URI debounce: cancel any pending compile for this URI and schedule a new one.
     */
    private void scheduleCompile(String uri, String content)
    {
        cancelPending(uri);
        ScheduledFuture<?> future = this.debounceExecutor.schedule(
                () -> compileAndPublish(uri, content),
                DEBOUNCE_MS,
                TimeUnit.MILLISECONDS
        );
        this.pendingCompiles.put(uri, future);
    }

    private void cancelPending(String uri)
    {
        ScheduledFuture<?> prev = this.pendingCompiles.remove(uri);
        if (prev != null)
        {
            prev.cancel(false);
        }
    }

    void compileAndPublish(String uri, String content)
    {
        try
        {
            LegendPureSession session = this.server.getSession();
            if (session == null || !session.isInitialized())
            {
                LOGGER.debug("Session not ready, skipping compile for {}", uri);
                // Store for later — will be compiled when session initializes
                return;
            }

            String sourceId = this.server.getUriMapper().toSourceId(uri);
            LegendPureSession.CompileResult result = session.modifyAndCompile(sourceId, content);

            handleResult(uri, result);
        }
        catch (Exception e)
        {
            LOGGER.error("Unexpected error in compileAndPublish for {}", uri, e);
        }
    }

    void handleResult(String uri, LegendPureSession.CompileResult result)
    {
        if (!result.isReady())
        {
            return;
        }

        if (result.isInternalError())
        {
            LOGGER.error("Internal error during compilation, triggering recovery", result.getError());
            this.server.getClient().showMessage(new MessageParams(
                    MessageType.Warning,
                    "Pure LSP: internal error, reinitializing. " + result.getError().getMessage()));
            this.server.triggerRecovery();
            return;
        }

        if (result.isSuccess())
        {
            DiagnosticsPublisher.clear(this.server.getClient(), uri);
            for (String modifiedSourceId : result.getModifiedFiles())
            {
                String modifiedUri = this.server.getUriMapper().toUri(modifiedSourceId);
                if (modifiedUri != null)
                {
                    DiagnosticsPublisher.clear(this.server.getClient(), modifiedUri);
                }
            }
            // Refresh symbol index after successful compilation
            LegendPureSession session = this.server.getSession();
            if (session != null)
            {
                this.server.getSymbolProvider().buildIndex(session.getPureRuntime());
            }
        }
        else
        {
            DiagnosticsPublisher.publish(
                    this.server.getClient(),
                    uri,
                    DiagnosticsPublisher.fromException(result.getError())
            );
        }
    }

    /**
     * Compile all documents that were opened before the session was ready.
     * Called once after session initialization completes.
     */
    void compileOpenDocuments()
    {
        for (Map.Entry<String, String> entry : this.openDocuments.entrySet())
        {
            String uri = entry.getKey();
            if (uri.startsWith("pure://"))
            {
                continue;
            }
            String content = entry.getValue();
            LspLog.info("Compiling pre-opened document: " + uri);
            compileAndPublish(uri, content);
        }
    }

    void shutdown()
    {
        this.debounceExecutor.shutdownNow();
    }
}
