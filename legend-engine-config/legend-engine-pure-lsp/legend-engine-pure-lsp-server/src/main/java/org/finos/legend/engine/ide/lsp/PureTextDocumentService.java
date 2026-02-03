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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.finos.legend.engine.ide.lsp.handlers.CompletionHandler;
import org.finos.legend.engine.ide.lsp.handlers.DefinitionHandler;
import org.finos.legend.engine.ide.lsp.handlers.DiagnosticsHandler;
import org.finos.legend.engine.ide.lsp.handlers.HoverHandler;
import org.finos.legend.engine.ide.lsp.handlers.ReferencesHandler;
import org.finos.legend.engine.ide.lsp.handlers.SymbolHandler;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TextDocumentService implementation for Pure language.
 */
public class PureTextDocumentService implements TextDocumentService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureTextDocumentService.class);

    private final LSPSession session;
    private final DiagnosticsHandler diagnosticsHandler;
    private final CompletionHandler completionHandler;
    private final DefinitionHandler definitionHandler;
    private final HoverHandler hoverHandler;
    private final ReferencesHandler referencesHandler;
    private final SymbolHandler symbolHandler;

    public PureTextDocumentService(LSPSession session, DiagnosticsHandler diagnosticsHandler)
    {
        this.session = session;
        this.diagnosticsHandler = diagnosticsHandler;
        this.completionHandler = new CompletionHandler(session);
        this.definitionHandler = new DefinitionHandler(session);
        this.hoverHandler = new HoverHandler(session);
        this.referencesHandler = new ReferencesHandler(session);
        this.symbolHandler = new SymbolHandler(session);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        int version = params.getTextDocument().getVersion();

        LOGGER.debug("Document opened: {}", uri);

        session.getDocumentManager().openDocument(uri, text, version);

        // Compile and publish diagnostics
        diagnosticsHandler.compileAndPublishDiagnostics(uri);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();
        int version = params.getTextDocument().getVersion();

        LOGGER.debug("Document changed: {}", uri);

        session.getDocumentManager().changeDocument(uri, params.getContentChanges(), version);

        // Compile and publish diagnostics
        diagnosticsHandler.compileAndPublishDiagnostics(uri);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();

        LOGGER.debug("Document closed: {}", uri);

        session.getDocumentManager().closeDocument(uri);

        // Clear diagnostics for closed document
        diagnosticsHandler.clearDiagnostics(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params)
    {
        String uri = params.getTextDocument().getUri();

        LOGGER.debug("Document saved: {}", uri);

        // Recompile on save
        diagnosticsHandler.compileAndPublishDiagnostics(uri);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return completionHandler.getCompletion(params);
            }
            catch (Exception e)
            {
                LOGGER.error("Error handling completion request", e);
                return Either.forLeft(java.util.Collections.emptyList());
            }
        });
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return definitionHandler.getDefinition(params);
            }
            catch (Exception e)
            {
                LOGGER.error("Error handling definition request", e);
                return Either.forLeft(java.util.Collections.emptyList());
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
                return hoverHandler.getHover(params);
            }
            catch (Exception e)
            {
                LOGGER.error("Error handling hover request", e);
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
                return referencesHandler.getReferences(params);
            }
            catch (Exception e)
            {
                LOGGER.error("Error handling references request", e);
                return java.util.Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return symbolHandler.getDocumentSymbols(params);
            }
            catch (Exception e)
            {
                LOGGER.error("Error handling document symbol request", e);
                return java.util.Collections.emptyList();
            }
        });
    }
}
