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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.profile.Profile;

import java.util.Objects;

public class MilestoningDatePropagationHelper {

    private static final String ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";
    private static final String ALL_VERSIONS_PROPERTY_NAME_SUFFIX = "AllVersions";
    private static final ImmutableList<String> MILESTONING_DATE_SOURCE_TYPES = Lists.immutable.of("getAll", "exists", "project", "filter", "subType", "map");

    public static boolean isGetAllFunctionWithMilestoningContext(SimpleFunctionExpression func)
    {
       return "getAll".equals(func._functionName()) && ("getAll_Class_1__Date_1__T_MANY_".equals(func._func().getName()) || "getAll_Class_1__Date_1__Date_1__T_MANY_".equals(func._func().getName()));
    }

    public static void setMilestoningPropagationContext(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
        Assert.assertTrue(isGetAllFunctionWithMilestoningContext(func), () -> "MilestoneDatePropagationContext can only be set from getAll() function");
        ValueSpecification parameterValue = func._parametersValues().getFirst();
        MilestoningStereotype milestoningStereotype = Milestoning.temporalStereotypes(((AnnotatedElement)parameterValue._genericType()._typeArguments().toList().getFirst()._rawType())._stereotypes());
        ListIterable<? extends ValueSpecification> temporalParameterValues = func._parametersValues().toList();
        setMilestoningDates(temporalParameterValues, milestoningStereotype, processingContext);
    }

    public static void setMilestoningDates(ListIterable<? extends ValueSpecification> temporalParameterValues, MilestoningStereotype milestoningStereotype, ProcessingContext processingContext)
    {
        if (isBusinessTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setBusinessDate(temporalParameterValues.get(1));
        }
        else if (isProcessingTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(temporalParameterValues.get(1));
        }
        else if (isBiTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(temporalParameterValues.get(1));
            processingContext.milestoningDatePropagationContext.setBusinessDate(temporalParameterValues.get(2));
        }
    }

    public static boolean isFilter(SimpleFunctionExpression func)
    {
        return "filter".equals(func._functionName());
    }

