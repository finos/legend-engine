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

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.FSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps between VS Code file URIs and PureRuntime source IDs.
 *
 * PureRuntime source IDs are classpath-relative paths like "/core_relational/tests/model.pure".
 * VS Code sends file URIs like "file:///home/.../src/main/resources/core_relational/tests/model.pure".
 *
 * For files under a standard Maven resources directory, the source ID is the path after
 * "src/main/resources/". For other files, we fall back to checking if PureRuntime already
 * knows a source whose path suffix matches.
 *
 * For reverse lookups (source ID → URI), the mapper first checks its cache, then
 * delegates to a {@link RepositoryScanner} that maps repository names to filesystem roots.
 */
public class UriMapper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UriMapper.class);
    private static final String RESOURCES_MARKER = "/src/main/resources/";

    private final Map<String, String> uriToSourceId = new ConcurrentHashMap<>();
    private final Map<String, String> sourceIdToUri = new ConcurrentHashMap<>();
    private volatile RepositoryScanner repositoryScanner;
    private volatile PureRuntime pureRuntime;

    /**
     * Register a known mapping between a URI and a source ID.
     */
    public void register(String uri, String sourceId)
    {
        this.uriToSourceId.put(uri, sourceId);
        this.sourceIdToUri.put(sourceId, uri);
    }

    /**
     * Convert a file URI to a PureRuntime source ID.
     */
    public String toSourceId(String uri)
    {
        String cached = this.uriToSourceId.get(uri);
        if (cached != null)
        {
            return cached;
        }

        String sourceId = deriveSourceId(uri);
        this.uriToSourceId.put(uri, sourceId);
        this.sourceIdToUri.put(sourceId, uri);
        return sourceId;
    }

    /**
     * Set the repository scanner for resolving unknown source IDs to filesystem paths.
     */
    public void setRepositoryScanner(RepositoryScanner scanner)
    {
        this.repositoryScanner = scanner;
    }

    public void setPureRuntime(PureRuntime runtime)
    {
        this.pureRuntime = runtime;
    }

    /**
     * Convert a PureRuntime source ID back to a file URI.
     * First checks the cache, then falls back to the repository scanner.
     * Returns null if no mapping can be determined.
     */
    public String toUri(String sourceId)
    {
        String cached = this.sourceIdToUri.get(sourceId);
        if (cached != null)
        {
            return cached;
        }

        // Try the alternate form: /foo -> foo, or foo -> /foo
        String alt = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
        cached = this.sourceIdToUri.get(alt);
        if (cached != null)
        {
            // Cache the result under the original key too
            this.sourceIdToUri.put(sourceId, cached);
            return cached;
        }

        // Strategy A: Query PureRuntime directly for the filesystem path.
        // If the source is backed by MutableFSCodeStorage, use FSCodeStorage.getRoot()
        // to construct the real file:// URI. This is the most reliable approach.
        PureRuntime runtime = this.pureRuntime;
        if (runtime != null)
        {
            String fileUri = resolveViaStorage(runtime, sourceId);
            if (fileUri == null)
            {
                fileUri = resolveViaStorage(runtime, alt);
            }
            if (fileUri != null)
            {
                this.sourceIdToUri.put(sourceId, fileUri);
                this.uriToSourceId.put(fileUri, sourceId);
                return fileUri;
            }
        }

        // Strategy B: Fall back to RepositoryScanner path construction
        RepositoryScanner scanner = this.repositoryScanner;
        if (scanner != null)
        {
            String resolved = scanner.resolveToUri(sourceId);
            if (resolved != null)
            {
                this.sourceIdToUri.put(sourceId, resolved);
                this.uriToSourceId.put(resolved, sourceId);
                return resolved;
            }
        }

        // Strategy C: pure:// for JAR-only sources (truly no filesystem equivalent)
        if (sourceId.startsWith("/"))
        {
            String pureUri = "pure://" + sourceId;
            this.sourceIdToUri.put(sourceId, pureUri);
            LOGGER.debug("JAR-only source, using pure:// URI: {}", sourceId);
            return pureUri;
        }

        LOGGER.debug("Cannot resolve source ID to any URI: {}", sourceId);
        return null;
    }

    /**
     * Query PureRuntime's code storage to find the filesystem path for a source.
     * If the source is backed by FSCodeStorage (MutableFS), returns a file:// URI.
     * Returns null for ClassLoader-backed (JAR) sources.
     */
    private String resolveViaStorage(PureRuntime runtime, String sourceId)
    {
        try
        {
            Source source = runtime.getSourceById(sourceId);
            if (source == null)
            {
                return null;
            }

            RepositoryCodeStorage codeStorage = runtime.getCodeStorage();
            if (!(codeStorage instanceof CompositeCodeStorage))
            {
                return null;
            }

            CompositeCodeStorage composite = (CompositeCodeStorage) codeStorage;
            CodeRepository repo = composite.getRepositoryForPath(sourceId);
            if (repo == null)
            {
                return null;
            }

            RepositoryCodeStorage repoStorage = composite.getOriginalCodeStorage(repo);
            if (repoStorage instanceof FSCodeStorage)
            {
                FSCodeStorage fsStorage = (FSCodeStorage) repoStorage;
                Path root = fsStorage.getRoot();
                if (root != null)
                {
                    // Source ID is /<repoName>/path/to/file.pure
                    // Root already IS the repo directory (resources/<repoName>/)
                    // So strip the /<repoName>/ prefix to get the relative path
                    String path = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
                    String repoName = repo.getName();
                    if (repoName != null && path.startsWith(repoName + "/"))
                    {
                        path = path.substring(repoName.length() + 1);
                    }
                    Path fullPath = root.resolve(path);
                    if (java.nio.file.Files.exists(fullPath))
                    {
                        return fullPath.toUri().toString();
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("resolveViaStorage failed for {}: {}", sourceId, e.getMessage());
        }
        return null;
    }

    public void clear()
    {
        this.uriToSourceId.clear();
        this.sourceIdToUri.clear();
    }

    /**
     * Derive a source ID from a file URI using a 3-tier strategy:
     *
     * 1. If the path contains "src/main/resources/", strip to get the repo-relative path.
     *    This handles standard Maven layout files.
     *
     * 2. If a RepositoryScanner is available, check if the file is inside any known
     *    repo's resources directory. This handles files opened from non-standard locations
     *    but still inside a repo tree.
     *
     * 3. Fallback: use just the filename (e.g., "welcome.pure") as an in-memory source ID.
     *    This handles scratch/playground files outside any repository.
     */
    String deriveSourceId(String uri)
    {
        // Handle pure:// scheme explicitly — the path IS the source ID
        if (uri.startsWith("pure://"))
        {
            return uri.substring("pure://".length());
        }

        String path;
        try
        {
            path = URI.create(uri).getPath();
        }
        catch (Exception e)
        {
            path = uri;
        }

        // Strategy 1: src/main/resources/ marker
        int idx = path.indexOf(RESOURCES_MARKER);
        if (idx >= 0)
        {
            return "/" + path.substring(idx + RESOURCES_MARKER.length());
        }

        // Strategy 2: check against known repo resource roots
        RepositoryScanner scanner = this.repositoryScanner;
        if (scanner != null)
        {
            try
            {
                java.nio.file.Path filePath = java.nio.file.Paths.get(path);
                String derived = scanner.deriveSourceIdFromPath(filePath);
                if (derived != null)
                {
                    LspLog.debug("Derived source ID from repo scanner: " + derived);
                    return derived;
                }
            }
            catch (Exception e)
            {
                // Path conversion failed; fall through
            }
        }

        // If the path looks like a Pure source ID (starts with /<repoName>/ where repoName
        // has no slashes and matches a known repo), return it as-is rather than truncating
        // to filename. This prevents creating in-memory duplicates when source IDs are
        // passed where URIs are expected.
        if (path.startsWith("/") && path.endsWith(".pure"))
        {
            // Check if first segment matches a known repo (or looks like one — short, no dots)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash > 1)
            {
                String firstSegment = path.substring(1, secondSlash);
                if (!firstSegment.contains(".") && !firstSegment.contains(" ") && firstSegment.length() < 60)
                {
                    // Looks like a repo name, not a filesystem path segment like "home" or "user"
                    RepositoryScanner repoScanner = this.repositoryScanner;
                    PureRuntime rt = this.pureRuntime;
                    if ((repoScanner != null && repoScanner.getWorkspaceRepoNames().contains(firstSegment))
                            || (rt != null && rt.getSourceById(path) != null))
                    {
                        return path;
                    }
                }
            }
        }

        // Strategy 3: use just the filename as an in-memory source (true scratch files)
        int lastSlash = path.lastIndexOf('/');
        String filename = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
        LspLog.debug("Scratch file (in-memory): " + filename + " (from " + path + ")");
        return filename;
    }
}
