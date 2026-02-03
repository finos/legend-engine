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

package org.finos.legend.engine.ide.lsp.handlers;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.engine.ide.lsp.converters.DiagnosticConverter;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m4.exception.PureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Handles compilation and publishes diagnostics to the client.
 */
public class DiagnosticsHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsHandler.class);

    private final LSPSession session;
    private LanguageClient client;

    public DiagnosticsHandler(LSPSession session)
    {
        this.session = session;
    }

    public void setClient(LanguageClient client)
    {
        this.client = client;
    }

    /**
     * Compile and publish diagnostics for the given document.
     */
    public void compileAndPublishDiagnostics(String uri)
    {
        if (client == null)
        {
            LOGGER.warn("Cannot publish diagnostics: client not set");
            return;
        }

        String sourceId = URIUtils.uriToSourceId(uri);
        String content = session.getDocumentManager().getDocumentContent(uri);

        if (content == null)
        {
            LOGGER.warn("No content found for document: {}", uri);
            return;
        }

        // Update source and compile
        LSPSession.CompilationResult result = session.updateSource(sourceId, content, true);

        // Build diagnostics
        MutableList<Diagnostic> diagnostics = Lists.mutable.empty();

        if (!result.isSuccess())
        {
            PureException pureException = result.getPureException();
            if (pureException != null)
            {
                // Only add diagnostic if the error is in this file
                String errorSourceId = DiagnosticConverter.getSourceId(pureException);
                if (errorSourceId == null || errorSourceId.equals(sourceId))
                {
                    diagnostics.add(DiagnosticConverter.toDiagnostic(pureException));
                }
            }
            else if (result.getException() != null)
            {
                diagnostics.add(DiagnosticConverter.toDiagnostic(result.getException()));
            }
        }

        // Publish diagnostics for this document
        publishDiagnostics(uri, diagnostics);
    }

    /**
     * Compile all open documents and publish diagnostics.
     */
    public void compileAllAndPublishDiagnostics()
    {
        if (client == null)
        {
            LOGGER.warn("Cannot publish diagnostics: client not set");
            return;
        }

        // Update all documents and compile
        LSPSession.CompilationResult result = session.updateAllAndCompile();

        // Group diagnostics by file
        MutableMap<String, MutableList<Diagnostic>> diagnosticsByFile = Maps.mutable.empty();

        // Initialize with empty lists for all open documents (to clear old diagnostics)
        session.getDocumentManager().getAllDocuments().keysView().forEach(sourceId ->
        {
            String uri = URIUtils.sourceIdToUri(sourceId);
            diagnosticsByFile.put(uri, Lists.mutable.empty());
        });

        if (!result.isSuccess())
        {
            PureException pureException = result.getPureException();
            if (pureException != null)
            {
                String errorSourceId = DiagnosticConverter.getSourceId(pureException);
                if (errorSourceId != null)
                {
                    String errorUri = URIUtils.sourceIdToUri(errorSourceId);
                    MutableList<Diagnostic> fileDiagnostics = diagnosticsByFile.getIfAbsentPut(errorUri, Lists.mutable::empty);
                    fileDiagnostics.add(DiagnosticConverter.toDiagnostic(pureException));
                }
                else
                {
                    // Error without specific file - add to all open documents
                    Diagnostic diagnostic = DiagnosticConverter.toDiagnostic(pureException);
                    diagnosticsByFile.values().forEach(list -> list.add(diagnostic));
                }
            }
        }

        // Publish diagnostics for all files
        diagnosticsByFile.forEachKeyValue(this::publishDiagnostics);
    }

    /**
     * Clear diagnostics for a document.
     */
    public void clearDiagnostics(String uri)
    {
        if (client == null)
        {
            return;
        }
        publishDiagnostics(uri, Lists.mutable.empty());
    }

    private void publishDiagnostics(String uri, MutableList<Diagnostic> diagnostics)
    {
        if (client == null)
        {
            return;
        }

        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.setUri(uri);
        params.setDiagnostics(diagnostics);
        params.setVersion(session.getDocumentManager().getDocumentVersion(uri));

        client.publishDiagnostics(params);
        LOGGER.debug("Published {} diagnostics for {}", diagnostics.size(), uri);
    }
}
