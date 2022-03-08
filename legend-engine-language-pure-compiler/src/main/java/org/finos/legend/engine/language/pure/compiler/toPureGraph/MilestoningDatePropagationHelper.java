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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.List;

public class MilestoningDatePropagationHelper {

    private static final String RANGE_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";
    private static final String ALL_VERSIONS_PROPERTY_NAME_SUFFIX = "AllVersionsInRange";

    public static boolean checkGetAllFunctionWithMilestoningContext(SimpleFunctionExpression func)
    {
       return "getAll".equals(func._functionName()) && ("getAll_Class_1__Date_1__T_MANY_".equals(func._func().getName()) || "getAll_Class_1__Date_1__Date_1__T_MANY_".equals(func._func().getName()));
    }

    public static void setMilestoningPropagationContext(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
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

    public static boolean checkForMapOrExists(SimpleFunctionExpression func)
    {
        return ("map".equals(func._functionName()) || "exists".equals(func._functionName()));
    }

    public static void updateMilestoningPropagationContext(SimpleFunctionExpression func, ProcessingContext processingContext)
    {
        if(!func.getName().endsWith(RANGE_PROPERTY_NAME_SUFFIX) && !func.getName().endsWith(ALL_VERSIONS_PROPERTY_NAME_SUFFIX))
        {
            Class target = (Class) func._genericType()._rawType();
            MilestoningStereotype targetTypeMilestoningStereotype = Milestoning.temporalStereotypes(target._stereotypes()).get(0);
            setMilestoningDates(func._parametersValues().toList(), targetTypeMilestoningStereotype, processingContext);
        }
        else
        {
            processingContext.milestoningDatePropagationContext.setBusinessDate(null);
            processingContext.milestoningDatePropagationContext.setProcessingDate(null);
        }
    }

    public static boolean checkForAutoMapOrSubType(ValueSpecification parameter)
    {
        return (parameter instanceof VariableExpression && ((VariableExpression) parameter)._name() == "v_automap") || (parameter instanceof SimpleFunctionExpression && ((SimpleFunctionExpression) parameter)._functionName() == "subType");
    }
}
