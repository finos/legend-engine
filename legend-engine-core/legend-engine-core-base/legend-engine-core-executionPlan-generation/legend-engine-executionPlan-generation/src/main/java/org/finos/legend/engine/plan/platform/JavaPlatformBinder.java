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

import java.util.List;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_generation;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_legendJavaPlatformBinding;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_main;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_ProjectDirectory;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_JavaClass;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_PlatformBindingConfig;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_LegendJavaPlatformBindingConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet;
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

    // [B] leaf: node code only + this service's TypeInfoSet
    @Override
    Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos bindPlansToNodeAst(List<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, List<String> planIds, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        MutableList<Root_meta_pure_executionPlan_platformBinding_PlatformBindingConfig> configs = ListIterate.collect(planIds, this::getLegendJavaPlatformBindingConfig);
        MutableList<? extends Root_meta_pure_executionPlan_ExecutionPlan> planList = Lists.mutable.withAll(plans);
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_bindPlansToNodeAst_ExecutionPlan_MANY__PlatformBindingConfig_MANY__Extension_MANY__NodesAndTypeInfos_1_(planList, configs, extensions, pureModel.getExecutionSupport());
    }

    // [B] root: one global union of TypeInfoSets
    @Override
    Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet unionTypeInfos(List<? extends Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet> sets, PureModel pureModel)
    {
        MutableList<? extends Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet> setList = Lists.mutable.withAll(sets);
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_mergeTypeInfoSets_TypeInfoSet_MANY__TypeInfoSet_1_(setList, pureModel.getExecutionSupport());
    }

    // [B] root: shared type layer generated once
    @Override
    Root_meta_external_language_java_metamodel_project_Project generateSharedTypes(Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet union, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_generateSharedTypes_TypeInfoSet_1__Extension_MANY__Project_1_(union, extensions, pureModel.getExecutionSupport());
    }

    // [B] root: dependencies recovered from the assembled project, resolved once
    @Override
    Root_meta_external_language_java_metamodel_project_Project resolveProjectDependencies(Root_meta_external_language_java_metamodel_project_Project project, PureModel pureModel)
    {
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_resolveProjectDependencies_Project_1__Project_$0_1$_(project, pureModel.getExecutionSupport());
    }

    // [D] serialize one package
    @Override
    RichIterable<? extends Root_meta_pure_executionPlan_JavaClass> serializeDirectory(Root_meta_external_language_java_metamodel_project_ProjectDirectory dir, PureModel pureModel)
    {
        return core_java_platform_binding_legendJavaPlatformBinding_main.Root_meta_pure_executionPlan_platformBinding_legendJava_serializeDirectoryClasses_ProjectDirectory_1__JavaClass_MANY_(dir, pureModel.getExecutionSupport());
    }

    // [D] attach classes serialized elsewhere, in caller order
    @Override
    RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> attachImplementationFromClasses(RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, List<? extends Root_meta_pure_executionPlan_JavaClass> classes, PureModel pureModel)
    {
        MutableList<? extends Root_meta_pure_executionPlan_JavaClass> classList = Lists.mutable.withAll(classes);
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_attachImplementationFromClasses_ExecutionPlan_MANY__JavaClass_MANY__ExecutionPlan_MANY_(plans, classList, pureModel.getExecutionSupport());
    }

    // [A] merge two Java projects
    @Override
    Root_meta_external_language_java_metamodel_project_Project mergeAsts(Root_meta_external_language_java_metamodel_project_Project a, Root_meta_external_language_java_metamodel_project_Project b, PureModel pureModel)
    {
        return core_java_platform_binding_legendJavaPlatformBinding_bindPlanToLegendJavaPlatform.Root_meta_pure_executionPlan_platformBinding_legendJava_mergeProjectPair_Project_1__Project_1__Project_1_(a, b, pureModel.getExecutionSupport());
    }

    private Root_meta_pure_executionPlan_platformBinding_PlatformBindingConfig getLegendJavaPlatformBindingConfig(String planId)
    {
        return new Root_meta_pure_executionPlan_platformBinding_legendJava_LegendJavaPlatformBindingConfig_Impl("")
                ._planId(planId);
    }
}
