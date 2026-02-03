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

package org.finos.legend.engine.ide.lsp.session;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages open documents and their content for the LSP server.
 * Tracks document state and provides content access for the Pure session.
 */
public class DocumentManager
{
    private final ConcurrentHashMap<String, DocumentState> documents = new ConcurrentHashMap<>();

    /**
     * Called when a document is opened.
     */
    public void openDocument(String uri, String text, int version)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        documents.put(sourceId, new DocumentState(uri, text, version));
    }

    /**
     * Called when a document is changed.
     * For incremental sync, applies the changes to the document content.
     * For full sync, replaces the entire content.
     */
    public void changeDocument(String uri, List<TextDocumentContentChangeEvent> changes, int version)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        DocumentState state = documents.get(sourceId);

        if (state == null)
        {
            // Document not tracked - this shouldn't happen in normal operation
            return;
        }

        String content = state.getContent();

        // Apply changes (for full sync, there's just one change with no range)
        for (TextDocumentContentChangeEvent change : changes)
        {
            if (change.getRange() == null)
            {
                // Full document sync
                content = change.getText();
            }
            else
            {
                // Incremental sync - apply the change
                content = applyChange(content, change);
            }
        }

        documents.put(sourceId, new DocumentState(uri, content, version));
    }

    /**
     * Called when a document is closed.
     */
    public void closeDocument(String uri)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        documents.remove(sourceId);
    }

    /**
     * Get the current content of a document.
     */
    public String getDocumentContent(String uri)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        DocumentState state = documents.get(sourceId);
        return state != null ? state.getContent() : null;
    }

    /**
     * Get all open documents as a map of sourceId to content.
     */
    public MutableMap<String, String> getAllDocuments()
    {
        MutableMap<String, String> result = Maps.mutable.empty();
        documents.forEach((sourceId, state) -> result.put(sourceId, state.getContent()));
        return result;
    }

    /**
     * Check if a document is currently open.
     */
    public boolean isDocumentOpen(String uri)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        return documents.containsKey(sourceId);
    }

    /**
     * Get the document version.
     */
    public int getDocumentVersion(String uri)
    {
        String sourceId = URIUtils.uriToSourceId(uri);
        DocumentState state = documents.get(sourceId);
        return state != null ? state.getVersion() : -1;
    }

    /**
     * Apply an incremental change to the document content.
     */
    private String applyChange(String content, TextDocumentContentChangeEvent change)
    {
        int startOffset = getOffset(content, change.getRange().getStart().getLine(), change.getRange().getStart().getCharacter());
        int endOffset = getOffset(content, change.getRange().getEnd().getLine(), change.getRange().getEnd().getCharacter());

        StringBuilder sb = new StringBuilder();
        sb.append(content, 0, startOffset);
        sb.append(change.getText());
        sb.append(content.substring(endOffset));
        return sb.toString();
    }

    /**
     * Convert line and column to absolute offset in the content string.
     * LSP uses 0-based line and column numbers.
     */
    private int getOffset(String content, int line, int character)
    {
        int offset = 0;
        int currentLine = 0;

        while (currentLine < line && offset < content.length())
        {
            if (content.charAt(offset) == '\n')
            {
                currentLine++;
            }
            offset++;
        }

        return Math.min(offset + character, content.length());
    }

    /**
     * Internal class to track document state.
     */
    private static class DocumentState
    {
        private final String uri;
        private final String content;
        private final int version;

        DocumentState(String uri, String content, int version)
        {
            this.uri = uri;
            this.content = content;
            this.version = version;
        }

        String getUri()
        {
            return uri;
        }

        String getContent()
        {
            return content;
        }

        int getVersion()
        {
            return version;
        }
    }
}
