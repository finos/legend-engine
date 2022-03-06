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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MergeOperationSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_OperationSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregationAwareSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_PureInstanceSetImplementation_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.List;
import java.util.Objects;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class ClassMappingFirstPassBuilder implements ClassMappingVisitor<Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>
{
    private final CompileContext context;
    private final Mapping parentMapping;

    public ClassMappingFirstPassBuilder(CompileContext context, Mapping parentMapping)
    {
        this.context = context;
        this.parentMapping = parentMapping;
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(ClassMapping classMapping)
    {
        return this.context.getCompilerExtensions().getExtraClassMappingFirstPassProcessors().stream()
                .map(processor -> processor.value(classMapping, this.parentMapping, this.context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported class mapping type '" + classMapping.getClass() + "'"));
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(OperationClassMapping classMapping) {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);

        if (classMapping instanceof MergeOperationClassMapping) {
            MergeOperationSetImplementation res = new Root_meta_pure_mapping_MergeOperationSetImplementation_Impl(id)._id(id);
            res._class(pureClass)
                    ._root(classMapping.root)
                    ._parent(parentMapping)
                    ._operation(this.context.pureModel.getFunction(OperationClassMapping.opsToFunc.get(classMapping.operation), false))
                    ._validationFunction(HelperValueSpecificationBuilder.buildLambda(((MergeOperationClassMapping) classMapping).validationFunction, this.context));
            return Tuples.pair(res, Lists.immutable.empty());
        } else {
            OperationSetImplementation res = new Root_meta_pure_mapping_OperationSetImplementation_Impl(id)._id(id);
            res._class(pureClass)
                    ._root(classMapping.root)
                    ._parent(parentMapping)
                    ._operation(this.context.pureModel.getFunction(OperationClassMapping.opsToFunc.get(classMapping.operation), false));
            return Tuples.pair(res, Lists.immutable.empty());
        }
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(PureInstanceClassMapping classMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> srcClass = classMapping.srcClass == null ? null : this.context.resolveClass(classMapping.srcClass, classMapping.sourceClassSourceInformation);
        PureInstanceSetImplementation mappingClass = new Root_meta_pure_mapping_modelToModel_PureInstanceSetImplementation_Impl("");
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        PureInstanceSetImplementation rootSetImpl = mappingClass
                ._id(id)
                ._root(classMapping.root)
                ._parent(parentMapping)
                ._class(pureClass)
                ._srcClass(srcClass)
                ._propertyMappings(ListIterate.collect(classMapping.propertyMappings, p -> p.accept(new PropertyMappingBuilder(this.context, mappingClass, id, HelperMappingBuilder.getAllEnumerationMappings(this.parentMapping)))));
        if (classMapping.filter != null)
        {
            String filterName = id + ".filter";
            String mappingPath = getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()).replace("::", "_");
            ProcessingContext ctx = new ProcessingContext("Create PureInstanceClassMapping Filter");
            List<Variable> params = new FastList<>();
            if (classMapping.filter.parameters == null || classMapping.filter.parameters.isEmpty())
            {
                Variable param = new Variable();
                param.name = "src";
                param._class = classMapping.srcClass;
                param.multiplicity = new Multiplicity(1, 1);
                params.add(param);
            }
            else
            {
                params = classMapping.filter.parameters;
            }

            LambdaFunction lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(filterName, classMapping.filter.body, params, this.context, ctx);
            lambda.setSourceInformation(new SourceInformation(mappingPath, 0, 0, 0, 0));
            rootSetImpl._filter(lambda);
        }
        HelperMappingBuilder.buildMappingClassOutOfLocalProperties(rootSetImpl, rootSetImpl._propertyMappings(), this.context);
        return Tuples.pair(rootSetImpl, Lists.immutable.empty());
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(AggregationAwareClassMapping classMapping)
    {
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        final AggregationAwareSetImplementation res = new Root_meta_pure_mapping_aggregationAware_AggregationAwareSetImplementation_Impl(id)._id(id);

        this.context.getCompilerExtensions().getExtraAggregationAwareClassMappingFirstPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));

        res._mainSetImplementation((InstanceSetImplementation) classMapping.mainSetImplementation.accept(new ClassMappingFirstPassBuilder(this.context, parentMapping)).getOne());
        for (AggregateSetImplementationContainer agg : classMapping.aggregateSetImplementations)
        {
            res._aggregateSetImplementationsAdd(HelperMappingBuilder.processAggregateSetImplementationContainer(agg, this.context, parentMapping));
        }
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        res._class(pureClass);
        res._parent(parentMapping);
        res._root(classMapping.root);

        if (classMapping.mappingClass != null)
        {
            res._mappingClass(HelperMappingBuilder.processMappingClass(classMapping.mappingClass, this.context, parentMapping));
        }
        for (PropertyMapping pm : classMapping.propertyMappings)
        {
            res._propertyMappingsAdd(pm.accept(new PropertyMappingBuilder(this.context, res, HelperMappingBuilder.getAllEnumerationMappings(this.parentMapping))));
        }
        return Tuples.pair(res, Lists.immutable.empty());
    }
}
