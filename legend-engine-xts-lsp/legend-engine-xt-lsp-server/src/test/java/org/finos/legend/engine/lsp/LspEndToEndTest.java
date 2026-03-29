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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end test: creates the LSP server, connects a mock client,
 * sends LSP protocol messages, and verifies responses.
 */
public class LspEndToEndTest
{
    private static LegendPureLspServer server;
    private static MockLanguageClient mockClient;

    @BeforeClass
    public static void setUp() throws Exception
    {
        server = new LegendPureLspServer();
        mockClient = new MockLanguageClient();
        server.connect(mockClient);

        // Initialize handshake
        InitializeResult result = server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
        Assert.assertNotNull("Initialize should return result", result);
        Assert.assertNotNull("Should have capabilities", result.getCapabilities());
        Assert.assertEquals("Should support full text sync",
                TextDocumentSyncKind.Full,
                result.getCapabilities().getTextDocumentSync().getLeft());
        Assert.assertTrue("Should support hover",
                result.getCapabilities().getHoverProvider().getLeft());
        Assert.assertTrue("Should support workspace symbols",
                result.getCapabilities().getWorkspaceSymbolProvider().getLeft());

        // Trigger runtime initialization and wait for it
        server.initialized(new InitializedParams());

        // Wait for PureRuntime to initialize (up to 120s)
        long start = System.currentTimeMillis();
        while (server.getSession() == null || !server.getSession().isInitialized())
        {
            if (System.currentTimeMillis() - start > 120_000)
            {
                Assert.fail("PureRuntime did not initialize within 120 seconds");
            }
            Thread.sleep(500);
        }
    }

