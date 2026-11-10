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
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.core_pure_router_operations_router_operations;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_Mapping.Root_meta_pure_mapping__classMappingByClass_Mapping_1__Class_1__SetImplementation_MANY_;
import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_Mapping.Root_meta_pure_mapping__classMappingByIdRecursive_Mapping_1__String_MANY__SetImplementation_MANY_;
import static org.finos.legend.pure.generated.platform_pure_essential_meta_type_getAllTypeGeneralisations.Root_meta_pure_functions_meta_getAllTypeGeneralisations_Type_1__Type_MANY_;

public class ClassMappingThirdPassBuilder implements ClassMappingVisitor<SetImplementation>
{
    private final CompileContext context;
    private final Mapping parentMapping;

    public ClassMappingThirdPassBuilder(CompileContext context, Mapping parentMapping)
    {
        this.context = context;
        this.parentMapping = parentMapping;
    }

    // NOTE: when we remove this visitor, we can return "void"
    @Override
    public SetImplementation visit(ClassMapping classMapping)
    {
        return null;
    }

    @Override
    public SetImplementation visit(OperationClassMapping classMapping)
    {
        return null;
    }

    @Override
    public SetImplementation visit(PureInstanceClassMapping classMapping)
    {
        PureInstanceSetImplementation s = (PureInstanceSetImplementation) this.parentMapping._classMappings().select(c -> c._id().equals(HelperMappingBuilder.getClassMappingId(classMapping, this.context))).getFirst();
        s._propertyMappings().forEachWithIndex((ObjectIntProcedure<PropertyMapping>) ((p, i) ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping pm = (org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping) p;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = pm._property();
            SourceInformation pSourceInformation = SourceInformationHelper.fromM3SourceInformation(p.getSourceInformation());
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification last = pm._transform()._expressionSequence().getLast();

            List<Type> typesToCheck;
            if (property._genericType()._rawType()._classifierGenericType()._rawType()._name().equals("Class"))
            {
                SetImplementation setImplementation;
                // `optionalProp: []` on a [0..1] class-typed property is a no-op — no target mapping needed.
                boolean unmappedOptionalPassThrough = isEmptyTransformOnOptionalClassProperty(last, property);
                if (p._targetSetImplementationId() != null && !p._targetSetImplementationId().equals(""))
                {
                    setImplementation = Root_meta_pure_mapping__classMappingByIdRecursive_Mapping_1__String_MANY__SetImplementation_MANY_(this.parentMapping, Lists.fixedSize.with(p._targetSetImplementationId()), this.context.pureModel.getExecutionSupport()).getFirst();
                    boolean idIsSyntheticDefault = HelperModelBuilder.getTypeFullPath(property._genericType()._rawType(), "_", this.context.pureModel.getExecutionSupport()).equals(p._targetSetImplementationId());
                    boolean allowComplexPropertyMappingPassThrough = setImplementation == null && idIsSyntheticDefault && checkTransformTypeMatchesPropertyType(property, last, context);
                    // id is a compiler-generated placeholder: resolve by class instead, and stamp the real id back
                    // so downstream consumers don't re-resolve and pick a different SetImplementation. Skipped for
                    // a pass-through — the instance already exists, so any target's srcClass would fail the check below.
                    if (setImplementation == null && idIsSyntheticDefault && !allowComplexPropertyMappingPassThrough)
                    {
                        setImplementation = Root_meta_pure_mapping__classMappingByClass_Mapping_1__Class_1__SetImplementation_MANY_(this.parentMapping, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()).getFirst();
                        if (setImplementation != null)
                        {
                            p._targetSetImplementationId(setImplementation._id());
                        }
                    }
                    if (setImplementation == null && idIsSyntheticDefault)
                    {
                        // We need to have this as we have been adding defaultId to property mappings when users don't provide something explicitly. This is valid now but having it raises wrong warning
                        p._targetSetImplementationId("");
                    }
                    Assert.assertTrue((setImplementation != null) || allowComplexPropertyMappingPassThrough || unmappedOptionalPassThrough,
                            () -> "Can't find class mapping for property '" + property._name() + "' of type '" + HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()) + "' in mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "' or any included mapping",
                            pSourceInformation, EngineErrorType.COMPILATION);
                }
                else
                {
                    setImplementation = Root_meta_pure_mapping__classMappingByClass_Mapping_1__Class_1__SetImplementation_MANY_(this.parentMapping, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()).getFirst();
                    boolean allowComplexPropertyMappingPassThrough = setImplementation == null && checkTransformTypeMatchesPropertyType(property, last, context);
                    Assert.assertTrue((setImplementation != null) || allowComplexPropertyMappingPassThrough || unmappedOptionalPassThrough, () -> "Can't find class mapping for '" + HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()) + "'", pSourceInformation, EngineErrorType.COMPILATION);
                }

                List<? extends InstanceSetImplementation> setImpls = setImplementation != null ? core_pure_router_operations_router_operations.Root_meta_pure_router_routing_resolveOperation_SetImplementation_MANY__Mapping_1__InstanceSetImplementation_MANY_(Lists.immutable.of(setImplementation), this.parentMapping, this.context.pureModel.getExecutionSupport()).toList() : Collections.emptyList();
                typesToCheck = setImpls.stream().map(setImpl -> ((PureInstanceSetImplementation) setImpl)._srcClass()).collect(Collectors.toList());
            }
            else if (((org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping) p)._transformer() != null)
            {
                EnumerationMapping<Object> m = ((EnumerationMapping<Object>) ((org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping) p)._transformer());
                Object val = m._enumValueMappings().getFirst()._sourceValues().getFirst();
                if (val instanceof String)
                {
                    typesToCheck = Collections.singletonList(this.context.pureModel.getType("String"));
                }
                else if (val instanceof Long)
                {
                    typesToCheck = Collections.singletonList(this.context.pureModel.getType("Integer"));
                }
                else if (val instanceof EnumValue)
                {
                    typesToCheck = Collections.singletonList(this.context.resolveEnumeration(((EnumValue) val).fullPath, pSourceInformation));
                }
                else if (val instanceof Enum)
                {
                    GenericType genericType = ((Enum) val)._classifierGenericType();
                    typesToCheck = genericType != null ? Collections.singletonList(this.context.resolveEnumeration(PackageableElement.getUserPathForPackageableElement(genericType._rawType()), pSourceInformation)) : Collections.emptyList();
                }
                else
                {
                    typesToCheck = Collections.emptyList();
                }
            }
            else
            {
                typesToCheck = Collections.singletonList(property._genericType()._rawType());
            }

            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicityToCheck = pm._explodeProperty() != null && pm._explodeProperty() ? this.context.pureModel.getMultiplicity("zeromany") : property._multiplicity();
            List<ValueSpecification> lines = ((PurePropertyMapping) classMapping.propertyMappings.get(i)).transform.body;
            typesToCheck.stream().filter(Objects::nonNull).forEach(t -> checkPureMappingCompatibility(this.context,
                    last._genericType()._rawType(),
                    t,
                    "Error in class mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "' for property '" + pm._property()._name() + "'",
                    lines.get(lines.size() - 1).sourceInformation));

            HelperModelBuilder.checkMultiplicityCompatibility(last._multiplicity(),
                    multiplicityToCheck,
                    "Error in class mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "' for property '" + pm._property()._name() + "'",
                    lines.get(lines.size() - 1).sourceInformation);
        }));
        return s;
    }


    @Override
    public SetImplementation visit(AggregationAwareClassMapping classMapping)
    {
        return null;
    }

    @Override
    public SetImplementation visit(RelationFunctionClassMapping classMapping)
    {
        String id = HelperMappingBuilder.getClassMappingId(classMapping, this.context);
        RelationFunctionInstanceSetImplementation setImpl =
                (RelationFunctionInstanceSetImplementation) this.parentMapping._classMappings()
                        .detect(c -> c._id().equals(id));
        if (setImpl == null)
        {
            return null;
        }
        compileRelationModelJoins(classMapping.propertyMappings, setImpl);

        // No explicit ~primaryKey → leave empty; runtime auto-infers via RelationElementAccessorExtension.
        if (classMapping.primaryKey == null || classMapping.primaryKey.isEmpty())
        {
            return setImpl;
        }

        // Function is a prerequisite (see ClassMappingPrerequisiteElementsPassBuilder),
        // so expressionSequence is fully typed here.
        RichIterable<? extends Column<?, ?>> relationColumns = getRelationFunctionColumns(setImpl);

        if (relationColumns.isEmpty())
        {
            throw new EngineException(
                    "Cannot resolve primary key columns: relation function '"
                            + setImpl._relationFunction()._functionName()
                            + "' does not return a concrete RelationType. "
                            + "Ensure the function body produces a typed relation (e.g., #>{db.table}#).",
                    classMapping.sourceInformation,
                    EngineErrorType.COMPILATION);
        }

        MutableList<Column<?, ?>> resolvedPK = Lists.mutable.empty();
        for (String pkName : classMapping.primaryKey)
        {
            Column<?, ?> col = (Column<?, ?>) relationColumns.detect(c -> pkName.equals(c._name()));
            if (col == null)
            {
                String available = relationColumns.collect(Column::_name).makeString(", ");
                String mappingPath = HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport());
                throw new EngineException(
                        "Primary key column '" + pkName + "' declared in class mapping '" + id
                                + "' (mapping '" + mappingPath + "') is not part of the columns returned by the relation function."
                                + " Available columns: [" + available + "]."
                                + " Use `~primaryKey: [col1, col2, ...]` referencing only columns from the relation function's output.",
                        classMapping.sourceInformation,
                        EngineErrorType.COMPILATION);
            }
            resolvedPK.add(col);
        }
        setImpl._primaryKey(resolvedPK);
        return setImpl;
    }

    private void compileRelationModelJoins(List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping> protocolPropertyMappings, RelationFunctionInstanceSetImplementation owningSet)
    {
        MutableList<? extends PropertyMapping> m3PropertyMappings = Lists.mutable.withAll(owningSet._propertyMappings());
        for (int i = 0; i < protocolPropertyMappings.size(); i++)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping protocolPm = protocolPropertyMappings.get(i);
            PropertyMapping m3Pm = m3PropertyMappings.get(i);
            if (m3Pm instanceof ModelJoinPropertyMapping)
            {
                org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction condition;
                if (protocolPm instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.modelJoin.ModelJoinPropertyMapping)
                {
                    condition = ((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.modelJoin.ModelJoinPropertyMapping) protocolPm).joinCondition;
                }
                else
                {
                    condition = RelationModelJoinBuilder.resolveJoinCondition((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping) protocolPm);
                }
                compileRelationModelJoin(condition, protocolPm.sourceInformation, (ModelJoinPropertyMapping) m3Pm, owningSet);
            }
            else if (protocolPm instanceof RelationFunctionEmbeddedPropertyMapping && m3Pm instanceof EmbeddedRelationFunctionSetImplementation)
            {
                RelationFunctionEmbeddedPropertyMapping pEmb = (RelationFunctionEmbeddedPropertyMapping) protocolPm;
                if (pEmb.propertyMappings != null && !pEmb.propertyMappings.isEmpty())
                {
                    compileRelationModelJoins(pEmb.propertyMappings, (EmbeddedRelationFunctionSetImplementation) m3Pm);
                }
            }
        }
    }

    private void compileRelationModelJoin(org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction condition, SourceInformation sourceInformation, ModelJoinPropertyMapping m3Pm, RelationFunctionInstanceSetImplementation sourceSet)
    {
        Type propertyType = m3Pm._localMappingPropertyType() != null ? m3Pm._localMappingPropertyType() : m3Pm._property()._genericType()._rawType();
        InstanceSetImplementation targetSet = inferRelationModelJoinTarget(propertyType, sourceInformation);
        m3Pm._targetSetImplementationId(targetSet._id());
        PropertyMappingBuilder.compileRelationModelJoinCondition(this.context, this.parentMapping, m3Pm, condition, sourceSet, targetSet);
    }

    /**
     * The single exact (non-embedded) class mapping of the property's class that is a root within its declaring mapping,
     * across this mapping and its includes: a class's sole class mapping within a mapping is its root even without '*',
     * otherwise the starred one is. Operation mappings count, as for root validation, so a starred Operation is the root
     * (and rejected) rather than an unstarred instance class mapping beside it being picked instead.
     */
    private InstanceSetImplementation inferRelationModelJoinTarget(Type propertyType, SourceInformation sourceInformation)
    {
        MutableList<SetImplementation> candidates = Lists.mutable.empty();
        for (Mapping mapping : HelperMappingBuilder.getAllIncludedMappings(this.parentMapping))
        {
            MutableList<SetImplementation> directClassMappings = Lists.mutable.empty();
            for (SetImplementation classMapping : mapping._classMappings())
            {
                if (!(classMapping instanceof EmbeddedSetImplementation) && classMapping._class() == propertyType)
                {
                    directClassMappings.add(classMapping);
                }
            }
            candidates.addAll(directClassMappings.size() == 1 ? directClassMappings : directClassMappings.select(SetImplementation::_root));
        }
        if (candidates.size() != 1)
        {
            throw new EngineException("modelJoin: expected exactly one root class mapping for type '" + HelperModelBuilder.getTypeFullPath(propertyType, this.context.pureModel.getExecutionSupport())
                    + "' in this mapping or its includes, found " + candidates.size()
                    + (candidates.isEmpty() ? "." : ": " + candidates.collect(SetImplementation::_id).sortThis().makeString("[", ", ", "]") + "."), sourceInformation, EngineErrorType.COMPILATION);
        }
        SetImplementation root = candidates.get(0);
        if (root instanceof OperationSetImplementation)
        {
            throw new EngineException("modelJoin: Operation mappings are not supported as the target of modelJoin; the root class mapping for type '"
                    + HelperModelBuilder.getTypeFullPath(propertyType, this.context.pureModel.getExecutionSupport()) + "' ('" + root._id() + "') is an Operation.", sourceInformation, EngineErrorType.COMPILATION);
        }
        return (InstanceSetImplementation) root;
    }

    // Columns of the relation function's last-expression RelationType, or [] if Relation<Any>.
    private static RichIterable<? extends Column<?, ?>> getRelationFunctionColumns(RelationFunctionInstanceSetImplementation setImpl)
    {
        GenericType rowType = HelperMappingBuilder.getRelationFunctionRowType(setImpl._relationFunction());
        return rowType != null && rowType._rawType() instanceof RelationType ? ((RelationType<?>) rowType._rawType())._columns() : Lists.immutable.empty();
    }


    private static void checkPureMappingCompatibility(CompileContext context, Type actualReturnType, Type signatureType, String errorStub, SourceInformation errorSourceInformation)
    {
        //In a pure mapping. The src implementation  can be anywhere in the class hierarchy of the return type
        if (signatureType != null && !actualReturnType.equals(signatureType) && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(actualReturnType, signatureType, context.pureModel.getExecutionSupport().getProcessorSupport()) && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(signatureType, actualReturnType, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            throw new EngineException(errorStub + " - Type error: '" + HelperModelBuilder.getTypeFullPath(actualReturnType, context.pureModel.getExecutionSupport()) + "' is not in the class hierarchy of '" + HelperModelBuilder.getTypeFullPath(signatureType, context.pureModel.getExecutionSupport()) + "'", errorSourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static boolean checkTransformTypeMatchesPropertyType(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification expression, CompileContext context)
    {
        RichIterable<? extends Type> classesTaxonomy = Root_meta_pure_functions_meta_getAllTypeGeneralisations_Type_1__Type_MANY_(expression._genericType()._rawType(), context.getExecutionSupport());
        return classesTaxonomy.contains(property._genericType()._rawType());
    }

    // True when the transform is empty (`[]`, upper mult 0) and the property is optional (lower mult 0).
    // Such a mapping needs no target class mapping — the field is deterministically empty.
    private static boolean isEmptyTransformOnOptionalClassProperty(
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification last,
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property)
    {
        Long transformUpper = last._multiplicity() == null ? null : last._multiplicity()._upperBound() == null ? null : last._multiplicity()._upperBound()._value();
        Long propertyLower = property._multiplicity() == null ? null : property._multiplicity()._lowerBound() == null ? null : property._multiplicity()._lowerBound()._value();
        return transformUpper != null && transformUpper == 0L && propertyLower != null && propertyLower == 0L;
    }
}
