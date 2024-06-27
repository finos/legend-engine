// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.core_dataquality_generation_dataquality;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.util.Objects;

public class DataQualityLambdaGenerator
{
    public static final int DEFAULT_QUERY_LIMIT = 100;

    public static LambdaFunction generateLambda(PureModel pureModel, String qualifiedPath)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(qualifiedPath);
        return generateLambda(pureModel, packageableElement);
    }

    public static LambdaFunction generateLambda(PureModel pureModel, PackageableElement packageableElement)
    {
        return core_dataquality_generation_dataquality.Root_meta_external_dataquality_executeDataQualityValidation_DataQuality_1__Integer_MANY__LambdaFunction_1_((Root_meta_external_dataquality_DataQuality)packageableElement, Lists.immutable.empty(), pureModel.getExecutionSupport());
    }

    public static LambdaFunction generateLambdaForTrial(PureModel pureModel, String qualifiedPath, Integer queryLimit)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(qualifiedPath);
        int trialQueryLimit = DEFAULT_QUERY_LIMIT;
        if (Objects.nonNull(queryLimit))
        {
            trialQueryLimit = queryLimit;
        }
        return core_dataquality_generation_dataquality.Root_meta_external_dataquality_executeDataQualityValidation_DataQuality_1__Integer_MANY__LambdaFunction_1_((Root_meta_external_dataquality_DataQuality)packageableElement, Lists.immutable.of((long)trialQueryLimit), pureModel.getExecutionSupport());
    }
}
