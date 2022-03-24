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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public class MilestoningDatePropagationHelper {

    private static final String ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";
    private static final String ALL_VERSIONS_PROPERTY_NAME_SUFFIX = "AllVersions";

    public static boolean checkGetAllFunctionWithMilestoningContext(SimpleFunctionExpression func)
    {
       return "getAll".equals(func._functionName()) && ("getAll_Class_1__Date_1__T_MANY_".equals(func._func().getName()) || "getAll_Class_1__Date_1__Date_1__T_MANY_".equals(func._func().getName()));
    }

    public static void setMilestoningPropagationContext(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
        Assert.assertTrue(checkGetAllFunctionWithMilestoningContext(func), () -> "MilestoneDatePropagationContext can only be set from getAll() function");
        ValueSpecification parameterValue = func._parametersValues().getFirst();
        MilestoningStereotype milestoningStereotype = Milestoning.temporalStereotypes(parameterValue._genericType()._typeArguments().toList().getFirst()._rawType()._stereotypes()).get(0);
        ListIterable<? extends ValueSpecification> temporalParameterValues = func._parametersValues().toList();
        setMilestoningDates(temporalParameterValues, milestoningStereotype, processingContext);
    }

    public static void setMilestoningDates(ListIterable<? extends ValueSpecification> temporalParameterValues, MilestoningStereotype milestoningStereotype, ProcessingContext processingContext)
    {
        if (Milestoning.isBusinessTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setBusinessDate(temporalParameterValues.get(1));
        }
        else if (Milestoning.isProcessingTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(temporalParameterValues.get(1));
        }
        else if (Milestoning.isBiTemporal(milestoningStereotype))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(temporalParameterValues.get(1));
            processingContext.milestoningDatePropagationContext.setBusinessDate(temporalParameterValues.get(2));
        }
    }

    public static boolean checkForFilter(SimpleFunctionExpression func)
    {
        return "filter".equals(func._functionName());
    }

    public static void updateMilestoningPropagationContext(SimpleFunctionExpression property, ProcessingContext processingContext)
    {
        if(!property._func()._functionName().endsWith(ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX) && !property._func()._functionName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX))
        {
            Class target = (Class) property._genericType()._rawType();
            MilestoningStereotype targetTypeMilestoningStereotype = Milestoning.temporalStereotypes(target._stereotypes()).get(0);
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
        if(parameterValue instanceof SimpleFunctionExpression && checkGetAllFunctionWithMilestoningContext((SimpleFunctionExpression) parameterValue))
        {
            setMilestoningPropagationContext((SimpleFunctionExpression) parameterValue, processingContext);
        }
    }

    public static void updateMilestoningPropagationContextForAutoMap(AbstractProperty<?> property, CompileContext context, int parametersCount, ValueSpecification processedParameter, ProcessingContext processingContext)
    {
        if (Milestoning.isGeneratedMilestonedQualifiedPropertyWithMissingDates(property, context, parametersCount) && processedParameter instanceof SimpleFunctionExpression && ((SimpleFunctionExpression) processedParameter)._func() instanceof AbstractProperty && Milestoning.isGeneratedMilestoningProperty((AbstractProperty<?>) ((SimpleFunctionExpression) processedParameter)._func(), context))
        {
            MilestoningDatePropagationHelper.updateMilestoningPropagationContext((SimpleFunctionExpression) processedParameter, processingContext);
        }
    }

    public static void updateMilestoningContext(AbstractProperty<?> property, ProcessingContext processingContext, CompileContext context, SimpleFunctionExpression func)
    {
        if (Milestoning.isGeneratedQualifiedPropertyWithDatePropagationSupported(property, context))
        {
            MilestoningDatePropagationHelper.updateMilestoningPropagationContext(func, processingContext);
        }
        else if(property instanceof QualifiedProperty || property.getName().endsWith(Milestoning.ALL_VERSIONS_PROPERTY_NAME_SUFFIX) || property.getName().endsWith(Milestoning.ALL_VERSIONS_IN_RANGE_PROPERTY_NAME_SUFFIX))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
        }
    }

    public static void checkForValidSource(AppliedFunction appliedFunction, boolean isDatePropagationSupported, ProcessingContext processingContext)
    {
        if (!MILESTONING_DATE_SOURCE_TYPES.contains(appliedFunction.function) && !appliedFunction.parameters.isEmpty() && appliedFunction.parameters.get(0) instanceof AppliedFunction && ((AppliedFunction) appliedFunction.parameters.get(0)).function.equals("getAll"))
        {
            processingContext.milestoningDatePropagationContext.isDatePropagationSupported = isDatePropagationSupported;
        }
    }

    public static void updateMilestoningContextFromValidSources(ValueSpecification result, ProcessingContext processingContext)
    {
        if (result instanceof SimpleFunctionExpression && "map".equals(((SimpleFunctionExpression) result)._functionName()))
        {
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
        }
        if (result instanceof SimpleFunctionExpression && MilestoningDatePropagationHelper.checkGetAllFunctionWithMilestoningContext((SimpleFunctionExpression) result) && processingContext.milestoningDatePropagationContext.isDatePropagationSupported) {
            MilestoningDatePropagationHelper.setMilestoningPropagationContext((SimpleFunctionExpression) result, processingContext);
        }
        if (result instanceof SimpleFunctionExpression && MilestoningDatePropagationHelper.checkForFilter((SimpleFunctionExpression) result))
        {
            MilestoningDatePropagationHelper.updateMilestoningPropagationContextForFilter((SimpleFunctionExpression) result, processingContext);
        }
    }

    private static final ImmutableList<String> MILESTONING_DATE_SOURCE_TYPES = Lists.immutable.of("getAll", "exists", "project", "filter", "subType", "map");
}
