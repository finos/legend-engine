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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.MetadataWrapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
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
import java.util.stream.Stream;
import javax.security.auth.Subject;

public class PureModel implements IPureModel
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final ImmutableSet<String> RESERVED_PACKAGES = Sets.immutable.with("$implicit");
    private static final MetadataLazy METADATA_LAZY = new MetadataLazy(PureModel.class.getClassLoader());
    private final CompiledExecutionSupport executionSupport;
    private final DeploymentMode deploymentMode;
    private final PureModelProcessParameter pureModelProcessParameter;
    private final org.finos.legend.pure.m3.coreinstance.Package root = new Package_Impl("Root")._name("Root");
    // NOTE: since we have states within each extension, we have to keep extensions local to `PureModel` rather than having
    // this as part of `CompileContext`
    final CompilerExtensions extensions;

    final Handlers handlers;

    private final MutableSet<String> immutables = Sets.mutable.empty();
    private final MutableMap<String, Multiplicity> multiplicitiesIndex = Maps.mutable.empty();
    final MutableMap<String, Section> sectionsIndex = Maps.mutable.empty();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type> typesIndex = Maps.mutable.empty();
    final MutableMap<String, GenericType> typesGenericTypeIndex = Maps.mutable.empty();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?>> functionsIndex = Maps.mutable.empty();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile> profilesIndex = Maps.mutable.empty();
    final MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association> associationsIndex = Maps.mutable.empty();
    final MutableMap<String, Store> storesIndex = Maps.mutable.empty();
    final MutableMap<String, Mapping> mappingsIndex = Maps.mutable.empty();
    final MutableMap<String, Connection> connectionsIndex = Maps.mutable.empty();
    final MutableMap<String, Runtime> runtimesIndex = Maps.mutable.empty();

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
        this.extensions = CompilerExtensions.fromAvailableExtensions();
        List<Procedure2<PureModel, PureModelContextData>> extraPostValidators = this.extensions.getExtraPostValidators();

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
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_INITIALIZED, (double) initFinished - start).toString());
            scope.span().log(LoggingEventType.GRAPH_INITIALIZED.toString());

            long parsingFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_PARSED, (double) parsingFinished - initFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_PARSED.toString());

            // Pre Validation
            PureModelContextDataValidator preValidator = new PureModelContextDataValidator();
            preValidator.validate(this, pureModelContextData);
            long preValidationFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED, (double) preValidationFinished - initFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED.toString());

            // Processing

            PureModelContextDataIndex pureModelContextDataIndex = index(pureModelContextData);
            this.loadSectionIndices(pureModelContextDataIndex);

            this.loadTypes(pureModelContextDataIndex);
            long loadTypesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_DOMAIN_BUILT, this.buildDomainStats(pureModelContextData), (double) loadTypesFinished - preValidationFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_DOMAIN_BUILT.toString());

            this.loadStores(pureModelContextDataIndex);
            long loadStoresFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_STORES_BUILT, this.buildStoreStats(pureModelContextData, this), (double) loadStoresFinished - loadTypesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_STORES_BUILT.toString());

            this.loadMappings(pureModelContextDataIndex);
            long loadMappingsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_MAPPINGS_BUILT, (double) loadMappingsFinished - loadStoresFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_MAPPINGS_BUILT.toString());

            this.loadConnectionsAndRuntimes(pureModelContextDataIndex);
            long loadConnectionsAndRuntimesFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_CONNECTIONS_AND_RUNTIMES_BUILT, (double) loadConnectionsAndRuntimesFinished - loadMappingsFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_CONNECTIONS_AND_RUNTIMES_BUILT.toString());

            this.loadOtherElements(pureModelContextDataIndex);
            long loadOtherElementsFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_OTHER_ELEMENTS_BUILT, (double) loadOtherElementsFinished - loadConnectionsAndRuntimesFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_OTHER_ELEMENTS_BUILT.toString());

            long processingFinished = System.currentTimeMillis();

            // Post Validation
            new ClassValidator().validate(this, pureModelContextData);
            new MappingValidator().validate(this, pureModelContextData);
            extraPostValidators.forEach(validator -> validator.value(this, pureModelContextData));
            long postValidationFinished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED, (double) postValidationFinished - processingFinished).toString());
            scope.span().log(LoggingEventType.GRAPH_POST_VALIDATION_COMPLETED.toString());

            long finished = System.currentTimeMillis();
            LOGGER.info(new LogInfo(subject, LoggingEventType.GRAPH_STOP, (double) finished - start).toString());
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
            this.extensions.getExtraStoreStatBuilders().forEach(processor -> processor.value(store, map));
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
        registerElementForPathToElement("meta::pure::mapping::modelToModel", Lists.mutable.with(
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_",
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_1__Runtime_1__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__Result_1_"
        ));
        registerElementForPathToElement("meta::pure::mapping::modelToModel::inMemory", Lists.mutable.with(
                "getterOverrideMapped_Any_1__PropertyMapping_1__Any_MANY_",
                "getterOverrideNonMapped_Any_1__Property_1__Any_MANY_"
        ));
        registerElementForPathToElement("meta::pure::router::store::platform", Lists.mutable.with(
                "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__Result_1_",
                "supports_FunctionExpression_1__Boolean_1_",
                "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_"
        ));
        registerElementForPathToElement("meta::protocols::pure::vX_X_X::invocation::execution::execute", Lists.mutable.with(
                "alloyExecute_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_$0_1$__String_1__Integer_1__String_1__String_1__RouterExtension_MANY__Result_1_",
                "executePlan_ExecutionPlan_1__String_1__Integer_1__RouterExtension_MANY__String_1_"
        ));
        registerElementForPathToElement("meta::pure::tds", Lists.mutable.with(
                "TDSRow"
        ));
        this.extensions.getExtraElementForPathToElementRegisters().forEach(register -> register.value(this::registerElementForPathToElement));
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

    private void loadSectionIndices(PureModelContextDataIndex pure)
    {
        pure.sectionIndices.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
    }

    private void loadTypes(PureModelContextDataIndex pure)
    {
        // First pass
        pure.profiles.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.classes.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.enumerations.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.functions.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.measures.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));

        // Second pass
        pure.classes.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        pure.measures.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));

        // Process - associations / inheritance
        pure.associations.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));

        // Third pass - milestoning
        pure.classes.forEach(el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
        pure.associations.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));

        // Fourth pass - qualifiers
        pure.classes.forEach(el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
        pure.associations.forEach(el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
        pure.functions.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
    }

    private void loadStores(PureModelContextDataIndex pure)
    {
        pure.stores.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.stores.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        pure.stores.forEach(el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
        pure.stores.forEach(el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
        pure.stores.forEach(el -> visitWithErrorHandling(el, new PackageableElementFifthPassBuilder(this.getContext(el))));
    }

    public void loadMappings(PureModelContextDataIndex pure)
    {
        pure.mappings.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.mappings.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        pure.mappings.forEach(el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
        pure.mappings.forEach(el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
    }

    public void loadConnectionsAndRuntimes(PureModelContextDataIndex pure)
    {
        // Connections must be loaded before runtimes
        pure.connections.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.connections.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
        pure.runtimes.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
        pure.runtimes.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
    }

    private void loadOtherElements(PureModelContextDataIndex pure)
    {
        this.extensions.sortExtraProcessors(pure.otherElementsByProcessor.keysView()).forEach(p ->
        {
            MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> elements = pure.otherElementsByProcessor.get(p);
            elements.forEach(el -> visitWithErrorHandling(el, new PackageableElementFirstPassBuilder(this.getContext(el))));
            elements.forEach(el -> visitWithErrorHandling(el, new PackageableElementSecondPassBuilder(this.getContext(el))));
            elements.forEach(el -> visitWithErrorHandling(el, new PackageableElementThirdPassBuilder(this.getContext(el))));
            elements.forEach(el -> visitWithErrorHandling(el, new PackageableElementFourthPassBuilder(this.getContext(el))));
            elements.forEach(el -> visitWithErrorHandling(el, new PackageableElementFifthPassBuilder(this.getContext(el))));
        });
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
            throw new EngineException("Can't find class '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class;
        try
        {
            _class = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find class '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
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
            throw new EngineException("Can't find enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
        }
        Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum> enumeration;
        try
        {
            enumeration = (Enumeration<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum>) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find enumeration '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
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
            throw new EngineException("Can't find measure '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
        }
        Measure measure;
        try
        {
            measure = (Measure) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find measure '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
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
            throw new EngineException("Can't find unit '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
        }
        Unit unit;
        try
        {
            unit = (Unit) type;
        }
        catch (ClassCastException e)
        {
            throw new EngineException("Can't find unit '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
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
                        throw new EngineException("Can't find profile '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
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

    private PureModelContextDataIndex index(PureModelContextData pureModelContextData)
    {
        PureModelContextDataIndex index = new PureModelContextDataIndex();
        MutableMap<java.lang.Class<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>,
                MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>> otherElementsByClass = Maps.mutable.empty();
        Stream.concat(pureModelContextData.streamAllElements(), pureModelContextData.sectionIndices.stream()).forEach(e ->
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
            // TODO eliminate special handling for stores
            else if (e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store)
            {
                index.stores.add((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store) e);
            }
            else
            {
                otherElementsByClass.getIfAbsentPut(e.getClass(), Lists.mutable::empty).add(e);
            }
        });
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
        private final MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store> stores = Lists.mutable.empty();
        private final MutableMap<Processor<?>, MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement>> otherElementsByProcessor = Maps.mutable.empty();
    }
}
