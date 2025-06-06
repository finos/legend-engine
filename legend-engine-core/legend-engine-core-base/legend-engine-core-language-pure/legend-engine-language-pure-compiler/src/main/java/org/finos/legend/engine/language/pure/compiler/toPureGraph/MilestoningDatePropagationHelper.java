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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.Objects;
import java.util.Stack;

public class MilestoningDatePropagationHelper
{

    private static final String ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";
    private static final String ALL_VERSIONS_PROPERTY_NAME_SUFFIX = "AllVersions";
    private static final ImmutableList<String> MILESTONING_DATE_SOURCE_TYPES = Lists.immutable.of("getAll", "exists", "project", "filter", "subType", "map", "graphFetch", "graphFetchChecked","serialize", "serializeChecked");

    public static boolean isGetAllFunctionWithMilestoningContext(SimpleFunctionExpression func)
    {
        return "getAll".equals(func._functionName()) && ("getAll_Class_1__Date_1__T_MANY_".equals(func._func().getName()) || "getAll_Class_1__Date_1__Date_1__T_MANY_".equals(func._func().getName()));
    }

    public static void setMilestoningPropagationContext(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
        Assert.assertTrue(isGetAllFunctionWithMilestoningContext(func), () -> "MilestoneDatePropagationContext can only be set from getAll() function");
        ValueSpecification parameterValue = func._parametersValues().getFirst();
        MilestoningStereotype milestoningStereotype = Milestoning.temporalStereotypes(((AnnotatedElement) parameterValue._genericType()._typeArguments().toList().getFirst()._rawType())._stereotypes());
        ListIterable<? extends ValueSpecification> temporalParameterValues = func._parametersValues().toList();
        setMilestoningDates(temporalParameterValues, milestoningStereotype, processingContext);
    }

    public static void setMilestoningDates(ListIterable<? extends ValueSpecification> temporalParameterValues, MilestoningStereotype milestoningStereotype, ProcessingContext processingContext)
    {
        if (isBusinessTemporal(milestoningStereotype))
        {
            ValueSpecification businessdate = null;
            if (temporalParameterValues.size() > 1)
            {
               businessdate = temporalParameterValues.get(1);
            }
            MilestoningDatePropagationContext context = new MilestoningDatePropagationContext(null, businessdate);
            processingContext.pushMilestoningDatePropagationContext(context);
        }
        else if (isProcessingTemporal(milestoningStereotype))
        {
            ValueSpecification processingDate = null;
            if (temporalParameterValues.size() > 1)
            {
                processingDate = temporalParameterValues.get(1);
            }
            MilestoningDatePropagationContext context = new MilestoningDatePropagationContext(processingDate, null);
            processingContext.pushMilestoningDatePropagationContext(context);
        }
        else if (isBiTemporal(milestoningStereotype))
        {
            ValueSpecification processingDate = null;
            ValueSpecification businessDate = null;
            if (temporalParameterValues.size() > 2)
            {
                processingDate = temporalParameterValues.get(1);
                businessDate = temporalParameterValues.get(2);
            }
            MilestoningDatePropagationContext context = new MilestoningDatePropagationContext(processingDate, businessDate);
            processingContext.pushMilestoningDatePropagationContext(context);
        }
    }

