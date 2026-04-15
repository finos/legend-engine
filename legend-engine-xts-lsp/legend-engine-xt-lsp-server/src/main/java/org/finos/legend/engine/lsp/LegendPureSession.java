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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureSession.class);

    private volatile PureRuntime pureRuntime;
    private volatile FunctionExecution functionExecution;
    private volatile boolean initialized;

    private volatile RepositoryScanner workspaceScanner;

    /**
     * Initialize with ClassLoaderCodeStorage only (tests, CI, no workspace).
     */
    public void initialize()
    {
        initialize(null);
    }

    /**
     * Initialize with Option A architecture:
     * - MutableFSCodeStorage for every repo found on disk in the workspace
     * - ClassLoaderCodeStorage ONLY for platform repos (Pure language primitives
     *   that live in legend-pure JARs and are never in the legend-engine filesystem)
     * - No overlap: platform repos are identified by name starting with "platform"
     */
    public void initialize(RepositoryScanner scanner)
    {
        long start = System.currentTimeMillis();
        this.workspaceScanner = scanner;

        MutableList<RepositoryCodeStorage> storages = Lists.mutable.empty();
        java.util.Set<String> workspaceRepoNames = java.util.Collections.emptySet();

        if (scanner != null && !scanner.getMappings().isEmpty())
        {
            // Step 1: Filesystem storage for all workspace repos
            MutableList<RepositoryCodeStorage> workspaceStorages = scanner.buildWorkspaceStorages();
            storages.addAll(workspaceStorages);
            workspaceRepoNames = scanner.getWorkspaceRepoNames();
            LspLog.debug("Loaded " + workspaceStorages.size()
                    + " workspace repos (MutableFS from disk)");
        }

        // Step 2: ClassLoader ONLY for platform repos (language primitives from JARs).
        // Platform repos have names starting with "platform" and live in legend-pure JARs.
        // They never exist in the legend-engine filesystem, so there's no overlap.
        org.eclipse.collections.api.RichIterable<CodeRepository> classpathRepos =
                CodeRepositoryProviderHelper.findCodeRepositories();
        java.util.Set<String> finalWorkspaceNames = workspaceRepoNames;
        MutableList<CodeRepository> platformRepos = Lists.mutable.empty();
        for (CodeRepository repo : classpathRepos)
        {
            String name = repo.getName();
            if (name == null)
            {
                // Welcome/scratch repos (null name) — always via ClassLoader
                platformRepos.add(repo);
            }
            else if (isPlatformRepo(name) && !finalWorkspaceNames.contains(name))
            {
                // Platform repo not on disk — load from JAR
                platformRepos.add(repo);
            }
            else if (!finalWorkspaceNames.contains(name))
            {
                // Non-platform repo not on disk — skip it entirely.
                // It's an extension the developer doesn't have checked out.
                // Better to skip than to load a stale JAR version.
                LspLog.debug("Skipping repo not on disk: " + name);
            }
            // else: repo is on disk (in workspaceRepoNames) — already loaded via MutableFS
        }
        if (!platformRepos.isEmpty())
        {
            storages.add(new ClassLoaderCodeStorage(platformRepos));
            LspLog.debug("Loaded " + platformRepos.size()
                    + " platform repos (ClassLoader from JARs)");
        }

        LOGGER.info("Building PureRuntime with {} storage(s)...", storages.size());
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(storages.toArray(new RepositoryCodeStorage[0]));

        this.pureRuntime = new PureRuntimeBuilder(codeStorage)
                .withMessage(new Message(""))
                .setUseFastCompiler(true)
                .build();

        LOGGER.info("Initializing Pure runtime...");
        this.pureRuntime.initialize(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                super.setMessage(message);
                LOGGER.info(message);
            }
        });

        this.functionExecution = new FunctionExecutionInterpreted();
        this.functionExecution.init(this.pureRuntime, new Message(""));
        LspLog.info("FunctionExecutionInterpreted initialized");

        this.initialized = true;
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        LOGGER.info("Pure runtime initialized in {}s", elapsed);
    }

    public synchronized void reinitialize()
    {
        this.initialized = false;
        this.pureRuntime = null;
        initialize(this.workspaceScanner);
    }

    /**
     * Restore a source to its on-disk state after the editor closes without saving.
     * For storage-backed sources, reloads from disk and recompiles.
     * For in-memory scratch sources, deletes them from the runtime.
     */
    public synchronized void restoreFromDisk(String sourceId)
    {
        if (!this.initialized || sourceId == null)
        {
            return;
        }
        try
        {
            String resolvedId = resolveSourceId(sourceId);
            if (resolvedId == null)
            {
                return;
            }
            org.finos.legend.pure.m3.serialization.runtime.Source source =
                    this.pureRuntime.getSourceById(resolvedId);
            if (source == null)
            {
                return;
            }
            if (source.isImmutable())
            {
                return;
            }
            if (source.isInMemory())
            {
                // Scratch file — delete from runtime
                this.pureRuntime.delete(resolvedId);
                this.pureRuntime.compile();
            }
            else
            {
                // Storage-backed file — reload from disk and recompile
                String diskContent = this.pureRuntime.getCodeStorage()
                        .getContentAsText(resolvedId);
                if (diskContent != null && !diskContent.equals(source.getContent()))
                {
                    this.pureRuntime.modify(resolvedId, diskContent);
                    this.pureRuntime.compile();
                }
            }
        }
        catch (Exception e)
        {
            LspLog.debug("restoreFromDisk failed for " + sourceId + ": " + e.getMessage());
        }
    }

    public synchronized CompileResult modifyAndCompile(String sourceId, String content)
    {
        if (!this.initialized)
        {
            return CompileResult.notReady();
        }
        try
        {
            SourceMutation mutation;
            // Resolve the effective source ID: check both /path and path (with/without leading /)
            String resolvedId = resolveSourceId(sourceId);

            if (resolvedId == null && sourceId.startsWith("/"))
            {
                try
                {
                    this.pureRuntime.loadSourceIfLoadable(sourceId);
                    resolvedId = sourceId;
                }
                catch (Exception ignored)
                {
                    // Source not in any repository
                }
            }

            // If the source ID is a bare filename (strategy 3 fallback from UriMapper),
            // check if a storage source exists with a matching filename suffix.
            // This prevents creating duplicate in-memory sources for files that are
            // already loaded by MutableFSCodeStorage under their full path.
            if (resolvedId == null && !sourceId.startsWith("/") && !sourceId.contains("/"))
            {
                String matchingSuffix = "/" + sourceId;
                for (org.finos.legend.pure.m3.serialization.runtime.Source s :
                        this.pureRuntime.getSourceRegistry().getSources())
                {
                    if (s.getId().endsWith(matchingSuffix))
                    {
                        resolvedId = s.getId();
                        LOGGER.info("Matched bare filename '{}' to storage source '{}'", sourceId, resolvedId);
                        break;
                    }
                }
            }

            if (resolvedId != null)
            {
                // Guard: never modify immutable/platform sources.
                // These are pre-compiled and use bootstrap syntax that the parser can't handle.
                org.finos.legend.pure.m3.serialization.runtime.Source existingSource =
                        this.pureRuntime.getSourceById(resolvedId);
                if (existingSource != null && existingSource.isImmutable())
                {
                    LspLog.debug("Skipping modification of immutable source: " + resolvedId);
                    return CompileResult.success(Collections.emptyList());
                }
                String originalContent = (existingSource != null) ? existingSource.getContent() : null;

                this.pureRuntime.modify(resolvedId, content);
                try
                {
                    mutation = this.pureRuntime.compile();
                }
                catch (Exception compileError)
                {
                    // Restore original content to prevent state pollution to other files.
                    // The error is still reported to the user, but the runtime stays clean.
                    if (originalContent != null)
                    {
                        try
                        {
                            this.pureRuntime.modify(resolvedId, originalContent);
                            this.pureRuntime.compile();
                            LOGGER.info("Restored original content for {} after compile failure", resolvedId);
                        }
                        catch (Exception restoreError)
                        {
                            LOGGER.warn("Failed to restore original content for {}, runtime may be inconsistent",
                                    resolvedId, restoreError);
                        }
                    }
                    throw compileError;
                }
            }
            else
            {
                // New source: create in memory (bare name, no leading /)
                String inMemoryId = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
                mutation = this.pureRuntime.createInMemoryAndCompile(
                        Tuples.pair(inMemoryId, content));
            }
            return CompileResult.success(mutation.getModifiedFiles());
        }
        catch (Exception e)
        {
            return toCompileResult(e);
        }
    }

    /**
     * Apply bulk file changes and compile once. All PureRuntime mutation
     * is serialized through this synchronized method.
     */
    public synchronized CompileResult applyBulkChangesAndCompile(List<FileChange> changes)
    {
        if (!this.initialized)
        {
            return CompileResult.notReady();
        }
        try
        {
            for (FileChange change : changes)
            {
                switch (change.type)
                {
                    case DELETE:
                        if (this.pureRuntime.getSourceById(change.sourceId) != null)
                        {
                            this.pureRuntime.delete(change.sourceId);
                        }
                        break;
                    case CREATE_OR_MODIFY:
                        if (this.pureRuntime.getSourceById(change.sourceId) == null && change.sourceId.startsWith("/"))
                        {
                            try
                            {
                                this.pureRuntime.loadSourceIfLoadable(change.sourceId);
                            }
                            catch (Exception ignored)
                            {
                                // Source not in any repository
                            }
                        }
                        // Skip immutable/platform sources
                        org.finos.legend.pure.m3.serialization.runtime.Source bulkSource =
                                this.pureRuntime.getSourceById(change.sourceId);
                        if (bulkSource != null && bulkSource.isImmutable())
                        {
                            break;
                        }
                        if (bulkSource == null)
                        {
                            this.pureRuntime.createInMemorySource(change.sourceId, change.content);
                        }
                        else
                        {
                            this.pureRuntime.modify(change.sourceId, change.content);
                        }
                        break; // createInMemorySource is batched; compile() runs after the loop
                }
            }
            SourceMutation mutation = this.pureRuntime.compile();
            return CompileResult.success(mutation.getModifiedFiles());
        }
        catch (Exception e)
        {
            return toCompileResult(e);
        }
    }

    /**
     * Execute the go():Any[*] function and return its console output.
     * The function must exist in the currently compiled sources.
     */
    public synchronized ExecuteResult executeGo()
    {
        if (!this.initialized)
        {
            return new ExecuteResult(false, "Runtime not initialized", null);
        }

        try
        {
            // Look up the go() function. Try multiple signatures since the function
            // may be defined with different return types.
            CoreInstance goFunction = this.pureRuntime.getFunction("go():Any[*]");
            if (goFunction == null)
            {
                goFunction = this.pureRuntime.getFunction("go():String[*]");
            }
            if (goFunction == null)
            {
                goFunction = this.pureRuntime.getFunction("go():String[1]");
            }
            if (goFunction == null)
            {
                // Try to find any function named "go" with no parameters
                LOGGER.info("Could not find go() with standard signatures, searching model...");
                CoreInstance goByPath = this.pureRuntime.getCoreInstance("go__Any_MANY_");
                if (goByPath == null)
                {
                    goByPath = this.pureRuntime.getCoreInstance("go__String_MANY_");
                }
                if (goByPath == null)
                {
                    goByPath = this.pureRuntime.getCoreInstance("go__String_1_");
                }
                goFunction = goByPath;
            }
            if (goFunction == null)
            {
                LspLog.info("executeGo: no go() function found");
                return new ExecuteResult(false, "No go() function found in compiled sources. " +
                        "Define: function go():Any[*] { ... }", null);
            }
            LspLog.debug("executeGo: found function " + goFunction.getClassifier().getName()
                    + " at " + (goFunction.getSourceInformation() != null ? goFunction.getSourceInformation().getSourceId() : "unknown"));

            // Capture console output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream capturePrintStream = new PrintStream(baos, true);

            Console console = this.functionExecution.getConsole();
            console.setPrintStream(capturePrintStream);
            console.setConsole(true);

            this.functionExecution.start(goFunction, FastList.newList());

            console.setConsole(false);

            String consoleOutput = baos.toString();
            if (consoleOutput.isEmpty())
            {
                consoleOutput = "(go() returned successfully with no console output. Use print() to see results.)";
            }

            LspLog.debug("executeGo completed, output length: " + consoleOutput.length());
            return new ExecuteResult(true, null, consoleOutput);
        }
        catch (Exception e)
        {
            LOGGER.error("executeGo failed", e);
            LspLog.debug("executeGo failed: " + e.getMessage());
            return new ExecuteResult(false, e.getMessage(), null);
        }
    }

    /**
     * Platform repos are Pure language primitives that live in legend-pure JARs.
     * They are identified by name: "platform" or names starting with "platform_".
     * These never exist in the legend-engine filesystem.
     */
    private static boolean isPlatformRepo(String name)
    {
        return "platform".equals(name) || name.startsWith("platform_");
    }

    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    /**
     * Resolve a source ID to an existing source in the registry.
     * Checks both the ID as-is and with/without leading slash,
     * because storage sources use "/" prefix and in-memory sources don't.
     */
    public String resolveSourceId(String sourceId)
    {
        if (this.pureRuntime.getSourceById(sourceId) != null)
        {
            return sourceId;
        }
        // Try the alternate form: /foo -> foo, or foo -> /foo
        String alt = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
        if (this.pureRuntime.getSourceById(alt) != null)
        {
            return alt;
        }
        return null;
    }

    private static CompileResult toCompileResult(Exception e)
    {
        boolean isInternal = PureException.findPureException(e) == null;
        return CompileResult.error(e, isInternal);
    }

    // -- Inner types --

    public static class ExecuteResult
    {
        private final boolean success;
        private final String error;
        private final String output;

        ExecuteResult(boolean success, String error, String output)
        {
            this.success = success;
            this.error = error;
            this.output = output;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public String getError()
        {
            return this.error;
        }

        public String getOutput()
        {
            return this.output;
        }
    }

    public enum FileChangeType
    {
        CREATE_OR_MODIFY,
        DELETE
    }

    public static class FileChange
    {
        final String sourceId;
        final String content; // null for DELETE
        final FileChangeType type;

        public FileChange(String sourceId, String content, FileChangeType type)
        {
            this.sourceId = sourceId;
            this.content = content;
            this.type = type;
        }
    }

    public static class CompileResult
    {
        private final boolean ready;
        private final boolean success;
        private final boolean internalError;
        private final Exception error;
        private final List<String> modifiedFiles;

        private CompileResult(boolean ready, boolean success, boolean internalError, Exception error, List<String> modifiedFiles)
        {
            this.ready = ready;
            this.success = success;
            this.internalError = internalError;
            this.error = error;
            this.modifiedFiles = modifiedFiles;
        }

        static CompileResult notReady()
        {
            return new CompileResult(false, false, false, null, Collections.emptyList());
        }

        static CompileResult success(Iterable<String> modifiedFiles)
        {
            return new CompileResult(true, true, false, null, Lists.mutable.withAll(modifiedFiles));
        }

        static CompileResult error(Exception e, boolean internal)
        {
            return new CompileResult(true, false, internal, e, Collections.emptyList());
        }

        public boolean isReady()
        {
            return this.ready;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        /** True if the error is an internal bug (NPE, IllegalState), not a compile error. */
        public boolean isInternalError()
        {
            return this.internalError;
        }

        public Exception getError()
        {
            return this.error;
        }

        public List<String> getModifiedFiles()
        {
            return this.modifiedFiles;
        }
    }
}
