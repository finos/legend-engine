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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.UserDefinedFunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.List;

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
        return this.context.getExtraProcessorOrThrow(element).processFirstPass(element, this.context);
    }

    @Override
    public PackageableElement visit(Profile profile)
    {
        String fullPath = this.context.pureModel.buildPackageString(profile._package, profile.name);
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile targetProfile = new Root_meta_pure_metamodel_extension_Profile_Impl(profile.name, SourceInformationHelper.toM3SourceInformation(profile.sourceInformation), null);
        this.context.pureModel.profilesIndex.put(fullPath, targetProfile);
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(profile._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile res = targetProfile
                ._name(profile.name)
                ._p_stereotypes(ListIterate.collect(profile.stereotypes, s -> new Root_meta_pure_metamodel_extension_Stereotype_Impl(s)._value(s)._profile(targetProfile)))
                ._p_tags(ListIterate.collect(profile.tags, t -> new Root_meta_pure_metamodel_extension_Tag_Impl(t)._value(t)._profile(targetProfile)))
                ._package(pack);
        pack._childrenAdd(res);
        return res;
    }

    @Override
    public PackageableElement visit(Enumeration _enum)
    {
        String fullPath = this.context.pureModel.buildPackageString(_enum._package, _enum.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Object> en = new Root_meta_pure_metamodel_type_Enumeration_Impl<>(_enum.name);
        this.context.pureModel.typesIndex.put(fullPath, en);
        final GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(en);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        GenericType classGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Enumeration"))._typeArguments(Lists.fixedSize.of(genericType));
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(_enum._package);
        pack._childrenAdd(en);
        return en._name(_enum.name)
                 ._classifierGenericType(classGenericType)
                 ._stereotypes(ListIterate.collect(_enum.stereotypes, s -> this.context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                 ._taggedValues(ListIterate.collect(_enum.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(this.context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                 ._package(pack)
                 ._values(ListIterate.collect(_enum.values, v -> new Root_meta_pure_metamodel_type_Enum_Impl(v.value, SourceInformationHelper.toM3SourceInformation(_enum.sourceInformation), null)
                         ._classifierGenericType(genericType)
                         ._stereotypes(ListIterate.collect(v.stereotypes, s -> this.context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                         ._taggedValues(ListIterate.collect(v.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(this.context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                         ._name(v.value)));
    }

    @Override
    public PackageableElement visit(Class _class)
    {
        String fullPath = this.context.pureModel.buildPackageString(_class._package, _class.name);
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = new Root_meta_pure_metamodel_type_Class_Impl<>(_class.name, SourceInformationHelper.toM3SourceInformation(_class.sourceInformation), null);
        this.context.pureModel.typesIndex.put(fullPath, targetClass);
        final GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(targetClass);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        GenericType classGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Class"))._typeArguments(Lists.fixedSize.of(genericType));
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(_class._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> res = targetClass
                ._name(_class.name)
                ._package(pack)
                ._classifierGenericType(classGenericType)
                ._stereotypes(ListIterate.collect(_class.stereotypes, s -> this.context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                ._taggedValues(ListIterate.collect(_class.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(this.context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)));
        pack._childrenAdd(res);
        return res;
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        String fullPath = this.context.pureModel.buildPackageString(measure._package, measure.name);
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = new Root_meta_pure_metamodel_type_Measure_Impl(measure.name, SourceInformationHelper.toM3SourceInformation(measure.sourceInformation), null);
        this.context.pureModel.typesIndex.put(fullPath, targetMeasure);
        final GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(targetMeasure);
        this.context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        GenericType measureGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Measure"));
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(measure._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure res = targetMeasure
                ._name(measure.name)
                ._classifierGenericType(measureGenericType)
                ._package(pack);
        pack._childrenAdd(res);
        HelperMeasureBuilder.processUnitPackageableElementFirstPass(measure.canonicalUnit, this.context);
        ListIterate.forEach(measure.nonCanonicalUnits, ncu -> HelperMeasureBuilder.processUnitPackageableElementFirstPass(ncu, this.context));
        return res;
    }

    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        String packageString = this.context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = new Root_meta_pure_metamodel_relationship_Association_Impl(srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(srcAssociation._package);
        this.context.pureModel.associationsIndex.put(packageString, association);
        pack._childrenAdd(association);

        if (srcAssociation.properties.size() != 2)
        {
            throw new EngineException("Expected 2 properties for an association '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class source = this.context.resolveClass(srcAssociation.properties.get(0).type, srcAssociation.properties.get(0).sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class target = this.context.resolveClass(srcAssociation.properties.get(1).type, srcAssociation.properties.get(1).sourceInformation);

        String property0Ref = this.context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(source, context.pureModel.getExecutionSupport()));
        String property1Ref = this.context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(target, context.pureModel.getExecutionSupport()));

        if (org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(source).equals("meta::pure::metamodel::type::Any") || org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(target).equals("meta::pure::metamodel::type::Any"))
        {
            throw new EngineException("Associations to Any are not allowed. Found in '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property1 = HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(property1Ref), association).valueOf(srcAssociation.properties.get(0));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property2 = HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(property0Ref), association).valueOf(srcAssociation.properties.get(1));

        source._propertiesFromAssociationsAdd(property2);
        target._propertiesFromAssociationsAdd(property1);

        ProcessingContext ctx = new ProcessingContext("Association " + packageString + " (second pass)");

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(this.context, property1Ref);
        ctx.addInferredVariables("this", thisVariable);

        ListIterable<QualifiedProperty<Object>> qualifiedProperties = ListIterate.collect(srcAssociation.qualifiedProperties, p ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class cl = this.context.resolveGenericType(p.returnType, p.sourceInformation)._rawType() == source ? target : source;
            return HelperModelBuilder.processQualifiedPropertyFirstPass(this.context, association, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(cl), ctx).valueOf(p);
        });
        for (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<Object> q : qualifiedProperties)
        {
            (q._genericType()._rawType() == source ? target : source)._qualifiedPropertiesFromAssociationsAdd(q);
        }
        ctx.flushVariable("this");
        return association._name(srcAssociation.name)
                          ._originalMilestonedProperties(ListIterate.collect(srcAssociation.originalMilestonedProperties, HelperModelBuilder.processProperty(this.context, this.context.pureModel.getGenericTypeFromIndex(srcAssociation.properties.get(0).type), association)))
                          ._properties(FastList.newListWith(property1, property2))
                          ._qualifiedProperties(qualifiedProperties)
                          ._stereotypes(ListIterate.collect(srcAssociation.stereotypes, s -> this.context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                          ._taggedValues(ListIterate.collect(srcAssociation.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(this.context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                          ._package(pack);
    }

    @Override
    public PackageableElement visit(Function function)
    {
        // NOTE: in the protocol, we still store the function name as is, but in the function index, we will store the function based on its function signature
        String functionSignature = HelperModelBuilder.getSignature(function);
        String functionFullName = this.context.pureModel.buildPackageString(function._package, functionSignature);
        String functionName = this.context.pureModel.buildPackageString(function._package,  HelperModelBuilder.getFunctionNameWithoutSignature(function) );
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = new Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl<>(functionSignature, SourceInformationHelper.toM3SourceInformation(function.sourceInformation), null);
        this.context.pureModel.functionsIndex.put(functionFullName, targetFunc);

        ProcessingContext ctx = new ProcessingContext("Function '" + functionFullName + "' First Pass");

        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(function._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> res = targetFunc
                ._name(HelperModelBuilder.getTerseSignature(function)) // function signature here - e.g. isAfterDay_Date_1__Date_1__Boolean_1_
                ._functionName(functionName) // function name to be used in the handler map -> meta::pure::functions::date::isAfterDay
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(this.context.pureModel.getType("meta::pure::metamodel::function::ConcreteFunctionDefinition"))
                                                                                                       ._typeArguments(Lists.fixedSize.of(PureModel.buildFunctionType(ListIterate.collect(function.parameters, p -> (VariableExpression) p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx))), this.context.resolveGenericType(function.returnType, function.sourceInformation), this.context.pureModel.getMultiplicity(function.returnMultiplicity)))))
                ._stereotypes(ListIterate.collect(function.stereotypes, s -> this.context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                ._taggedValues(ListIterate.collect(function.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(this.context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                ._package(pack);
        HelperModelBuilder.processFunctionConstraints(function, this.context, res, ctx);
        pack._childrenAdd(res);

        this.context.pureModel.handlers.register(new UserDefinedFunctionHandler(functionFullName, res,
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
        return res;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = new Root_meta_pure_mapping_Mapping_Impl(mapping.name);
        this.context.pureModel.mappingsIndex.put(this.context.pureModel.buildPackageString(mapping._package, mapping.name), pureMapping);
        GenericType mappingGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(this.context.pureModel.getType("meta::pure::mapping::Mapping"));
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(mapping._package);
        pureMapping._name(mapping.name)
                   ._package(pack)
                   ._classifierGenericType(mappingGenericType);
        pack._childrenAdd(pureMapping);
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        // NOTE: we stub out since this element doesn't have an equivalent packageable element form in PURE metamodel
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(packageableRuntime._package);
        PackageableElement stub = new Root_meta_pure_runtime_PackageableRuntime_Impl("")._package(pack)._name(packageableRuntime.name);
        pack._childrenAdd(stub);
        return stub;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        // NOTE: we stub out since this element doesn't have an equivalent packageable element form in PURE metamodel
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(packageableConnection._package);
        PackageableElement stub = new Root_meta_pure_runtime_PackageableConnection_Impl("")._package(pack)._name(packageableConnection.name);
        pack._childrenAdd(stub);
        // NOTE: the whole point of this processing is to put the Pure Connection in an index
        final org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection connection = packageableConnection.connectionValue.accept(new ConnectionFirstPassBuilder(this.context));
        this.context.pureModel.connectionsIndex.put(this.context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), connection);
        return stub;
    }

    @Override
    public PackageableElement visit(SectionIndex sectionIndex)
    {
        // NOTE: we stub out since this element doesn't have an equivalent packageable element form in PURE metamodel
        org.finos.legend.pure.m3.coreinstance.Package pack = this.context.pureModel.getOrCreatePackage(sectionIndex._package);
        PackageableElement stub = new Root_meta_pure_metamodel_PackageableElement_Impl("")._package(pack)._name(sectionIndex.name);
        pack._childrenAdd(stub);
        // NOTE: we don't really need to add section index to the PURE graph
        ListIterate.forEach(sectionIndex.sections, section -> ListIterate.forEach(section.elements, elementPath -> this.context.pureModel.sectionsIndex.putIfAbsent(elementPath, section)));
        return stub;
    }
}
