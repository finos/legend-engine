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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.UserDefinedFunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Mapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Profile_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Stereotype_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Tag_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Association_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_section_SectionIndex;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_section_SectionIndex_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enum_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Measure_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime_Impl;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.List;
import java.util.Objects;

public class PackageableElementFirstPassBuilder implements PackageableElementVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>
{
    private final CompileContext context;

    public PackageableElementFirstPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public PackageableElement visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return setNameAndPackage(this.context.getExtraProcessorOrThrow(element).processFirstPass(element, this.context), element);
    }

    @Override
    public PackageableElement visit(Profile profile)
    {
        String fullPath = this.context.pureModel.buildPackageString(profile._package, profile.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile targetProfile = new Root_meta_pure_metamodel_extension_Profile_Impl(profile.name, SourceInformationHelper.toM3SourceInformation(profile.sourceInformation), this.context.pureModel.getClass("meta::pure::metamodel::extension::Profile"));
        this.context.pureModel.profilesIndex.put(fullPath, targetProfile);
        setNameAndPackage(targetProfile, profile);
        return targetProfile
                ._p_stereotypes(ListIterate.collect(profile.stereotypes, st -> newStereotype(targetProfile, st)))
                ._p_tags(ListIterate.collect(profile.tags, t -> newTag(targetProfile, t)));
    }

    @Override
    public PackageableElement visit(Enumeration _enum)
    {
        String fullPath = this.context.pureModel.buildPackageString(_enum._package, _enum.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Object> en = new Root_meta_pure_metamodel_type_Enumeration_Impl<>(_enum.name, SourceInformationHelper.toM3SourceInformation(_enum.sourceInformation), this.context.pureModel.getClass("meta::pure::metamodel::type::Enumeration"));
        this.context.pureModel.typesIndex.put(fullPath, en);
        GenericType genericType = newGenericType(en);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        setNameAndPackage(en, _enum);
        return en._classifierGenericType(newGenericType(this.context.pureModel.getClass("meta::pure::metamodel::type::Enumeration"), genericType))
                ._stereotypes(ListIterate.collect(_enum.stereotypes, this::resolveStereotype))
                ._taggedValues(ListIterate.collect(_enum.taggedValues, this::newTaggedValue))
                ._values(ListIterate.collect(_enum.values, v -> new Root_meta_pure_metamodel_type_Enum_Impl(v.value, SourceInformationHelper.toM3SourceInformation(v.sourceInformation), null)
                        ._classifierGenericType(genericType)
                        ._stereotypes(ListIterate.collect(v.stereotypes, this::resolveStereotype))
                        ._taggedValues(ListIterate.collect(v.taggedValues, this::newTaggedValue))
                        ._name(v.value)));
    }

    @Override
    public PackageableElement visit(Class _class)
    {
        String fullPath = this.context.pureModel.buildPackageString(_class._package, _class.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = new Root_meta_pure_metamodel_type_Class_Impl<>(_class.name, SourceInformationHelper.toM3SourceInformation(_class.sourceInformation), this.context.pureModel.getClass("meta::pure::metamodel::type::Class"));
        this.context.pureModel.typesIndex.put(fullPath, targetClass);
        GenericType genericType = newGenericType(targetClass);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        setNameAndPackage(targetClass, _class);
        return targetClass._classifierGenericType(newGenericType(this.context.pureModel.getType("meta::pure::metamodel::type::Class"), genericType))
                ._stereotypes(ListIterate.collect(_class.stereotypes, this::resolveStereotype))
                ._taggedValues(ListIterate.collect(_class.taggedValues, this::newTaggedValue));
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        String fullPath = this.context.pureModel.buildPackageString(measure._package, measure.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = new Root_meta_pure_metamodel_type_Measure_Impl(measure.name, SourceInformationHelper.toM3SourceInformation(measure.sourceInformation), null);
        this.context.pureModel.typesIndex.put(fullPath, targetMeasure);
        GenericType genericType = newGenericType(targetMeasure);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        targetMeasure._classifierGenericType(newGenericType(this.context.pureModel.getType("meta::pure::metamodel::type::Measure")));
        HelperMeasureBuilder.processUnitPackageableElementFirstPass(measure.canonicalUnit, this.context);
        measure.nonCanonicalUnits.forEach(ncu -> HelperMeasureBuilder.processUnitPackageableElementFirstPass(ncu, this.context));
        return setNameAndPackage(targetMeasure, measure);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        String packageString = this.context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = new Root_meta_pure_metamodel_relationship_Association_Impl(srcAssociation.name, null, this.context.pureModel.getClass("meta::pure::metamodel::relationship::Association"));
        this.context.pureModel.associationsIndex.put(packageString, association);

        if (srcAssociation.properties.size() != 2)
        {
            throw new EngineException("Expected 2 properties for an association '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }
        setNameAndPackage(association, srcAssociation);
        return association._stereotypes(ListIterate.collect(srcAssociation.stereotypes, this::resolveStereotype))
                ._taggedValues(ListIterate.collect(srcAssociation.taggedValues, this::newTaggedValue));
    }

    @Override
    public PackageableElement visit(Function function)
    {
        // NOTE: in the protocol, we still store the function name as is, but in the function index, we will store the function based on its function signature
        String functionSignature = HelperModelBuilder.getSignature(function);
        String functionFullName = this.context.pureModel.buildPackageString(function._package, functionSignature);
        String functionName = this.context.pureModel.buildPackageString(function._package, HelperModelBuilder.getFunctionNameWithoutSignature(function));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = new Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl<>(functionSignature, SourceInformationHelper.toM3SourceInformation(function.sourceInformation), null);
        this.context.pureModel.functionsIndex.put(functionFullName, targetFunc);

        ProcessingContext ctx = new ProcessingContext("Function '" + functionFullName + "' First Pass");

        setNameAndPackage(targetFunc, functionSignature, function._package, function.sourceInformation)
                ._functionName(functionName) // function name to be used in the handler map -> meta::pure::functions::date::isAfterDay
                ._classifierGenericType(newGenericType(this.context.pureModel.getType("meta::pure::metamodel::function::ConcreteFunctionDefinition"), PureModel.buildFunctionType(ListIterate.collect(function.parameters, p -> (VariableExpression) p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx))), this.context.resolveGenericType(function.returnType, function.sourceInformation), this.context.pureModel.getMultiplicity(function.returnMultiplicity), this.context.pureModel)))
                ._stereotypes(ListIterate.collect(function.stereotypes, this::resolveStereotype))
                ._taggedValues(ListIterate.collect(function.taggedValues, this::newTaggedValue));
        HelperModelBuilder.processFunctionConstraints(function, this.context, targetFunc, ctx);

        this.context.pureModel.handlers.register(new UserDefinedFunctionHandler(this.context.pureModel, functionFullName, targetFunc,
                ps -> new TypeAndMultiplicity(this.context.resolveGenericType(function.returnType, function.sourceInformation), this.context.pureModel.getMultiplicity(function.returnMultiplicity)),
                ps ->
                {
                    List<ValueSpecification> vs = ListIterate.collect(function.parameters, p -> p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
                    if (ps.size() == function.parameters.size())
                    {
                        int size = ps.size();
                        // TODO clean up the check....
                        try
                        {
                            for (int i = 0; i < size; i++)
                            {
                                HelperModelBuilder.checkCompatibility(this.context, ps.get(i)._genericType()._rawType(), ps.get(i)._multiplicity(), vs.get(i)._genericType()._rawType(), vs.get(i)._multiplicity(), "Error in function '" + functionFullName + "'", function.body.get(function.body.size() - 1).sourceInformation);
                            }
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }));
        return targetFunc;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = new Root_meta_pure_mapping_Mapping_Impl(mapping.name, SourceInformationHelper.toM3SourceInformation(mapping.sourceInformation), null);
        this.context.pureModel.mappingsIndex.put(this.context.pureModel.buildPackageString(mapping._package, mapping.name), pureMapping);
        GenericType mappingGenericType = newGenericType(this.context.pureModel.getType("meta::pure::mapping::Mapping"));
        pureMapping._classifierGenericType(mappingGenericType);
        return setNameAndPackage(pureMapping, mapping);
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        Root_meta_pure_runtime_PackageableRuntime metamodel = new Root_meta_pure_runtime_PackageableRuntime_Impl(packageableRuntime.name, SourceInformationHelper.toM3SourceInformation(packageableRuntime.sourceInformation), this.context.pureModel.getClass("meta::pure::runtime::PackageableRuntime"));
        this.context.pureModel.packageableRuntimesIndex.put(this.context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name), metamodel);
        GenericType packageableRuntimeGenericType = newGenericType(this.context.pureModel.getType("meta::pure::runtime::PackageableRuntime"));
        metamodel._classifierGenericType(packageableRuntimeGenericType);

        // NOTE: the whole point of this processing is to put the Pure Runtime in an index
        Root_meta_core_runtime_Runtime pureRuntime = new Root_meta_core_runtime_Runtime_Impl("Root::meta::core::runtime::Runtime", SourceInformationHelper.toM3SourceInformation(packageableRuntime.sourceInformation), this.context.pureModel.getClass("meta::core::runtime::Runtime"));
        this.context.pureModel.runtimesIndex.put(this.context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name), pureRuntime);

        return setNameAndPackage(metamodel, packageableRuntime);
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        Root_meta_pure_runtime_PackageableConnection metamodel = new Root_meta_pure_runtime_PackageableConnection_Impl(packageableConnection.name, SourceInformationHelper.toM3SourceInformation(packageableConnection.sourceInformation), this.context.pureModel.getClass("meta::pure::runtime::PackageableConnection"));
        this.context.pureModel.packageableConnectionsIndex.put(this.context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), metamodel);
        // NOTE: the whole point of this processing is to put the Pure Connection in an index
        Root_meta_core_runtime_Connection connection = packageableConnection.connectionValue.accept(new ConnectionFirstPassBuilder(this.context));
        this.context.pureModel.connectionsIndex.put(this.context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), connection);
        return setNameAndPackage(metamodel, packageableConnection);
    }

    @Override
    public PackageableElement visit(SectionIndex sectionIndex)
    {
        Root_meta_pure_metamodel_section_SectionIndex stub = new Root_meta_pure_metamodel_section_SectionIndex_Impl(sectionIndex.name, SourceInformationHelper.toM3SourceInformation(sectionIndex.sourceInformation), this.context.pureModel.getClass("meta::pure::metamodel::section::SectionIndex"));
        // NOTE: we don't really need to add section index to the PURE graph
        sectionIndex.sections.forEach(section -> section.elements.forEach(elementPath -> this.context.pureModel.sectionsIndex.putIfAbsent(elementPath, section)));
        return setNameAndPackage(stub, sectionIndex);
    }

    @Override
    public PackageableElement visit(DataElement dataElement)
    {
        Root_meta_pure_data_DataElement compiled = new Root_meta_pure_data_DataElement_Impl(dataElement.name, SourceInformationHelper.toM3SourceInformation(dataElement.sourceInformation), null);
        GenericType mappingGenericType = newGenericType(this.context.pureModel.getType("meta::pure::data::DataElement"));
        setNameAndPackage(compiled, dataElement);
        return compiled._classifierGenericType(mappingGenericType)
                ._stereotypes(ListIterate.collect(dataElement.stereotypes, this::resolveStereotype))
                ._taggedValues(ListIterate.collect(dataElement.taggedValues, this::newTaggedValue));
    }

    private GenericType newGenericType(Type rawType)
    {
        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(rawType);
    }

    private GenericType newGenericType(Type rawType, GenericType typeArgument)
    {
        return newGenericType(rawType, Lists.fixedSize.with(typeArgument));
    }

    private GenericType newGenericType(Type rawType, RichIterable<? extends GenericType> typeArguments)
    {
        return newGenericType(rawType)._typeArguments(typeArguments);
    }

    private Stereotype newStereotype(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String name)
    {
        return new Root_meta_pure_metamodel_extension_Stereotype_Impl(name, null, this.context.pureModel.getClass("meta::pure::metamodel::extension::Stereotype"))
                ._value(name)
                ._profile(profile);
    }

    private Tag newTag(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String name)
    {
        return new Root_meta_pure_metamodel_extension_Tag_Impl(name, null, this.context.pureModel.getClass("meta::pure::metamodel::extension::Tag"))
                ._value(name)
                ._profile(profile);
    }

    private TaggedValue newTaggedValue(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue taggedValue)
    {
        return new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))
                ._tag(resolveTag(taggedValue.tag))
                ._value(taggedValue.value);
    }

    private Tag resolveTag(TagPtr tagPointer)
    {
        return this.context.resolveTag(tagPointer.profile, tagPointer.value, tagPointer.profileSourceInformation, tagPointer.sourceInformation);
    }

    private Stereotype resolveStereotype(StereotypePtr stereotypePointer)
    {
        return this.context.resolveStereotype(stereotypePointer.profile, stereotypePointer.value, stereotypePointer.profileSourceInformation, stereotypePointer.sourceInformation);
    }

    private <T extends PackageableElement> T setNameAndPackage(T pureElement, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement sourceElement)
    {
        return setNameAndPackage(pureElement, sourceElement.name, sourceElement._package, sourceElement.sourceInformation);
    }

    private <T extends PackageableElement> T setNameAndPackage(T pureElement, String name, String packagePath, SourceInformation sourceInformation)
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

        synchronized (this.context.pureModel)
        {
            // Validate and set package
            Package pack = this.context.pureModel.getOrCreatePackage(packagePath);
            if (pack._children().anySatisfy(c -> name.equals(c._name())))
            {
                throw new EngineException("An element named '" + name + "' already exists in the package '" + packagePath + "'", sourceInformation, EngineErrorType.COMPILATION);
            }
            pureElement._package(pack);
            pureElement.setSourceInformation(SourceInformationHelper.toM3SourceInformation(sourceInformation));
            pack._childrenAdd(pureElement);
        }
        return pureElement;
    }
}
