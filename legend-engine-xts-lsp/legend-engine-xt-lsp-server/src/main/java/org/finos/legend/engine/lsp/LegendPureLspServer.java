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

import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureLspServer implements LanguageServer, LanguageClientAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureLspServer.class);
    private static final String VERSION = "0.3.0-2026-04-01";

    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private LanguageClient client;
    private volatile LegendPureSession session;
    private volatile int recoveryAttempts;
    private final UriMapper uriMapper = new UriMapper();
    private final RepositoryScanner repositoryScanner = new RepositoryScanner();
    private final WorkspaceSymbolProvider symbolProvider = new WorkspaceSymbolProvider();
    private final LegendTextDocumentService textDocumentService;
    private final LegendWorkspaceService workspaceService;
    private volatile List<Path> workspaceRoots = new ArrayList<>();

    public LegendPureLspServer()
    {
        this.textDocumentService = new LegendTextDocumentService(this);
        this.workspaceService = new LegendWorkspaceService(this);
    }

    @Override
    public void connect(LanguageClient client)
    {
        this.client = client;
    }

    LanguageClient getClient()
    {
        return this.client;
    }

    LegendPureSession getSession()
    {
        return this.session;
    }

    UriMapper getUriMapper()
    {
        return this.uriMapper;
    }

    WorkspaceSymbolProvider getSymbolProvider()
    {
        return this.symbolProvider;
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        // Capture workspace roots for repository scanning
        this.workspaceRoots = extractWorkspaceRoots(params);
        LspLog.info("Legend Pure LSP v" + VERSION + " starting");
        LspLog.info("Workspace roots: " + this.workspaceRoots);

        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions(false, java.util.Arrays.asList(":","$",".")));
        caps.setDefinitionProvider(true);
        caps.setReferencesProvider(true);
        caps.setHoverProvider(true);
        caps.setDocumentSymbolProvider(true);
        caps.setWorkspaceSymbolProvider(true);

        SemanticTokensLegend legend = new SemanticTokensLegend(
                SemanticTokensProvider.TOKEN_TYPES,
                SemanticTokensProvider.TOKEN_MODIFIERS);
        SemanticTokensWithRegistrationOptions semanticOpts =
                new SemanticTokensWithRegistrationOptions(legend);
        semanticOpts.setFull(true);
        semanticOpts.setRange(false);
        caps.setSemanticTokensProvider(semanticOpts);
        caps.setExecuteCommandProvider(
                new ExecuteCommandOptions(Arrays.asList(LegendWorkspaceService.CMD_REINDEX)));
        return CompletableFuture.completedFuture(new InitializeResult(caps));
    }

    private static List<Path> extractWorkspaceRoots(InitializeParams params)
    {
        List<Path> roots = new ArrayList<>();

        // Try workspaceFolders first (LSP 3.6+)
        List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null)
        {
            for (WorkspaceFolder folder : folders)
            {
                Path path = uriToPath(folder.getUri());
                if (path != null)
                {
                    roots.add(path);
                }
            }
        }

        // Fall back to rootUri (deprecated but widely used)
        if (roots.isEmpty() && params.getRootUri() != null)
        {
            Path path = uriToPath(params.getRootUri());
            if (path != null)
            {
                roots.add(path);
            }
        }

        return roots;
    }

    private static Path uriToPath(String uri)
    {
        if (uri == null || uri.isEmpty())
        {
            return null;
        }
        try
        {
            return Paths.get(URI.create(uri));
        }
        catch (Exception e)
        {
            LOGGER.warn("Cannot convert URI to path: {}", uri, e);
            return null;
        }
    }

    @Override
    public void initialized(InitializedParams params)
    {
        this.client.showMessage(new MessageParams(MessageType.Info, "Pure LSP v" + VERSION + ": initializing runtime..."));
        CompletableFuture.runAsync(() ->
        {
            try
            {
                // Scan workspace for repository definitions
                if (!this.workspaceRoots.isEmpty())
                {
                    this.repositoryScanner.scan(this.workspaceRoots);
                    this.uriMapper.setRepositoryScanner(this.repositoryScanner);
                    LspLog.info("Mapped " + this.repositoryScanner.getMappings().size()
                            + " repositories to filesystem paths");
                }
                else
                {
                    LspLog.warn("No workspace roots provided; source ID resolution will be limited");
                }

                LspLog.info("Initializing PureRuntime...");
                this.session = new LegendPureSession();
                this.session.initialize(this.repositoryScanner);
                LspLog.info("PureRuntime initialized");

                // Wire UriMapper to PureRuntime for direct storage queries
                this.uriMapper.setPureRuntime(this.session.getPureRuntime());

                // Build the workspace symbol index
                LspLog.info("Building symbol index...");
                this.symbolProvider.buildIndex(this.session.getPureRuntime());

                // Compile any documents that were opened before the session was ready
                this.textDocumentService.compileOpenDocuments();

                this.client.showMessage(new MessageParams(MessageType.Info, "Pure LSP v" + VERSION + ": ready ("
                        + this.repositoryScanner.getMappings().size() + " repos, "
                        + this.symbolProvider.size() + " symbols)"));
                LspLog.info("Pure LSP initialized successfully — "
                        + this.symbolProvider.size() + " symbols indexed");
            }
            catch (Exception e)
            {
                LspLog.error("Pure LSP initialization FAILED: " + e.getMessage());
                e.printStackTrace(System.err);
                this.client.showMessage(new MessageParams(MessageType.Error, "Pure LSP failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Rescan workspace roots for repository definitions. Called during reindex
     * to discover newly added/removed repos since startup.
     */
    void rescanWorkspaceRoots()
    {
        this.repositoryScanner.clear();
        if (!this.workspaceRoots.isEmpty())
        {
            this.repositoryScanner.scan(this.workspaceRoots);
            this.uriMapper.setRepositoryScanner(this.repositoryScanner);
        }
    }

    /**
     * Automatic recovery: reinitialize PureRuntime on a background thread
     * after an internal error (NPE, IllegalState, etc.).
     */
    void triggerRecovery()
    {
        if (this.recoveryAttempts >= MAX_RECOVERY_ATTEMPTS)
        {
            LOGGER.error("Max recovery attempts ({}) reached. Manual restart required.", MAX_RECOVERY_ATTEMPTS);
            this.client.showMessage(new MessageParams(MessageType.Error,
                    "Pure LSP: recovery failed " + MAX_RECOVERY_ATTEMPTS + " times. Please restart."));
            return;
        }
        this.recoveryAttempts++;

        CompletableFuture.runAsync(() ->
        {
            try
            {
                LOGGER.warn("Triggering automatic recovery (attempt {}/{})...", this.recoveryAttempts, MAX_RECOVERY_ATTEMPTS);
                this.uriMapper.clear();
                this.repositoryScanner.clear();
                if (!this.workspaceRoots.isEmpty())
                {
                    this.repositoryScanner.scan(this.workspaceRoots);
                    this.uriMapper.setRepositoryScanner(this.repositoryScanner);
                }
                if (this.session != null)
                {
                    this.session.reinitialize();
                    // Bug fix: rewire UriMapper to new PureRuntime after reinitialize
                    this.uriMapper.setPureRuntime(this.session.getPureRuntime());
                    // Rebuild symbol index
                    this.symbolProvider.buildIndex(this.session.getPureRuntime());
                    // Bug fix: replay unsaved editor buffers so the runtime
                    // matches what the user sees on screen
                    this.textDocumentService.compileOpenDocuments();
                }
                this.recoveryAttempts = 0;
                this.client.showMessage(new MessageParams(MessageType.Info, "Pure LSP: recovered"));
            }
            catch (Exception e)
            {
                LOGGER.error("Recovery failed", e);
                this.client.showMessage(new MessageParams(MessageType.Error, "Pure LSP recovery failed: " + e.getMessage()));
            }
        });
    }

    @Override
    public void setTrace(SetTraceParams params)
    {
        // No-op: VS Code sends $/setTrace on every connection; the default
        // LSP4J implementation throws UnsupportedOperationException.
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        this.textDocumentService.shutdown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit()
    {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService()
    {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService()
    {
        return this.workspaceService;
    }

    /**
     * Custom request: get children of a Pure package for the tree view.
     * Pass "" or "::" for root. Returns a list of PackageChildInfo objects.
     */
    @JsonRequest("legend/getPackageChildren")
    public CompletableFuture<List<PackageChildInfo>> getPackageChildren(String packagePath)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LegendPureSession s = this.session;
            if (s == null || !s.isInitialized())
            {
                return Collections.<PackageChildInfo>emptyList();
            }

            synchronized (s)
            {
                return PackageTreeProvider.getChildren(
                        s.getPureRuntime(), this.uriMapper, packagePath);
            }
        });
    }

    /**
     * Custom request: execute the go():Any[*] function and return the result.
     */
    @JsonRequest("legend/executeGo")
    public CompletableFuture<ExecuteGoResult> executeGo()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LegendPureSession s = this.session;
            if (s == null || !s.isInitialized())
            {
                return new ExecuteGoResult(false, "Runtime not initialized", null);
            }

            synchronized (s)
            {
                LegendPureSession.ExecuteResult result = s.executeGo();
                return new ExecuteGoResult(result.isSuccess(), result.getError(), result.getOutput());
            }
        });
    }

    /**
     * Custom request: return the content of a Pure source by its source ID.
     * Used by the VS Code extension's FileSystemProvider for pure:// URIs.
     */
    @JsonRequest("legend/getSourceContent")
    public CompletableFuture<String> getSourceContent(String sourceId)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            LegendPureSession s = this.session;
            if (s == null || !s.isInitialized())
            {
                return null;
            }

            // Normalize: pure:///core/... → /core/...
            String id = sourceId;
            if (id.startsWith("pure://"))
            {
                id = id.substring("pure://".length());
            }

            String resolvedId = s.resolveSourceId(id);
            if (resolvedId == null)
            {
                LspLog.debug("getSourceContent: unknown source ID: " + sourceId);
                return null;
            }

            Source source = s.getPureRuntime().getSourceById(resolvedId);
            if (source == null)
            {
                return null;
            }

            LspLog.debug("getSourceContent: serving " + resolvedId + " (" + source.getContent().length() + " chars)");
            return source.getContent();
        });
    }

    // -- DTO for executeGo response --

    public static class ExecuteGoResult
    {
        private boolean success;
        private String error;
        private String output;

        public ExecuteGoResult()
        {
        }

        ExecuteGoResult(boolean success, String error, String output)
        {
            this.success = success;
            this.error = error;
            this.output = output;
        }

        public boolean isSuccess()
        {
            return success;
        }

        public void setSuccess(boolean success)
        {
            this.success = success;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }

        public String getOutput()
        {
            return output;
        }

        public void setOutput(String output)
        {
            this.output = output;
        }
    }

    public static void main(String[] args) throws Exception
    {
        // Capture stdout BEFORE any library code (e.g., SLF4J) can write to it,
        // then redirect System.out to stderr so only JSON-RPC goes over stdout.
        PrintStream originalOut = new PrintStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(java.io.FileDescriptor.out)), true);
        PrintStream stderrOut = new PrintStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(java.io.FileDescriptor.err)), true);
        System.setOut(stderrOut);
        System.setErr(stderrOut);

        // Log which JAR we're running from, so misconfigurations are visible.
        java.security.CodeSource cs = LegendPureLspServer.class.getProtectionDomain().getCodeSource();
        String jarLocation = cs != null ? cs.getLocation().toString() : "unknown";
        System.err.println("[LSP] Running from: " + jarLocation);

        // Eagerly load critical classes at startup. The JVM caches class-init
        // failures permanently (NoClassDefFoundError is sticky), so if Gson or
        // a provider class fails to load lazily on a concurrent request thread,
        // ALL subsequent uses of that class fail for the lifetime of the process.
        // Loading them here on the main thread ensures any failure is immediate
        // and visible, and that the classes are ready before requests arrive.
        try
        {
            // Dependency libraries
            Class.forName("com.google.gson.Gson");
            Class.forName("com.google.gson.internal.bind.NumberTypeAdapter");
            Class.forName("org.eclipse.collections.impl.block.procedure.MinComparatorProcedure");
            Class.forName("org.eclipse.lsp4j.adapters.SymbolInformationTypeAdapter");
            // LSP provider classes
            Class.forName("org.finos.legend.engine.lsp.HoverProvider");
            Class.forName("org.finos.legend.engine.lsp.NavigationProvider");
            Class.forName("org.finos.legend.engine.lsp.ReferencesProvider");
            Class.forName("org.finos.legend.engine.lsp.SemanticTokensProvider");
            Class.forName("org.finos.legend.engine.lsp.DocumentOutlineProvider");
            Class.forName("org.finos.legend.engine.lsp.PackageTreeProvider");
            Class.forName("org.finos.legend.engine.lsp.WorkspaceSymbolProvider");
            Class.forName("org.finos.legend.engine.lsp.CompletionProvider");
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("[LSP-ERROR] Critical class not found: " + e.getMessage()
                    + ". Are you running the shaded *-server.jar?");
            System.exit(1);
        }

        LegendPureLspServer server = new LegendPureLspServer();
        org.eclipse.lsp4j.jsonrpc.Launcher<LanguageClient> launcher =
                LSPLauncher.createServerLauncher(server, System.in, originalOut);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
