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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.FunctionExpressionBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CompileContext
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    // NOTE: this list is taken from ImportStub resolution in PURE
    public static final ImmutableSet<String> PRIMITIVE_TYPES = Sets.immutable.with(
            "Boolean", "String", "Binary",
            "Date", "StrictDate", "DateTime", "LatestDate",
            "Number", "Float", "Decimal", "Integer"
    );
    public static final ImmutableSet<String> SPECIAL_TYPES = PRIMITIVE_TYPES.union(Sets.immutable.with(
            "Package"
    ));
    private static final String PACKAGE_SEPARATOR = "::";
    // NOTE: this list is taken from m3.pure in PURE
    private static final List<String> AUTO_IMPORTS = FastList.newListWith(
            "meta::pure::metamodel",
            "meta::pure::metamodel::type",
            "meta::pure::metamodel::type::generics",
            "meta::pure::metamodel::relationship",
            "meta::pure::metamodel::valuespecification",
            "meta::pure::metamodel::multiplicity",
            "meta::pure::metamodel::function",
            "meta::pure::metamodel::function::property",
            "meta::pure::metamodel::extension",
            "meta::pure::metamodel::import",
            "meta::pure::functions::date",
            "meta::pure::functions::string",
            "meta::pure::functions::collection",
            "meta::pure::functions::meta",
            "meta::pure::functions::constraints",
            "meta::pure::functions::lang",
            "meta::pure::functions::boolean",
            "meta::pure::functions::tools",
            "meta::pure::functions::io",
            "meta::pure::functions::math",
            "meta::pure::functions::asserts",
            "meta::pure::functions::test",
            "meta::pure::functions::multiplicity",
            "meta::pure::router",
            "meta::pure::service",
            "meta::pure::tds",
            "meta::pure::tools",
            "meta::pure::profiles"
    );

    public final PureModel pureModel;
    private final List<String> imports;
    public final List<CompilerExtension> extensions;
    public final List<Function2<PackageableElement, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> extraPackageableElementFirstPassProcessors;
    public final List<Procedure2<PackageableElement, CompileContext>> extraPackageableElementSecondPassProcessors;
    public final List<Procedure2<PackageableElement, CompileContext>> extraPackageableElementThirdPassProcessors;
    public final List<Procedure2<PackageableElement, CompileContext>> extraPackageableElementFourthPassProcessors;
    public final List<Procedure2<PackageableElement, CompileContext>> extraPackageableElementFifthPassProcessors;
    public final List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> extraClassMappingFirstPassProcessors;
    public final List<Procedure3<ClassMapping, Mapping, CompileContext>> extraClassMappingSecondPassProcessors;
    public final List<Function3<AssociationMapping, Mapping, CompileContext, AssociationImplementation>> extraAssociationMappingProcessors;
    public final List<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection, CompileContext, Connection>> extraConnectionValueProcessors;
    public final List<Procedure2<InputData, CompileContext>> extraMappingTestInputDataProcessors;
    public final List<org.eclipse.collections.api.block.function.Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> extraFunctionHandlerDispatchBuilderInfoCollectors;
    public final List<org.eclipse.collections.api.block.function.Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> extraFunctionExpressionBuilderRegistrationInfoCollectors;
    public final List<org.eclipse.collections.api.block.function.Function<Handlers, List<FunctionHandlerRegistrationInfo>>> extraFunctionHandlerRegistrationInfoCollectors;
    public final List<Function4<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification, CompileContext, List<String>, ProcessingContext, ValueSpecification>> extraValueSpecificationProcessors;
    public final List<Procedure2<PackageableElement, MutableMap<String, String>>> extraStoreStatBuilders;
    public final List<Function2<ExecutionContext, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext>> extraExecutionContextProcessors;
    public final List<Procedure<Procedure2<String, List<String>>>> extraElementForPathToElementRegisters;
    public final List<Procedure3<SetImplementation, Set<String>, CompileContext>> extraSetImplementationSourceScanners;

    private CompileContext(Builder builder)
    {
        this.pureModel = builder.pureModel;
        this.imports = builder.imports;
        this.extensions = builder.pureModel.extensions;
        this.extraPackageableElementFirstPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPackageableElementFirstPassProcessors);
        this.extraPackageableElementSecondPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPackageableElementSecondPassProcessors);
        this.extraPackageableElementThirdPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPackageableElementThirdPassProcessors);
        this.extraPackageableElementFourthPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPackageableElementFourthPassProcessors);
        this.extraPackageableElementFifthPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraPackageableElementFifthPassProcessors);
        this.extraClassMappingFirstPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraClassMappingFirstPassProcessors);
        this.extraClassMappingSecondPassProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraClassMappingSecondPassProcessors);
        this.extraAssociationMappingProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraAssociationMappingProcessors);
        this.extraConnectionValueProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraConnectionValueProcessors);
        this.extraMappingTestInputDataProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraMappingTestInputDataProcessors);
        this.extraFunctionHandlerDispatchBuilderInfoCollectors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraFunctionHandlerDispatchBuilderInfoCollectors);
        this.extraFunctionExpressionBuilderRegistrationInfoCollectors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraFunctionExpressionBuilderRegistrationInfoCollectors);
        this.extraFunctionHandlerRegistrationInfoCollectors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraFunctionHandlerRegistrationInfoCollectors);
        this.extraValueSpecificationProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraValueSpecificationProcessors);
        this.extraStoreStatBuilders = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraStoreStatBuilders);
        this.extraExecutionContextProcessors = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraExecutionContextProcessors);
        this.extraElementForPathToElementRegisters = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraElementForPathToElementRegisters);
        this.extraSetImplementationSourceScanners = ListIterate.flatCollect(this.extensions, CompilerExtension::getExtraSetImplementationSourceScanners);
    }

    public static class Builder
    {
        private final PureModel pureModel;
        private List<String> imports = new ArrayList<>();

        public Builder(PureModel pureModel)
        {
            this.pureModel = pureModel;
        }

        public Builder withElement(String elementPath)
        {
            if (elementPath == null)
            {
                return this;
            }
            return this.withSection(pureModel.getSection(elementPath));
        }

        public Builder withElement(PackageableElement element)
        {
            if (element == null)
            {
                return this;
            }
            return this.withSection(pureModel.getSection(element));
        }

        public Builder withSection(Section section)
        {
            this.imports = new ArrayList<>();
            // NOTE: we add auto-imports regardless the type of the section or whether if there is any section at all
            // so system elements will always be resolved no matter what.
            this.imports.addAll(AUTO_IMPORTS);
            if (section instanceof ImportAwareCodeSection)
            {
                this.imports.addAll(((ImportAwareCodeSection) section).imports);
            }
            this.imports = ListIterate.distinct(this.imports); // remove duplicates
            return this;
        }

        public CompileContext build()
        {
            return new CompileContext(this);
        }
    }

    public <T> T resolve(String path, SourceInformation sourceInformation, Function<String, T> resolver)
    {
        if (path == null)
        {
            throw new EngineException("Can't resolve from 'null' path", SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION);
        }

        // Try the find from special types (not user-defined top level types)
        if (CompileContext.SPECIAL_TYPES.contains(path))
        {
            return resolver.apply(path);
        }

        // if the path is a path with package, no resolution from import is needed
        if (path.contains(PACKAGE_SEPARATOR))
        {
            return resolver.apply(path);
        }

        // NOTE: here we make the assumption that we have populated the indices properly so the same element
        // is not referred using 2 different paths in the same element index
        MutableMap<String, T> results = UnifiedMap.newMap();
        ListIterate.distinct(this.imports).forEach(importPackage ->
        {
            try
            {
                String fullPath = importPackage + PACKAGE_SEPARATOR + path;
                T result = resolver.apply(fullPath);
                if (result != null)
                {
                    results.put(fullPath, result);
                }
            }
            catch (Exception ignored)
            {
            }
        });

        switch (results.size())
        {
            case 0:
            {
                // NOTE: if nothing is found then we will try to find user-defined elements at root package (i.e. no package)
                // We place this after import resolution since we want to emphasize that this type of element has the lowest precedence
                // In fact, due to the restriction that engine imposes on element path, the only kinds of element
                // we could find at this level are packages, but they will not fit the type we look for
                // in PURE, since we resolve to CoreInstance, further validation needs to be done to make the resolution complete
                // here we count on the `resolver` to do the validation of the type of element instead
                return resolver.apply(path);
            }
            case 1:
            {
                return results.values().iterator().next();
            }
            default:
            {
                throw new EngineException(results.keysView().makeString("Can't resolve element with path '" + path + "' - multiple matches found [", ", ", "]"), sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }


    // ------------------------------------------ ELEMENT RESOLVER -----------------------------------------

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type resolveType(String fullPath)
    {
        return this.resolveType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type resolveType(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getType(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Object> resolveClass(String fullPath)
    {
        return this.resolveClass(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Object> resolveClass(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getClass(path, sourceInformation));
    }

    public Enumeration<Enum> resolveEnumeration(String fullPath)
    {
        return this.resolveEnumeration(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Enumeration<Enum> resolveEnumeration(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getEnumeration(path, sourceInformation));
    }

    public Measure resolveMeasure(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getMeasure(path, sourceInformation));
    }

    public Unit resolveUnit(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getUnit(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association resolveAssociation(String fullPath)
    {
        return this.resolveAssociation(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association resolveAssociation(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getAssociation(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile resolveProfile(String fullPath)
    {
        return this.resolveProfile(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile resolveProfile(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getProfile(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition resolveConcreteFunctionDefinition(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getConcreteFunctionDefinition(path, sourceInformation));
    }

    public Store resolveStore(String fullPath)
    {
        return this.resolveStore(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Store resolveStore(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getStore(path, sourceInformation));
    }

    public Mapping resolveMapping(String fullPath)
    {
        return this.resolveMapping(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Mapping resolveMapping(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getMapping(path, sourceInformation));
    }

    public Runtime resolveRuntime(String fullPath)
    {
        return this.resolveRuntime(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Runtime resolveRuntime(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getRuntime(path, sourceInformation));
    }

    public Connection resolveConnection(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, (String path) -> this.pureModel.getConnection(path, sourceInformation));
    }


    // ------------------------------------------ SUB-ELEMENT RESOLVER -----------------------------------------

    public GenericType resolveGenericType(String fullPath)
    {
        return this.resolveGenericType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public GenericType resolveGenericType(String fullPath, SourceInformation sourceInformation)
    {
        return this.pureModel.getGenericType(this.resolveType(fullPath, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> resolveProperty(String fullPath, String propertyName)
    {
        return this.resolveProperty(fullPath, propertyName, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty<?> resolveProperty(String fullPath, String propertyName, SourceInformation classSourceInformation, SourceInformation sourceInformation)
    {
        return this.pureModel.getProperty(this.resolveClass(fullPath, classSourceInformation), fullPath, propertyName, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum resolveEnumValue(String fullPath, String value)
    {
        return this.resolveEnumValue(fullPath, value, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum resolveEnumValue(String fullPath, String value, SourceInformation enumerationSourceInformation, SourceInformation sourceInformation)
    {
        return this.pureModel.getEnumValue(this.resolveEnumeration(fullPath, enumerationSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype resolveStereotype(String fullPath, String value)
    {
        return this.resolveStereotype(fullPath, value, SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype resolveStereotype(String fullPath, String value, SourceInformation profileSourceInformation, SourceInformation sourceInformation)
    {
        return this.pureModel.getStereotype(this.resolveProfile(fullPath, profileSourceInformation), fullPath, value, sourceInformation);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag resolveTag(String fullPath, String value, SourceInformation profileSourceInformation, SourceInformation sourceInformation)
    {
        return this.pureModel.getTag(this.resolveProfile(fullPath, profileSourceInformation), fullPath, value, sourceInformation);
    }


    // ------------------------------------------ FUNCTION EXPRESSION BUILDER -----------------------------------------

    public Pair<SimpleFunctionExpression, List<ValueSpecification>> buildFunctionExpression(String functionName, String fControl, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, MutableList<String> openVariables, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        Pair<SimpleFunctionExpression, List<ValueSpecification>> functionExpression;
        if (functionName.substring(functionName.lastIndexOf(':') + 1).contains("_"))
        {
            // Keep for old flow: something like `toOne_T_MANY__T_1_`
            functionExpression = this.pureModel.handlers.buildFunctionExpression(functionName.substring(0, functionName.indexOf("_")), parameters, openVariables, sourceInformation, this, processingContext);
            testFunction(functionName, processingContext, functionExpression.getOne());
        }
        else
        {
            functionExpression = this.pureModel.handlers.buildFunctionExpression(functionName, parameters, openVariables, sourceInformation, this, processingContext);
            if (fControl != null)
            {
                testFunction(fControl, processingContext, functionExpression.getOne());
            }
        }
        return functionExpression;
    }

    private void testFunction(String functionName, ProcessingContext processingContext, SimpleFunctionExpression handler)
    {
        if (handler == null || !functionName.equals(handler._func()._name()))
        {
            String message = handler == null
                    ? "Pure graph function: '" + functionName + "' doesn't have a matched function - stack:" + processingContext.getStack()
                    // TODO: check if we call function by its name or full path here
                    : "Pure graph function: '" + functionName + "' doesn't match the found function: '" + handler._func()._name() + "' - stack:" + processingContext.getStack();
            LOGGER.warn(new LogInfo(null, LoggingEventType.GRAPH_WRONG_FUNCTION_MATCHING_WARN, message).toString());
            if (DeploymentMode.TEST == this.pureModel.getDeploymentMode())
            {
                throw new EngineException(message);
            }
        }
    }

    public FunctionExpressionBuilder resolveFunctionBuilder(String functionName, Map<String, FunctionExpressionBuilder> functionHandlerMap, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        // First do an optimistic check in the current handler to see if the function we are finding is available
        // so we don't waste time going through all of the auto-imports
        if (functionHandlerMap.containsKey(functionName))
        {
            return functionHandlerMap.get(functionName);
        }

        MutableMap<String, FunctionExpressionBuilder> results = UnifiedMap.newMap();
        ListIterate.distinct(this.imports).forEach(importPackage ->
        {
            try
            {
                String fullPath = importPackage + PACKAGE_SEPARATOR + functionName;
                FunctionExpressionBuilder result = functionHandlerMap.get(fullPath);
                if (result != null)
                {
                    results.put(fullPath, result);
                }
            }
            catch (Exception ignored)
            {
            }
        });

        switch (results.size())
        {
            case 0:
            {
                // Since we have tried to find basic function initially, this means the function builder is not found, we report error
                String message = "Can't resolve the builder for function '" + functionName + "' - stack:" + processingContext.getStack();
                LOGGER.error(new LogInfo(null, LoggingEventType.GRAPH_MISSING_FUNCTION, message).toString());
                throw new EngineException(message, sourceInformation, EngineErrorType.COMPILATION);
            }
            case 1:
            {
                return results.values().iterator().next();
            }
            default:
            {
                throw new EngineException(results.keysView().makeString("Can't resolve the builder for function '" + functionName + "' - multiple matches found [", ", ", "]"), sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }
}
