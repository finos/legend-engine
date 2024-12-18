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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_relation_RelationFunctionInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_aggregationAware_AggregationAwarePropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_model_PurePropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_relation_RelationFunctionPropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_xStore_XStorePropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.xStore.XStoreAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.function.Function;

public class PropertyMappingBuilder implements PropertyMappingVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping>
{
    private final CompileContext context;

    private PropertyMappingsImplementation immediateParent;
    private RichIterable<EnumerationMapping<Object>> allEnumerationMappings;

    private PureInstanceSetImplementation owner;
    private String setImplId;

    private Mapping mapping;
    private XStoreAssociationImplementation parent;
    private RichIterable<SetImplementation> allClassMappings;

    public PropertyMappingBuilder(CompileContext context, PropertyMappingsImplementation immediateParent, RichIterable<EnumerationMapping<Object>> allEnumerationMappings)
    {
        this.context = context;
        this.immediateParent = immediateParent;
        this.allEnumerationMappings = allEnumerationMappings;
    }

    public PropertyMappingBuilder(CompileContext context, Mapping mapping, XStoreAssociationImplementation parent, RichIterable<SetImplementation> allClassMappings)
    {
        this.context = context;
        this.mapping = mapping;
        this.parent = parent;
        this.allClassMappings = allClassMappings;
    }

    public PropertyMappingBuilder(CompileContext context, PureInstanceSetImplementation mappingClass, String id, RichIterable<EnumerationMapping<Object>> allEnumerationMappings)
    {
        this.context = context;
        this.owner = mappingClass;
        this.setImplId = id;
        this.allEnumerationMappings = allEnumerationMappings;
    }

