// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.relationship.Association;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Association_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.Set;

public class AssociationCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Association");
    }

    @Override
    public CompilerExtension build()
    {
        return new AssociationCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Association.class,
                        Lists.fixedSize.with(Class.class),
                        this::associationFirstPass,
                        this::associationSecondPass,
                        this::associationThirdPass,
                        this::associationPrerequisiteElementsPass
                )
        );
    }

    private PackageableElement associationFirstPass(Association srcAssociation, CompileContext context)
    {
        String packageString = context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = new Root_meta_pure_metamodel_relationship_Association_Impl(srcAssociation.name, null, context.pureModel.getClass("meta::pure::metamodel::relationship::Association"));

        if (srcAssociation.properties.size() != 2)
        {
            throw new EngineException("Expected 2 properties for an association '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }
        return association._stereotypes(ListIterate.collect(srcAssociation.stereotypes, context::resolveStereotype))
                ._taggedValues(ListIterate.collect(srcAssociation.taggedValues, context::newTaggedValue));
    }

    private void associationSecondPass(Association srcAssociation, CompileContext context)
    {
        String packageString = context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = context.pureModel.getAssociation(packageString, srcAssociation.sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class source = context.resolveClass(((PackageableType) srcAssociation.properties.get(0).genericType.rawType).fullPath, srcAssociation.properties.get(0).sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class target = context.resolveClass(((PackageableType) srcAssociation.properties.get(1).genericType.rawType).fullPath, srcAssociation.properties.get(1).sourceInformation);

        String property0Ref = context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(source, context.pureModel.getExecutionSupport()));
        String property1Ref = context.pureModel.addPrefixToTypeReference(HelperModelBuilder.getElementFullPath(target, context.pureModel.getExecutionSupport()));

        // TODO generalize this validation to all platform/core types
        if ("meta::pure::metamodel::type::Any".equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(source)) ||
                "meta::pure::metamodel::type::Any".equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(target)))
        {
            throw new EngineException("Associations to Any are not allowed. Found in '" + packageString + "'", srcAssociation.sourceInformation, EngineErrorType.COMPILATION);
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property1 = HelperModelBuilder.processProperty(context, context.pureModel.getGenericTypeFromIndex(property1Ref), association).valueOf(srcAssociation.properties.get(0));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object> property2 = HelperModelBuilder.processProperty(context, context.pureModel.getGenericTypeFromIndex(property0Ref), association).valueOf(srcAssociation.properties.get(1));

        synchronized (source)
        {
            source._propertiesFromAssociationsAdd(property2);
        }
        synchronized (target)
        {
            target._propertiesFromAssociationsAdd(property1);
        }

        ProcessingContext ctx = new ProcessingContext("Association " + packageString + " (second pass)");

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(context, property1Ref);
        ctx.addInferredVariables("this", thisVariable);

        ListIterable<QualifiedProperty<Object>> qualifiedProperties = ListIterate.collect(srcAssociation.qualifiedProperties, p ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class cl = context.newGenericType(p.returnGenericType)._rawType() == source ? target : source;
            return HelperModelBuilder.processQualifiedPropertyFirstPass(context, association, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(cl), ctx).valueOf(p);
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
        association._originalMilestonedProperties(ListIterate.collect(srcAssociation.originalMilestonedProperties, HelperModelBuilder.processProperty(context, context.newGenericType(srcAssociation.properties.get(0).genericType), association)))
                ._properties(Lists.mutable.with(property1, property2))
                ._qualifiedProperties(qualifiedProperties);
    }

    private void associationThirdPass(Association srcAssociation, CompileContext context)
    {
        String property0Ref = context.pureModel.addPrefixToTypeReference(((PackageableType)srcAssociation.properties.get(0).genericType.rawType).fullPath);
        String property1Ref = context.pureModel.addPrefixToTypeReference(((PackageableType)srcAssociation.properties.get(1).genericType.rawType).fullPath);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = context.pureModel.getAssociation(context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name), srcAssociation.sourceInformation);
        ProcessingContext ctx = new ProcessingContext("Association " + context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name) + " (third pass)");

        ListIterate.collect(srcAssociation.qualifiedProperties, property ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(context, ((PackageableType) srcAssociation.properties.get(0).genericType.rawType).fullPath.equals(((PackageableType)property.returnGenericType.rawType).fullPath) ? property1Ref : property0Ref);
            ctx.addInferredVariables("this", thisVariable);
            ctx.push("Qualified Property " + property.name);
            ListIterate.collect(property.parameters, expression -> expression.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
            MutableList<ValueSpecification> body = ListIterate.collect(property.body, expression -> expression.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?> prop = association._qualifiedProperties().detect(o -> o._name().equals(property.name));
            ctx.pop();
            ctx.flushVariable("this");
            return prop._expressionSequence(body);
        });
    }

    private Set<PackageableElementPointer> associationPrerequisiteElementsPass(Association srcAssociation, CompileContext context)
    {
        Set<PackageableElementPointer> prerequisiteElements = Sets.mutable.empty();
        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, ((PackageableType) srcAssociation.properties.get(0).genericType.rawType).fullPath, srcAssociation.properties.get(0).sourceInformation));
        prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, ((PackageableType) srcAssociation.properties.get(1).genericType.rawType).fullPath, srcAssociation.properties.get(1).sourceInformation));
        return prerequisiteElements;
    }
}
