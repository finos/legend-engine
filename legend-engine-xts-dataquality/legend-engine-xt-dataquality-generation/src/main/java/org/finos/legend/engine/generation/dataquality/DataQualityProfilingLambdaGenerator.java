// Copyright 2025 Goldman Sachs
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
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidation;
import org.finos.legend.pure.generated.core_dataquality_generation_dataprofile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class DataQualityProfilingLambdaGenerator
{

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambda(PureModel pureModel, String qualifiedPath)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(qualifiedPath);
        return generateLambda(pureModel, packageableElement);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambda(PureModel pureModel, PackageableElement packageableElement)
    {
        if (packageableElement instanceof Root_meta_external_dataquality_DataQualityRelationValidation)
        {
            return generateDataProfileLambda(pureModel, (Root_meta_external_dataquality_DataQualityRelationValidation) packageableElement);
        }
        throw new EngineException("Unsupported Dataquality element! " + packageableElement.getClass().getSimpleName(), ExceptionCategory.USER_EXECUTION_ERROR);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateDataProfileLambda(PureModel pureModel, Root_meta_external_dataquality_DataQualityRelationValidation packageableElement)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) core_dataquality_generation_dataprofile.Root_meta_external_dataquality_dataprofile_getProfilingLambda_DataQualityRelationValidation_1__Boolean_1__LambdaFunction_1_(packageableElement, true,  pureModel.getExecutionSupport());
    }
}
