// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.generation.dataquality;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.pure.generated.core_dataquality_generation_samplevalues;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

public class DataQualitySampleValuesLambdaGenerator
{
    /**
     * Resolve from an inline protocol LambdaFunction.
     */
    public static LambdaFunction<?> generateLambda(
            PureModel pureModel,
            org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction query,
            Integer maxNumberOfSampleValues)
    {
        LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(query, pureModel.getContext());
        return invokePure(pureModel, pureLambda, maxNumberOfSampleValues);
    }

    /**
     * Resolve from a path to a ConcreteFunctionDefinition that returns a Relation.
     * <p>
     * The ConcreteFunctionDefinition's expression sequence is copied into a new
     * LambdaFunction so the Pure-side {@code getSampleValuesLambda} receives a
     * proper LambdaFunction instance (not a ConcreteFunctionDefinition).
     */
    public static LambdaFunction<?> generateLambda(
            PureModel pureModel,
            String functionPath,
            Integer maxNumberOfSampleValues)
    {
        PackageableElement element = pureModel.getPackageableElement(functionPath);
        if (!(element instanceof ConcreteFunctionDefinition))
        {
            throw new EngineException(
                    "The element at path '" + functionPath + "' is not a ConcreteFunctionDefinition",
                    ExceptionCategory.USER_EXECUTION_ERROR);
        }
        ConcreteFunctionDefinition<?> funcDef = (ConcreteFunctionDefinition<?>) element;

        // Build a LambdaFunction that carries the same expression sequence and
        // generic type information as the ConcreteFunctionDefinition.
        LambdaFunction<?> pureLambda = core_dataquality_generation_samplevalues
                .Root_meta_external_dataquality_samplevalues_wrapFunctionDefinitionAsLambda_FunctionDefinition_1__LambdaFunction_1_(funcDef, pureModel.getExecutionSupport());

        return invokePure(pureModel, pureLambda, maxNumberOfSampleValues);
    }

    private static LambdaFunction<?> invokePure(
            PureModel pureModel,
            LambdaFunction<?> pureLambda,
            Integer maxNumberOfSampleValues)
    {
        if (maxNumberOfSampleValues != null)
        {
            return core_dataquality_generation_samplevalues
                            .Root_meta_external_dataquality_samplevalues_getSampleValuesLambda_LambdaFunction_1__Integer_1__Boolean_1__LambdaFunction_1_(
                                    pureLambda, (long) maxNumberOfSampleValues, true, pureModel.getExecutionSupport());
        }

        // maxNumberOfSampleValues omitted — sensible default from within the PURE layer applies
        return core_dataquality_generation_samplevalues
                        .Root_meta_external_dataquality_samplevalues_getSampleValuesLambda_LambdaFunction_1__Boolean_1__LambdaFunction_1_(
                                pureLambda, true, pureModel.getExecutionSupport());
    }
}
