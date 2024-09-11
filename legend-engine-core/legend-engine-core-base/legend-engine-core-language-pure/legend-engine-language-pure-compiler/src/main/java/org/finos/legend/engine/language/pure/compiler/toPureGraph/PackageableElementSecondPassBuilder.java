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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
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
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElementReference;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.Set;

public class PackageableElementSecondPassBuilder implements PackageableElementVisitor<PackageableElement>
{
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
        GenericType _classGenericType = this.context.resolveGenericType(fullPath, srcClass.sourceInformation);
        Set<String> uniqueSuperTypes = Sets.mutable.empty();
        MutableList<Generalization> generalization = ListIterate.collect(srcClass.superTypes, superTypePtr ->
        {
            String superType = superTypePtr.path;
            // validate no duplicated class supertype
            if (!uniqueSuperTypes.add(superType))
            {
                throw new EngineException("Duplicated super type '" + superType + "' in class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "'", srcClass.sourceInformation, EngineErrorType.COMPILATION);
            }
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", SourceInformationHelper.toM3SourceInformation(superTypePtr.sourceInformation), this.context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))._general(this.context.resolveGenericType(superType, superTypePtr.sourceInformation))._specific(_class);
            if (!this.context.pureModel.isImmutable(superType))
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> superTypeClass;
                Type type = this.context.resolveType(superType, superTypePtr.sourceInformation);
                try
                {
                    superTypeClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
                }
                catch (ClassCastException e)
                {
                    throw new EngineException("Invalid supertype: '" + srcClass.name + "' cannot extend '" + superType + "' as it is not a class.", srcClass.sourceInformation, EngineErrorType.COMPILATION);
                }
                synchronized (superTypeClass)
                {
                    superTypeClass._specializationsAdd(g);
                }
            }
            return g;
        });

        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> properties = ListIterate.collect(srcClass.properties, HelperModelBuilder.processProperty(this.context, _classGenericType, _class));
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> restrictedMilestoningProperties = Milestoning.restrictedMilestoningProperties(_class, srcClass, properties, this.context.pureModel);
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> withMilestoningProperties = properties.select(p -> !restrictedMilestoningProperties.contains(p)).withAll(Milestoning.generateMilestoningProperties(_class, this.context));

        ProcessingContext ctx = new ProcessingContext("Class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' Second Pass");
        ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(this.context, this.context.pureModel.buildPackageString(srcClass._package, srcClass.name));
        ctx.addInferredVariables("this", thisVariable);

        RichIterable<QualifiedProperty<?>> qualifiedProperties = ListIterate.collect(srcClass.qualifiedProperties, HelperModelBuilder.processQualifiedPropertyFirstPass(this.context, _class, this.context.pureModel.buildPackageString(srcClass._package, srcClass.name), ctx));
        _class._originalMilestonedProperties(ListIterate.collect(srcClass.originalMilestonedProperties, HelperModelBuilder.processProperty(this.context, _classGenericType, _class)))
                ._generalizations(generalization)
                ._qualifiedProperties(qualifiedProperties)
                ._properties(withMilestoningProperties);
        if (_class._generalizations().isEmpty())
        {
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                    ._general(this.context.pureModel.getGenericType("meta::pure::metamodel::type::Any"))
                    ._specific(_class);
            _class._generalizationsAdd(g);
        }
        ctx.flushVariable("this");
        return _class;
    }

    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        String packageString = this.context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = this.context.pureModel.getAssociation(packageString, srcAssociation.sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class source = this.context.resolveClass(srcAssociation.properties.get(0).type, srcAssociation.properties.get(0).sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class target = this.context.resolveClass(srcAssociation.properties.get(1).type, srcAssociation.properties.get(1).sourceInformation);

        String property0Ref = this.context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(source, this.context.pureModel.getExecutionSupport()));
        String property1Ref = this.context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(target, this.context.pureModel.getExecutionSupport()));

        // TODO generalize this validation to all platform/core types
        if ("meta::pure::metamodel::type::Any".equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(source)) ||
                "meta::pure::metamodel::type::Any".equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(target)))
        {
            throw new EngineException("Associations to Any are not allowed. Found in '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property1 = HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(property1Ref), association).valueOf(srcAssociation.properties.get(0));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property2 = HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(property0Ref), association).valueOf(srcAssociation.properties.get(1));

        synchronized (source)
        {
            source._propertiesFromAssociationsAdd(property2);
        }
        synchronized (target)
        {
            target._propertiesFromAssociationsAdd(property1);
        }

        ProcessingContext ctx = new ProcessingContext("Association " + packageString + " (second pass)");

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(this.context, property1Ref);
        ctx.addInferredVariables("this", thisVariable);

        ListIterable<QualifiedProperty<Object>> qualifiedProperties = ListIterate.collect(srcAssociation.qualifiedProperties, p ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class cl = this.context.resolveGenericType(p.returnType, p.sourceInformation)._rawType() == source ? target : source;
            return HelperModelBuilder.processQualifiedPropertyFirstPass(this.context, association, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(cl), ctx).valueOf(p);
        });
        qualifiedProperties.forEach(q ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class _class = q._genericType()._rawType() == source ? target : source;
            synchronized (_class)
            {
                _class._qualifiedPropertiesFromAssociationsAdd(q);
            }
        });
        ctx.flushVariable("this");
        return association._originalMilestonedProperties(ListIterate.collect(srcAssociation.originalMilestonedProperties, HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(srcAssociation.properties.get(0).type), association)))
                ._properties(Lists.mutable.with(property1, property2))
                ._qualifiedProperties(qualifiedProperties);
    }

    @Override
    public PackageableElement visit(Function function)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = this.context.pureModel.getMeasure(this.context.pureModel.buildPackageString(measure._package, measure.name), measure.sourceInformation);
        if (measure.canonicalUnit != null)
        {
            targetMeasure._canonicalUnit(HelperMeasureBuilder.processUnitPackageableElementSecondPass(measure.canonicalUnit, this.context));
        }
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
            CompilerExtensions extensions = context.pureModel.extensions;
            RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude> mappingIncludes =
                    ListIterate.collect(mapping.includedMappings, i ->
                    {
                        IncludedMappingHandler handler = extensions.getExtraIncludedMappingHandlers(i.getClass().getName());
                        return handler.processMappingInclude(i, this.context, pureMapping,
                                handler.resolveMapping(i, this.context));
                    });
            pureMapping._includesAddAll(mappingIncludes);
            // validate no duplicated included mappings
            Set<String> uniqueMappingIncludes = Sets.mutable.empty();
            mappingIncludes.forEach(includedMapping ->
            {
                String mappingName = IncludedMappingHandler.parseIncludedMappingNameRecursively(includedMapping);
                if (!uniqueMappingIncludes.add(mappingName))
                {
                    throw new EngineException("Duplicated mapping include '" + mappingName +
                            "' in " + "mapping " +
                            "'" + this.context.pureModel.buildPackageString(mapping._package, mapping.name) + "'", mapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        }
        pureMapping._enumerationMappings(enumerationMappings);
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        return null;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        final Root_meta_core_runtime_Connection pureConnection = this.context.pureModel.getConnection(this.context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), packageableConnection.sourceInformation);
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
        return null;
    }
}
