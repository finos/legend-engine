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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_QualifiedProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.List;
import java.util.function.BiConsumer;

public class Milestoning
{
    private enum GeneratedMilestoningStereotype
    {
        generatedmilestoningproperty,
        generatedmilestoningdateproperty
    }

    private static class MilestoningPropertyTransformation
    {
        private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> originalProperty;
        private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> edgePointProperty;
        private MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?>> qualfiedPropertiesToAdd;
        private boolean isMilestonedProperty;

        MilestoningPropertyTransformation(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> originalProperty)
        {
            this.originalProperty = originalProperty;
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getOriginalProperty()
        {
            return this.originalProperty;
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getEdgePointProperty()
        {
            return this.edgePointProperty;
        }

        void setEdgePointProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property)
        {
            this.edgePointProperty = property;
            this.isMilestonedProperty = true;
        }

        void setQualifiedProperties(MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?>> qualfiedProperties)
        {
            this.qualfiedPropertiesToAdd = qualfiedProperties;
        }

        boolean isTransformed()
        {
            return this.isMilestonedProperty;
        }
    }

    public static Iterable<? extends Property> generateMilestoningProperties(Class<Object> owner, CompileContext context)
    {
        MutableList<Property> generatedMilestoningProperties = Lists.mutable.empty();
        MutableList<MilestoningStereotype> milestoningStereotype = Lists.mutable.ofAll(temporalStereotypes(owner._stereotypes()));
        if (milestoningStereotype.notEmpty())
        {
            MutableList<Property> generatedMilestoningDateProperties = generateMilestoningDateProperties(context, milestoningStereotype.getFirst(), owner);
            Property generatedMilestoningRangeProperty = generateMilestoningRangeProperty(context, milestoningStereotype.getFirst(), owner);
            generatedMilestoningProperties.withAll(generatedMilestoningDateProperties).with(generatedMilestoningRangeProperty);
        }
        return generatedMilestoningProperties;
    }

    private static MutableList<Property> generateMilestoningDateProperties(CompileContext context, MilestoningStereotype milestoningStereotype, Class<Object> owner)
    {
        GenericType dateGenericType = context.pureModel.getGenericType("Date");
        MutableList<Property> generatedMilestoningDateProperties = Lists.mutable.ofAll(milestoningStereotype.getTemporalDatePropertyNames()).collect(name -> new Root_meta_pure_metamodel_function_property_Property_Impl<>(name)
                ._name(name)
                ._genericType(dateGenericType)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                        ._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::Property"))
                        ._typeArguments(Lists.fixedSize.of(owner._classifierGenericType(), dateGenericType)))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._stereotypes(Lists.fixedSize.of(generatedMilestoningStereotype(context, GeneratedMilestoningStereotype.generatedmilestoningdateproperty)))
                ._owner(owner));
        return generatedMilestoningDateProperties;
    }

