// Copyright 2025 Goldman Sachs
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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;

import java.util.Set;

public class PropertyMappingPrerequisiteElementsBuilder implements PropertyMappingVisitor<Set<PackageableElementPointer>>
{
    private final CompileContext context;
    private final Set<PackageableElementPointer> prerequisiteElements;

    public PropertyMappingPrerequisiteElementsBuilder(CompileContext context, Set<PackageableElementPointer> prerequisiteElements)
    {
        this.context = context;
        this.prerequisiteElements = prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(PurePropertyMapping propertyMapping)
    {
        HelperMappingBuilder.collectPrerequisiteElementsFromMappedProperty(this.prerequisiteElements, propertyMapping);
        HelperMappingBuilder.collectPrerequisiteElementsFromPurePropertyMappingTransform(this.prerequisiteElements, propertyMapping, this.context);
        if (propertyMapping.localMappingProperty != null)
        {
            this.prerequisiteElements.add(new PackageableElementPointer(null, propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
        }
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(XStorePropertyMapping propertyMapping)
    {
        ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(this.context, this.prerequisiteElements);
        ListIterate.forEach(propertyMapping.crossExpression.body, p -> p.accept(valueSpecificationPrerequisiteElementsPassBuilder));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(AggregationAwarePropertyMapping propertyMapping)
    {
        HelperMappingBuilder.collectPrerequisiteElementsFromMappedProperty(this.prerequisiteElements, propertyMapping);
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(RelationFunctionPropertyMapping propertyMapping)
    {
        HelperMappingBuilder.collectPrerequisiteElementsFromMappedProperty(this.prerequisiteElements, propertyMapping);
        if (propertyMapping.localMappingProperty != null)
        {
            this.prerequisiteElements.add(new PackageableElementPointer(null, propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
        }
        return this.prerequisiteElements;
    }
}
