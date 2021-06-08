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

package org.finos.legend.engine.plan.generation.transformers;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PlanTransformers implements PlanTransformer
{
    @Override
    public boolean supports(String version)
    {
        return PureClientVersions.versionAGreaterThanOrEqualsVersionB("v1_18_0", version);
    }

    @Override
    public Object transformToVersionedModel(Root_meta_pure_executionPlan_ExecutionPlan purePlan, String version, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, ExecutionSupport executionSupport)
    {
        try
        {
            Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_executionPlan");
            Method method = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_executionPlan_transformPlan_ExecutionPlan_1__RouterExtension_MANY__ExecutionPlan_1_", Root_meta_pure_executionPlan_ExecutionPlan.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            return method.invoke(null, purePlan, extensions, executionSupport);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
