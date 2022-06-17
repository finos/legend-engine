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
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

public enum PlanPlatform
{
    JAVA(new JavaPlatformBinder());

    private final PlatformBinder platformBinder;

    PlanPlatform(PlatformBinder platformBinder)
    {
        this.platformBinder = platformBinder;
    }

    public Root_meta_pure_executionPlan_ExecutionPlan bindPlan(Root_meta_pure_executionPlan_ExecutionPlan plan, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return bindPlan(plan, null, pureModel, extensions);
    }

    public Root_meta_pure_executionPlan_ExecutionPlan bindPlan(Root_meta_pure_executionPlan_ExecutionPlan plan, String planId, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return this.platformBinder.bindPlanToPlatform(plan, planId, pureModel, extensions);
    }

    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getPureEnumValue(PureModel pureModel)
    {
        return pureModel.getEnumValue("meta::pure::executionPlan::Platform", name());
    }
}
