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

import org.eclipse.collections.api.list.MutableList;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class PackageableElementThirdPassBuilder implements PackageableElementVisitor<PackageableElement>
{
    private final CompileContext context;

    public PackageableElementThirdPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public PackageableElement visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        this.context.getExtraProcessorOrThrow(element).processThirdPass(element, this.context);
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

    @Override
    public PackageableElement visit(Class srcClass)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = this.context.pureModel.getClass(this.context.pureModel.buildPackageString(srcClass._package, srcClass.name), srcClass.sourceInformation);
        Milestoning.applyMilestoningClassTransformations(this.context, targetClass);
        return targetClass;
    }

    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        String property0Ref = this.context.pureModel.addPrefixToTypeReference(srcAssociation.properties.get(0).type);
        String property1Ref = this.context.pureModel.addPrefixToTypeReference(srcAssociation.properties.get(1).type);

        String packageString = this.context.pureModel.buildPackageString(srcAssociation._package, srcAssociation.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = this.context.pureModel.getAssociation(packageString, srcAssociation.sourceInformation);

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
        return null;
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        return null;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        return null;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
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
        return null;
    }
}
