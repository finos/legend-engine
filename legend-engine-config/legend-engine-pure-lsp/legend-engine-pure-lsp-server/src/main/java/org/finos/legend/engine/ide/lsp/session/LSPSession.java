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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Adapts PureSession for thread-safe LSP access.
 * Provides synchronized access to PureRuntime for compilation, navigation, and completion.
 */
public class LSPSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPSession.class);

    private final PureSession pureSession;
    private final DocumentManager documentManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path workspaceRoot;

    /**
     * Create a new LSPSession for the given workspace root.
     */
    public LSPSession(Path workspaceRoot)
    {
        this.workspaceRoot = workspaceRoot;
        this.documentManager = new DocumentManager();

        // Initialize code storage with workspace directory and classpath repositories
        MutableList<RepositoryCodeStorage> repos = Lists.mutable.empty();

        // Add filesystem storage for workspace
        if (workspaceRoot != null)
        {
            CodeRepository workspaceRepo = CodeRepository.newPlatformCodeRepository();
            MutableFSCodeStorage fsStorage = new MutableFSCodeStorage(workspaceRepo, workspaceRoot);
            repos.add(fsStorage);
        }

        // Add classpath repositories for Pure standard library
        RichIterable<CodeRepository> codeRepositories = CodeRepositoryProviderHelper.findCodeRepositories();
        repos.add(new ClassLoaderCodeStorage(codeRepositories));

        this.pureSession = new PureSession(null, false, repos);

        LOGGER.info("LSPSession initialized for workspace: {}", workspaceRoot);
    }

    /**
     * Get the document manager for tracking open files.
     */
    public DocumentManager getDocumentManager()
    {
        return documentManager;
    }

    /**
     * Get the Pure runtime.
     */
    public PureRuntime getPureRuntime()
    {
        return pureSession.getPureRuntime();
    }

    /**
     * Get the code storage.
     */
    public MutableRepositoryCodeStorage getCodeStorage()
    {
        return pureSession.getCodeStorage();
    }

    /**
     * Get the processor support.
     */
    public ProcessorSupport getProcessorSupport()
    {
        return getPureRuntime().getProcessorSupport();
    }

    /**
     * Update a source file with new content and optionally compile.
     */
    public CompilationResult updateSource(String sourceId, String content, boolean compile)
    {
        lock.writeLock().lock();
        try
        {
            PureRuntime runtime = getPureRuntime();

            // Load source if not already loaded
            if (runtime.getSourceById(sourceId) == null)
            {
                runtime.loadSourceIfLoadable(sourceId);
            }

            // Modify the source
            runtime.modify(sourceId, content);

            // Compile if requested
            if (compile)
            {
                return compile();
            }

            return CompilationResult.success();
        }
        catch (Exception e)
        {
            LOGGER.error("Error updating source: {}", sourceId, e);
            return CompilationResult.failure(e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Update all open documents and compile.
     */
    public CompilationResult updateAllAndCompile()
    {
        lock.writeLock().lock();
        try
        {
            MutableMap<String, String> documents = documentManager.getAllDocuments();
            PureRuntime runtime = getPureRuntime();

            for (String sourceId : documents.keysView())
            {
                String content = documents.get(sourceId);
                if (runtime.getSourceById(sourceId) == null)
                {
                    runtime.loadSourceIfLoadable(sourceId);
                }
                runtime.modify(sourceId, content);
            }

            return compile();
        }
        catch (Exception e)
        {
            LOGGER.error("Error updating and compiling documents", e);
            return CompilationResult.failure(e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Compile all loaded sources.
     */
    public CompilationResult compile()
    {
        lock.writeLock().lock();
        try
        {
            getPureRuntime().compile();
            return CompilationResult.success();
        }
        catch (PureException e)
        {
            LOGGER.debug("Compilation failed with Pure exception", e);
            return CompilationResult.failure(e);
        }
        catch (Exception e)
        {
            LOGGER.error("Compilation failed with unexpected exception", e);
            return CompilationResult.failure(e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Navigate to the definition at the given position.
     * Returns the CoreInstance found at that position.
     */
    public CoreInstance navigate(String sourceId, int line, int column)
    {
        lock.readLock().lock();
        try
        {
            Source source = getPureRuntime().getSourceById(sourceId);
            if (source == null)
            {
                return null;
            }

            // Ensure source is compiled before navigation
            if (!source.isCompiled())
            {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try
                {
                    if (!source.isCompiled())
                    {
                        try
                        {
                            getPureRuntime().compile();
                        }
                        catch (PureException e)
                        {
                            // Continue even if compilation fails - the source may still be navigable
                            LOGGER.debug("Compilation failed during navigation", e);
                        }
                    }
                }
                finally
                {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            return source.navigate(line, column, getProcessorSupport());
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the source at the given source ID.
     */
    public Source getSource(String sourceId)
    {
        lock.readLock().lock();
        try
        {
            return getPureRuntime().getSourceById(sourceId);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a core instance by user path (e.g., "meta::pure::metamodel::type::Class").
     */
    public CoreInstance getCoreInstance(String path)
    {
        lock.readLock().lock();
        try
        {
            return getPureRuntime().getCoreInstance(path);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the PureSession for direct access when needed.
     */
    public PureSession getPureSession()
    {
        return pureSession;
    }

    /**
     * Represents the result of a compilation attempt.
     */
    public static class CompilationResult
    {
        private final boolean success;
        private final Throwable exception;

        private CompilationResult(boolean success, Throwable exception)
        {
            this.success = success;
            this.exception = exception;
        }

        public static CompilationResult success()
        {
            return new CompilationResult(true, null);
        }

        public static CompilationResult failure(Throwable exception)
        {
            return new CompilationResult(false, exception);
        }

        public boolean isSuccess()
        {
            return success;
        }

        public Throwable getException()
        {
            return exception;
        }

        public PureException getPureException()
        {
            return exception != null ? PureException.findPureException(exception) : null;
        }
    }
}
