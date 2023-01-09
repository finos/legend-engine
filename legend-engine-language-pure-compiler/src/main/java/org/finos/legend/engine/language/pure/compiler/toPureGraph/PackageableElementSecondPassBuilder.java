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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElementReference;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_EngineRuntime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class PackageableElementSecondPassBuilder implements PackageableElementVisitor<PackageableElement>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final CompileContext context;

    public PackageableElementSecondPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public PackageableElement visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        this.context.getExtraProcessorOrThrow(element).processSecondPass(element, this.context);
        return null;
    }

    @Override
    public PackageableElement visit(Profile profile)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Enumeration _enum)
    {
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public PackageableElement visit(Class srcClass)
    {
        String fullPath = this.context.pureModel.buildPackageString(srcClass._package, srcClass.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class _class = this.context.pureModel.getClass(fullPath, srcClass.sourceInformation);
        final GenericType _classGenericType = this.context.resolveGenericType(fullPath, srcClass.sourceInformation);
        Set<String> uniqueSuperTypes = new HashSet<>();
        MutableList<Generalization> generalization = ListIterate.collect(srcClass.superTypes, superType ->
        {
            // validate no duplicated class supertype
            if (!uniqueSuperTypes.add(superType))
            {
                throw new EngineException("Duplicated super type '" + superType + "' in class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "'", srcClass.sourceInformation, EngineErrorType.COMPILATION);
            }
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))._general(this.context.resolveGenericType(superType, srcClass.sourceInformation))._specific(_class);
            if (!this.context.pureModel.isImmutable(superType))
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> superTypeClass;
                Type type = this.context.resolveType(superType, srcClass.sourceInformation);
                try
                {
                    superTypeClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
                }
                catch (ClassCastException e)
                {
                    throw new EngineException("Invalid supertype: '" + srcClass.name + "' cannot extend '" + superType + "' as it is not a class.", srcClass.sourceInformation, EngineErrorType.COMPILATION);
                }
                superTypeClass._specializationsAdd(g);
            }
            return g;
        });

        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> properties = ListIterate.collect(srcClass.properties, HelperModelBuilder.processProperty(this.context, _classGenericType, _class));
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> restrictedMilestoningProperties = Milestoning.restrictedMilestoningProperties(_class, srcClass, properties, this.context.pureModel);
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> withMilestoningProperties = properties.select(p -> !restrictedMilestoningProperties.contains(p)).withAll(Milestoning.generateMilestoningProperties(_class, this.context));

        ProcessingContext ctx = new ProcessingContext("Class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' Second Pass");
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(this.context, this.context.pureModel.buildPackageString(srcClass._package, srcClass.name));
        ctx.addInferredVariables("this", thisVariable);

        RichIterable<QualifiedProperty<?>> qualifiedProperties = ListIterate.collect(srcClass.qualifiedProperties, HelperModelBuilder.processQualifiedPropertyFirstPass(this.context, _class, this.context.pureModel.buildPackageString(srcClass._package, srcClass.name), ctx));
        _class._originalMilestonedProperties(ListIterate.collect(srcClass.originalMilestonedProperties, HelperModelBuilder.processProperty(this.context, _classGenericType, _class)))
                ._generalizations(generalization)
                ._qualifiedProperties(qualifiedProperties)
                ._properties(withMilestoningProperties);
        ctx.flushVariable("this");
        return _class;
    }

    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        String property0Ref = this.context.pureModel.addPrefixToTypeReference(srcAssociation.properties.get(0).type);
        String property1Ref = this.context.pureModel.addPrefixToTypeReference(srcAssociation.properties.get(1).type);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = this.context.pureModel.getAssociation(this.context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name), srcAssociation.sourceInformation);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> class1 = this.context.resolveClass(property0Ref, srcAssociation.properties.get(0).sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> class2 = this.context.resolveClass(property1Ref, srcAssociation.properties.get(1).sourceInformation);

        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> properties = association._properties().toList();
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> originalMilestonedProperties = association._originalMilestonedProperties().toList();
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?>> qualifiedProperties = association._qualifiedProperties().toList();

        boolean sourceIsTemporal = Milestoning.temporalStereotypes(class1._stereotypes()) != null;
        boolean targetIsTemporal = Milestoning.temporalStereotypes(class2._stereotypes()) != null;

        if (sourceIsTemporal)
        {
            Milestoning.applyMilestoningPropertyTransformations(association, class1, class2, properties, qualifiedProperties, originalMilestonedProperties);
        }

        if (targetIsTemporal)
        {
            Milestoning.applyMilestoningPropertyTransformations(association, class2, class1, properties, qualifiedProperties, originalMilestonedProperties);
        }

        assert properties.size() >= association._properties().size();
        assert qualifiedProperties.size() >= association._qualifiedProperties().size();
        assert originalMilestonedProperties.size() >= association._originalMilestonedProperties().size();

        return association._properties(properties)._qualifiedProperties(qualifiedProperties)._originalMilestonedProperties(originalMilestonedProperties);
    }

    @Override
    public PackageableElement visit(Function function)
    {
        String packageString = this.context.pureModel.buildPackageString(function._package, HelperModelBuilder.getSignature(function));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = this.context.pureModel.getConcreteFunctionDefinition(packageString, function.sourceInformation);
        ProcessingContext ctx = new ProcessingContext("Function '" + packageString + "' Second Pass");
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> body;
        try
        {
            function.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
            body = ListIterate.collect(function.body, expression -> expression.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
        }
        catch (Exception e)
        {
            LOGGER.warn(new LogInfo(null, LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build function '" + packageString + "' - stack: " + ctx.getStack()).toString());
            throw e;
        }
        FunctionType fType = ((FunctionType) targetFunc._classifierGenericType()._typeArguments().getFirst()._rawType());
        HelperModelBuilder.checkCompatibility(this.context, body.getLast()._genericType()._rawType(), body.getLast()._multiplicity(), fType._returnType()._rawType(), fType._returnMultiplicity(), "Error in function '" + packageString + "'", function.body.get(function.body.size() - 1).sourceInformation);
        ctx.pop();
        return targetFunc._expressionSequence(body);
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = this.context.pureModel.getMeasure(this.context.pureModel.buildPackageString(measure._package, measure.name), measure.sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit canonicalUnit = HelperMeasureBuilder.processUnitPackageableElementSecondPass(measure.canonicalUnit, this.context);
        targetMeasure._canonicalUnit(canonicalUnit);
        targetMeasure._nonCanonicalUnits(ListIterate.collect(measure.nonCanonicalUnits, ncu -> HelperMeasureBuilder.processUnitPackageableElementSecondPass(ncu, this.context)));
        return targetMeasure;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = this.context.pureModel.getMapping(this.context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object>> enumerationMappings = ListIterate.collect(mapping.enumerationMappings, em -> HelperMappingBuilder.processEnumMapping(em, pureMapping, this.context));
        if (enumerationMappings.isEmpty() && mapping.includedMappings.isEmpty())
        {
            return pureMapping;
        }
        if (!mapping.includedMappings.isEmpty())
        {
            RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude> mappingIncludes = ListIterate.collect(mapping.includedMappings, i ->
                    HelperMappingBuilder.processMappingInclude(i, this.context, pureMapping, this.context.resolveMapping(i.getIncludedMapping(), i.sourceInformation)));
            pureMapping._includesAddAll(mappingIncludes);
            // validate no duplicated included mappings
            Set<String> uniqueMappingIncludes = new HashSet<>();
            mapping.includedMappings.forEach(includedMapping ->
            {
                if (!uniqueMappingIncludes.add(includedMapping.getIncludedMapping()))
                {
                    throw new EngineException("Duplicated mapping include '" + includedMapping.getIncludedMapping() + "' in mapping '" + this.context.pureModel.buildPackageString(mapping._package, mapping.name) + "'", mapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        }
        pureMapping._enumerationMappings(enumerationMappings);
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        Root_meta_pure_runtime_PackageableRuntime metamodel = this.context.pureModel.getPackageableRuntime(this.context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name), packageableRuntime.sourceInformation);
        // NOTE: the whole point of this processing is to put the Pure Runtime in an index
        final org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildEngineRuntime(packageableRuntime.runtimeValue, this.context);
        this.context.pureModel.runtimesIndex.put(this.context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name), runtime);
        metamodel._runtimeValue(new Root_meta_pure_runtime_EngineRuntime_Impl("", null, context.pureModel.getClass("meta::pure::runtime::EngineRuntime"))._mappings(ListIterate.collect(packageableRuntime.runtimeValue.mappings, mappingPointer -> context.resolveMapping(mappingPointer.path, mappingPointer.sourceInformation))));
        return metamodel;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection pureConnection = this.context.pureModel.getConnection(this.context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), packageableConnection.sourceInformation);
        packageableConnection.connectionValue.accept(new ConnectionSecondPassBuilder(this.context, pureConnection));
        return null;
    }

    @Override
    public PackageableElement visit(SectionIndex sectionIndex)
    {
        return null;
    }

    @Override
    public PackageableElement visit(DataElement dataElement)
    {
        String fullPath = context.pureModel.buildPackageString(dataElement._package, dataElement.name);
        Root_meta_pure_data_DataElement compiled = (Root_meta_pure_data_DataElement) context.pureModel.getPackageableElement(fullPath);

        ProcessingContext processingContext = new ProcessingContext("Data '" + fullPath + "' Second Pass");
        Root_meta_pure_data_EmbeddedData compiledData = dataElement.data.accept(new EmbeddedDataFirstPassBuilder(context, processingContext));
        if (compiledData instanceof Root_meta_pure_data_DataElementReference)
        {
            throw new EngineException("Cannot use Data element reference in a Data element", dataElement.data.sourceInformation, EngineErrorType.COMPILATION);
        }
        compiled._data(compiledData);
        compiled._documentation(dataElement.documentation);
        return null;
    }
}