    public static void updateMilestoningPropagationContext(AbstractProperty<?> property, ListIterable<? extends ValueSpecification> parameterValues, ProcessingContext processingContext)
    {
        if (!property.getName().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX) && !property.getName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX))
        {
            Class target = (Class) property._genericType()._rawType();
            MilestoningStereotype targetTypeMilestoningStereotype = Milestoning.temporalStereotypes(target._stereotypes());
            setMilestoningDates(parameterValues, targetTypeMilestoningStereotype, processingContext);
        }
        else
        {
            processingContext.pushMilestoningDatePropagationContext(new MilestoningDatePropagationContext(null, null));
        }
    }

    public static void updateMilestoningPropagationContextWhileReprocessingFunctionExpression(ProcessingContext processingContext)
    {
        if (processingContext.milestoningDatePropagationContext.size() != 0)
        {
            processingContext.popMilestoningDatePropagationContext();
            if (processingContext.milestoningDatePropagationContext.size() != 0)
            {
                processingContext.pushMilestoningDatePropagationContext(processingContext.peekMilestoningDatePropagationContext());
            }
            else
            {
                processingContext.pushMilestoningDatePropagationContext(new MilestoningDatePropagationContext(null, null));
            }
        }
    }

    public static void updateMilestoningContext(AbstractProperty<?> property, ListIterable<? extends ValueSpecification> parameterValues, ProcessingContext processingContext, CompileContext context)
    {
        if (isGeneratedQualifiedPropertyWithDatePropagationSupported(property, context))
        {
            if (processingContext.milestoningDatePropagationContext.size() != 0)
            {
                processingContext.popMilestoningDatePropagationContext();
            }
            updateMilestoningPropagationContext(property, parameterValues, processingContext);
        }
        else if (property instanceof QualifiedProperty || property.getName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX) || property.getName().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX))
        {
            if (processingContext.milestoningDatePropagationContext.size() != 0)
            {
                processingContext.popMilestoningDatePropagationContext();
            }
            processingContext.pushMilestoningDatePropagationContext(new MilestoningDatePropagationContext(null, null));
        }
    }

    public static void isValidSource(AppliedFunction appliedFunction, ProcessingContext processingContext)
    {
        if (!MILESTONING_DATE_SOURCE_TYPES.contains(appliedFunction.function) && !appliedFunction.parameters.isEmpty() && appliedFunction.parameters.get(0) instanceof AppliedFunction && ((AppliedFunction) appliedFunction.parameters.get(0)).function.equals("getAll"))
        {
            processingContext.isDatePropagationSupported = false;
        } 
            else
        {
            processingContext.isDatePropagationSupported = true;
        }
    }

    public static void updateMilestoningContextFromValidSources(ValueSpecification result, ProcessingContext processingContext)
    {
        if (result instanceof SimpleFunctionExpression && isGetAllFunctionWithMilestoningContext((SimpleFunctionExpression) result) && processingContext.isDatePropagationSupported)
        {
            processingContext.milestoningDatePropagationContext = new Stack<>();
            setMilestoningPropagationContext((SimpleFunctionExpression) result, processingContext);
        }
    }


    public static boolean isGeneratedQualifiedPropertyWithDatePropagationSupported(AbstractProperty<?> property, CompileContext context)
    {
        return Milestoning.isGeneratedMilestoningQualifiedProperty(property) && !Milestoning.isAllVersionsInRangeGeneratedMilestoningQualifiedProperty(property);
    }



    public static boolean isGeneratedMilestonedQualifiedPropertyWithMissingDates(AbstractProperty<?> property, CompileContext context, Integer parametersCount)
    {
        if (isGeneratedQualifiedPropertyWithDatePropagationSupported(property, context))
        {
            return parametersCount != Milestoning.getCountOfParametersSatisfyingMilestoningDateRequirments((QualifiedProperty) property);
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

    private static Type getMilestonedPropertyOwningType(AbstractProperty<?> property)
    {
        if (property._owner() instanceof Class)
        {
            return (Class) property._owner();
        }
        else if (property._owner() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association)
        {
            MutableList<? extends Property<? extends Object, ? extends Object>> milestonedProperty = ((Association) property._owner())._originalMilestonedProperties().toList().select(prop -> prop._name() != property._name());
            if (milestonedProperty.isEmpty())
            {
                throw new EngineException("The property '" + property._name() + "' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]", EngineErrorType.COMPILATION);
            }
            return milestonedProperty.getFirst()._genericType()._rawType();
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

    public static MutableList<? extends ValueSpecification> getMilestoningDatesFromMilestoningContext(AbstractProperty<?> func, MutableList<? extends ValueSpecification> parametersValues, ProcessingContext processingContext)
    {
        Pair<MilestoningStereotype, MilestoningStereotype> sourceTargetMilestoningStereotypes = getSourceTargetMilestoningStereotypes(func);
        MilestoningStereotype sourceTypeMilestoning = sourceTargetMilestoningStereotypes.getOne();
        MilestoningStereotype targetTypeMilestoning = sourceTargetMilestoningStereotypes.getTwo();
        ValueSpecification[] milestoningDateParameters = new ValueSpecification[targetTypeMilestoning.getTemporalDatePropertyNames().size()];

        MilestoningDatePropagationContext propagationContext = processingContext.milestoningDatePropagationContext.size() == 0 ? new MilestoningDatePropagationContext(null, null) : processingContext.peekMilestoningDatePropagationContext();

        if (isBiTemporal(targetTypeMilestoning))
        {
            if (isBiTemporal(sourceTypeMilestoning) && oneDateParamSupplied(parametersValues))
            {
                milestoningDateParameters[0] = propagationContext.getProcessingDate();
                milestoningDateParameters[1] = parametersValues.get(1);
            }
            else if (isSingleDateTemporal(sourceTypeMilestoning) && oneDateParamSupplied(parametersValues))
            {
                int propagatedDateIndex = Objects.requireNonNull(sourceTypeMilestoning).positionInTemporalParameterValues();
                ValueSpecification propagatedDate;
                int otherPropagatedDateIndex;
                if (isProcessingTemporal(sourceTypeMilestoning))
                {
                    propagatedDate = propagationContext.getProcessingDate();
                    otherPropagatedDateIndex = 1;
                }
                else
                {
                    propagatedDate = propagationContext.getBusinessDate();
                    otherPropagatedDateIndex = 0;
                }
                setMilestoningDateParameters(milestoningDateParameters, propagatedDateIndex, propagatedDate);
                setMilestoningDateParameters(milestoningDateParameters, otherPropagatedDateIndex, parametersValues.get(1));
            }
            if (isBiTemporal(sourceTypeMilestoning) && noDateParamSupplied(parametersValues))
            {
                milestoningDateParameters[0] = propagationContext.getProcessingDate();
                milestoningDateParameters[1] = propagationContext.getBusinessDate();
            }
        }
        else if (isSingleDateTemporal(targetTypeMilestoning) && noDateParamSupplied(parametersValues))
        {
            ValueSpecification propagatedDate;
            if (isProcessingTemporal(targetTypeMilestoning))
            {
                propagatedDate = propagationContext.getProcessingDate();
            }
            else
            {
                propagatedDate = propagationContext.getBusinessDate();
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
        if (!ArrayIterate.isEmpty(milestoningDateParameters) && !ArrayIterate.contains(milestoningDateParameters, null))
        {
            return LazyIterate.concatenate(FastList.<ValueSpecification>newListWith(parametersValues.get(0)), FastList.newListWith(milestoningDateParameters)).toList();
        }
        else
        {
            return parametersValues;
        }
    }

    public static void updateFunctionExpressionWithMilestoningDateParams(FunctionExpression functionExpression, AbstractProperty<?> propertyFunc, SourceInformation sourceInformation, ProcessingContext processingContext)
    {
        functionExpression._originalMilestonedPropertyParametersValues(functionExpression._parametersValues());

        MutableList<ValueSpecification> newParametersValues  = (MutableList<ValueSpecification>) getMilestoningDatesFromMilestoningContext(propertyFunc, functionExpression._parametersValues().toList(), processingContext);
        functionExpression._parametersValues(newParametersValues);

        functionExpression._func(propertyFunc);
    }

}