    public static void updateMilestoningPropagationContext(SimpleFunctionExpression property, ProcessingContext processingContext)
    {
        if(!property._func()._functionName().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX) && !property._func()._functionName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX))
        {
            Class target = (Class) property._genericType()._rawType();
            MilestoningStereotype targetTypeMilestoningStereotype = Milestoning.temporalStereotypes(target._stereotypes());
            setMilestoningDates(property._parametersValues().toList(), targetTypeMilestoningStereotype, processingContext);
        }
        else
        {
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
        }
    }

    public static void updateMilestoningPropagationContextForFilter(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
        ValueSpecification parameterValue = func._parametersValues().getFirst();
        if(parameterValue instanceof SimpleFunctionExpression && isGetAllFunctionWithMilestoningContext((SimpleFunctionExpression) parameterValue))
        {
            setMilestoningPropagationContext((SimpleFunctionExpression) parameterValue, processingContext);
        }
    }

    public static void updateMilestoningPropagationContextForAutoMap(AbstractProperty<?> property, CompileContext context, int parametersCount, ValueSpecification processedParameter, ProcessingContext processingContext)
    {
        if (isGeneratedMilestonedQualifiedPropertyWithMissingDates(property, context, parametersCount) && processedParameter instanceof SimpleFunctionExpression && ((SimpleFunctionExpression) processedParameter)._func() instanceof AbstractProperty && isGeneratedMilestoningProperty((AbstractProperty<?>) ((SimpleFunctionExpression) processedParameter)._func(), context))
        {
            updateMilestoningPropagationContext((SimpleFunctionExpression) processedParameter, processingContext);
        }
    }

    public static void updateMilestoningContext(AbstractProperty<?> property, ProcessingContext processingContext, CompileContext context, SimpleFunctionExpression func)
    {
        if (isGeneratedQualifiedPropertyWithDatePropagationSupported(property, context))
        {
            updateMilestoningPropagationContext(func, processingContext);
        }
        else if(property instanceof QualifiedProperty || property.getName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX) || property.getName().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
        }
    }

    public static void isValidSource(AppliedFunction appliedFunction, ProcessingContext processingContext)
    {
        if (!MILESTONING_DATE_SOURCE_TYPES.contains(appliedFunction.function) && !appliedFunction.parameters.isEmpty() && appliedFunction.parameters.get(0) instanceof AppliedFunction && ((AppliedFunction) appliedFunction.parameters.get(0)).function.equals("getAll"))
        {
            processingContext.milestoningDatePropagationContext.isDatePropagationSupported = false;
        }
    }

    public static void updateMilestoningContextFromValidSources(ValueSpecification result, ProcessingContext processingContext)
    {
        if (result instanceof SimpleFunctionExpression && "map".equals(((SimpleFunctionExpression) result)._functionName()))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
        }
        if (result instanceof SimpleFunctionExpression && isGetAllFunctionWithMilestoningContext((SimpleFunctionExpression) result) && processingContext.milestoningDatePropagationContext.isDatePropagationSupported) {
            setMilestoningPropagationContext((SimpleFunctionExpression) result, processingContext);
        }
        if (result instanceof SimpleFunctionExpression && isFilter((SimpleFunctionExpression) result))
        {
            updateMilestoningPropagationContextForFilter((SimpleFunctionExpression) result, processingContext);
        }
    }

    public static boolean isAllVersionsInRangeProperty(AbstractProperty<?> property, CompileContext context)
    {
        return isGeneratedMilestoningProperty(property, context) && property._name().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX);
    }

    public static boolean isGeneratedMilestoningProperty(AbstractProperty<?> property, CompileContext context)
    {
        String stereotype = String.valueOf(Milestoning.GeneratedMilestoningStereotype.generatedmilestoningproperty);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile = context.pureModel.getProfile("meta::pure::profiles::milestoning");
        Stereotype milestoningStereotype = (Stereotype) Profile.findStereotype(profile, stereotype);
        RichIterable<? extends Stereotype> stereotypes =  property._stereotypes();
        return stereotypes.detect(s -> s != null && milestoningStereotype.equals(s)) != null;
    }

    public static boolean isGeneratedQualifiedPropertyWithDatePropagationSupported(AbstractProperty<?> property, CompileContext context)
    {
        return property instanceof QualifiedProperty && isGeneratedMilestoningProperty(property, context) && !isAllVersionsInRangeProperty(property, context);
    }

    private static int getCountOfParametersSatisfyingMilestoningDateRequirments(QualifiedProperty milestonedQualifiedProperty, CompileContext context)
    {
        if (!isGeneratedMilestoningProperty(milestonedQualifiedProperty, context))
        {
            throw new EngineException("Unable to get milestoning date parameters for non milestoned QualifiedProperty: " + milestonedQualifiedProperty.getName());
        }
        Class returnType = (Class)milestonedQualifiedProperty._genericType()._rawType();
        MilestoningStereotype milestoningStereotype = Milestoning.temporalStereotypes(returnType._stereotypes());
        return 1 + milestoningStereotype.getTemporalDatePropertyNames().size();
    }

    public static boolean isGeneratedMilestonedQualifiedPropertyWithMissingDates(AbstractProperty<?> property, CompileContext context, Integer parametersCount)
    {
        if (isGeneratedQualifiedPropertyWithDatePropagationSupported(property, context))
        {
            return parametersCount != getCountOfParametersSatisfyingMilestoningDateRequirments((QualifiedProperty)property, context);
        }
        return false;
    }

    public static boolean isProcessingTemporal(MilestoningStereotype milestoningStereotype)
    {
        return milestoningStereotype != null && milestoningStereotype.getPurePlatformStereotypeName() == "processingtemporal";
    }

    public static boolean isBusinessTemporal(MilestoningStereotype milestoningStereotype)
    {
        return milestoningStereotype != null && milestoningStereotype.getPurePlatformStereotypeName() == "businesstemporal";
    }

    private static boolean isSingleDateTemporal(MilestoningStereotype milestoningStereotype)
    {
        return milestoningStereotype != null && isProcessingTemporal(milestoningStereotype) || isBusinessTemporal(milestoningStereotype);
    }

    private static Type getMilestonedPropertyOwningType(AbstractProperty<?>property)
    {
        if (property._owner() instanceof Class)
        {
            return (Class) property._owner();
        }
        else if (property._owner() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association)
        {
            return ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association) property._owner())._originalMilestonedProperties().toList().select(prop -> prop._name() != property._name()).getFirst()._genericType()._rawType();

        }
        return null;
    }

    private static Pair<MilestoningStereotype, MilestoningStereotype> getSourceTargetMilestoningStereotypes(AbstractProperty<?> property)
    {
        Class source = (Class) getMilestonedPropertyOwningType(property);
        Class target = (Class) property._genericType()._rawType();
        MilestoningStereotype sourceTypeMilestoningStereotype = Milestoning.temporalStereotypes(source._stereotypes()) != null ? Milestoning.temporalStereotypes(source._stereotypes()) : null;
        MilestoningStereotype targetTypeMilestoningStereotype = Milestoning.temporalStereotypes(target._stereotypes()) != null ? Milestoning.temporalStereotypes(target._stereotypes()) : null;
        return Tuples.pair(sourceTypeMilestoningStereotype, targetTypeMilestoningStereotype);
    }

    public static boolean isBiTemporal(MilestoningStereotype milestoningStereotype)
    {
        return milestoningStereotype != null && milestoningStereotype.getPurePlatformStereotypeName() == "bitemporal";
    }

    private static void setMilestoningDateParameters(ValueSpecification[] dateParamValues, int index, ValueSpecification milestoningDate)
    {
        dateParamValues[index] = milestoningDate;
    }

    private static boolean oneDateParamSupplied(ListIterable<? extends ValueSpecification> parameterValues)
    {
        return parameterValues.size() == 2;
    }

    private static boolean noDateParamSupplied(ListIterable<? extends ValueSpecification> parameterValues)
    {
        return parameterValues.size() == 1;
    }

    public static void applyPropertyFunctionExpressionMilestonedDates(FunctionExpression fe, AbstractProperty<?> func, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        Pair<MilestoningStereotype, MilestoningStereotype> sourceTargetMilestoningStereotypes = getSourceTargetMilestoningStereotypes(func);
        MilestoningStereotype sourceTypeMilestoning = sourceTargetMilestoningStereotypes.getOne();
        MilestoningStereotype targetTypeMilestoning = sourceTargetMilestoningStereotypes.getTwo();
        String propertyName = func.getName();
        MutableList<? extends ValueSpecification> parametersValues = fe._parametersValues().toList();
        ValueSpecification[] milestoningDateParameters = new ValueSpecification[targetTypeMilestoning.getTemporalDatePropertyNames().size()];
        fe._originalMilestonedPropertyParametersValues(fe._parametersValues());

        if (isBiTemporal(targetTypeMilestoning))
        {
            if (isBiTemporal(sourceTypeMilestoning) && oneDateParamSupplied(parametersValues))
            {
                milestoningDateParameters[0] =  processingContext.milestoningDatePropagationContext.getProcessingDate();
                milestoningDateParameters[1] = parametersValues.get(1);
            }
            else if (isSingleDateTemporal(sourceTypeMilestoning) && oneDateParamSupplied(parametersValues))
            {
                int propagatedDateIndex = Objects.requireNonNull(sourceTypeMilestoning).positionInTemporalParameterValues();
                ValueSpecification propagatedDate;
                int otherPropagatedDateIndex;
                if (isProcessingTemporal(sourceTypeMilestoning))
                {
                    propagatedDate =  processingContext.milestoningDatePropagationContext.getProcessingDate();
                    otherPropagatedDateIndex = 1;
                }
                else
                {
                    propagatedDate =  processingContext.milestoningDatePropagationContext.getBusinessDate();
                    otherPropagatedDateIndex = 0;
                }
                setMilestoningDateParameters(milestoningDateParameters, propagatedDateIndex, propagatedDate);
                setMilestoningDateParameters(milestoningDateParameters, otherPropagatedDateIndex, parametersValues.get(1));
            }
            if (isBiTemporal(sourceTypeMilestoning) && noDateParamSupplied(parametersValues))
            {
                milestoningDateParameters[0] = processingContext.milestoningDatePropagationContext.getProcessingDate();
                milestoningDateParameters[1] = processingContext.milestoningDatePropagationContext.getBusinessDate();
            }
        }
        else if (isSingleDateTemporal(targetTypeMilestoning) && noDateParamSupplied(parametersValues))
        {
            ValueSpecification propagatedDate;
            if (isProcessingTemporal(targetTypeMilestoning))
            {
                propagatedDate = processingContext.milestoningDatePropagationContext.getProcessingDate();
            }
            else
            {
                propagatedDate = processingContext.milestoningDatePropagationContext.getBusinessDate();
            }
            if (isBiTemporal(sourceTypeMilestoning))
            {
                setMilestoningDateParameters(milestoningDateParameters, 0, propagatedDate);
            }
            if (sourceTypeMilestoning == targetTypeMilestoning)
            {
                setMilestoningDateParameters(milestoningDateParameters, 0, propagatedDate);
            }
        }
        if (!ArrayIterate.isEmpty(milestoningDateParameters))
        {
            parametersValues = LazyIterate.concatenate(FastList.<ValueSpecification>newListWith(parametersValues.get(0)), FastList.newListWith(milestoningDateParameters)).toList();
            fe._parametersValues(parametersValues);
        }
    }

    public static void updateFunctionExpressionWithMilestoningDateParams(FunctionExpression functionExpression, AbstractProperty<?> propertyFunc, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        applyPropertyFunctionExpressionMilestonedDates(functionExpression, propertyFunc, sourceInformation, processingContext);
        String propertyName = propertyFunc._name();
        Class owner = (Class) getMilestonedPropertyOwningType(propertyFunc);
        Object prop = ListIterate.select(owner._qualifiedProperties().toList(), p -> p instanceof QualifiedProperty && ((QualifiedProperty) p)._name() == propertyName).getFirst();
        if (prop == null)
        {
            prop = ListIterate.select(owner._qualifiedPropertiesFromAssociations().toList(), p -> p instanceof QualifiedProperty && ((QualifiedProperty) p)._name() == propertyName).getFirst();
        }
        functionExpression._func((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends Object>) prop);
    }
}
