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

package org.finos.legend.engine.plan.platform;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_PlatformCodeGenerationConfig;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_PlatformCodeGenerationConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_generation;

abstract class PlatformBinder
{
    Root_meta_pure_executionPlan_ExecutionPlan bindPlanToPlatform(Root_meta_pure_executionPlan_ExecutionPlan plan, String planId, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_generatePlatformCode_ExecutionPlan_1__PlatformCodeGenerationConfig_1__Extension_MANY__ExecutionPlan_1_(plan, getPlatformCodeGenerationConfig(planId, pureModel), extensions, pureModel.getExecutionSupport());
    }

    protected abstract PlanPlatform getPlatform();

    protected Root_meta_pure_executionPlan_PlatformCodeGenerationConfig getPlatformCodeGenerationConfig(String planId, PureModel pureModel)
    {
        return new Root_meta_pure_executionPlan_PlatformCodeGenerationConfig_Impl("", null, pureModel.getClass("meta::pure::executionPlan::PlatformCodeGenerationConfig"))
                ._platform(getPlatform().getPureEnumValue(pureModel))
                ._planId(planId);
    }
}
