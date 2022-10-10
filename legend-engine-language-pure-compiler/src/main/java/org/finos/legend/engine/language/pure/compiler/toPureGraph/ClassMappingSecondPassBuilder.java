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

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_SetImplementationContainer_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;

import java.util.stream.Collectors;

public class ClassMappingSecondPassBuilder implements ClassMappingVisitor<SetImplementation>
{
    private final CompileContext context;
    private final Mapping parentMapping;

    public ClassMappingSecondPassBuilder(CompileContext context, Mapping parentMapping)
    {
        this.context = context;
        this.parentMapping = parentMapping;
    }

    // NOTE: when we remove this visitor, we can return "void"
    @Override
    public SetImplementation visit(ClassMapping classMapping)
    {
        if (classMapping.extendsClassMappingId != null)
        {
            String superSetId = classMapping.extendsClassMappingId;
            ImmutableSet<SetImplementation> superSets = HelperMappingBuilder.getAllClassMappings(parentMapping).select(c -> c._id().equals(superSetId));
            if (superSets.size() == 0)
            {
                throw new EngineException("Can't find extends class mapping '" + superSetId + "' in mapping '" + HelperModelBuilder.getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()) + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            if (superSets.size() > 1)
            {
                String parents = superSets.stream().map(superSet -> "'" + HelperModelBuilder.getElementFullPath(superSet._parent(), this.context.pureModel.getExecutionSupport()) + "'").sorted().collect(Collectors.joining(", "));
                throw new EngineException("Duplicated class mappings found with ID '" + superSetId + "' in mapping '" + HelperModelBuilder.getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()) + "'" + "; parent mapping for duplicated: " + parents, classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
        this.context.getCompilerExtensions().getExtraClassMappingSecondPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));
        return null;
    }

    @Override
    public SetImplementation visit(OperationClassMapping classMapping)
    {
        OperationSetImplementation operationSetImplementation = (OperationSetImplementation) parentMapping._classMappings().detect(c -> c._id().equals(HelperMappingBuilder.getClassMappingId(classMapping, this.context)));
        return operationSetImplementation._parameters(ListIterate.collect(classMapping.parameters, classMappingId ->
        {
            SetImplementation match = HelperMappingBuilder.getAllClassMappings(parentMapping).detect(c -> c._id().equals(classMappingId));
            if (match == null)
            {
                throw new EngineException("Can't find class mapping '" + classMappingId + "' in mapping '" + HelperModelBuilder.getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()) + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            return new Root_meta_pure_mapping_SetImplementationContainer_Impl("", null, context.pureModel.getClass("meta::pure::mapping::SetImplementationContainer"))._id(classMappingId)._setImplementation(match);
        }));
    }

    @Override
    public SetImplementation visit(PureInstanceClassMapping classMapping)
    {
        return this.visit((ClassMapping)classMapping);
    }

    @Override
    public SetImplementation visit(AggregationAwareClassMapping classMapping)
    {
        this.context.getCompilerExtensions().getExtraAggregationAwareClassMappingSecondPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));
        return null;
    }
}