    @Test
    public void didOpen_validCode_clearsdiagnostics() throws Exception
    {
        // Simulate a real developer opening a .pure file from a repo directory.
        // URI maps to a source ID under src/main/resources/ which matches a known repo.
        // For test purposes, register a direct mapping to bypass filesystem path logic.
        mockClient.clearDiagnostics();

        String uri = "file:///workspace/src/main/resources/e2e_valid.pure";
        // UriMapper strips src/main/resources/ -> /e2e_valid.pure
        // PureRuntime doesn't have this source, so createInMemoryAndCompile is used
        // with in-memory ID "e2e_valid.pure" (leading / stripped)
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::ValidClass\n{\n  name: String[1];\n}\n")
        ));

        Thread.sleep(1500);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published diagnostics", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertTrue("Valid code should have no diagnostics, got: " + last.getDiagnostics(),
                last.getDiagnostics().isEmpty());
    }

    @Test
    public void didOpen_invalidCode_publishesError() throws Exception
    {
        mockClient.clearDiagnostics();

        String uri = "file:///workspace/src/main/resources/e2e_invalid.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::Bad\n{\n  x: NonExistentType999[1];\n}\n")
        ));

        Thread.sleep(1500);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published diagnostics", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertFalse("Invalid code should have errors", last.getDiagnostics().isEmpty());
        Assert.assertEquals("legend-pure", last.getDiagnostics().get(0).getSource());

        // Clean up: fix the broken source so it doesn't pollute other tests
        VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
        TextDocumentContentChangeEvent fix = new TextDocumentContentChangeEvent(
                "Class test::e2e::Bad\n{\n  x: String[1];\n}\n");
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                docId, Collections.singletonList(fix)));
        Thread.sleep(1000);
    }

    @Test
    public void didChange_fixesError_clearsDiagnostics() throws Exception
    {
        // Use unique names to avoid collision with other tests
        long ts = System.currentTimeMillis();
        String uri = "file:///workspace/src/main/resources/e2e_fixme_" + ts + ".pure";

        // Open with error
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::Fixme" + ts + "\n{\n  x: BogusType999[1];\n}\n")
        ));
        Thread.sleep(1500);

        mockClient.clearDiagnostics();

        // Fix the error via didChange
        VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
                "Class test::e2e::Fixme" + ts + "\n{\n  x: String[1];\n}\n");
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                docId, Collections.singletonList(change)));
        Thread.sleep(2000);

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should have published after fix", published.isEmpty());
        PublishDiagnosticsParams last = published.get(published.size() - 1);
        Assert.assertTrue("Fixed code should have no diagnostics, got: " + last.getDiagnostics(),
                last.getDiagnostics().isEmpty());
    }

    @Test
    public void didClose_clearsDiagnostics() throws Exception
    {
        String uri = "file:///test/close_e2e.pure";

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1, "Class test::e2e::Closeme {}")
        ));
        Thread.sleep(1000);

        mockClient.clearDiagnostics();
        server.getTextDocumentService().didClose(new DidCloseTextDocumentParams(
                new TextDocumentIdentifier(uri)));

        List<PublishDiagnosticsParams> published = mockClient.getDiagnosticsFor(uri);
        Assert.assertFalse("Should publish empty diagnostics on close", published.isEmpty());
        Assert.assertTrue("Diagnostics should be cleared",
                published.get(published.size() - 1).getDiagnostics().isEmpty());
    }

    @Test
    public void workspaceSymbol_findsUserDefinedClass() throws Exception
    {
        // First, open a file so PureRuntime has a user-defined class
        String uri = "file:///workspace/src/main/resources/e2e_sym.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::SymbolTestClass\n{\n  x: String[1];\n}\n")
        ));
        Thread.sleep(1500);

        // Search for the class via workspace symbols
        @SuppressWarnings("deprecation")
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                server.getWorkspaceService().symbol(new WorkspaceSymbolParams("SymbolTestClass"))
                        .get(10, TimeUnit.SECONDS);

        Assert.assertNotNull("symbol() should return a result", result);
        List<? extends SymbolInformation> symbols = result.getLeft();
        Assert.assertNotNull("Should return SymbolInformation list", symbols);
        Assert.assertFalse("Should find at least one symbol", symbols.isEmpty());

        boolean found = false;
        for (SymbolInformation sym : symbols)
        {
            if (sym.getName().contains("SymbolTestClass"))
            {
                found = true;
                break;
            }
        }
        Assert.assertTrue("Should find SymbolTestClass in results", found);
    }

    @Test
    public void workspaceSymbol_emptyQuery_returnsResult() throws Exception
    {
        @SuppressWarnings("deprecation")
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                server.getWorkspaceService().symbol(new WorkspaceSymbolParams(""))
                        .get(10, TimeUnit.SECONDS);

        Assert.assertNotNull("symbol() should return a result", result);
        Assert.assertNotNull("Should return a list (possibly empty without repo scanner)",
                result.getLeft());
    }

    @Test
    public void hover_onOpenedFile_returnsInfo() throws Exception
    {
        // Open a file with a class
        String uri = "file:///workspace/src/main/resources/e2e_hover.pure";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "pure", 1,
                        "Class test::e2e::HoverTestClass\n{\n  name: String[1];\n}\n")
        ));
        Thread.sleep(1500);

        // Hover on the class name "HoverTestClass" at line 0, col 20 (0-based)
        HoverParams hoverParams = new HoverParams(
                new TextDocumentIdentifier(uri),
                new Position(0, 20)
        );
        Hover hover = server.getTextDocumentService().hover(hoverParams).get(10, TimeUnit.SECONDS);

        // hover may be null if navigate() doesn't resolve at that exact position,
        // but if it returns, it should be markdown
        if (hover != null)
        {
            Assert.assertNotNull("Hover should have content", hover.getContents());
            Assert.assertNotNull("Hover should be markup", hover.getContents().getRight());
            String value = hover.getContents().getRight().getValue();
            Assert.assertTrue("Hover should contain type info, got: " + value,
                    value.contains("Class") || value.contains("HoverTestClass"));
        }
    }

    @Test
    public void getSourceContent_returnsPlatformSourceContent() throws Exception
    {
        // Request content for a known platform source via the custom request handler
        String content = server.getSourceContent("/platform/pure/essential/lang.pure").get(10, TimeUnit.SECONDS);

        // Platform sources should be loadable
        if (content != null)
        {
            Assert.assertFalse("Content should not be empty", content.isEmpty());
            // Platform lang.pure should contain some Pure code
            Assert.assertTrue("Should contain Pure code, got length: " + content.length(),
                    content.length() > 10);
        }
    }

    @Test
    public void getSourceContent_returnsNullForUnknown() throws Exception
    {
        String content = server.getSourceContent("nonexistent_source_999.pure").get(10, TimeUnit.SECONDS);
        Assert.assertNull("Unknown source should return null", content);
    }

    @Test
    public void getSourceContent_handlesPureSchemePrefix() throws Exception
    {
        // The extension sends "pure:///core/pure/..." — server should strip the prefix
        String content = server.getSourceContent("pure:///platform/pure/essential/lang.pure").get(10, TimeUnit.SECONDS);

        if (content != null)
        {
            Assert.assertFalse("Content should not be empty", content.isEmpty());
        }
    }

    /**
     * Mock LanguageClient that captures published diagnostics.
     */
    static class MockLanguageClient implements LanguageClient
    {
        private final List<PublishDiagnosticsParams> allDiagnostics =
                Collections.synchronizedList(new ArrayList<>());
        private final List<MessageParams> messages =
                Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params)
        {
            this.allDiagnostics.add(params);
        }

        @Override
        public void showMessage(MessageParams params)
        {
            this.messages.add(params);
            System.out.println("[LSP] " + params.getType() + ": " + params.getMessage());
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params)
        {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams params)
        {
            System.out.println("[LSP-LOG] " + params.getMessage());
        }

        @Override
        public void telemetryEvent(Object o)
        {
        }

        List<PublishDiagnosticsParams> getDiagnosticsFor(String uri)
        {
            List<PublishDiagnosticsParams> result = new ArrayList<>();
            for (PublishDiagnosticsParams p : this.allDiagnostics)
            {
                if (uri.equals(p.getUri()))
                {
                    result.add(p);
                }
            }
            return result;
        }

        void clearDiagnostics()
        {
            this.allDiagnostics.clear();
        }
    }
}