    @Override
    public PropertyMapping visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping)
    {
        throw new UnsupportedOperationException("Unsupported property mapping type '" + propertyMapping.getClass() + "'");
    }

    @Override
    public PropertyMapping visit(PurePropertyMapping propertyMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping pm = new Root_meta_external_store_model_PurePropertyMapping_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getType("meta::external::store::model::PurePropertyMapping"));
        Property property = HelperMappingBuilder.getMappedProperty(propertyMapping, this.context);
        pm.setSourceInformation(SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation));
        pm._property(property)
                ._explodeProperty(propertyMapping.explodeProperty)
                ._sourceSetImplementationId(propertyMapping.source)
                ._targetSetImplementationId(HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping, property, context))
                ._owner(owner);
        String mappingName = owner._parent().getName();
        pm._transform(HelperMappingBuilder.processPurePropertyMappingTransform(propertyMapping, propertyMapping.transform, owner, owner._srcClass(), this.context, mappingName));

        if (propertyMapping.enumMappingId != null)
        {
            EnumerationMapping<Object> eMap = allEnumerationMappings.select(e -> e._name().equals(propertyMapping.enumMappingId)).getFirst();
            Assert.assertTrue(eMap != null, () -> "Can't find enumeration mapping '" + propertyMapping.enumMappingId + "'", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
            pm._transformer(eMap);
        }

        if (propertyMapping.localMappingProperty != null)
        {
            pm._localMappingProperty(true);
            pm._localMappingPropertyType(this.context.resolveType(propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
            pm._localMappingPropertyMultiplicity(this.context.pureModel.getMultiplicity(propertyMapping.localMappingProperty.multiplicity));
        }

        return pm;
    }

    @Override
    public PropertyMapping visit(XStorePropertyMapping propertyMapping)
    {
        ProcessingContext ctx = new ProcessingContext("Create Xstore Property Mapping");

        InstanceSetImplementation sourceSet = (InstanceSetImplementation) allClassMappings.detect(c -> c._id().equals(propertyMapping.source));
        if (sourceSet == null)
        {
            throw new EngineException("Can't find class mapping '" + propertyMapping.source + "' in mapping '" + HelperModelBuilder.getElementFullPath(mapping, this.context.pureModel.getExecutionSupport()) + "'", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        InstanceSetImplementation targetSet = (InstanceSetImplementation) allClassMappings.detect(c -> c._id().equals(HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping)));
        if (targetSet == null)
        {
            throw new EngineException("Can't find class mapping '" + propertyMapping.target + "' in mapping '" + HelperModelBuilder.getElementFullPath(mapping, this.context.pureModel.getExecutionSupport()) + "'", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        Class thisClass = sourceSet._mappingClass() == null ? sourceSet._class() : sourceSet._mappingClass();
        Class thatClass = targetSet._mappingClass() == null ? targetSet._class() : targetSet._mappingClass();

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity oneMultiplicity = this.context.pureModel.getMultiplicity("one");

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression thisVariable = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name("this");
        thisVariable._genericType(context.newGenericType(thisClass));
        thisVariable._multiplicity(oneMultiplicity);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression thatVariable = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name("that");
        thatVariable._genericType(context.newGenericType(thatClass));
        thatVariable._multiplicity(oneMultiplicity);

        MutableList<VariableExpression> pureParameters = FastList.newListWith(thisVariable, thatVariable);

        ctx.addInferredVariables("this", thisVariable);
        ctx.addInferredVariables("that", thatVariable);

        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications = ListIterate.collect(propertyMapping.crossExpression.body, p -> p.accept(new ValueSpecificationBuilder(this.context, openVariables, ctx)));
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        cleanedOpenVariables.removeAll(pureParameters.collect(e -> e._name()));
        GenericType functionType = PureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity(), context.pureModel);
        String mappingPath = HelperModelBuilder.getElementFullPath(mapping, this.context.pureModel.getExecutionSupport()).replace("::", "_");
        ctx.flushVariable("that");
        ctx.flushVariable("this");

        if (!valueSpecifications.getLast()._genericType()._rawType().equals(context.pureModel.getType("Boolean")) || !valueSpecifications.getLast()._multiplicity().equals(context.pureModel.getMultiplicity("one")))
        {
            throw new EngineException("XStore property mapping function should return 'Boolean[1]'", propertyMapping.crossExpression.body.get(propertyMapping.crossExpression.body.size() - 1).sourceInformation, EngineErrorType.COMPILATION);
        }

        LambdaFunction lambda = new Root_meta_pure_metamodel_function_LambdaFunction_Impl(parent._id() + "." + propertyMapping.property.property, new SourceInformation(mappingPath, 0, 0, 0, 0), null)
                ._classifierGenericType(context.newGenericType(this.context.pureModel.getType(M3Paths.LambdaFunction), FastList.newListWith(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.xStore.XStorePropertyMapping xpm = new Root_meta_pure_mapping_xStore_XStorePropertyMapping_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::xStore::XStorePropertyMapping"));

        String propertyName = propertyMapping.property.property;
        String edgePointPropertyName = MilestoningFunctions.getEdgePointPropertyName(propertyName);
        Function<Type, Boolean> isTypeTemporalMilestoned = type -> Milestoning.temporalStereotypes(((PackageableElement) type)._stereotypes()) != null;
        Property property = parent._association()._properties().detect(p -> (propertyName.equals(p.getName())) || (isTypeTemporalMilestoned.apply(p._genericType()._rawType()) && edgePointPropertyName.equals(p.getName())));
        Assert.assertTrue(property != null, () -> "Can't find property '" + propertyName + "' in association '" + (HelperModelBuilder.getElementFullPath(parent._association(), context.pureModel.getExecutionSupport())) + "'", propertyMapping.property.sourceInformation, EngineErrorType.COMPILATION);

        return xpm._property(property)
                ._localMappingProperty(propertyMapping.localMappingProperty != null)
                ._sourceSetImplementationId(propertyMapping.source == null ? parent._id() : propertyMapping.source)
                ._targetSetImplementationId(HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping))
                ._owner(parent)._crossExpression(lambda);
    }

    @Override
    public PropertyMapping visit(AggregationAwarePropertyMapping propertyMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwarePropertyMapping apm = new Root_meta_pure_mapping_aggregationAware_AggregationAwarePropertyMapping_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::aggregationAware::AggregationAwarePropertyMapping"));
        Property property = HelperMappingBuilder.getMappedProperty(propertyMapping, this.context);
        apm._localMappingProperty(propertyMapping.localMappingProperty != null)
                ._property(property)
                ._sourceSetImplementationId(propertyMapping.source == null || propertyMapping.source.isEmpty() ? immediateParent._id() : propertyMapping.source)
                ._targetSetImplementationId(HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping, property, context))
                ._owner(immediateParent);
        return apm;
    }

    @Override
    public PropertyMapping visit(RelationFunctionPropertyMapping propertyMapping)
    {
        SourceInformation sourceInfo = SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation);
        ProcessorSupport processorSupport = context.pureModel.getExecutionSupport().getProcessorSupport();

        Property<?, ?> property = HelperMappingBuilder.getMappedProperty(propertyMapping, this.context);
        Multiplicity propertyMultiplicity = property._multiplicity();
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(propertyMultiplicity) && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isZeroToOne(propertyMultiplicity))
        {
            throw new EngineException("Properties in relation mappings can only have multiplicity 1 or 0..1, but the property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has multiplicity " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyMultiplicity) + ".", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        Type propertyType = property._genericType()._rawType();
        if (!processorSupport.type_isPrimitiveType(propertyType))
        {
            throw new EngineException("Relation mapping is only supported for primitive properties, but the property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property) + "' has type " + propertyType._name() + ".", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        RelationType<?> newRelationType = _RelationType.build(Lists.mutable.with(_Column.getColumnInstance(propertyMapping.column, false, propertyType._name(), property._multiplicity(), sourceInfo, processorSupport)), sourceInfo, processorSupport);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping relationFunctionPropertyMapping = new Root_meta_pure_mapping_relation_RelationFunctionPropertyMapping_Impl("", sourceInfo, context.pureModel.getClass("meta::pure::mapping::relation::RelationFunctionPropertyMapping"))
            ._property(property)
            ._sourceSetImplementationId(propertyMapping.source == null || propertyMapping.source.isEmpty() ? immediateParent._id() : propertyMapping.source)
            ._targetSetImplementationId(HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping, property, context))
            ._column(newRelationType._columns().toList().get(0))
            ._owner(immediateParent);

        if (propertyMapping.localMappingProperty != null)
        {
            relationFunctionPropertyMapping._localMappingProperty(true);
            relationFunctionPropertyMapping._localMappingPropertyType(this.context.resolveType(propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
            relationFunctionPropertyMapping._localMappingPropertyMultiplicity(this.context.pureModel.getMultiplicity(propertyMapping.localMappingProperty.multiplicity));
        }
        return relationFunctionPropertyMapping;
    }
}
