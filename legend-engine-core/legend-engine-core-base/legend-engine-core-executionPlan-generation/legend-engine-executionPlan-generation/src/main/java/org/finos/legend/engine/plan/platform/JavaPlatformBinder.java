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
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_generation;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_legendJavaPlatformBinding;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_PlatformBindingConfig;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_LegendJavaPlatformBindingConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

class JavaPlatformBinder extends PlatformBinder
{
    @Override
    protected PlanPlatform getPlatform()
    {
        return PlanPlatform.JAVA;
    }

    @Override
    Root_meta_pure_executionPlan_ExecutionPlan bindPlanToPlatform(Root_meta_pure_executionPlan_ExecutionPlan plan, String planId, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        String platformId = core_java_platform_binding_legendJavaPlatformBinding_legendJavaPlatformBinding.Root_meta_pure_executionPlan_platformBinding_legendJava_legendJavaPlatformBindingId__String_1_(pureModel.getExecutionSupport());
        return core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_generatePlatformCode_ExecutionPlan_1__String_1__PlatformBindingConfig_1__Extension_MANY__ExecutionPlan_1_(plan, platformId, getLegendJavaPlatformBindingConfig(planId), extensions, pureModel.getExecutionSupport());
    }

    private Root_meta_pure_executionPlan_platformBinding_PlatformBindingConfig getLegendJavaPlatformBindingConfig(String planId)
    {
        return new Root_meta_pure_executionPlan_platformBinding_legendJava_LegendJavaPlatformBindingConfig_Impl("")
                ._planId(planId);
    }
}
