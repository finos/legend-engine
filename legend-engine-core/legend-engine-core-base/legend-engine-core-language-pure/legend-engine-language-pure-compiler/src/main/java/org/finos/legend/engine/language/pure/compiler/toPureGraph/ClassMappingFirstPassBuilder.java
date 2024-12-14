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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MergeOperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.pure.generated.Root_meta_external_store_model_PureInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MergeOperationSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_OperationSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_relation_RelationFunctionInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregationAwareSetImplementation_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MergeOperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

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
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(OperationClassMapping classMapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);

        if (classMapping instanceof MergeOperationClassMapping)
        {
            MergeOperationSetImplementation res = new Root_meta_pure_mapping_MergeOperationSetImplementation_Impl(id, SourceInformationHelper.toM3SourceInformation(classMapping.sourceInformation), this.context.pureModel.getClass("meta::pure::mapping::MergeOperationSetImplementation"))._id(id);
            res._class(pureClass)
                    ._root(classMapping.root)
                    ._parent(this.parentMapping)
                    ._operation(this.context.pureModel.getFunction(OperationClassMapping.opsToFunc.get(classMapping.operation), false))
                    ._validationFunction(HelperValueSpecificationBuilder.buildLambda(((MergeOperationClassMapping) classMapping).validationFunction, this.context));
            return Tuples.pair(res, Lists.immutable.empty());
        }
        else
        {
            OperationSetImplementation res = new Root_meta_pure_mapping_OperationSetImplementation_Impl(id, SourceInformationHelper.toM3SourceInformation(classMapping.sourceInformation), this.context.pureModel.getClass("meta::pure::mapping::OperationSetImplementation"))._id(id);
            res._class(pureClass)
                    ._root(classMapping.root)
                    ._parent(this.parentMapping)
                    ._operation(this.context.pureModel.getFunction(OperationClassMapping.opsToFunc.get(classMapping.operation), false));
            return Tuples.pair(res, Lists.immutable.empty());
        }
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(PureInstanceClassMapping classMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.sourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type srcClass = classMapping.srcClass == null ? null : this.context.resolveType(classMapping.srcClass, classMapping.sourceClassSourceInformation);
        PureInstanceSetImplementation mappingClass = new Root_meta_external_store_model_PureInstanceSetImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(classMapping.sourceInformation), context.pureModel.getClass("meta::external::store::model::PureInstanceSetImplementation"));
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        PureInstanceSetImplementation rootSetImpl = mappingClass
                ._id(id)
                ._root(classMapping.root)
                ._parent(this.parentMapping)
                ._class(pureClass)
                ._srcClass(srcClass)
                ._propertyMappings(ListIterate.collect(classMapping.propertyMappings, p -> p.accept(new PropertyMappingBuilder(this.context, mappingClass, id, HelperMappingBuilder.getAllEnumerationMappings(this.parentMapping)))));
        if (classMapping.filter != null)
        {
            String filterName = id + ".filter";
            ProcessingContext ctx = new ProcessingContext("Create PureInstanceClassMapping Filter");
            List<Variable> params;
            if (classMapping.filter.parameters == null || classMapping.filter.parameters.isEmpty())
            {
                Variable param = new Variable();
                param.name = "src";
                param.genericType = new GenericType(new PackageableType(classMapping.srcClass));
                param.multiplicity = new Multiplicity(1, 1);
                params = Lists.mutable.with(param);
            }
            else
            {
                params = classMapping.filter.parameters;
            }

            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(filterName, classMapping.filter.body, params, this.context, ctx);
            lambda.setSourceInformation(SourceInformationHelper.toM3SourceInformation(classMapping.filter.sourceInformation));
            rootSetImpl._filter(lambda);
        }
        HelperMappingBuilder.buildMappingClassOutOfLocalProperties(rootSetImpl, rootSetImpl._propertyMappings(), this.context);
        return Tuples.pair(rootSetImpl, Lists.immutable.empty());
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(AggregationAwareClassMapping classMapping)
    {
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        final AggregationAwareSetImplementation res = new Root_meta_pure_mapping_aggregationAware_AggregationAwareSetImplementation_Impl(id, SourceInformationHelper.toM3SourceInformation(classMapping.sourceInformation), this.context.pureModel.getClass("meta::pure::mapping::aggregationAware::AggregationAwareSetImplementation"))._id(id);

        this.context.getCompilerExtensions().getExtraAggregationAwareClassMappingFirstPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));

        res._mainSetImplementation((InstanceSetImplementation) classMapping.mainSetImplementation.accept(new ClassMappingFirstPassBuilder(this.context, this.parentMapping)).getOne());
        for (AggregateSetImplementationContainer agg : classMapping.aggregateSetImplementations)
        {
            res._aggregateSetImplementationsAdd(HelperMappingBuilder.processAggregateSetImplementationContainer(agg, this.context, this.parentMapping));
        }
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        res._class(pureClass);
        res._parent(this.parentMapping);
        res._root(classMapping.root);

        if (classMapping.mappingClass != null)
        {
            res._mappingClass(HelperMappingBuilder.processMappingClass(classMapping.mappingClass, this.context, this.parentMapping));
        }
        for (PropertyMapping pm : classMapping.propertyMappings)
        {
            res._propertyMappingsAdd(pm.accept(new PropertyMappingBuilder(this.context, res, HelperMappingBuilder.getAllEnumerationMappings(this.parentMapping))));
        }
        return Tuples.pair(res, Lists.immutable.empty());
    }

    @Override
    public Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>> visit(RelationFunctionClassMapping classMapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> pureClass = this.context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        final RelationFunctionInstanceSetImplementation baseSetImpl = new Root_meta_pure_mapping_relation_RelationFunctionInstanceSetImplementation_Impl(id, SourceInformationHelper.toM3SourceInformation(classMapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation"));
        final RelationFunctionInstanceSetImplementation setImpl = baseSetImpl
                ._class(pureClass)
                ._id(id)
                ._superSetImplementationId(classMapping.extendsClassMappingId)
                ._root(classMapping.root)
                ._parent(parentMapping)
                ._propertyMappings(ListIterate.collect(classMapping.propertyMappings, p -> p.accept(new PropertyMappingBuilder(this.context, baseSetImpl, Lists.mutable.empty()))));
        HelperMappingBuilder.buildMappingClassOutOfLocalProperties(setImpl, setImpl._propertyMappings(), this.context);
        return Tuples.pair(setImpl, Lists.immutable.empty());
    }
}