    private static Property generateMilestoningRangeProperty(CompileContext context, MilestoningStereotype milestoningStereotype, Class<Object> owner)
    {
        GenericType milestoningRangePropertyGenericType = context.resolveGenericType(milestoningStereotype.getMilestoningPropertyClassName());
        Property generatedMilestoningRangeProperty = new Root_meta_pure_metamodel_function_property_Property_Impl<>(MilestoningFunctions.MILESTONING)
                ._name(MilestoningFunctions.MILESTONING)
                ._genericType(milestoningRangePropertyGenericType)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                        ._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::Property"))
                        ._typeArguments(Lists.fixedSize.of(owner._classifierGenericType(), milestoningRangePropertyGenericType)))
                ._multiplicity(context.pureModel.getMultiplicity("zeroone"))
                ._stereotypes(Lists.fixedSize.of(generatedMilestoningStereotype(context, GeneratedMilestoningStereotype.generatedmilestoningdateproperty)))
                ._owner(owner);
        return generatedMilestoningRangeProperty;
    }

    public static void applyMilestoningClassTransformations(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> clazz)
    {
        applyMilestoningPropertyTransformations(context, clazz, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_properties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_qualifiedProperties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_originalMilestonedProperties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_properties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_qualifiedProperties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_originalMilestonedPropertiesAddAll);
        applyMilestoningPropertyTransformations(context, clazz, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_propertiesFromAssociations, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_qualifiedPropertiesFromAssociations, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_originalMilestonedProperties, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_propertiesFromAssociations, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_qualifiedPropertiesFromAssociations, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class::_originalMilestonedPropertiesAddAll);
    }

    public static void applyMilestoningPropertyTransformations(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class source, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class target, MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends Object, ? extends Object>> properties, MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<? extends Object>> qualifiedProperties, MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends Object, ? extends Object>> originalMilestonedProperties)
    {
        properties.removeAllIterable(properties.select(x -> x._genericType()._rawType().equals(source)));
        properties.addAllIterable(target._propertiesFromAssociations().select(x -> ((AbstractProperty<?>) x)._owner().equals(association)));
        qualifiedProperties.removeAllIterable(qualifiedProperties.select(x -> x._genericType()._rawType().equals(source)));
        qualifiedProperties.addAllIterable(target._qualifiedPropertiesFromAssociations().select(x -> ((AbstractProperty<?>) x)._owner().equals(association)));
        originalMilestonedProperties.removeAllIterable(originalMilestonedProperties.select(x -> x._genericType()._rawType().equals(source)));
        originalMilestonedProperties.addAllIterable(target._originalMilestonedProperties().select(x -> ((AbstractProperty<?>) x)._owner().equals(association)));
    }

    private static void applyMilestoningPropertyTransformations(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> clazz, Function<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property>> propertiesGetter, Function<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty>> qualifiedPropertiesGetter,org.eclipse.collections.api.block.function.Function<Class, RichIterable<? extends Property>> originalMilestonedPropertiesGetter, BiConsumer<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class, ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property>> propertiesSetter, BiConsumer<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class, ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty>> qualifiedPropertiesSetter, BiConsumer<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class, ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property>> originalPropertySetter)
    {
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property> properties = propertiesGetter.valueOf(clazz).reject(isGeneratedMilestoningNonDateProperty());
        RichIterable<MilestoningPropertyTransformation> milestoningPropertyTransformations = properties.collect(p -> milestoningPropertyTransformations(p, context, clazz, p._owner())).select(MilestoningPropertyTransformation::isTransformed);

        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> originalProperties = milestoningPropertyTransformations.collect(MilestoningPropertyTransformation::getOriginalProperty);
        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> edgePointProperties = milestoningPropertyTransformations.collect(MilestoningPropertyTransformation::getEdgePointProperty);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property> originalMilestonedProperties = originalMilestonedPropertiesGetter.valueOf(clazz);
        if (!edgePointProperties.isEmpty())
        {
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property> propertiesUpdated = Lists.mutable.withAll(properties.select(p -> !originalProperties.contains(p))).withAll((Iterable) edgePointProperties);
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty> qualifiedPropertiesUpdated = Lists.mutable.withAll(qualifiedPropertiesGetter.valueOf(clazz)).withAll((Iterable) milestoningPropertyTransformations.flatCollect(t -> t.qualfiedPropertiesToAdd));
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property> originalMilestonedPropertiesUpdated = Lists.mutable.withAll(originalProperties.reject(p -> originalMilestonedProperties.anySatisfy(o -> o.getName().equals(p.getName()))));
            propertiesSetter.accept(clazz, propertiesUpdated);
            qualifiedPropertiesSetter.accept(clazz, qualifiedPropertiesUpdated);
            originalPropertySetter.accept(clazz, originalMilestonedPropertiesUpdated);
        }
    }

    private static MilestoningPropertyTransformation milestoningPropertyTransformations(Property<?, ?> originalProperty, CompileContext context, Class<?> sourceClass, PropertyOwner propertyOwner)
    {
        List<MilestoningStereotype> returnTypeMilestoningStereotypes = temporalStereotypes(originalProperty._genericType()._rawType()._stereotypes());
        MilestoningPropertyTransformation milestoningPropertyTransformation = new MilestoningPropertyTransformation(originalProperty);

        if (!returnTypeMilestoningStereotypes.isEmpty())
        {
            MilestoningStereotype returnTypeMilestoningStereotype = returnTypeMilestoningStereotypes.get(0);
            MutableList<Stereotype> stereotypes = Lists.mutable.withAll(originalProperty._stereotypes());
            MutableList<Stereotype> withMilestoningStereotype = stereotypes.with(generatedMilestoningStereotype(context, GeneratedMilestoningStereotype.generatedmilestoningproperty));

            Property<?, ?> edgePointProperty = edgePointProperty(propertyOwner, originalProperty, stereotypes);
            milestoningPropertyTransformation.setEdgePointProperty(edgePointProperty);

            MutableList<QualifiedProperty<?>> milestoningQualifiedPropertyWithArg = newSingleDateMilestoningQualifiedPropertyWithArg(context, sourceClass, propertyOwner, originalProperty, returnTypeMilestoningStereotype, withMilestoningStereotype, edgePointProperty);
            MutableList<QualifiedProperty<?>> milestoningRangeQualifiedProperty = generateMilestoningRangeQualifiedProperty(context, sourceClass, propertyOwner, originalProperty, returnTypeMilestoningStereotype, withMilestoningStereotype, edgePointProperty);

            List<MilestoningStereotype> sourceMilestoningStereotype = temporalStereotypes(sourceClass._stereotypes());
            milestoningQualifiedPropertyWithArg.withAll(milestoningRangeQualifiedProperty);
            if(!sourceMilestoningStereotype.isEmpty() && (sourceMilestoningStereotype.get(0).equals(returnTypeMilestoningStereotypes.get(0)) || MilestoningStereotypeEnum.bitemporal.equals(sourceMilestoningStereotype.get(0))))
            {
                QualifiedProperty<?> milestoningQualifiedPropertyNoArg = newSingleDateMilestoningQualifiedPropertyNoArg(context, sourceClass, propertyOwner, originalProperty, returnTypeMilestoningStereotype, withMilestoningStereotype, edgePointProperty);
                milestoningQualifiedPropertyWithArg.add(milestoningQualifiedPropertyNoArg);
            }
            milestoningPropertyTransformation.setQualifiedProperties(milestoningQualifiedPropertyWithArg);
        }
        return milestoningPropertyTransformation;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> edgePointProperty(PropertyOwner owner, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> originalProperty, MutableList<Stereotype> stereotypes)
    {
        String edgePointPropertyName = MilestoningFunctions.getEdgePointPropertyName(originalProperty._name());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity = new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("")
                ._lowerBound(originalProperty._multiplicity()._lowerBound())
                ._upperBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("")._value(-1L));
        return newProperty(owner, originalProperty, edgePointPropertyName, multiplicity, stereotypes);
    }

    private static QualifiedProperty<?> newSingleDateMilestoningQualifiedPropertyNoArg(CompileContext context, Class<?> sourceClass, PropertyOwner propertyOwner, Property originalProperty, MilestoningStereotype returnTypeMilestoningStereotype, MutableList<Stereotype> stereotypes, Property edgePointProperty)
    {
        String qualifiedPropertyName = originalProperty._name();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?> qualifiedProperty = getQualifiedProperty(propertyOwner, originalProperty, qualifiedPropertyName, originalProperty._multiplicity(), stereotypes);

        VariableExpression thisVar = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name("this")._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(sourceClass._classifierGenericType()._typeArguments().toList().get(0));
        VariableExpression v_milestoning = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name("v_milestoning")._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(edgePointProperty._genericType());

        ListIterable<Pair<VariableExpression, SimpleFunctionExpression>> datesToCompare = returnTypeMilestoningStereotype.getTemporalDatePropertyNames().collect(d ->
        {
            VariableExpression inputTemporalDate = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name(d)._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(context.pureModel.getGenericType("Date"));
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> temporalDateProperty = ((Class<Object>) originalProperty._genericType()._rawType())._properties().detect(p -> p._name().equals(d));
            SimpleFunctionExpression temporalDatePropertyExp = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(temporalDateProperty)
                    ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.fixedSize.of(d)))
                    ._genericType(context.pureModel.getGenericType("Date"))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._parametersValues(Lists.fixedSize.of(v_milestoning));
            return Tuples.pair(inputTemporalDate, temporalDatePropertyExp);
        });

        ListIterable<SimpleFunctionExpression> equalExpressions = datesToCompare.collect(p -> new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_", true))
                ._functionName("eq")
                ._genericType(context.pureModel.getGenericType("Boolean"))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._parametersValues(Lists.fixedSize.<ValueSpecification>of(p.getTwo(), p.getOne())));
        SimpleFunctionExpression equalExpression = equalExpressions.size() == 1 ? equalExpressions.get(0) : new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_", true))
                ._functionName("and")
                ._genericType(context.pureModel.getGenericType("Boolean"))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._parametersValues(equalExpressions);

        GenericType functionType = PureModel.buildFunctionType(Lists.fixedSize.of(v_milestoning), context.pureModel.getGenericType("Boolean"), context.pureModel.getMultiplicity("one"));

        LambdaFunction filterLambda = new Root_meta_pure_metamodel_function_LambdaFunction_Impl("")
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                        ._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))
                        ._typeArguments(FastList.newListWith(functionType)))
                ._openVariables(Lists.fixedSize.of("this"))
                ._expressionSequence(Lists.fixedSize.of(equalExpression));

        InstanceValue filterInstanceValue = new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")
                ._genericType(filterLambda._classifierGenericType())
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._values(Lists.fixedSize.of(filterLambda));

        SimpleFunctionExpression filterLhs = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(edgePointProperty)
                ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.fixedSize.of(edgePointProperty._name())))
                ._genericType(edgePointProperty._genericType())
                ._multiplicity(edgePointProperty._multiplicity())
                ._parametersValues(Lists.fixedSize.of(thisVar));

        SimpleFunctionExpression filterExpression = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_", true))
                ._functionName("filter")
                ._genericType(qualifiedProperty._genericType())
                ._multiplicity(context.pureModel.getMultiplicity("zeromany"))
                ._parametersValues(Lists.fixedSize.of(filterLhs, filterInstanceValue));

        GenericType classifierGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                ._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::QualifiedProperty"))
                ._typeArguments(Lists.fixedSize.of(PureModel.buildFunctionType(Lists.mutable.of(thisVar), qualifiedProperty._genericType(), originalProperty._multiplicity())));

        qualifiedProperty._classifierGenericType(classifierGenericType);
        qualifiedProperty._expressionSequence(Lists.fixedSize.of(filterExpression));

        return qualifiedProperty;
    }

    private static MutableList<QualifiedProperty<?>> newSingleDateMilestoningQualifiedPropertyWithArg(CompileContext context, Class<?> sourceClass, PropertyOwner propertyOwner, Property originalProperty, MilestoningStereotype returnTypeMilestoningStereotype, MutableList<Stereotype> stereotypes, Property edgePointProperty)
    {
        String qualifiedPropertyName = originalProperty._name();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?> qualifiedProperty = getQualifiedProperty(propertyOwner, originalProperty, qualifiedPropertyName, originalProperty._multiplicity(), stereotypes);

        VariableExpression thisVar = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name("this")._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(sourceClass._classifierGenericType()._typeArguments().toList().get(0));
        VariableExpression v_milestoning = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name("v_milestoning")._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(edgePointProperty._genericType());

        ListIterable<Pair<VariableExpression, SimpleFunctionExpression>> datesToCompare = returnTypeMilestoningStereotype.getTemporalDatePropertyNames().collect(d ->
        {
            VariableExpression inputTemporalDate = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")._name(d)._multiplicity(context.pureModel.getMultiplicity("one"))._genericType(context.pureModel.getGenericType("Date"));
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> temporalDateProperty = ((Class<Object>) originalProperty._genericType()._rawType())._properties().detect(p -> p._name().equals(d));
            SimpleFunctionExpression temporalDatePropertyExp = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(temporalDateProperty)
                    ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.fixedSize.of(d)))
                    ._genericType(context.pureModel.getGenericType("Date"))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._parametersValues(Lists.fixedSize.of(v_milestoning));
            return Tuples.pair(inputTemporalDate, temporalDatePropertyExp);
        });

        ListIterable<SimpleFunctionExpression> equalExpressions = datesToCompare.collect(p -> new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_", true))
                ._functionName("eq")
                ._genericType(context.pureModel.getGenericType("Boolean"))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._parametersValues(Lists.fixedSize.<ValueSpecification>of(p.getTwo(), p.getOne())));
        SimpleFunctionExpression equalExpression = equalExpressions.size() == 1 ? equalExpressions.get(0) : new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_", true))
                ._functionName("and")
                ._genericType(context.pureModel.getGenericType("Boolean"))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._parametersValues(equalExpressions);

        GenericType functionType = PureModel.buildFunctionType(Lists.fixedSize.of(v_milestoning), context.pureModel.getGenericType("Boolean"), context.pureModel.getMultiplicity("one"));

        LambdaFunction filterLambda = new Root_meta_pure_metamodel_function_LambdaFunction_Impl("")
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                        ._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))
                        ._typeArguments(FastList.newListWith(functionType)))
                ._openVariables(returnTypeMilestoningStereotype.getTemporalDatePropertyNames())
                ._expressionSequence(Lists.fixedSize.of(equalExpression));

        InstanceValue filterInstanceValue = new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")
                ._genericType(filterLambda._classifierGenericType())
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._values(Lists.fixedSize.of(filterLambda));

        SimpleFunctionExpression filterLhs = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(edgePointProperty)
                ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.fixedSize.of(edgePointProperty._name())))
                ._genericType(edgePointProperty._genericType())
                ._multiplicity(edgePointProperty._multiplicity())
                ._parametersValues(Lists.fixedSize.of(thisVar));

        SimpleFunctionExpression filterExpression = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                ._func(context.pureModel.getFunction("meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_", true))
                ._functionName("filter")
                ._genericType(qualifiedProperty._genericType())
                ._multiplicity(context.pureModel.getMultiplicity("zeromany"))
                ._parametersValues(Lists.fixedSize.of(filterLhs, filterInstanceValue));

        GenericType classifierGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                ._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::QualifiedProperty"))
                ._typeArguments(Lists.fixedSize.of(PureModel.buildFunctionType(Lists.mutable.of(thisVar).withAll(datesToCompare.collect(Functions.firstOfPair())), qualifiedProperty._genericType(), originalProperty._multiplicity())));

        qualifiedProperty._classifierGenericType(classifierGenericType);
        qualifiedProperty._expressionSequence(Lists.fixedSize.of(filterExpression));

        return Lists.mutable.of(qualifiedProperty);
    }

    private static MutableList<QualifiedProperty<?>> generateMilestoningRangeQualifiedProperty(CompileContext context, Class<?> sourceClass, PropertyOwner propertyOwner, Property originalProperty, MilestoningStereotype returnTypeMilestoningStereotype, MutableList<Stereotype> stereotypes, Property edgePointProperty)
    {
        MutableList<QualifiedProperty<?>> generatedMilestoningRangeQualifiedProperty = Lists.mutable.empty();

        if (UNI_TEMPORAL_STEREOTYPE_NAMES.contains(returnTypeMilestoningStereotype.getPurePlatformStereotypeName()))
        {
            String qualifiedPropertyName = MilestoningFunctions.getRangePropertyName(originalProperty._name());
            String temporalDatePropertyName = returnTypeMilestoningStereotype.getTemporalDatePropertyNames().getFirst();

            VariableExpression thisVar = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")
                    ._name("this")
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(sourceClass._classifierGenericType()._typeArguments().getFirst());

            VariableExpression v_milestoning = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")
                    ._name("v_milestoning")
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(edgePointProperty._genericType());

            VariableExpression inputStartDate = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")
                    ._name("start")
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(context.pureModel.getGenericType("Date"));

            VariableExpression inputEndDate = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("")
                    ._name("end")
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(context.pureModel.getGenericType("Date"));

            SimpleFunctionExpression temporalDatePropertyExp = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(HelperModelBuilder.getOwnedProperty((Class<Object>) originalProperty._genericType()._rawType(), temporalDatePropertyName, context.pureModel.getExecutionSupport()))
                    ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.mutable.of(temporalDatePropertyName)))
                    ._genericType(context.pureModel.getGenericType("Date"))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._parametersValues(Lists.mutable.of(v_milestoning));

            SimpleFunctionExpression equalExpression = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(context.pureModel.getFunction("meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_", true))
                    ._functionName("eq")
                    ._genericType(context.pureModel.getGenericType("Boolean"))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._parametersValues(Lists.mutable.of(temporalDatePropertyExp, inputStartDate));

            LambdaFunction filterLambda = new Root_meta_pure_metamodel_function_LambdaFunction_Impl("")
                    ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                            ._rawType(context.pureModel.getType("meta::pure::metamodel::function::LambdaFunction"))
                            ._typeArguments(Lists.mutable.of(PureModel.buildFunctionType(Lists.mutable.of(v_milestoning), context.pureModel.getGenericType("Boolean"), context.pureModel.getMultiplicity("one")))))
                    ._openVariables(Lists.mutable.of(temporalDatePropertyName))
                    ._expressionSequence(Lists.mutable.of(equalExpression));

            InstanceValue filterInstanceValue = new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")
                    ._genericType(filterLambda._classifierGenericType())
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._values(Lists.mutable.of(filterLambda));

            SimpleFunctionExpression filterLhs = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(edgePointProperty)
                    ._propertyName(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")._values(Lists.mutable.of(edgePointProperty._name())))
                    ._genericType(edgePointProperty._genericType())
                    ._multiplicity(edgePointProperty._multiplicity())
                    ._parametersValues(Lists.mutable.of(thisVar));

            SimpleFunctionExpression filterExpression = new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("")
                    ._func(context.pureModel.getFunction("meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_", true))
                    ._functionName("filter")
                    ._genericType(originalProperty._genericType())
                    ._multiplicity(context.pureModel.getMultiplicity("zeromany"))
                    ._parametersValues(Lists.mutable.of(filterLhs, filterInstanceValue));

            QualifiedProperty<?> milestoningRangeQualifiedProperty = getQualifiedProperty(propertyOwner, originalProperty, qualifiedPropertyName, originalProperty._multiplicity(), stereotypes)
                    ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                            ._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::QualifiedProperty"))
                            ._typeArguments(Lists.mutable.of(PureModel.buildFunctionType(Lists.mutable.of(thisVar, inputStartDate, inputEndDate), originalProperty._genericType(), originalProperty._multiplicity()))))
                    ._expressionSequence(Lists.mutable.of(filterExpression));

            generatedMilestoningRangeQualifiedProperty.add(milestoningRangeQualifiedProperty);
        }
        return generatedMilestoningRangeQualifiedProperty;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> newProperty(PropertyOwner owner, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> originalProperty, String name, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity, ListIterable<Stereotype> stereotypes)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = newAbstractProperty(owner, originalProperty, name, multiplicity, stereotypes, new Root_meta_pure_metamodel_function_property_Property_Impl(name));
        property._classifierGenericType(originalProperty._classifierGenericType());
        return property;
    }

    private static QualifiedProperty<?> getQualifiedProperty(PropertyOwner owner, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> originalProperty, String name, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity, ListIterable<Stereotype> stereotypes)
    {
        return newAbstractProperty(owner, originalProperty, name, multiplicity, stereotypes, new Root_meta_pure_metamodel_function_property_QualifiedProperty_Impl(name));
    }

    private static <T extends AbstractProperty<?>> T newAbstractProperty(PropertyOwner owner, Property<?, ?> originalProperty, String name, Multiplicity multiplicity, ListIterable<Stereotype> stereotypes, T property)
    {
        property._name(name)
                ._functionName(name)
                ._genericType(originalProperty._genericType())
                ._multiplicity(multiplicity)
                ._stereotypes(stereotypes)
                ._taggedValues(originalProperty._taggedValues())
                ._owner(owner);
        return property;
    }

    private static Predicate<Property<?, ?>> isGeneratedMilestoningDateProperty()
    {
        return p -> p._stereotypes().anySatisfy(s -> s._value().equals(GeneratedMilestoningStereotype.generatedmilestoningdateproperty.name()));
    }

    private static Predicate<Property> isGeneratedMilestoningNonDateProperty()
    {
        return p -> p._stereotypes().anySatisfy(s -> s._value().equals(GeneratedMilestoningStereotype.generatedmilestoningproperty.name()));
    }

    public static List<MilestoningStereotype> temporalStereotypes(RichIterable<? extends Stereotype> stereotypes)
    {
        return ArrayIterate.select(MilestoningStereotypeEnum.values(), e -> stereotypes.anySatisfy(s -> s._value().equals(e.getPurePlatformStereotypeName())));
    }

    private static Stereotype generatedMilestoningStereotype(CompileContext context, GeneratedMilestoningStereotype generatedMilestoningStereotype)
    {
        return context.pureModel.getStereotype("meta::pure::profiles::milestoning", generatedMilestoningStereotype.name());
    }

    private static final ImmutableList<String> UNI_TEMPORAL_STEREOTYPE_NAMES = Lists.immutable.of("businesstemporal", "processingtemporal");
}
