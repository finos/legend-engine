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
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.MetadataWrapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.UserDefinedFunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.ClassValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.MappingValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.PureModelContextDataValidator;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Package_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_FunctionType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_PrimitiveType_LazyImpl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.PackageableMultiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.security.auth.Subject;

public class PureModel implements IPureModel
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final List<String> RESERVED_PACKAGES = FastList.newListWith("$implicit");
    private static final MetadataLazy METADATA_LAZY = new MetadataLazy(PureModel.class.getClassLoader());
    private final CompiledExecutionSupport executionSupport;
    private final DeploymentMode deploymentMode;
    private final PureModelProcessParameter pureModelProcessParameter;
    private final org.finos.legend.pure.m3.coreinstance.Package root = new Package_Impl("Root")._name("Root");
    // NOTE: since we have states within each extension, we have to keep extensions local to `PureModel` rather than having
    // this as part of `CompileContext`
    public final List<CompilerExtension> extensions;

    final Handlers handlers;

    private final MutableSet<String> immutables = Sets.mutable.empty();
    private final MutableMap<String, Multiplicity> multiplicitiesIndex = UnifiedMap.newMap();
    final MutableMap<String, Section> sectionsIndex = UnifiedMap.newMap();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type> typesIndex = UnifiedMap.newMap();
    final MutableMap<String, GenericType> typesGenericTypeIndex = UnifiedMap.newMap();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?>> functionsIndex = UnifiedMap.newMap();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile> profilesIndex = UnifiedMap.newMap();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association> associationsIndex = UnifiedMap.newMap();
    final MutableMap<String, Store> storesIndex = UnifiedMap.newMap();
    final MutableMap<String, Mapping> mappingsIndex = UnifiedMap.newMap();
    final MutableMap<String, Connection> connectionsIndex = UnifiedMap.newMap();
    final MutableMap<String, Runtime> runtimesIndex = UnifiedMap.newMap();

    public PureModel(PureModelContextData pure, Subject subject, DeploymentMode deploymentMode)
    {
        this(pure, subject, null, deploymentMode, new PureModelProcessParameter());
    }

    public PureModel(PureModelContextData pure, Subject subject, DeploymentMode deploymentMode, PureModelProcessParameter pureModelProcessParameter)
    {
        this(pure, subject, null, deploymentMode, pureModelProcessParameter);
    }

    public PureModel(PureModelContextData pure, Subject subject, ClassLoader classLoader, DeploymentMode deploymentMode)
    {
        this(pure, subject, classLoader, deploymentMode, new PureModelProcessParameter());
    }

    public PureModel(PureModelContextData pureModelContextData, Subject subject, ClassLoader classLoader, DeploymentMode deploymentMode, PureModelProcessParameter pureModelProcessParameter)
    {
        this.extensions = CompilerExtensionLoader.extensions();
        List<Procedure2<PureModel, PureModelContextData>> extraPostValidators = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPostValidators);

        if (classLoader == null)
        {
            classLoader = Pure.class.getClassLoader();
        }
        this.deploymentMode = deploymentMode;
        this.pureModelProcessParameter = pureModelProcessParameter;
        try (Scope scope = GlobalTracer.get().buildSpan("Build Pure Model").startActive(true))
        {
            this.executionSupport = new CompiledExecutionSupport(
                    new JavaCompilerState(null, classLoader),
                    new CompiledProcessorSupport(classLoader, new MetadataWrapper(this.root, METADATA_LAZY, this), Sets.mutable.empty()),
                    null,
                    new PureCodeStorage(null, new VersionControlledClassLoaderCodeStorage(classLoader, Lists.mutable.of(
                            CodeRepository.newPlatformCodeRepository(),
                            SVNCodeRepository.newSystemCodeRepository()
                    ), null)),
                    null,
                    null,
                    new ConsoleCompiled(),
                    new FunctionCache(),
                    new ClassCache(),
                    null,
                    Sets.mutable.empty()
            );
            registerElementsForPathToElement();

            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_START, pureModelContextData.origin == null ? "" : ((PureSDLC) pureModelContextData.origin.sdlcInfo).packageableElementPointers).toString());
            scope.span().log(LoggingEventType.GRAPH_START.toString());

            this.handlers = new Handlers(this);
            this.initializeMultiplicities();
            this.initializePrimitiveTypes();
            long initFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_INITIALIZED, (double)initFinished - start).toString());
            scope.span().log(LoggingEventType.GRAPH_INITIALIZED.toString());

            long parsingFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_PARSED, (double)parsingFinished - initFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_PARSED.toString());

            // Pre Validation
            PureModelContextDataValidator preValidator = new PureModelContextDataValidator();
            preValidator.validate(this, pureModelContextData);
            long preValidationFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED, (double)preValidationFinished - initFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED.toString());

            // Processing
            this.loadSectionIndices(pureModelContextData);

            this.loadTypes(pureModelContextData);
            long loadTypesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_DOMAIN_BUILT, this.buildDomainStats(pureModelContextData), (double)loadTypesFinished - preValidationFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_DOMAIN_BUILT.toString());

            this.loadStores(pureModelContextData);
            long loadStoresFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_STORES_BUILT, this.buildStoreStats(pureModelContextData, this), (double)loadStoresFinished - loadTypesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_STORES_BUILT.toString());

            this.loadDataStoreSpecifications(pureModelContextData);
            long loadDsSpecsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_DATASTORESPECIFICATIONS_BUILT, (double)loadDsSpecsFinished - loadStoresFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_DATASTORESPECIFICATIONS_BUILT.toString());

            this.loadMappings(pureModelContextData);
            long loadMappingsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_MAPPINGS_BUILT, (double)loadMappingsFinished - loadDsSpecsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_MAPPINGS_BUILT.toString());

            this.loadConnectionsAndRuntimes(pureModelContextData);
            long loadConnectionsAndRuntimesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_CONNECTIONS_AND_RUNTIMES_BUILT, (double)loadConnectionsAndRuntimesFinished - loadMappingsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_CONNECTIONS_AND_RUNTIMES_BUILT.toString());

            this.loadServices(pureModelContextData);
            long loadServicesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_SERVICES_BUILT, (double)loadServicesFinished - loadConnectionsAndRuntimesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_SERVICES_BUILT.toString());

            this.loadCacheables(pureModelContextData);
            long loadCacheablesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_CACHEABLES_BUILT, (double)loadCacheablesFinished - loadServicesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_CACHEABLES_BUILT.toString());

            this.loadCaches(pureModelContextData);
            long loadCachesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_CACHES_BUILT, (double)loadCachesFinished - loadCacheablesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_CACHES_BUILT.toString());

            this.loadPipelines(pureModelContextData);
            long loadPipelinesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_PIPELINES_BUILT, (double)loadPipelinesFinished - loadCachesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_PIPELINES_BUILT.toString());

            this.loadFlattenSpecifications(pureModelContextData);
            long loadFlattensFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_FLATTENSPECIFICATIONS_BUILT, (double)loadFlattensFinished - loadPipelinesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_FLATTENSPECIFICATIONS_BUILT.toString());

            this.loadFileGenerations(pureModelContextData);
            long loadFileGenerationsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_FLATTENSPECIFICATIONS_BUILT, (double)loadFileGenerationsFinished - loadFlattensFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_FLATTENSPECIFICATIONS_BUILT.toString());

            this.loadSerializableModelSpecifications(pureModelContextData);
            long loadSerializableModelSpecsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_SERIALIZABLE_MODEL_SPECIFICATIONS_BUILT, (double)loadSerializableModelSpecsFinished - loadFileGenerationsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_SERIALIZABLE_MODEL_SPECIFICATIONS_BUILT.toString());

            this.loadGenerationSpecifications(pureModelContextData);
            long loadGenerationSpecificationsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_GENERATION_TREES_BUILT, (double)loadGenerationSpecificationsFinished - loadSerializableModelSpecsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_GENERATION_TREES_BUILT.toString());

            this.loadDiagrams(pureModelContextData);
            long loadDiagramsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_DIAGRAMS_BUILT, (double)loadDiagramsFinished - loadFileGenerationsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_DIAGRAMS_BUILT.toString());

            // NOTE: we don't load texts

            long processingFinished = System.currentTimeMillis();

            // Post Validation
            new ClassValidator().validate(this, pureModelContextData);
            new MappingValidator().validate(this, pureModelContextData);
            extraPostValidators.forEach(validator -> validator.value(this, pureModelContextData));
            long postValidationFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED, (double)postValidationFinished - processingFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED.toString());

            long finished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_STOP, (double)finished - start).toString());
            scope.span().log(LoggingEventType.GRAPH_STOP.toString());
        }
        catch (Exception e)
        {
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_ERROR, e).toString());
            // Since EngineException extends RuntimeException it is more straight forward to just
            // throw EngineException as is. This will make downstream handling of exception easier
            // TODO: we need to have a better strategy to throw compilation error instead of the generic exeception
            if (e instanceof EngineException)
            {
                throw e;
            }
            throw new RuntimeException(e);
        }
    }

    private ImmutableMap<String, Integer> buildDomainStats(PureModelContextData pure)
    {
        return Maps.immutable.of("classes", pure.domain == null ? 0 : pure.domain.classes.size(),
                "enums", pure.domain == null ? 0 : pure.domain.enums.size(),
                "associations", pure.domain == null ? 0 : pure.domain.associations.size(),
                "functions", pure.domain == null ? 0 : pure.domain.functions.size()
        );
    }

    private ListIterable<Map<String, String>> buildStoreStats(PureModelContextData pure, PureModel pureModel)
    {
        return ListIterate.collect(pure.stores, store ->
        {
            MutableMap<String, String> map = Maps.mutable.of("name", pureModel.buildPackageString(store._package, store.name));
            this.getContext().extraStoreStatBuilders.forEach(processor -> processor.value(store, map));
            return map;
        });
    }


    // ------------------------------------------ INITIALIZATION -----------------------------------------

    /**
     * This method add elements from packages that belong to METADATA LAZY root to the packages that belong to this graph (PureModel) root
     * as this is needed for `pathToElement` to work on older graph
     */
    private void registerElementsForPathToElement()
    {
        registerElementForPathToElement("meta::pure::mapping::modelToModel", FastList.newListWith(
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_",
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_1__Runtime_1__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__Result_1_"
        ));
        registerElementForPathToElement("meta::pure::mapping::modelToModel::inMemory", FastList.newListWith(
                "getterOverrideMapped_Any_1__PropertyMapping_1__Any_MANY_",
                "getterOverrideNonMapped_Any_1__Property_1__Any_MANY_"
        ));
        registerElementForPathToElement("meta::pure::router::store::platform", FastList.newListWith(
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__Result_1_",
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_"
        ));
        registerElementForPathToElement("meta::protocols::pure::vX_X_X::invocation::execution::execute", FastList.newListWith(
                "alloyExecute_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_$0_1$__String_1__Integer_1__String_1__String_1__RouterExtension_MANY__Result_1_",
                "executePlan_ExecutionPlan_1__String_1__Integer_1__RouterExtension_MANY__String_1_"
        ));
        registerElementForPathToElement("meta::pure::tds", FastList.newListWith(
                "TDSRow"
        ));
        this.getContext().extraElementForPathToElementRegisters.forEach(register -> register.value(this::registerElementForPathToElement));
    }

    private void registerElementForPathToElement(String pack, List<String> children)
    {
        org.finos.legend.pure.m3.coreinstance.Package newPkg = getOrCreatePackage(root, pack);
        org.finos.legend.pure.m3.coreinstance.Package oldPkg = getPackage((org.finos.legend.pure.m3.coreinstance.Package) METADATA_LAZY.getMetadata("Package", "Root"), pack);
        for (String child : children)
        {
            // allow duplicated registration, but only the first one will actually get registered
            if (newPkg._children().detect(c -> child.equals(c._name())) == null)
            {
                newPkg._childrenAdd(Objects.requireNonNull(oldPkg._children().detect(c -> child.equals(c._name())), "Can't find child element '" + child + "' in package '" + pack + "' for path registration"));
            }
        }
    }

    private void initializeMultiplicities()
    {
        this.multiplicitiesIndex.put("zero", (PackageableMultiplicity) executionSupport.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "Root::meta::pure::metamodel::multiplicity::PureZero"));
        this.multiplicitiesIndex.put("one", (PackageableMultiplicity) executionSupport.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "Root::meta::pure::metamodel::multiplicity::PureOne"));
        this.multiplicitiesIndex.put("zeroone", (PackageableMultiplicity) executionSupport.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "Root::meta::pure::metamodel::multiplicity::ZeroOne"));
        this.multiplicitiesIndex.put("onemany", (PackageableMultiplicity) executionSupport.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "Root::meta::pure::metamodel::multiplicity::OneMany"));
        this.multiplicitiesIndex.put("zeromany", (PackageableMultiplicity) executionSupport.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "Root::meta::pure::metamodel::multiplicity::ZeroMany"));
    }

    private void initializePrimitiveTypes()
    {
        this.typesIndex.put("String", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "String"));
        this.immutables.add("String");
        this.typesIndex.put("Binary", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Binary"));
        this.immutables.add("Binary");
        this.typesIndex.put("Boolean", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Boolean"));
        this.immutables.add("Boolean");
        this.typesIndex.put("Integer", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Integer"));
        this.immutables.add("Integer");
        this.typesIndex.put("Number", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Number"));
        this.immutables.add("Number");
        this.typesIndex.put("Float", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Float"));
        this.immutables.add("Float");
        this.typesIndex.put("Decimal", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Decimal"));
        this.immutables.add("Decimal");
        this.typesIndex.put("Date", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "Date"));
        this.immutables.add("Date");
        this.typesIndex.put("StrictDate", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "StrictDate"));
        this.immutables.add("StrictDate");
        this.typesIndex.put("DateTime", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "DateTime"));
        this.immutables.add("DateTime");
        this.typesIndex.put("LatestDate", (PrimitiveType) executionSupport.getMetadata("meta::pure::metamodel::type::PrimitiveType", "LatestDate"));
        this.immutables.add("LatestDate");
    }


    // ------------------------------------------ LOADER -----------------------------------------

    private void loadSectionIndices(PureModelContextData pure)
    {
        if (pure.sectionIndices != null)
        {
            ListIterate.forEach(pure.sectionIndices, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadTypes(PureModelContextData pure)
    {
        if (pure.domain != null)
        {
            // First pass
            ListIterate.forEach(pure.domain.profiles, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.classes, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.enums, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.functions, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.measures, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));

            // Second pass
            ListIterate.forEach(pure.domain.classes, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.measures, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));

            // Process - associations / inheritance
            ListIterate.forEach(pure.domain.associations, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));

            // Third pass - milestoning
            ListIterate.forEach(pure.domain.classes, el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.associations, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));

            // Fourth pass - qualifiers
            ListIterate.forEach(pure.domain.classes, el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.associations, el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.domain.functions, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        }
    }

    private void loadStores(PureModelContextData pure)
    {
        if (pure.stores != null)
        {
            ListIterate.forEach(pure.stores, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.stores, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.stores, el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.stores, el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.stores, el -> visitWithErrorHandling(el, new PackageableElementFifthPassBuilder(this.getContext(el))));
        }
    }

    private void loadDataStoreSpecifications(PureModelContextData pure)
    {
        if (pure.dataStoreSpecifications != null)
        {
            ListIterate.collect(pure.dataStoreSpecifications, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    public void loadMappings(PureModelContextData pure)
    {
        if (pure.mappings != null)
        {
            ListIterate.forEach(pure.mappings, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.mappings, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.mappings, el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.mappings, el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
        }
    }

    public void loadConnectionsAndRuntimes(PureModelContextData pure)
    {
        // Connections must be loaded before runtimes
        if (pure.connections != null)
        {
            ListIterate.forEach(pure.connections, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.connections, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        }
        if (pure.runtimes != null)
        {
            ListIterate.forEach(pure.runtimes, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.runtimes, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        }
    }

    private void loadServices(PureModelContextData pure)
    {
        if (pure.services != null)
        {
            ListIterate.forEach(pure.services, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadCacheables(PureModelContextData pure)
    {
        if (pure.cacheables != null)
        {
            ListIterate.forEach(pure.cacheables, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadCaches(PureModelContextData pure)
    {
        if (pure.caches != null)
        {
            ListIterate.forEach(pure.caches, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadPipelines(PureModelContextData pure)
    {
        if (pure.pipelines != null)
        {
            ListIterate.forEach(pure.pipelines, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadFlattenSpecifications(PureModelContextData pure)
    {
        if (pure.flattenSpecifications != null)
        {
            ListIterate.forEach(pure.flattenSpecifications, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            ListIterate.forEach(pure.flattenSpecifications, el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        }
    }

    private void loadSerializableModelSpecifications(PureModelContextData pureModelContextData)
    {
        if (pureModelContextData.serializableModelSpecifications != null)
        {
            ListIterate.forEach(pureModelContextData.serializableModelSpecifications, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadGenerationSpecifications(PureModelContextData pureModelContextData)
    {
        if (pureModelContextData.generationSpecifications != null)
        {
            ListIterate.forEach(pureModelContextData.generationSpecifications, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadFileGenerations(PureModelContextData pure)
    {
        if (pure.fileGenerations != null)
        {
            ListIterate.forEach(pure.fileGenerations, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
    }

    private void loadDiagrams(PureModelContextData pure)
    {
        if (pure.diagrams != null)
        {
            // NOTE: we don't compile diagram like other element types in engine,
            // in fact the shape of Diagram in protocol is incompatible with the metamodel of diagram in PURE
            ListIterate.forEach(pure.diagrams, el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        }
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

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type getType(String fullPath)
    {
        return getType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type getType(String fullPath, SourceInformation sourceInformation)
    {
        return this.typesIndex.getIfAbsentPut(addPrefixToTypeReference(fullPath),
                () ->
                {
                    this.immutables.add(addPrefixToTypeReference(fullPath));
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type type = null;
                    try
                    {
                        type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) executionSupport.getMetadata("meta::pure::metamodel::type::Class", "Root::" + fullPath);
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) executionSupport.getMetadata("meta::pure::metamodel::type::Enumeration", "Root::" + fullPath);
                        }
                        catch (Exception ee)
                        {
                            try
                            {
                                type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) executionSupport.getMetadata("meta::pure::metamodel::type::Unit", "Root::" + fullPath);
                            }
                            catch (Exception eee)
                            {
                                try
                                {
                                    type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) executionSupport.getMetadata("meta::pure::metamodel::type::Measure", "Root::" + fullPath);
                                }
                                catch (Exception ignored)
                                {
                                    // do nothing
                                }
                            }
                        }
                    }
                    Assert.assertTrue(type != null, () -> "Can't find type '" + addPrefixToTypeReference(fullPath) + "'", sourceInformation, EngineErrorType.COMPILATION);
                    return type;
                }
        );
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getClass(String fullPath)
    {
        return this.getClass(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getClass(String fullPath, SourceInformation sourceInformation)
    {
        Type type;
        try
        {
            type = this.getType(fullPath, sourceInformation);
        }
        catch (EngineException e)
        {
            throw new EngineException("Can't find class '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class;
        try
        {
            _class = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find class '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return _class;
    }

    public Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum> getEnumeration(String fullPath, SourceInformation sourceInformation)
    {
        Type type;
        try
        {
            type = this.getType(fullPath, sourceInformation);
        }
        catch (EngineException e)
        {
            throw new EngineException("Can't find enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum> enumeration;
        try
        {
            enumeration = (Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum>) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return enumeration;
    }

    public Measure getMeasure(String fullPath, SourceInformation sourceInformation)
    {
        Type type;
        try
        {
            type = this.getType(fullPath, sourceInformation);
        }
        catch (EngineException e)
        {
            throw new EngineException("Can't find measure '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        Measure measure;
        try
        {
            measure = (Measure) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find measure '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return measure;
    }

    public Unit getUnit(String fullPath, SourceInformation sourceInformation)
    {
        Type type;
        try
        {
            type = this.getType(fullPath, sourceInformation);
        }
        catch (EngineException e)
        {
            throw new EngineException("Can't find unit '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        Unit unit;
        try
        {
            unit = (Unit) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find unit '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return unit;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association getAssociation(String fullPath)
    {
        return this.getAssociation(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association getAssociation(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = this.associationsIndex.get(addPrefixToTypeReference(fullPath));
        Assert.assertTrue(association != null, () -> "Can't find association '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return association;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile getProfile(String fullPath)
    {
        return getProfile(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile getProfile(String fullPath, SourceInformation sourceInformation)
    {
        return this.profilesIndex.getIfAbsentPut(fullPath,
                () ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile = null;
                    try
                    {
                        profile = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile) executionSupport.getMetadata("meta::pure::metamodel::extension::Profile", "Root::" + fullPath);
                    }
                    catch (Exception e)
                    {
                        throw new EngineException("Can't find profile '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
                    }
                    Assert.assertTrue(profile != null, () -> "Can't find profile '" + fullPath + "'");
                    return profile;
                }
        );
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> getConcreteFunctionDefinition(String fullPath, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> func = this.functionsIndex.get(fullPath);
        Assert.assertTrue(func != null, () -> "Can't find function '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return func;
    }

    public Store getStore(String fullPath)
    {
        return this.getStore(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Store getStore(String fullPath, SourceInformation sourceInformation)
    {
        String updatedPath = packagePrefix(fullPath);
        Store store = this.storesIndex.get(updatedPath);
        Assert.assertTrue(store != null, () -> "Can't find store '" + updatedPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return store;
    }

    public Mapping getMapping(String fullPath)
    {
        return getMapping(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Mapping getMapping(String fullPath, SourceInformation sourceInformation)
    {
        String updatedPath = packagePrefix(fullPath);
        Mapping resultMapping = this.mappingsIndex.get(updatedPath);
        Assert.assertTrue(resultMapping != null, () -> "Can't find mapping '" + updatedPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return resultMapping;
    }

    public Runtime getRuntime(String fullPath)
    {
        return getRuntime(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Runtime getRuntime(String fullPath, SourceInformation sourceInformation)
    {
        String path = packagePrefix(fullPath);
        Runtime runtime = this.runtimesIndex.get(path);
        Assert.assertTrue(runtime != null, () -> "Can't find runtime '" + path + "'", sourceInformation, EngineErrorType.COMPILATION);
        return runtime;
    }

    public Connection getConnection(String fullPath, SourceInformation sourceInformation)
    {
        String path = packagePrefix(fullPath);
        Connection connection = this.connectionsIndex.get(path);
        Assert.assertTrue(connection != null, () -> "Can't find connection '" + path + "'", sourceInformation, EngineErrorType.COMPILATION);
        return connection;
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
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype stereotype = profile._p_stereotypes().select(s -> s._value().equals(value)).getFirst();
        Assert.assertTrue(stereotype != null, () -> "Can't find stereotype '" + value + "' in profile '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return stereotype;
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag getTag(String fullPath, String value, SourceInformation profileSourceInformation, SourceInformation sourceInformation)
    {
        return this.getTag(this.getProfile(fullPath, profileSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag getTag(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String fullPath, String value, SourceInformation sourceInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag tag = profile._p_tags().select(t -> t._value().equals(value)).getFirst();
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
        return this.typesGenericTypeIndex.getIfAbsentPut(HelperModelBuilder.getElementFullPath(type, this.getExecutionSupport()), () -> new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(type));
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

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type getTypeFromIndex(String fullPath)
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
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>) this.executionSupport.getMetadata(isNative
                ? "meta::pure::metamodel::function::NativeFunction"
                : "meta::pure::metamodel::function::ConcreteFunctionDefinition", "Root::" + functionName);
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

        return new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("")
                ._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("")._value((long) multiplicity.lowerBound))
                ._upperBound(multiplicity.isInfinite() ? new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("") : new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("")._value((long) multiplicity.getUpperBoundInt()));
    }

    public static GenericType buildFunctionType(MutableList<VariableExpression> parameters, GenericType returnType, Multiplicity returnMultiplicity)
    {
        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(new Root_meta_pure_metamodel_type_FunctionType_Impl("")._parameters(parameters)._returnType(returnType)._returnMultiplicity(returnMultiplicity));
    }

    public String buildPackageString(String pack, String name)
    {
        return pack.equals("") ? name : this.packagePrefix(pack) + "::" + name;
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
            child = new Package_Impl(name)._name(name)._package(parent);
            parent._childrenAdd(child);
        }

        return (end == -1) ? child : getOrCreatePackage_int(child, pack, insert, end + 2);
    }

    private org.finos.legend.pure.m3.coreinstance.Package findChildPackage(org.finos.legend.pure.m3.coreinstance.Package parent, String childName)
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

    public CompiledExecutionSupport getExecutionSupport()
    {
        return this.executionSupport;
    }

    public boolean isImmutable(String s)
    {
        return this.immutables.contains(s);
    }

    private String packagePrefix(String packageName)
    {
        if (pureModelProcessParameter.packagePrefix != null
                && !isImmutable(packageName)
                && !packageName.equals("meta::pure::metamodel::type::Any")
                && !packageName.equals("meta::pure::metamodel::type::Enumeration")
                && !packageName.equals("meta::pure::metamodel::type::Class")
                && !packageName.startsWith(pureModelProcessParameter.packagePrefix)
        )
        {
            return pureModelProcessParameter.packagePrefix + packageName;
        }
        return packageName;
    }

    public RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> getModelClasses()
    {
        return this.typesIndex.valuesView().reject(t -> (t == null) || (t instanceof Root_meta_pure_metamodel_type_Class_LazyImpl) || (t instanceof Root_meta_pure_metamodel_type_PrimitiveType_LazyImpl));
    }

    public void loadModelFromFunctionHandler(FunctionHandler f)
    {
        if (!(f instanceof UserDefinedFunctionHandler))
        {
            String pkg = HelperModelBuilder.getElementFullPath(f.getFunc()._package(), this.getExecutionSupport());
            org.finos.legend.pure.m3.coreinstance.Package n = getOrCreatePackage(root, pkg);
            org.finos.legend.pure.m3.coreinstance.Package o = getPackage((org.finos.legend.pure.m3.coreinstance.Package) METADATA_LAZY.getMetadata("Package", "Root"), pkg);
            n._childrenAdd(o._children().detect(c -> f.getFunctionSignature().equals(c._name())));
        }
    }

    public Handlers getHandlers()
    {
        return handlers;
    }
}
