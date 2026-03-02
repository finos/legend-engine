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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_datarecon_DataQualityReconInput;
import org.finos.legend.pure.generated.core_dataquality_generation_datarecon;

public class DataQualityReconLambdaGenerator
{

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambda(PureModel pureModel, Root_meta_external_dataquality_datarecon_DataQualityReconInput reconInput)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) core_dataquality_generation_datarecon.Root_meta_external_dataquality_datarecon_getDataReconLambda_DataQualityReconInput_1__LambdaFunction_1_(reconInput, pureModel.getExecutionSupport());
    }
}
