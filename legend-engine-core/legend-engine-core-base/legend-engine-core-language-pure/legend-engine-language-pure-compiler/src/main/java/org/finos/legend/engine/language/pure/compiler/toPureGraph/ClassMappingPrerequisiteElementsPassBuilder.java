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
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MergeOperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;

import java.util.Set;

public class ClassMappingPrerequisiteElementsPassBuilder implements ClassMappingVisitor<Set<PackageableElementPointer>>
{
    private final CompileContext context;
    private final Set<PackageableElementPointer> prerequisiteElements;

    public ClassMappingPrerequisiteElementsPassBuilder(CompileContext context, Set<PackageableElementPointer> prerequisiteElements)
    {
        this.context = context;
        this.prerequisiteElements = prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(ClassMapping classMapping)
    {
        this.context.getCompilerExtensions().getExtraClassMappingPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(classMapping, this.context, this.prerequisiteElements));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(OperationClassMapping classMapping)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping._class, classMapping.classSourceInformation));
        if (classMapping instanceof MergeOperationClassMapping)
        {
            MergeOperationClassMapping mergeOperationClassMapping = (MergeOperationClassMapping) classMapping;
            mergeOperationClassMapping.validationFunction.accept(new ValueSpecificationPrerequisiteElementsPassBuilder(this.context, this.prerequisiteElements));
        }
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(PureInstanceClassMapping classMapping)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping._class, classMapping.sourceInformation));
        if (classMapping.srcClass != null)
        {
            this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping.srcClass, classMapping.sourceClassSourceInformation));
        }
        PropertyMappingPrerequisiteElementsBuilder propertyMappingPrerequisiteElementsBuilder = new PropertyMappingPrerequisiteElementsBuilder(this.context, this.prerequisiteElements);
        ListIterate.forEach(classMapping.propertyMappings, p -> p.accept(propertyMappingPrerequisiteElementsBuilder));
        if (classMapping.filter != null)
        {
            ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(this.context, this.prerequisiteElements);
            ListIterate.forEach(classMapping.filter.body, v -> v.accept(valueSpecificationPrerequisiteElementsPassBuilder));
            if (classMapping.filter.parameters != null && !classMapping.filter.parameters.isEmpty())
            {
                ListIterate.forEach(classMapping.filter.parameters, p -> p.accept(valueSpecificationPrerequisiteElementsPassBuilder));
            }
        }
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(AggregationAwareClassMapping classMapping)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping._class, classMapping.classSourceInformation));
        this.context.getCompilerExtensions().getExtraAggregationAwareClassMappingPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(classMapping, this.prerequisiteElements));
        classMapping.mainSetImplementation.accept(this);
        ListIterate.forEach(classMapping.aggregateSetImplementations, agg -> HelperMappingBuilder.collectPrerequisiteElementsFromAggregateSetImplementationContainer(this.prerequisiteElements, agg, this.context));
        if (classMapping.mappingClass != null)
        {
            HelperMappingBuilder.collectPrerequisiteElementsFromMappingClass(this.prerequisiteElements, classMapping.mappingClass, context);
        }
        PropertyMappingPrerequisiteElementsBuilder propertyMappingPrerequisiteElementsBuilder = new PropertyMappingPrerequisiteElementsBuilder(this.context, this.prerequisiteElements);
        ListIterate.forEach(classMapping.propertyMappings, pm -> pm.accept(propertyMappingPrerequisiteElementsBuilder));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(RelationFunctionClassMapping classMapping)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, classMapping._class, classMapping.classSourceInformation));
        PropertyMappingPrerequisiteElementsBuilder propertyMappingPrerequisiteElementsBuilder = new PropertyMappingPrerequisiteElementsBuilder(this.context, this.prerequisiteElements);
        ListIterate.forEach(classMapping.propertyMappings, pm -> pm.accept(propertyMappingPrerequisiteElementsBuilder));
        return this.prerequisiteElements;
    }
}
