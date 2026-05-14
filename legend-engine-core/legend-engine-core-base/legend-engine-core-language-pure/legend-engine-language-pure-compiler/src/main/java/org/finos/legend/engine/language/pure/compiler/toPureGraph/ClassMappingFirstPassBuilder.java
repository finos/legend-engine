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
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
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
        // Validate explicit primary key column names (if any) against the columns of the
        // Relation Function's return type — NOT against the declared property mappings.
        // A PK column may be unmapped (no property mapping) but still legitimately exist
        // on the relation. The PK *resolution* algorithm itself lives in core Pure
        // (meta::pure::mapping::relation::resolveRelationFunctionPrimaryKey, in
        //  legend-engine-pure-code-compiled-core/.../core/pure/mapping/
        //  relationFunctionMapping.pure) so per-operator rules stay co-located with the
        // operators they describe and are reachable from this core Java module.
        org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<?, ?>> relationColumns = getRelationFunctionColumns(setImpl);
        if (classMapping.primaryKey != null && !classMapping.primaryKey.isEmpty() && relationColumns.notEmpty())
        {
            java.util.Set<String> availableColumnNames = new java.util.LinkedHashSet<>();

            relationColumns.forEach(c ->
            {
                if (c._name() != null)
                {
                    availableColumnNames.add(c._name());
                }
            });
            for (String pkColumn : classMapping.primaryKey)
            {
                if (!availableColumnNames.contains(pkColumn))
                {
                    throw new org.finos.legend.engine.shared.core.operational.errorManagement.EngineException(
                            "Primary key column '" + pkColumn + "' declared in class mapping '" + id
                                    + "' is not part of the columns returned by the relation function '"
                                    + getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) setImpl._relationFunction(), this.context.pureModel.getExecutionSupport()) + "' ("
                                    + String.join(", ", availableColumnNames) + ")."
                                    + " Use syntax `~primaryKey: [col1, col2, ...]` referencing only columns from the relation function's output.",
                            classMapping.sourceInformation,
                            org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType.COMPILATION);
                }
            }
        }
        else if (classMapping.primaryKey != null && !classMapping.primaryKey.isEmpty())
        {
            // User declared ~primaryKey but we can't resolve the function's RelationType
            // to validate/persist it — log so it's not silently swallowed.
            context.pureModel.addWarnings(Lists.mutable.with(
                new Warning(classMapping.sourceInformation,
                    "Primary key columns declared but cannot be validated — "
                    + "relation function does not expose a concrete RelationType")));
        }
        // Resolve the typed PK columns by calling the core Pure resolver. Picks the user's
        // explicit list when non-empty, otherwise auto-infers by walking the relation
        // function body. If neither yields a PK, fail with a clear, syntax-aware error so
        // the user knows exactly how to recover.
        org.eclipse.collections.api.list.MutableList<String> explicitNames =
                (classMapping.primaryKey == null)
                        ? Lists.mutable.empty()
                        : Lists.mutable.withAll(classMapping.primaryKey);
        org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<? extends Object, ? extends Object>> resolvedPK =
                org.finos.legend.pure.generated.core_pure_mapping_relationFunctionMapping
                        .Root_meta_pure_mapping_relation_resolveRelationFunctionPrimaryKey_RelationFunctionInstanceSetImplementation_1__String_MANY__Column_MANY_(
                                setImpl, explicitNames, this.context.pureModel.getExecutionSupport());
        if (resolvedPK == null || resolvedPK.isEmpty())
        {
            throw new org.finos.legend.engine.shared.core.operational.errorManagement.EngineException(
                    "Unable to determine primary key for relation function class mapping '" + id
                            + "'. No `~primaryKey` was declared and the primary key could not be inferred from the body of relation function '"
                            + getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) setImpl._relationFunction(), this.context.pureModel.getExecutionSupport())
                            + "'. Please specify it explicitly using `~primaryKey: [col1, col2, ...]` (referencing one or more columns of the relation function's output).",
                    classMapping.sourceInformation,
                    org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType.COMPILATION);
        }
        setImpl._primaryKey(resolvedPK);
        HelperMappingBuilder.buildMappingClassOutOfLocalProperties(setImpl, setImpl._propertyMappings(), this.context);
        return Tuples.pair(setImpl, Lists.immutable.empty());
    }

    /**
     * Returns the columns of a RelationFunctionInstanceSetImplementation's relation function
     * by walking to the last expression of its body and reading the RelationType column list
     * off its return type. Returns an empty iterable if the return type is not a RelationType
     * (e.g. malformed function); callers should treat that as "no columns available".
     */
    private static org.eclipse.collections.api.RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<?, ?>> getRelationFunctionColumns(RelationFunctionInstanceSetImplementation setImpl)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> fn =
                (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?>) setImpl._relationFunction();
        if (fn == null)
        {
            return Lists.immutable.empty();
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification last =
                fn._expressionSequence().toList().getLast();
        if (last == null || last._genericType() == null)
        {
            return Lists.immutable.empty();
        }
        org.eclipse.collections.api.list.MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> typeArgs =
                last._genericType()._typeArguments().toList();
        if (typeArgs.isEmpty())
        {
            return Lists.immutable.empty();
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type rawType = typeArgs.get(0)._rawType();
        if (!(rawType instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType))
        {
            return Lists.immutable.empty();
        }
        return ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?>) rawType)._columns();
    }
}
