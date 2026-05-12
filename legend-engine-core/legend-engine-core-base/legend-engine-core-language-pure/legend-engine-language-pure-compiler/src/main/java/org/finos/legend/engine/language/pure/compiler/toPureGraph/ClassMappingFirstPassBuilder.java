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
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MergeOperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
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
        // Validate explicit primary key column names (if any) against the declared property mappings.
        // The PK *resolution* algorithm lives in Pure
        // (meta::relational::mapping::resolveRelationFunctionPrimaryKey) so per-operator rules stay
        // co-located with the operators they describe. Java's job here is just (a) validate the
        // user input and (b) call the Pure resolver, then persist its result on the set impl.
        if (classMapping.primaryKey != null && !classMapping.primaryKey.isEmpty())
        {
            java.util.Set<String> declaredColumns = new java.util.HashSet<>();
            classMapping.propertyMappings.stream()
                    .filter(pm -> pm instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping)
                    .map(pm -> ((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping) pm).column)
                    .filter(Objects::nonNull)
                    .forEach(declaredColumns::add);
            for (String pkColumn : classMapping.primaryKey)
            {
                if (!declaredColumns.contains(pkColumn))
                {
                    throw new org.finos.legend.engine.shared.core.operational.errorManagement.EngineException(
                            "Primary key column '" + pkColumn + "' declared in class mapping '" + id
                                    + "' does not match any of its relation property mappings ("
                                    + String.join(", ", declaredColumns) + ")",
                            classMapping.sourceInformation,
                            org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType.COMPILATION);
                }
            }
        }
        // Resolve the typed PK columns via the Pure resolver and persist on the set impl.
        // Explicit names from the protocol win when non-empty; otherwise the resolver falls back
        // to auto-inference. Each name is mapped to its Column<Nil,Any|*> reference held by the
        // matching property mapping. See helperFunctions.pure for the full algorithm.
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<?, ?>> resolvedPK =
                org.finos.legend.pure.generated.core_relational_relational_helperFunctions_helperFunctions
                        .Root_meta_relational_mapping_resolveRelationFunctionPrimaryKey_RelationFunctionInstanceSetImplementation_1__String_MANY__Column_MANY_(
                                setImpl,
                                Lists.mutable.withAll(
                                        classMapping.primaryKey == null ? java.util.Collections.<String>emptyList() : classMapping.primaryKey),
                                context.pureModel.getExecutionSupport());
        setImpl._primaryKey(resolvedPK);
        HelperMappingBuilder.buildMappingClassOutOfLocalProperties(setImpl, setImpl._propertyMappings(), this.context);
        return Tuples.pair(setImpl, Lists.immutable.empty());
    }
}
