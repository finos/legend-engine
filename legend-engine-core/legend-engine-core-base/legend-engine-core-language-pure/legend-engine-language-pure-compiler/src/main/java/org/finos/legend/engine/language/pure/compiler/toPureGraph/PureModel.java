// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.MetadataWrapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.UserDefinedFunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.AssociationValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.ClassValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.EnumerationValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.FunctionValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.ProfileValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.PureModelContextDataValidator;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.pure.generated.Package_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_FunctionType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_PrimitiveType_LazyImpl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.PackageableMultiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.tools.ConcurrentHashSet;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PureModel implements IPureModel
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureModel.class);
    private static final ImmutableSet<String> RESERVED_PACKAGES = Sets.immutable.with("$implicit");

    public static final MetadataLazy METADATA_LAZY = MetadataLazy.fromClassLoader(PureModel.class.getClassLoader(), CodeRepositoryProviderHelper.findCodeRepositories(PureModel.class.getClassLoader(), true).collectIf(r -> !r.getName().startsWith("test_") && !r.getName().startsWith("other_"), CodeRepository::getName));

    private final CompiledExecutionSupport executionSupport;
    private final DeploymentMode deploymentMode;
    private final PureModelProcessParameter pureModelProcessParameter;
    private final org.finos.legend.pure.m3.coreinstance.Package root = new Package_Impl(M3Paths.Root)._name(M3Paths.Root);
    // NOTE: since we have states within each extension, we have to keep extensions local to `PureModel` rather than having
    // this as part of `CompileContext`
    final CompilerExtensions extensions;

    private final MutableList<Warning> warnings = Lists.mutable.empty();

    final Handlers handlers;

    private final MutableSet<String> immutables;
    private final MutableMap<String, Multiplicity> multiplicitiesIndex;
    private final MutableMap<String, Section> sectionsIndex;
    final MutableMap<String, Type> typesIndex;
    final MutableMap<String, GenericType> typesGenericTypeIndex;
    private final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> packageableElementsIndex;
    private final ConcurrentLinkedQueue<EngineException> engineExceptions = new ConcurrentLinkedQueue<>();

    public PureModel(PureModelContextData pure, String user, DeploymentMode deploymentMode)
    {
        this(pure, user, null, deploymentMode, new PureModelProcessParameter(), null);
    }

    public PureModel(PureModelContextData pure, String user, DeploymentMode deploymentMode, PureModelProcessParameter pureModelProcessParameter, Metadata metaData)
    {
        this(pure, user, null, deploymentMode, pureModelProcessParameter, metaData);
    }

    public PureModel(PureModelContextData pure, String user, ClassLoader classLoader, DeploymentMode deploymentMode)
    {
        this(pure, user, classLoader, deploymentMode, new PureModelProcessParameter(), null);
    }

    public PureModel(PureModelContextData pureModelContextData, String user, ClassLoader classLoader, DeploymentMode deploymentMode, PureModelProcessParameter pureModelProcessParameter, Metadata metaData)
    {
        this(pureModelContextData, CompilerExtensions.fromAvailableExtensions(), user, classLoader, deploymentMode, pureModelProcessParameter, metaData);
    }

    public PureModel(PureModelContextData pureModelContextData, CompilerExtensions extensions, String user, ClassLoader classLoader, DeploymentMode deploymentMode, PureModelProcessParameter pureModelProcessParameter, Metadata metaData)
    {
        user = user == null ? Identity.getAnonymousIdentity().getName() : user;
        long start = System.nanoTime();

        if (classLoader == null)
        {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        this.extensions = extensions;
        this.deploymentMode = deploymentMode;
        this.pureModelProcessParameter = pureModelProcessParameter;
        Span span = GlobalTracer.get().buildSpan("Build Pure Model").start();
        try (Scope ignore = GlobalTracer.get().scopeManager().activate(span))
        {
            ConsoleCompiled console = new ConsoleCompiled();
            console.disable();
            this.executionSupport = new CompiledExecutionSupport(
                    new JavaCompilerState(null, classLoader),
                    new CompiledProcessorSupport(classLoader, metaData == null ? new MetadataWrapper(this.root, METADATA_LAZY, this) : metaData, Sets.mutable.empty()),
                    null,
                    new CompositeCodeStorage(
                            new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findCodeRepositories().select(CodeRepositoryProviderHelper.platformAndCore)),
                            //TODO eventually remove, keep system so that we can get latestVersion in Alloy.
                            new VersionControlledClassLoaderCodeStorage(classLoader, new GenericCodeRepository("system", ""), null)),
                    null,
                    null,
                    console,
                    null,
                    Sets.mutable.empty(),
                    CompiledExtensionLoader.extensions()
            );

            ForkJoinPool forkJoinPool = pureModelProcessParameter.getForkJoinPool();
            if (forkJoinPool == null)
            {
                this.immutables = Sets.mutable.empty();
                this.multiplicitiesIndex = Maps.mutable.empty();
                this.sectionsIndex = Maps.mutable.empty();
                this.typesIndex = Maps.mutable.empty();
                this.typesGenericTypeIndex = Maps.mutable.empty();
                this.packageableElementsIndex = Maps.mutable.empty();
            }
            else
            {
                this.immutables = new ConcurrentHashSet<>();
                this.multiplicitiesIndex = new ConcurrentHashMap<>();
                this.sectionsIndex = new ConcurrentHashMap<>();
                this.typesIndex = new ConcurrentHashMap<>();
                this.typesGenericTypeIndex = new ConcurrentHashMap<>();
                this.packageableElementsIndex = new ConcurrentHashMap<>();
            }
            this.typesIndex.put("Package", this.executionSupport.getMetadataAccessor().getClass("Package"));
            this.immutables.add("Package");
            modifyRootClassifier();

            registerElementsForPathToElement();
            long preInitEnd = System.nanoTime();

            LOGGER.info("{}", new LogInfo(user, "GRAPH_START", (pureModelContextData.origin == null || pureModelContextData.origin.sdlcInfo == null) ? "" : pureModelContextData.origin.sdlcInfo.packageableElementPointers, nanosDurationToMillis(start, preInitEnd)));
            span.log("GRAPH_START");

            long initStart = System.nanoTime();
            this.handlers = new Handlers(this);
            initializeMultiplicities();
            initializePrimitiveTypes();
            long initEnd = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_INITIALIZED", nanosDurationToMillis(initStart, initEnd)));
            span.log("GRAPH_INITIALIZED");

            // Pre Validation
            long preValidationStart = System.nanoTime();
            new PureModelContextDataValidator().validate(this, pureModelContextData);
            long preValidationEnd = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_PRE_VALIDATION_COMPLETED", nanosDurationToMillis(preValidationStart, preValidationEnd)));
            span.log("GRAPH_PRE_VALIDATION_COMPLETED");

            // Processing
            long indexStart = System.nanoTime();
            PureModelContextDataIndex pureModelContextDataIndex = index(pureModelContextData);
            long indexEnd = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_INDEX_INPUT", pureModelContextDataIndex, nanosDurationToMillis(indexStart, indexEnd)));
            span.log("GRAPH_INDEX_INPUT");

            List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> elements = pureModelContextData.getElements();
            FastListMultimap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> classToElements =
                    ListIterate.groupBy(elements, x ->
                    {
                        Processor<?> extraProcessor = this.extensions.getExtraProcessor(x);
                        if (extraProcessor != null)
                        {
                            return extraProcessor.getElementClass();
                        }
                        return x.getClass();
                    });

            Runnable runPasses = () ->
            {
                MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> sectionIndices = classToElements.removeAll(SectionIndex.class);
                sectionIndices.forEach(sectionIndex -> ((SectionIndex) sectionIndex).sections.forEach(section -> section.elements.forEach(elementPath -> this.sectionsIndex.putIfAbsent(elementPath, section))));
                this.maybeParallel(Stream.concat(sectionIndices.stream(), classToElements.removeAll(Profile.class).stream()))
                        .forEach(handleEngineExceptions(this::processFirstPass));

                MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, Collection<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>>> dependencyGraph = Maps.mutable.empty();
                dependencyGraph.put(Class.class, Lists.fixedSize.with(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure.class));
                dependencyGraph.put(Association.class, Lists.fixedSize.with(Class.class));
                dependencyGraph.put(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, Lists.fixedSize.with(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration.class, Class.class, Association.class, Binding.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store.class));
                dependencyGraph.put(PackageableConnection.class, Lists.fixedSize.with(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class));
                dependencyGraph.put(PackageableRuntime.class, Lists.fixedSize.with(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, PackageableConnection.class));
                dependencyGraph.put(Function.class, Lists.fixedSize.with(DataElement.class, Class.class, Association.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, Binding.class));
                this.extensions.getExtraProcessors().forEach(x -> dependencyGraph.put(x.getElementClass(), (Collection<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>>) x.getPrerequisiteClasses()));

                DependencyManagement dependencyManagement = new DependencyManagement(dependencyGraph);
                MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, Collection<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>>> dependentToDependencies = dependencyManagement.getDependentToDependencies();
                dependencyManagement.detectCircularDependency();
                MutableSet<MutableSet<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>>> disjointDependencyGraphs = dependencyManagement.getDisjointDependencyGraphs();
                this.maybeParallel(disjointDependencyGraphs.stream()).forEach(disjointDependencyGraph ->
                {
                    processPass("firstPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processFirstPass), disjointDependencyGraph);
                    processPass("secondPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processSecondPass), disjointDependencyGraph);
                    processPass("thirdPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processThirdPass), disjointDependencyGraph);
                    processPass("fourthPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processFourthPass), disjointDependencyGraph);
                    processPass("fifthPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processFifthPass), disjointDependencyGraph);
                    processPass("sixthPass", classToElements, dependentToDependencies, handleEngineExceptions(this::processSixthPass), disjointDependencyGraph);
                });
            };

            if (forkJoinPool != null)
            {
                CompletableFuture<Void> pureModelCompilationFuture = CompletableFuture.runAsync(runPasses, forkJoinPool);
                try
                {
                    pureModelCompilationFuture.get(10, TimeUnit.MINUTES);
                }
                catch (InterruptedException | TimeoutException e)
                {
                    String threadDump = Stream.of(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true))
                            .map(ThreadInfo::toString)
                            .collect(Collectors.joining("\n\t\t"));
                    throw new RuntimeException("Failure while waiting for compiler to finish.\n\nPool state: " + forkJoinPool + "\n\nThread Dump: " + threadDump, e);
                }
                catch (ExecutionException | CompletionException e)
                {
                    Throwable cause = e.getCause();
                    if (cause instanceof Error)
                    {
                        throw (Error) cause;
                    }
                    else if (cause instanceof RuntimeException)
                    {
                        throw (RuntimeException) cause;
                    }
                    else
                    {
                        throw new RuntimeException(cause);
                    }
                }
            }
            else
            {
                runPasses.run();
            }


            // Post Validation
            long postValidationStart = System.nanoTime();
            try
            {
                processPostValidation(pureModelContextData, extensions);
            }
            catch (EngineException e)
            {
                if (this.pureModelProcessParameter.getEnablePartialCompilation())
                {
                    engineExceptions.add(e);
                }
                else
                {
                    throw e;
                }
            }
            long postValidationEnd = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_POST_VALIDATION_COMPLETED", nanosDurationToMillis(postValidationStart, postValidationEnd)));
            span.log("GRAPH_POST_VALIDATION_COMPLETED");

            long end = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_STOP", nanosDurationToMillis(start, end)));
            span.log("GRAPH_STOP");
        }
        catch (Exception e)
        {
            long end = System.nanoTime();
            LOGGER.info("{}", new LogInfo(user, "GRAPH_ERROR", e, nanosDurationToMillis(start, end)));
            span.log("GRAPH_ERROR");
            // TODO: we need to have a better strategy to throw compilation error instead of the generic exception
            throw e;
        }
        finally
        {
            span.finish();
        }
    }

    private <T> Stream<T> maybeParallel(Stream<T> stream)
    {
        if (Thread.currentThread() instanceof ForkJoinWorkerThread)
        {
            return stream.parallel();
        }
        else
        {
            return stream.sequential();
        }
    }

    private void processPostValidation(PureModelContextData pureModelContextData, CompilerExtensions extensions)
    {
        new ProfileValidator().validate(this, pureModelContextData);
        new EnumerationValidator().validate(this, pureModelContextData);
        new ClassValidator().validate(this, pureModelContextData);
        new AssociationValidator().validate(this, pureModelContextData);
        new FunctionValidator().validate(getContext(), pureModelContextData);
        new org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.MappingValidator().validate(this, pureModelContextData, extensions);
        this.extensions.getExtraPostValidators().forEach(validator -> validator.value(this, pureModelContextData));
    }

    public static PureModel getCorePureModel()
    {
        return new PureModel(PureModelContextData.newBuilder().build(), CompilerExtensions.fromExtensions(Lists.mutable.empty()), Identity.getAnonymousIdentity().getName(), null, null, new PureModelProcessParameter(), null);
    }

    private void modifyRootClassifier()
    {
        try
        {
            Field f = Package_Impl.class.getDeclaredField("classifier");
            f.setAccessible(true);
            f.set(this.root, this.typesIndex.get("Package"));
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Package getRoot()
    {
        return root;
    }

    public synchronized void addWarnings(Iterable<Warning> warnings)
    {
        this.warnings.addAllIterable(warnings);
    }

    public MutableList<Warning> getWarnings()
    {
        return this.warnings;
    }

    // ------------------------------------------ INITIALIZATION -----------------------------------------

    /**
     * This method add elements from packages that belong to METADATA LAZY root to the packages that belong to this graph (PureModel) root
     * as this is needed for `pathToElement` to work on older graph
     */
    private void registerElementsForPathToElement()
    {
        registerElementForPathToElement("meta::pure::mapping::modelToModel::contract", Lists.mutable.with(
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionNode_1_",
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_1__Runtime_1__ExecutionContext_1__Extension_MANY__DebugContext_1__Result_1_",
                "getterOverrideMapped_Any_1__PropertyMapping_1__Any_MANY_",
                "getterOverrideNonMapped_Any_1__Property_1__Any_MANY_"
        ));
        registerElementForPathToElement("meta::pure::mapping::aggregationAware::contract", Lists.mutable.with(
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionNode_1_",
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_1__Runtime_1__ExecutionContext_1__Extension_MANY__DebugContext_1__Result_1_"
        ));
        registerElementForPathToElement("meta::protocols::pure::vX_X_X::invocation::execution::execute", Lists.mutable.with(
                "alloyExecute_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_$0_1$__String_1__Integer_1__String_1__String_1__Extension_MANY__Result_1_",
                "executePlan_ExecutionPlan_1__String_1__Integer_1__Extension_MANY__String_1_"
        ));
        registerElementForPathToElement("meta::pure::tds", Lists.mutable.with(
                "TDSRow"
        ));
        this.extensions.getExtraElementForPathToElementRegisters().forEach(register -> register.value(this::registerElementForPathToElement));
    }

    private void registerElementForPathToElement(String pack, List<String> children)
    {
        try (AutoCloseableLock ignored = this.pureModelProcessParameter.writeLock())
        {
            org.finos.legend.pure.m3.coreinstance.Package newPkg = getOrCreatePackage(root, pack);
            org.finos.legend.pure.m3.coreinstance.Package oldPkg = getPackage((org.finos.legend.pure.m3.coreinstance.Package) METADATA_LAZY.getMetadata(M3Paths.Package, M3Paths.Root), pack);
            for (String child : children)
            {
                // allow duplicated registration, but only the first one will actually get registered
                if (newPkg._children().detect(c -> child.equals(c._name())) == null)
                {
                    newPkg._childrenAdd(Objects.requireNonNull(oldPkg._children().detect(c -> child.equals(c._name())), "Can't find child element '" + child + "' in package '" + pack + "' for path registration"));
                }
            }
        }
    }

    private void initializeMultiplicities()
    {
        this.multiplicitiesIndex.put("zero", (PackageableMultiplicity) this.executionSupport.getMetadata(M3Paths.PackageableMultiplicity, "Root::meta::pure::metamodel::multiplicity::PureZero"));
        this.multiplicitiesIndex.put("one", (PackageableMultiplicity) this.executionSupport.getMetadata(M3Paths.PackageableMultiplicity, "Root::meta::pure::metamodel::multiplicity::PureOne"));
        this.multiplicitiesIndex.put("zeroone", (PackageableMultiplicity) this.executionSupport.getMetadata(M3Paths.PackageableMultiplicity, "Root::meta::pure::metamodel::multiplicity::ZeroOne"));
        this.multiplicitiesIndex.put("onemany", (PackageableMultiplicity) this.executionSupport.getMetadata(M3Paths.PackageableMultiplicity, "Root::meta::pure::metamodel::multiplicity::OneMany"));
        this.multiplicitiesIndex.put("zeromany", (PackageableMultiplicity) this.executionSupport.getMetadata(M3Paths.PackageableMultiplicity, "Root::meta::pure::metamodel::multiplicity::ZeroMany"));
    }

    private void initializePrimitiveTypes()
    {
        MetadataAccessor metadataAccessor = this.executionSupport.getMetadataAccessor();
        ModelRepository.PRIMITIVE_TYPE_NAMES.newWith(M3Paths.Number).forEach(typeName ->
        {
            this.typesIndex.put(typeName, metadataAccessor.getPrimitiveType(typeName));
            this.immutables.add(typeName);
        });

        // This is added to support legacy Binary primitive type usages
        this.typesIndex.put("Binary", metadataAccessor.getPrimitiveType(ModelRepository.BYTE_TYPE_NAME));
        this.immutables.add("Binary");
    }


    // ------------------------------------------ LOADER -----------------------------------------

    private Consumer<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> handleEngineExceptions(Consumer<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> passConsumer)
    {
        if (this.pureModelProcessParameter.getEnablePartialCompilation())
        {
            return x ->
            {
                try
                {
                    passConsumer.accept(x);
                }
                catch (EngineException e)
                {
                    engineExceptions.add(e);
                }
            };
        }
        return passConsumer;
    }

    private void processPass(String name, FastListMultimap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> classToElements, MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, Collection<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>>> dependentToDependencies, Consumer<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> passConsumer, MutableSet<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>> disjointDependencyGraph)
    {
        MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>, CompletableFuture<Void>> tracker = Maps.mutable.empty();
        disjointDependencyGraph.forEach(dependent -> tracker.put(dependent, new CompletableFuture<>()));
        this.maybeParallel(disjointDependencyGraph.stream()).forEach(dependent ->
        {
            MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> elementsToCompile = classToElements.get(dependent);

            CompletableFuture<Void> allDependencyFutures = CompletableFuture.allOf(dependentToDependencies.get(dependent)
                    .stream()
                    .map(dependency -> Objects.requireNonNull(tracker.get(dependency), "Prerequisites are defined incorrectly."))
                    .toArray(CompletableFuture[]::new));
            allDependencyFutures.thenRun(() ->
                    {
                        try
                        {
                            this.maybeParallel(elementsToCompile.stream()).forEach(passConsumer);
                            tracker.get(dependent).complete(null);
                            LOGGER.debug("{} - Completed {}", name, dependent);
                        }
                        catch (Exception e)
                        {
                            tracker.get(dependent).completeExceptionally(e);
                            LOGGER.debug("{} - Completed {}", name, dependent, e);
                        }
                    })
                    .exceptionally(e ->
                    {
                        tracker.get(dependent).completeExceptionally(e);
                        LOGGER.debug("{} - Completed {}", name, dependent, e);
                        return null;
                    });
        });
        try
        {
            CompletableFuture.allOf(tracker.values().toArray(new CompletableFuture[0])).join();
        }
        catch (CompletionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            else if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            else
            {
                throw new RuntimeException(cause);
            }
        }
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement processFirstPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return visitWithErrorHandling(element, new PackageableElementFirstPassBuilder(getContext(element)));
    }

    private void processSecondPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        visitWithErrorHandling(element, new PackageableElementSecondPassBuilder(getContext(element)));
    }

    private void processThirdPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        visitWithErrorHandling(element, new PackageableElementThirdPassBuilder(getContext(element)));
    }

    private void processFourthPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        visitWithErrorHandling(element, new PackageableElementFourthPassBuilder(getContext(element)));
    }

    private void processFifthPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        visitWithErrorHandling(element, new PackageableElementFifthPassBuilder(getContext(element)));
    }

    private void processSixthPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        visitWithErrorHandling(element, new PackageableElementSixthPassBuilder(getContext(element)));
    }

    private <T> T visitWithErrorHandling(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element, PackageableElementVisitor<T> visitor)
    {
        try
        {
            return element.accept(visitor);
        }
        catch (Exception e)
        {
            if (e instanceof EngineException)
            {
                SourceInformation sourceInformation = ((EngineException) e).getSourceInformation();
                if ((sourceInformation != null) && (sourceInformation != SourceInformation.getUnknownSourceInformation()))
                {
                    throw e;
                }
            }
            StringBuilder builder = new StringBuilder("Error in '").append(element.getPath()).append("'");
            String message = e.getMessage();
            if (message != null)
            {
                builder.append(": ").append(message);
            }
            throw new EngineException(builder.toString(), (element.sourceInformation == null) ? SourceInformation.getUnknownSourceInformation() : element.sourceInformation, e);
        }
    }


    // ------------------------------------------ GETTER -----------------------------------------

    private <T> T lookupAndCastPackageableElement(String fullPath, java.lang.Class<T> expectedType)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.packageableElementsIndex.get(fullPath);
        return expectedType.isInstance(packageableElement) ? expectedType.cast(packageableElement) : null;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement getPackageableElement(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return this.getPackageableElement(element.getPath(), element.sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement getPackageableElement(String fullPath)
    {
        return getPackageableElement(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement getPackageableElement(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = getPackageableElement_safe(fullPath);
        Assert.assertTrue(packageableElement != null, () -> "Can't find the packageable element '" + addPrefixToTypeReference(fullPath) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return packageableElement;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement getPackageableElement_safe(String fullPath)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.packageableElementsIndex.get(packagePrefix(fullPath));
        if (packageableElement != null)
        {
            return packageableElement;
        }

        Type type = getType_safe(fullPath);
        if (type != null)
        {
            return (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) ?
                   (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type :
                   null;
        }

        packageableElement = getGraphFunctions(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }
        packageableElement = getAssociation_safe(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }
        packageableElement = getProfile_safe(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }
        packageableElement = getConcreteFunctionDefinition_safe(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }
        packageableElement = getStore_safe(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }
        packageableElement = getMapping_safe(fullPath);
        if (packageableElement != null)
        {
            return packageableElement;
        }

        // For other elements search the package tree
        return findPackageableElement(packagePrefix(fullPath));
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement findPackageableElement(String fullPath)
    {
        if ("".equals(fullPath) || "::".equals(fullPath) || M3Paths.Root.equals(fullPath))
        {
            return this.root;
        }

        try (AutoCloseableLock ignored = this.pureModelProcessParameter.readLock())
        {
            org.finos.legend.pure.m3.coreinstance.Package currentPackage = this.root;
            int start = 0;
            int end;
            while ((end = fullPath.indexOf(':', start)) != -1)
            {
                String name = fullPath.substring(start, end);
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement child = currentPackage._children().detect(c -> name.equals(c._name()));
                if (!(child instanceof org.finos.legend.pure.m3.coreinstance.Package))
                {
                    return null;
                }
                currentPackage = (org.finos.legend.pure.m3.coreinstance.Package) child;
                start = end + 2;
            }
            String name = fullPath.substring(start);
            return currentPackage._children().detect(c -> name.equals(c._name()));
        }
    }

    public Type getType(String fullPath)
    {
        return getType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Type getType(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        Assert.assertTrue(type != null, () -> "Can't find type '" + addPrefixToTypeReference(fullPath) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return type;
    }

    public Type getType_safe(String fullPath)
    {
        String fullPathWithPrefix = addPrefixToTypeReference(fullPath);
        // Search in the user graph (and cached types found subsequently in the Pure graph)
        Type type = this.typesIndex.get(fullPathWithPrefix);
        if (type != null)
        {
            return type;
        }

        // Search for system types in the Pure graph
        MetadataAccessor metadataAccessor = this.executionSupport.getMetadataAccessor();
        String metadataId = "Root::" + fullPath;
        try
        {
            type = metadataAccessor.getClass(metadataId);
        }
        catch (Exception ignore)
        {
            // metadata may throw if the instance is not found
        }
        if (type == null)
        {
            try
            {
                type = metadataAccessor.getEnumeration(metadataId);
            }
            catch (Exception ignore)
            {
                // metadata may throw if the instance is not found
            }
            if (type == null)
            {
                try
                {
                    type = metadataAccessor.getUnit(metadataId);
                }
                catch (Exception ignore)
                {
                    // metadata may throw if the instance is not found
                }
            }
            if (type == null)
            {
                try
                {
                    type = metadataAccessor.getMeasure(metadataId);
                }
                catch (Exception ignore)
                {
                    // metadata may throw if the instance is not found
                }
            }
        }
        if (type != null)
        {
            this.immutables.add(fullPathWithPrefix);
            this.typesIndex.put(fullPathWithPrefix, type);
        }
        return type;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getClass(String fullPath)
    {
        return this.getClass(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getClass(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        if (!(type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class))
        {
            throw new EngineException("Can't find class '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
    }

    public PropertyOwner getPropertyOwner(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        if (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
        {
            return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
        }
        if (type != null)
        {
            throw new EngineException("Can't find property owner '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = getAssociation_safe(fullPath);
        if (association == null)
        {
            throw new EngineException("Can't find property owner '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return association;
    }

    @SuppressWarnings("unchecked")
    public Enumeration<Enum> getEnumeration(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        if (!(type instanceof Enumeration))
        {
            throw new EngineException("Can't find enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return (Enumeration<Enum>) type;
    }

    public Measure getMeasure(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        if (!(type instanceof Measure))
        {
            throw new EngineException("Can't find measure '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return (Measure) type;
    }

    public Unit getUnit(String fullPath, SourceInformation sourceInformation)
    {
        Type type = getType_safe(fullPath);
        if (!(type instanceof Unit))
        {
            throw new EngineException("Can't find unit '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return (Unit) type;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association getAssociation(String fullPath)
    {
        return this.getAssociation(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association getAssociation(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = this.getAssociation_safe(fullPath);
        Assert.assertTrue(association != null, () -> "Can't find association '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return association;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction<?> getGraphFunctions(String fullPath)
    {
        String metadataId = "Root::" + fullPath;
        try
        {
            ConcreteFunctionDefinition<?> func = this.executionSupport.getMetadataAccessor().getConcreteFunctionDefinition(metadataId);
            if (func != null)
            {
                return func;
            }
        }
        catch (Exception ignore)
        {
            // metadata may throw if element is not found
        }

        try
        {
            return (NativeFunction<?>) this.executionSupport.getMetadata(M3Paths.NativeFunction, metadataId);
        }
        catch (Exception ignore)
        {
            // metadata may throw if element is not found
            return null;
        }
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association getAssociation_safe(String fullPath)
    {
        String fullPathWithPrefix = addPrefixToTypeReference(fullPath);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = lookupAndCastPackageableElement(fullPathWithPrefix, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association.class);
        if (association == null)
        {
            // Search for system types in the Pure graph
            try
            {
                association = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association) this.executionSupport.getMetadata("meta::pure::metamodel::relationship::Association", "Root::" + fullPath);
            }
            catch (Exception ignored)
            {
                // do nothing
            }
            if (association != null)
            {
                this.immutables.add(fullPathWithPrefix);
                this.packageableElementsIndex.put(fullPathWithPrefix, association);
            }
        }
        return association;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile getProfile(String fullPath)
    {
        return getProfile(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile getProfile(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile = getProfile_safe(fullPath);
        Assert.assertTrue(profile != null, () -> "Can't find the profile '" + addPrefixToTypeReference(fullPath) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return profile;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile getProfile_safe(String fullPath)
    {
        String pathWithTypeReference = addPrefixToTypeReference(fullPath);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile = lookupAndCastPackageableElement(pathWithTypeReference, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile.class);
        if (profile == null)
        {
            try
            {
                profile = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) this.executionSupport.getMetadata("meta::pure::metamodel::extension::Profile", "Root::" + pathWithTypeReference);
            }
            catch (Exception ignore)
            {
                //Do Nothing
            }
            if (profile != null)
            {
                this.packageableElementsIndex.put(pathWithTypeReference, profile);
            }
        }
        return profile;
    }


    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> getConcreteFunctionDefinition(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> func = getConcreteFunctionDefinition_safe(fullPath);
        Assert.assertTrue(func != null, () -> "Can't find function '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return func;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> getConcreteFunctionDefinition_safe(String fullPath)
    {
        return lookupAndCastPackageableElement(fullPath, ConcreteFunctionDefinition.class);
    }


    public Store getStore(String fullPath)
    {
        return this.getStore(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Store getStore(String fullPath, SourceInformation sourceInformation)
    {
        Store store = getStore_safe(fullPath);
        Assert.assertTrue(store != null, () -> "The store '" + fullPath + "' can't be found.", sourceInformation, EngineErrorType.COMPILATION);
        return store;
    }

    public Store getStore_safe(String fullPath)
    {
        return lookupAndCastPackageableElement(packagePrefix(fullPath), Store.class);
    }

    public Mapping getMapping(String fullPath)
    {
        return getMapping(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Mapping getMapping(String fullPath, SourceInformation sourceInformation)
    {
        Mapping resultMapping = getMapping_safe(fullPath);
        Assert.assertTrue(resultMapping != null, () -> "Can't find mapping '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return resultMapping;
    }

    public Mapping getMapping_safe(String fullPath)
    {
        return lookupAndCastPackageableElement(packagePrefix(fullPath), Mapping.class);
    }

    public Root_meta_pure_runtime_PackageableRuntime getPackageableRuntime(String fullPath, SourceInformation sourceInformation)
    {
        Root_meta_pure_runtime_PackageableRuntime metamodel = lookupAndCastPackageableElement(packagePrefix(fullPath), Root_meta_pure_runtime_PackageableRuntime.class);
        Assert.assertTrue(metamodel != null, () -> "Can't find packageable runtime '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return metamodel;
    }

    public Root_meta_pure_runtime_PackageableConnection getPackageableConnection(String fullPath, SourceInformation sourceInformation)
    {
        Root_meta_pure_runtime_PackageableConnection metamodel = lookupAndCastPackageableElement(packagePrefix(fullPath), Root_meta_pure_runtime_PackageableConnection.class);
        Assert.assertTrue(metamodel != null, () -> "Can't find packageable connection '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return metamodel;
    }

    public RichIterable<Root_meta_pure_runtime_PackageableRuntime> getAllRuntimes()
    {
        return getAllPackageableElementsOfType(Root_meta_pure_runtime_PackageableRuntime.class);
    }

    public RichIterable<Store> getAllStores()
    {
        return getAllPackageableElementsOfType(Store.class);
    }

    public <T> RichIterable<T> getAllPackageableElementsOfType(java.lang.Class<T> type)
    {
        return LazyIterate.selectInstancesOf(this.packageableElementsIndex.values(), type);
    }

    public Root_meta_core_runtime_Runtime getRuntime(String fullPath)
    {
        return getRuntime(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Root_meta_core_runtime_Runtime getRuntime(String fullPath, SourceInformation sourceInformation)
    {
        Root_meta_core_runtime_Runtime runtime = getRuntime_safe(fullPath);
        Assert.assertTrue(runtime != null, () -> "Can't find runtime '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return runtime;
    }

    public String getRuntimePath(Root_meta_core_runtime_Runtime runtime)
    {
        Predicate<String> predicate = (path) ->
        {
            Root_meta_pure_runtime_PackageableRuntime packageableRuntime = lookupAndCastPackageableElement(path, Root_meta_pure_runtime_PackageableRuntime.class);
            return packageableRuntime != null && packageableRuntime._runtimeValue().equals(runtime);
        };
        return ListIterate.detect(this.packageableElementsIndex.keysView().toList(), predicate);
    }

    public Root_meta_core_runtime_Runtime getRuntime_safe(String fullPath)
    {
        Root_meta_pure_runtime_PackageableRuntime packageableRuntime = lookupAndCastPackageableElement(packagePrefix(fullPath), Root_meta_pure_runtime_PackageableRuntime.class);
        return packageableRuntime == null ? null : packageableRuntime._runtimeValue();
    }

    public Root_meta_core_runtime_Connection getConnection(String fullPath, SourceInformation sourceInformation)
    {
        Root_meta_core_runtime_Connection connection = this.getConnection_safe(fullPath);
        Assert.assertTrue(connection != null, () -> "Can't find connection '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return connection;
    }

    public Root_meta_core_runtime_Connection getConnection_safe(String fullPath)
    {
        Root_meta_pure_runtime_PackageableConnection packageableConnection = lookupAndCastPackageableElement(packagePrefix(fullPath), Root_meta_pure_runtime_PackageableConnection.class);
        return packageableConnection == null ? null : packageableConnection._connectionValue();
    }

    public ImmutableList<EngineException> getEngineExceptions()
    {
        if (this.pureModelProcessParameter.getEnablePartialCompilation())
        {
            return Lists.immutable.withAll(this.engineExceptions);
        }
        return Lists.immutable.empty();
    }

    // ------------------------------------------ SUB-ELEMENT GETTER -----------------------------------------

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype getStereotype(String fullPath, String value)
    {
        return getStereotype(fullPath, value, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype getStereotype(String fullPath, String value, SourceInformation profileSourceInformation, SourceInformation sourceInformation)
    {
        return this.getStereotype(this.getProfile(fullPath, profileSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype getStereotype(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String fullPath, String value, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype stereotype = profile._p_stereotypes().detect(s -> value.equals(s._value()));
        Assert.assertTrue(stereotype != null, () -> "Can't find stereotype '" + value + "' in profile '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return stereotype;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag getTag(String fullPath, String value, SourceInformation profileSourceInformation, SourceInformation sourceInformation)
    {
        return this.getTag(this.getProfile(fullPath, profileSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag getTag(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String fullPath, String value, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag tag = profile._p_tags().detect(t -> value.equals(t._value()));
        Assert.assertTrue(tag != null, () -> "Can't find tag '" + value + "' in profile '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return tag;
    }

    public GenericType getGenericType(String fullPath)
    {
        return getGenericType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public GenericType getGenericType(String fullPath, SourceInformation sourceInformation)
    {
        return this.getGenericType(this.getType(fullPath, sourceInformation));
    }

    public GenericType getGenericType(Type type)
    {
        return this.typesGenericTypeIndex.getIfAbsentPut(buildTypeId(type), () -> new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(type));
    }

    public String buildTypeId(Type type)
    {
        return HelperModelBuilder.getTypeFullPath(type, getExecutionSupport());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> getProperty(String fullPath, String propertyName)
    {
        return this.getProperty(fullPath, propertyName, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> getProperty(String fullPath, String propertyName, SourceInformation classSourceInformation, SourceInformation sourceInformation)
    {
        return this.getProperty(this.getClass(fullPath, classSourceInformation), fullPath, propertyName, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> getProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, String fullPath, String propertyName, SourceInformation sourceInformation)
    {
        return HelperModelBuilder.getOwnedProperty(_class, fullPath, propertyName, sourceInformation, this.getExecutionSupport());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getEnumValue(String fullPath, String value)
    {
        return getEnumValue(fullPath, value, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getEnumValue(String fullPath, String value, SourceInformation enumerationSourceInformation, SourceInformation sourceInformation)
    {
        return this.getEnumValue(this.getEnumeration(fullPath, enumerationSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getEnumValue(Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum> enumeration, String fullPath, String value, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum res = enumeration._values().detect(e -> value.equals(e._name()));
        Assert.assertTrue(res != null, () -> "Can't find enum value '" + value + "' in enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return res;
    }


    // ------------------------------------------ UTILITY -----------------------------------------

    public CompileContext getContext()
    {
        return new CompileContext.Builder(this).build();
    }

    public CompileContext getContext(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return new CompileContext.Builder(this).withElement(element).build();
    }

    public Section getSection(String fullPath)
    {
        return this.sectionsIndex.get(fullPath);
    }

    public Section getSection(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return this.getSection(element.getPath());
    }

    public Type getTypeFromIndex(String fullPath)
    {
        return this.typesIndex.get(addPrefixToTypeReference(fullPath));
    }

    public GenericType getGenericTypeFromIndex(String fullPath)
    {
        if (fullPath.equals("meta::pure::metamodel::type::Any"))
        {
            return getGenericType(fullPath);
        }
        return this.typesGenericTypeIndex.get(addPrefixToTypeReference(fullPath));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> getFunction(String functionName, boolean isNative)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>) this.executionSupport.getMetadata(isNative ? M3Paths.NativeFunction : M3Paths.ConcreteFunctionDefinition, "Root::" + functionName);
    }

    public DeploymentMode getDeploymentMode()
    {
        return this.deploymentMode;
    }

    public Multiplicity getMultiplicity(String m)
    {
        return this.multiplicitiesIndex.get(m.toLowerCase());
    }

    public Multiplicity getMultiplicity(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity multiplicity)
    {
        if (multiplicity.lowerBound == 1 && multiplicity.isUpperBoundEqualTo(1))
        {
            return this.multiplicitiesIndex.get("one");
        }
        if (multiplicity.lowerBound == 0 && multiplicity.isUpperBoundEqualTo(1))
        {
            return this.multiplicitiesIndex.get("zeroone");
        }
        if (multiplicity.lowerBound == 0 && multiplicity.isInfinite())
        {
            return this.multiplicitiesIndex.get("zeromany");
        }
        if (multiplicity.lowerBound == 1 && multiplicity.isInfinite())
        {
            return this.multiplicitiesIndex.get("onemany");
        }
        if (multiplicity.lowerBound == 0 && multiplicity.isUpperBoundEqualTo(0))
        {
            return this.multiplicitiesIndex.get("zero");
        }

        return new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("", null, this.getType(M3Paths.Multiplicity))
                ._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("", null, this.getType(M3Paths.MultiplicityValue))._value((long) multiplicity.lowerBound))
                ._upperBound(multiplicity.isInfinite() ? new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("", null, this.getType(M3Paths.MultiplicityValue)) : new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("", null, this.getType(M3Paths.MultiplicityValue))._value((long) multiplicity.getUpperBoundInt()));
    }

    public static GenericType buildFunctionType(MutableList<VariableExpression> parameters, GenericType returnType, Multiplicity returnMultiplicity, PureModel pureModel)
    {
        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, pureModel.getClass(M3Paths.GenericType))._rawType(new Root_meta_pure_metamodel_type_FunctionType_Impl("", null, pureModel.getClass(M3Paths.FunctionType))._parameters(parameters)._returnType(returnType)._returnMultiplicity(returnMultiplicity));
    }

    public String buildPackageString(String pack, String name)
    {
        return ((pack == null) || pack.isEmpty()) ? name : this.packagePrefix(pack) + "::" + name;
    }

    public String addPrefixToTypeReference(String pack)
    {
        return this.packagePrefix(pack);
    }

    private org.finos.legend.pure.m3.coreinstance.Package getPackage(org.finos.legend.pure.m3.coreinstance.Package parent, String pack)
    {
        return getOrCreatePackage_int(parent, pack, false);
    }

    public org.finos.legend.pure.m3.coreinstance.Package getOrCreatePackage(String pack)
    {
        return "".equals(pack) ? this.root : getOrCreatePackage(this.root, this.packagePrefix(pack));
    }

    private org.finos.legend.pure.m3.coreinstance.Package getOrCreatePackage(org.finos.legend.pure.m3.coreinstance.Package parent, String pack)
    {
        return getOrCreatePackage_int(parent, pack, true);
    }

    private org.finos.legend.pure.m3.coreinstance.Package getOrCreatePackage_int(org.finos.legend.pure.m3.coreinstance.Package parent, String pack, boolean insert)
    {
        return getOrCreatePackage_int(parent, pack, insert, 0);
    }

    private org.finos.legend.pure.m3.coreinstance.Package getOrCreatePackage_int(org.finos.legend.pure.m3.coreinstance.Package parent, String pack, boolean insert, int start)
    {
        int end = pack.indexOf(':', start);
        String name = (end == -1) ? pack.substring(start) : pack.substring(start, end);
        org.finos.legend.pure.m3.coreinstance.Package child = findChildPackage(parent, name);
        if (child == null)
        {
            if (!insert)
            {
                StringBuilder builder = new StringBuilder("Can't find package '").append(pack, start, pack.length()).append("' in '");
                PackageableElement.writeUserPathForPackageableElement(builder, parent);
                builder.append("'");
                throw new EngineException(builder.toString());
            }
            if (RESERVED_PACKAGES.contains(name))
            {
                throw new EngineException("Can't create package with reserved name '" + name + "'");
            }

            try (AutoCloseableLock ignored1 = this.pureModelProcessParameter.writeLock())
            {
                child = findChildPackage(parent, name);
                if (child == null)
                {
                    child = new Package_Impl(name, null, this.getClass("Package"))._name(name)._package(parent);
                    parent._childrenAdd(child);
                }
            }
        }

        return (end == -1) ? child : getOrCreatePackage_int(child, pack, insert, end + 2);
    }

    private org.finos.legend.pure.m3.coreinstance.Package findChildPackage(org.finos.legend.pure.m3.coreinstance.Package parent, String childName)
    {
        try (AutoCloseableLock ignored = this.pureModelProcessParameter.readLock())
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement child = parent._children().detect(c -> childName.equals(c.getName()));
            if ((child != null) && !(child instanceof org.finos.legend.pure.m3.coreinstance.Package))
            {
                StringBuilder builder = new StringBuilder("Element ").append(childName).append(" in ");
                PackageableElement.writeUserPathForPackageableElement(builder, parent);
                builder.append(" is not a package");
                throw new RuntimeException(builder.toString());
            }
            return (org.finos.legend.pure.m3.coreinstance.Package) child;
        }
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement removePackageableElement(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement packageableElement)
    {
        Package pkg = getOrCreatePackage(packageableElement._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement existingElement = pkg._children().detect(c -> c._name().equals(packageableElement.name));
        if (existingElement != null)
        {
            pkg._childrenRemove(existingElement);
            this.packageableElementsIndex.remove(buildPackageString(packageableElement._package, packageableElement.name));
        }
        return existingElement;
    }

    public CompiledExecutionSupport getExecutionSupport()
    {
        return this.executionSupport;
    }

    public boolean isImmutable(String s)
    {
        return this.immutables.contains(s);
    }

    protected String buildNameForAppliedFunction(String functionName)
    {
        if (pureModelProcessParameter.getPackagePrefix() != null
                && !isImmutable(functionName)
                && !functionName.startsWith("meta::")
                && !functionName.startsWith(pureModelProcessParameter.getPackagePrefix())
                && functionName.contains("::"))
        {
            return pureModelProcessParameter.getPackagePrefix() + functionName;
        }
        return functionName;
    }

    private String packagePrefix(String packageName)
    {
        if (pureModelProcessParameter.getPackagePrefix() != null
                && !isImmutable(packageName)
                && !packageName.startsWith("meta::")
                && !packageName.startsWith(pureModelProcessParameter.getPackagePrefix())
        )
        {
            return pureModelProcessParameter.getPackagePrefix() + packageName;
        }
        return packageName;
    }

    public RichIterable<? extends Type> getModelClasses()
    {
        return this.typesIndex.valuesView().reject(t -> (t == null) || (t instanceof Root_meta_pure_metamodel_type_Class_LazyImpl) || (t instanceof Root_meta_pure_metamodel_type_PrimitiveType_LazyImpl));
    }

    public void loadModelFromFunctionHandler(FunctionHandler f)
    {
        if (!(f instanceof UserDefinedFunctionHandler))
        {
            try (AutoCloseableLock ignored = this.pureModelProcessParameter.writeLock())
            {
                String pkg = HelperModelBuilder.getElementFullPath(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) f.getFunc())._package(), this.getExecutionSupport());
                org.finos.legend.pure.m3.coreinstance.Package n = getOrCreatePackage(root, pkg);
                org.finos.legend.pure.m3.coreinstance.Package o = getPackage((org.finos.legend.pure.m3.coreinstance.Package) METADATA_LAZY.getMetadata(M3Paths.Package, M3Paths.Root), pkg);
                n._childrenAdd(o._children().detect(c -> f.getFunctionSignature().equals(c._name())));
            }
        }
    }

    public Handlers getHandlers()
    {
        return handlers;
    }

    private PureModelContextDataIndex index(PureModelContextData pureModelContextData)
    {
        PureModelContextDataIndex index = new PureModelContextDataIndex();
        MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>,
                MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store>> stores = Maps.mutable.empty();

        MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>,
                MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>> otherElementsByClass = Maps.mutable.empty();
        pureModelContextData.getElements().forEach(e ->
        {
            if (e instanceof Association)
            {
                index.associations.add((Association) e);
            }
            else if (e instanceof Class)
            {
                index.classes.add((Class) e);
            }
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration)
            {
                index.enumerations.add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration) e);
            }
            else if (e instanceof Function)
            {
                index.functions.add((Function) e);
            }
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping)
            {
                index.mappings.add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping) e);
            }
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure)
            {
                index.measures.add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure) e);
            }
            else if (e instanceof PackageableConnection)
            {
                index.connections.add((PackageableConnection) e);
            }
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime)
            {
                index.runtimes.add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime) e);
            }
            else if (e instanceof Profile)
            {
                index.profiles.add((Profile) e);
            }
            else if (e instanceof SectionIndex)
            {
                index.sectionIndices.add((SectionIndex) e);
            }
            else if (e instanceof DataElement)
            {
                index.dataElements.add((DataElement) e);
            }
            // TODO eliminate special handling for stores
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store)
            {
                stores.getIfAbsentPut(e.getClass(), Lists.mutable::empty).add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store) e);
            }
            else
            {
                otherElementsByClass.getIfAbsentPut(e.getClass(), Lists.mutable::empty).add(e);
            }
        });
        stores.forEach((cls, elements) -> index.stores.getIfAbsentPut(this.extensions.getExtraProcessorOrThrow(cls), Lists.mutable::empty).addAll(elements));
        otherElementsByClass.forEach((cls, elements) -> index.otherElementsByProcessor.getIfAbsentPut(this.extensions.getExtraProcessorOrThrow(cls), Lists.mutable::empty).addAll(elements));
        return index;
    }

    private static class PureModelContextDataIndex
    {
        private final MutableList<Association> associations = Lists.mutable.empty();
        private final MutableList<Class> classes = Lists.mutable.empty();
        private final MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration> enumerations = Lists.mutable.empty();
        private final MutableList<Function> functions = Lists.mutable.empty();
        private final MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping> mappings = Lists.mutable.empty();
        private final MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure> measures = Lists.mutable.empty();
        private final MutableList<PackageableConnection> connections = Lists.mutable.empty();
        private final MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime> runtimes = Lists.mutable.empty();
        private final MutableList<Profile> profiles = Lists.mutable.empty();
        private final MutableList<SectionIndex> sectionIndices = Lists.mutable.empty();
        private final MutableList<DataElement> dataElements = Lists.mutable.empty();
        private final MutableMap<Processor<?>, MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store>> stores = Maps.mutable.empty();
        private final MutableMap<Processor<?>, MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>> otherElementsByProcessor = Maps.mutable.empty();

        public Map<String, Number> getStats()
        {
            MutableMap<String, Number> result = Maps.mutable.empty();
            possiblyAddStats(result, "associations", this.associations);
            possiblyAddStats(result, "classes", this.classes);
            possiblyAddStats(result, "enumerations", this.enumerations);
            possiblyAddStats(result, "functions", this.functions);
            possiblyAddStats(result, "mappings", this.mappings);
            possiblyAddStats(result, "measures", this.measures);
            possiblyAddStats(result, "connections", this.connections);
            possiblyAddStats(result, "runtimes", this.runtimes);
            possiblyAddStats(result, "profiles", this.profiles);
            possiblyAddStats(result, "sectionIndices", this.sectionIndices);
            possiblyAddStats(result, "dataElements", this.dataElements);
            if (this.stores.notEmpty())
            {
                result.put("stores", this.stores.valuesView().sumOfInt(MutableList::size));
            }
            if (this.otherElementsByProcessor.notEmpty())
            {
                result.put("otherElements", this.otherElementsByProcessor.valuesView().sumOfInt(MutableList::size));
            }
            return result;
        }

        private static void possiblyAddStats(MutableMap<String, Number> stats, String name, MutableList<?> list)
        {
            if (list.notEmpty())
            {
                stats.put(name, list.size());
            }
        }
    }

    private static double nanosDurationToMillis(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000.0d;
    }

    protected <T extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> T setNameAndPackage(T pureElement, String name, String packagePath, SourceInformation sourceInformation)
    {
        // Validate and set name
        if ((name == null) || name.isEmpty())
        {
            throw new EngineException("PackageableElement name may not be null or empty", sourceInformation, EngineErrorType.COMPILATION);
        }
        if (!name.equals(pureElement.getName()))
        {
            throw new EngineException("PackageableElement name '" + name + "' must match CoreInstance name '" + pureElement.getName() + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        pureElement._name(name);

        try (AutoCloseableLock ignored = this.pureModelProcessParameter.writeLock())
        {
            // Validate and set package
            Package pack = this.getOrCreatePackage(packagePath);
            if (pack._children().anySatisfy(c -> name.equals(c._name())))
            {
                throw new EngineException("An element named '" + name + "' already exists in the package '" + packagePath + "'", sourceInformation, EngineErrorType.COMPILATION);
            }
            pureElement._package(pack);
            pureElement.setSourceInformation(SourceInformationHelper.toM3SourceInformation(sourceInformation));
            pack._childrenAdd(pureElement);
        }
        this.packageableElementsIndex.put(buildPackageString(packagePath, name), pureElement);
        return pureElement;
    }
}
