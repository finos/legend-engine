// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_ParameterValue;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionValidator
{

    public void validate(CompileContext compileContext, PureModelContextData pureModelContextData)
    {
        ListIterate.selectInstancesOf(pureModelContextData.getElements(), Function.class)
                .forEach(_func -> validateFunction(_func, compileContext));
    }

    public void validateFunction(Function func, CompileContext compileContext)
    {
        if (func.tests != null && !func.tests.isEmpty())
        {
            PureModel pureModel = compileContext.pureModel;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = pureModel.getConcreteFunctionDefinition(func);
            for (org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test test: targetFunc._tests())
            {
                Root_meta_legend_function_metamodel_FunctionTestSuite metamodelSuite = (Root_meta_legend_function_metamodel_FunctionTestSuite) test;
                FunctionTestSuite protocolSuite =  ListIterate.detect(func.tests, t -> t.id.equals(metamodelSuite._id()));
                for (Root_meta_pure_test_AtomicTest atomicTest: metamodelSuite._tests())
                {
                    Root_meta_legend_function_metamodel_FunctionTest functionTest = (Root_meta_legend_function_metamodel_FunctionTest) atomicTest;
                    RichIterable<? extends VariableExpression> parameters = ((FunctionType)targetFunc._classifierGenericType()._typeArguments().getOnly()._rawType())._parameters();
                    FunctionTest protocolTest = (FunctionTest) ListIterate.detect(protocolSuite.tests, t -> t.id.equals(functionTest._id()));
                    validateFunctionTestParameterValues(compileContext,(List<Root_meta_legend_function_metamodel_ParameterValue>)  functionTest._parameters().toList(), parameters, protocolTest.sourceInformation);
                }
            }
        }
    }

    public static void validateFunctionTestParameterValues(CompileContext context, List<Root_meta_legend_function_metamodel_ParameterValue> parameterValues, RichIterable<? extends VariableExpression> parameters, SourceInformation sourceInformation)
    {
        Set<String> processedParams = parameterValues.stream().map(e -> e._name()).collect(Collectors.toSet());
        for (VariableExpression param : parameters)
        {
            Optional<Root_meta_legend_function_metamodel_ParameterValue> parameterValue = ListIterate.detectOptional(parameterValues, p -> p._name().equals(param._name()));

            if (parameterValue.isPresent())
            {
                InstanceValue paramValue = (InstanceValue) parameterValue.get()._value().getOnly();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity paramMultiplicity = param._multiplicity();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity paramValueMultiplicity = paramValue._multiplicity();
                if (!"Nil".equals(paramValue._genericType()._rawType()))
                {
                    HelperModelBuilder.checkCompatibility(context, paramValue._genericType(), paramValueMultiplicity, param._genericType(), paramMultiplicity, "Parameter value type does not match with parameter type for parameter: '" + param._name() + "'", sourceInformation);
                }
            }
            else
            {
                if (param._multiplicity()._lowerBound() != null && param._multiplicity()._lowerBound()._value() != null && param._multiplicity()._lowerBound()._value() != 0)
                {
                    throw new EngineException("Parameter value required for parameter: '" + param._name() + "'", sourceInformation, EngineErrorType.COMPILATION);
                }
            }
            processedParams.remove(param._name());
        }
        if (!processedParams.isEmpty())
        {
            throw new EngineException("Parameter values not found in function parameter: " + String.join(",", processedParams), sourceInformation, EngineErrorType.COMPILATION);
        }
    }

}
