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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.FunctionExpressionBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
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
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CompileContext
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private static final ImmutableSet<String> SPECIAL_TYPES = _Package.SPECIAL_TYPES;
    private static final String PACKAGE_SEPARATOR = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.DEFAULT_PATH_SEPARATOR;
    private static final String META_PACKAGE_NAME = "meta";
    // NOTE: this list is taken from m3.pure in PURE
    private static final ImmutableSet<String> META_IMPORTS = Sets.immutable.with(
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
    private final ImmutableSet<String> imports;

    private CompileContext(Builder builder)
    {
        this.pureModel = builder.pureModel;
        this.imports = builder.imports;
    }

    public static class Builder
    {
        private final PureModel pureModel;
        private ImmutableSet<String> imports = META_IMPORTS;

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
            return this.withSection(this.pureModel.getSection(elementPath));
        }

        public Builder withElement(PackageableElement element)
        {
            if (element == null)
            {
                return this;
            }
            return this.withSection(this.pureModel.getSection(element));
        }

        public Builder withSection(Section section)
        {
            // NOTE: we add auto-imports regardless the type of the section or whether if there is any section at all
            // so system elements will always be resolved no matter what.
            if (section instanceof ImportAwareCodeSection)
            {
                this.imports = META_IMPORTS.newWithAll(((ImportAwareCodeSection) section).imports);
            }
            return this;
        }

        public CompileContext build()
        {
            return new CompileContext(this);
        }
    }

    public CompilerExtensions getCompilerExtensions()
    {
        return this.pureModel.extensions;
    }

    public ExecutionSupport getExecutionSupport()
    {
        return pureModel.getExecutionSupport();
    }

    public Processor<?> getExtraProcessorOrThrow(PackageableElement element)
    {
        return getCompilerExtensions().getExtraProcessorOrThrow(element);
    }

    public <T> T resolve(String path, SourceInformation sourceInformation, Function<String, T> resolver)
    {
        if (path == null)
        {
            throw new EngineException("Can't resolve from 'null' path", SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION);
        }

        // Try the find from special types (not user-defined top level types)
        if (SPECIAL_TYPES.contains(path))
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
        MutableMap<String, T> results = searchImports(path, resolver);
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
                return results.valuesView().getAny();
            }
            default:
            {
                throw new EngineException(results.keysView().makeString("Can't resolve element with path '" + path + "' - multiple matches found [", ", ", "]"), sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }


    // ------------------------------------------ ELEMENT RESOLVER -----------------------------------------

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement resolvePackageableElement(String fullPath)
    {
        return this.resolve(fullPath, SourceInformation.getUnknownSourceInformation(), path -> this.pureModel.getPackageableElement(path, SourceInformation.getUnknownSourceInformation()));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement resolvePackageableElement(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getPackageableElement(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type resolveType(String fullPath)
    {
        return this.resolveType(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type resolveType(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getType(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> resolveClass(String fullPath)
    {
        return this.resolveClass(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> resolveClass(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getClass(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner resolvePropertyOwner(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getPropertyOwner(path, sourceInformation));
    }

    public Enumeration<Enum> resolveEnumeration(String fullPath)
    {
        return this.resolveEnumeration(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Enumeration<Enum> resolveEnumeration(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getEnumeration(path, sourceInformation));
    }

    public Measure resolveMeasure(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getMeasure(path, sourceInformation));
    }

    public Unit resolveUnit(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getUnit(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association resolveAssociation(String fullPath)
    {
        return this.resolveAssociation(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association resolveAssociation(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getAssociation(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile resolveProfile(String fullPath)
    {
        return this.resolveProfile(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile resolveProfile(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getProfile(path, sourceInformation));
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> resolveConcreteFunctionDefinition(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getConcreteFunctionDefinition(path, sourceInformation));
    }

    public Store resolveStore(String fullPath)
    {
        return this.resolveStore(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Store resolveStore(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getStore(path, sourceInformation));
    }

    public Mapping resolveMapping(String fullPath)
    {
        return this.resolveMapping(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Mapping resolveMapping(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getMapping(path, sourceInformation));
    }

    public Root_meta_pure_runtime_PackageableRuntime resolvePackageableRuntime(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getPackageableRuntime(path, sourceInformation));
    }

    public Root_meta_pure_runtime_PackageableConnection resolvePackagebleConnection(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getPackageableConnection(path, sourceInformation));
    }

    public Runtime resolveRuntime(String fullPath)
    {
        return this.resolveRuntime(fullPath, SourceInformation.getUnknownSourceInformation());
    }

    public Runtime resolveRuntime(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getRuntime(path, sourceInformation));
    }

    public Connection resolveConnection(String fullPath, SourceInformation sourceInformation)
    {
        return this.resolve(fullPath, sourceInformation, path -> this.pureModel.getConnection(path, sourceInformation));
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
        functionExpression = this.pureModel.handlers.buildFunctionExpression(functionName, parameters, openVariables, sourceInformation, this, processingContext);
        if (fControl != null)
        {
            testFunction(fControl, processingContext, functionExpression.getOne());
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
            LOGGER.warn(new LogInfo((String)null, LoggingEventType.GRAPH_WRONG_FUNCTION_MATCHING_WARN, message).toString());
            if (DeploymentMode.TEST == this.pureModel.getDeploymentMode())
            {
                throw new EngineException(message);
            }
        }
    }

    public FunctionExpressionBuilder resolveFunctionBuilder(String functionName, Set<String> metaPackages, Map<String, FunctionExpressionBuilder> functionHandlerMap, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        // First do an optimistic check in the current handler to see if the function we are finding is available
        // so we don't waste time going through all of the auto-imports
        String extractedFunctionName = extractMetaFunctionName(functionName, metaPackages);

        if (functionHandlerMap.containsKey(extractedFunctionName))
        {
            return functionHandlerMap.get(extractedFunctionName);
        }

        MutableMap<String, FunctionExpressionBuilder> results = searchImports(extractedFunctionName, functionHandlerMap::get);
        switch (results.size())
        {
            case 0:
            {
                // Since we have tried to find basic function initially, this means the function builder is not found, we report error
                String message = "Can't resolve the builder for function '" + functionName + "' - stack:" + processingContext.getStack();
                LOGGER.error(new LogInfo((String)null, LoggingEventType.GRAPH_MISSING_FUNCTION, message).toString());
                throw new EngineException(message, sourceInformation, EngineErrorType.COMPILATION);
            }
            case 1:
            {
                return results.valuesView().getAny();
            }
            default:
            {
                throw new EngineException(results.keysView().makeString("Can't resolve the builder for function '" + functionName + "' - multiple matches found [", ", ", "]"), sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    private String extractMetaFunctionName(String functionName, Set<String> metaPackages)
    {
        String extractedFunctionName = functionName;
        if (functionName.contains(this.META_PACKAGE_NAME + this.PACKAGE_SEPARATOR))
        {
            String packageName = functionName.substring(0, functionName.lastIndexOf(this.PACKAGE_SEPARATOR));
            String name = functionName.substring(functionName.lastIndexOf(this.PACKAGE_SEPARATOR) + this.PACKAGE_SEPARATOR.length());
            if (metaPackages.contains(packageName))
            {
                extractedFunctionName = name;
            }
        }
        return extractedFunctionName;
    }

    private <T> MutableMap<String, T> searchImports(String name, Function<String, T> resolver)
    {
        MutableMap<String, T> results = Maps.mutable.empty();
        this.imports.forEach(importPackage ->
        {
            String fullPath = importPackage + PACKAGE_SEPARATOR + name;
            T result = null;
            try
            {
                result = resolver.apply(fullPath);
            }
            catch (Exception ignored)
            {
                // could not resolve
            }
            if (result != null)
            {
                results.put(fullPath, result);
            }
        });
        return results;
    }
}
