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

package org.finos.legend.engine.pure.repl.core;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ScratchCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ReplSession manages the Pure runtime and function execution environment for the REPL.
 * It provides methods for compiling and executing Pure expressions.
 *
 * Repository loading follows the same two-layer approach as PureIDEServer:
 * 1. Classpath repos via ClassLoaderCodeStorage (ServiceLoader-discovered)
 * 2. Filesystem repos via MutableFSCodeStorage (when a source root is provided)
 * Both are merged via CodeRepositorySet to resolve dependencies correctly.
 */
public class ReplSession implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplSession.class);
    private static final String PURE_OPTION_PREFIX = "pure.option.";

    private final PureRuntime pureRuntime;
    private final FunctionExecution functionExecution;
    private final MutableRepositoryCodeStorage codeStorage;
    private final Message message;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final MutableList<RepositoryCodeStorage> repositories;
    private final Map<String, Boolean> pureRuntimeOptions = new ConcurrentHashMap<>();

    /**
     * Creates a new ReplSession with repositories loaded from the classpath only.
     */
    public ReplSession()
    {
        this(null, null);
    }

    /**
     * Creates a new ReplSession with optional source root and repository filtering.
     *
     * @param sourceRoot the root directory of the legend-engine checkout (enables MutableFSCodeStorage loading),
     *                   or null for classpath-only mode
     * @param requiredRepositories optional list of repository names to load (subset filtering),
     *                             or null to load all discovered repositories
     */
    public ReplSession(String sourceRoot, List<String> requiredRepositories)
    {
        this.message = new Message("");

        // Load pure.option.* system properties (same as PureSession)
        for (String property : System.getProperties().stringPropertyNames())
        {
            if (property.startsWith(PURE_OPTION_PREFIX))
            {
                setPureRuntimeOption(property.substring(PURE_OPTION_PREFIX.length()), Boolean.getBoolean(property));
            }
        }

        this.repositories = getRepositories(sourceRoot, requiredRepositories);
        this.codeStorage = new CompositeCodeStorage(this.repositories.toArray(new RepositoryCodeStorage[0]));

        this.pureRuntime = new PureRuntimeBuilder(this.codeStorage)
                .withMessage(this.message)
                .setUseFastCompiler(true)
                .withOptions(new RuntimeOptions()
                {
                    @Override
                    public boolean isOptionSet(String name)
                    {
                        return getPureRuntimeOption(name);
                    }
                })
                .build();

        this.functionExecution = new FunctionExecutionInterpreted();
        this.functionExecution.init(this.pureRuntime, this.message);
    }

    /**
     * Builds the merged list of code repositories using the same two-layer approach as PureIDEServer.getRepositories().
     *
     * Layer 1: classpath repos discovered via ServiceLoader (CodeRepositoryProviderHelper)
     * Layer 2: filesystem repos from MutableFSCodeStorage (when sourceRoot is provided)
     *
     * Both layers are merged via CodeRepositorySet, then optionally filtered by requiredRepositories.
     */
    private MutableList<RepositoryCodeStorage> getRepositories(String sourceRoot, List<String> requiredRepositories)
    {
        Map<CodeRepository, RepositoryCodeStorage> repoToCodeStorageMap = Maps.mutable.empty();

        // Layer 1: Classpath repos via ServiceLoader
        repoToCodeStorageMap.putAll(CodeRepositoryProviderHelper.findCodeRepositories().toMap(r -> r, ClassLoaderCodeStorage::new));

        // Add scratch repository for REPL expression evaluation (has visibility to all repos)
        CodeRepository scratchRepo = CodeRepository.newScratchCodeRepository();
        repoToCodeStorageMap.put(scratchRepo, new ClassLoaderCodeStorage(scratchRepo));

        // Layer 2: Filesystem repos (when source root is provided)
        if (sourceRoot != null)
        {
            MutableList<RepositoryCodeStorage> fsRepos = buildFSRepositories(sourceRoot);
            repoToCodeStorageMap.putAll(fsRepos.toMap(cs -> cs.getAllRepositories().getOnly(), cs -> cs));
        }

        CodeRepositorySet codeRepositorySet = CodeRepositorySet.newBuilder().withCodeRepositories(repoToCodeStorageMap.keySet()).build();

        // Optional subset filtering
        if (requiredRepositories != null && !requiredRepositories.isEmpty())
        {
            MutableSet<String> requiredSet = Sets.mutable.withAll(requiredRepositories);
            codeRepositorySet = codeRepositorySet.subset(requiredSet);
        }

        return codeRepositorySet.getRepositories().collect(repoToCodeStorageMap::get, Lists.mutable.ofInitialCapacity(codeRepositorySet.size()));
    }

    /**
     * Builds filesystem repositories from the source root, mirroring PureIDELight.buildRepositories().
     * Each module is loaded from its src/main/resources directory using its .definition.json file.
     */
    private MutableList<RepositoryCodeStorage> buildFSRepositories(String sourceRoot)
    {
        MutableList<RepositoryCodeStorage> repos = Lists.mutable.empty();

        // Core modules (same as PureIDELight MINIMUM set + extensions)
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core", ""));
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-unclassified/legend-engine-pure-functions-unclassified-pure", "functions_unclassified"));
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-variant/legend-engine-pure-functions-variant-pure", "functions_variant"));
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-json/legend-engine-pure-functions-json-pure", "functions_json"));
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure", "functions_relation"));
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-standard/legend-engine-pure-functions-standard-pure", "functions_standard"));

        // Relational
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure", "relational"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-test", "relational_test"));

        // Service
        repos.add(buildCore(sourceRoot, "legend-engine-xts-serviceStore/legend-engine-xt-serviceStore-pure", "servicestore"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-service/legend-engine-language-pure-dsl-service-pure", "service"));

        // JSON external format
        repos.add(buildCore(sourceRoot, "legend-engine-xts-json/legend-engine-xt-json-pure", "external_format_json"));

        // Generation
        repos.add(buildCore(sourceRoot, "legend-engine-xts-generation/legend-engine-language-pure-dsl-generation-pure", "generation"));

        // Authentication
        repos.add(buildCore(sourceRoot, "legend-engine-xts-authentication/legend-engine-xt-authentication-pure", "authentication"));

        // Diagram
        repos.add(buildCore(sourceRoot, "legend-engine-xts-diagram/legend-engine-xt-diagram-pure-metamodel", "diagram-metamodel"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-diagram/legend-engine-xt-diagram-pure", "diagram"));

        // Data space
        repos.add(buildCore(sourceRoot, "legend-engine-xts-data-space/legend-engine-xt-data-space-pure-metamodel", "data_space_metamodel"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-data-space/legend-engine-xt-data-space-pure", "data-space"));

        // Flatdata
        repos.add(buildCore(sourceRoot, "legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure", "external-format-flatdata"));

        // SQL
        repos.add(buildCore(sourceRoot, "legend-engine-xts-sql/legend-engine-xt-sql-transformation/legend-engine-xt-sql-pure", "external-query-sql"));

        // Data quality
        repos.add(buildCore(sourceRoot, "legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure", "dataquality"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test", "dataquality_test"));

        // Persistence
        repos.add(buildCore(sourceRoot, "legend-engine-xts-persistence/legend-engine-xt-persistence-pure", "persistence"));

        // Function activator
        repos.add(buildCore(sourceRoot, "legend-engine-xts-functionActivator/legend-engine-xt-functionActivator-pure", "function_activator"));

        // Java platform binding
        repos.add(buildCore(sourceRoot, "legend-engine-xts-java/legend-engine-xt-javaGeneration-pure", "external-language-java"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-java/legend-engine-xt-javaPlatformBinding-pure", "java-platform-binding"));

        // Analytics
        repos.add(buildCore(sourceRoot, "legend-engine-xts-analytics/legend-engine-xts-analytics-mapping/legend-engine-xt-analytics-mapping-pure", "analytics-mapping"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-analytics/legend-engine-xts-analytics-lineage/legend-engine-xt-analytics-lineage-pure", "analytics-lineage"));

        // GraphQL
        repos.add(buildCore(sourceRoot, "legend-engine-xts-graphQL/legend-engine-xt-graphQL-pure", "external-query-graphql"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-graphQL/legend-engine-xt-graphQL-pure-metamodel", "external-query-graphql-metamodel"));

        // Relational DB extensions
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-pure", "relational_duckdb"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-snowflake/legend-engine-xt-relationalStore-snowflake-pure", "relational_snowflake"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-postgres/legend-engine-xt-relationalStore-postgres-pure", "relational_postgres"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-sqlserver/legend-engine-xt-relationalStore-sqlserver-pure", "relational_sqlserver"));

        // Relational store platform binding
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-javaPlatformBinding-pure", "relational-java-platform-binding"));

        // SQL planning / dialect translation
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-sqlPlanning-pure", "external-store-relational-sql-planning"));
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-sqlDialectTranslation-pure", "external-store-relational-sql-dialect-translation"));

        // PCT
        repos.add(buildCore(sourceRoot, "legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-PCT/legend-engine-pure-functions-relationalStore-PCT-pure", "external_test_connection"));

        // Changetoken
        repos.add(buildCore(sourceRoot, "legend-engine-xts-changetoken/legend-engine-xt-changetoken-pure", "pure-changetoken"));

        // Reverse PCT
        repos.add(buildCore(sourceRoot, "legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-reversePCT/legend-engine-pure-code-reversePCT-pure", "reverse-pct"));

        return repos;
    }

    /**
     * Builds a MutableFSCodeStorage for a core module (prepends "core_" to module name).
     * Same as PureIDELight.buildCore().
     */
    private MutableFSCodeStorage buildCore(String sourceRoot, String path, String module)
    {
        return buildFS(sourceRoot, path, module, true);
    }

    /**
     * Builds a MutableFSCodeStorage from a module's definition.json and resource directory.
     * Same as PureIDELight.build().
     */
    private MutableFSCodeStorage buildFS(String sourceRoot, String path, String module, boolean core)
    {
        String fullPath = sourceRoot + "/" + path;
        String resourceDir = fullPath + "/src/main/resources/";
        String moduleName = core ? "".equals(module) ? "core" : ("core_" + module.replace("-", "_")) : module.replace("-", "_");
        GenericCodeRepository repository = GenericCodeRepository.build(Paths.get(resourceDir + moduleName + ".definition.json"));
        return new MutableFSCodeStorage(
                GenericCodeRepository.build(repository.getName(), repository.getAllowedPackagesPattern(), repository.getDependencies().toSet()),
                Paths.get(resourceDir + moduleName)
        );
    }

    /**
     * Initializes the session by loading and compiling all Pure sources.
     * Uses pureRuntime.initialize() (same as PureIDEServer) instead of separate
     * loadAndCompileCore() + loadAndCompileSystem() calls.
     *
     * After initialization, sets up LegendCompileMixedProcessorSupport (same as PureIDELight.postInit()).
     *
     * @throws RuntimeException if compilation fails
     */
    public void initialize()
    {
        if (initialized.compareAndSet(false, true))
        {
            LOGGER.info("Initializing Pure runtime...");
            long startTime = System.currentTimeMillis();

            this.pureRuntime.initialize(new Message("")
            {
                @Override
                public void setMessage(String message)
                {
                    super.setMessage(message);
                    LOGGER.info(message);
                }
            });

            // Set up LegendCompileMixedProcessorSupport (same as PureIDELight.postInit())
            FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.functionExecution;
            fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(
                    fe.getRuntime().getContext(),
                    fe.getRuntime().getModelRepository(),
                    fe.getProcessorSupport()
            ));

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Pure runtime initialized in {}ms", elapsed);
        }
    }

    /**
     * Returns whether the session has been initialized.
     */
    public boolean isInitialized()
    {
        return initialized.get();
    }

    /**
     * Gets the Pure runtime.
     */
    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    /**
     * Gets the function execution engine.
     */
    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    /**
     * Gets the code storage.
     */
    public MutableRepositoryCodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    /**
     * Gets a core instance by path.
     *
     * @param path the Pure path (e.g., "meta::pure::functions::string::toUpper")
     * @return the CoreInstance, or null if not found
     */
    public CoreInstance getCoreInstance(String path)
    {
        return this.pureRuntime.getCoreInstance(path);
    }

    /**
     * Gets a function by signature.
     *
     * @param signature the function signature (e.g., "go():Any[*]")
     * @return the function CoreInstance, or null if not found
     */
    public CoreInstance getFunction(String signature)
    {
        return this.pureRuntime.getFunction(signature);
    }

    /**
     * Recompiles all Pure sources.
     */
    public void reload()
    {
        LOGGER.info("Reloading Pure sources...");
        long startTime = System.currentTimeMillis();

        this.pureRuntime.compile();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Pure sources reloaded in {}ms", elapsed);
    }

    /**
     * Refreshes all mutable sources from disk and incrementally recompiles
     * only the changed ones. Returns the number of sources that were refreshed.
     */
    public int refreshAndCompile()
    {
        LOGGER.info("Refreshing sources from disk and recompiling...");
        long startTime = System.currentTimeMillis();

        int refreshed = 0;
        for (Source source : this.pureRuntime.getSourceRegistry().getSources())
        {
            if (source.refreshContent())
            {
                refreshed++;
            }
        }

        this.pureRuntime.compile();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Incremental recompile completed in {}ms ({} sources refreshed)", elapsed, refreshed);
        return refreshed;
    }

    /**
     * Fully resets and reinitializes the Pure runtime.
     * Picks up all changes including new files.
     */
    public void fullRecompile()
    {
        LOGGER.info("Full recompile of Pure runtime...");
        long startTime = System.currentTimeMillis();

        this.pureRuntime.reset();
        this.pureRuntime.initialize(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                super.setMessage(message);
                LOGGER.info(message);
            }
        });

        // Re-setup LegendCompileMixedProcessorSupport (same as initialize())
        FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.functionExecution;
        fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(
                fe.getRuntime().getContext(),
                fe.getRuntime().getModelRepository(),
                fe.getProcessorSupport()
        ));

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Full recompile completed in {}ms", elapsed);
    }

    /**
     * Gets the current message from the runtime.
     */
    public String getMessage()
    {
        return this.message.getMessage();
    }

    /**
     * Gets information about loaded repositories.
     */
    public MutableList<String> getRepositoryInfo()
    {
        MutableList<String> info = Lists.mutable.empty();
        for (RepositoryCodeStorage repo : this.repositories)
        {
            info.add(repo.getClass().getSimpleName());
        }
        return info;
    }

    /**
     * Gets all repository names.
     */
    public RichIterable<String> getRepositoryNames()
    {
        return this.codeStorage.getAllRepositories().collect(CodeRepository::getName);
    }

    /**
     * Gets the count of loaded repositories.
     */
    public int getRepositoryCount()
    {
        return this.codeStorage.getAllRepositories().size();
    }

    public boolean getPureRuntimeOption(String optionName)
    {
        Boolean value = this.pureRuntimeOptions.get(optionName);
        return value != null && value;
    }

    public void setPureRuntimeOption(String optionName, boolean value)
    {
        this.pureRuntimeOptions.put(optionName, value);
    }

    public Map<String, Boolean> getAllPureRuntimeOptions()
    {
        return this.pureRuntimeOptions;
    }

    @Override
    public void close()
    {
        LOGGER.debug("Closing ReplSession");
    }
}
