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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_ProjectDirectory;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_JavaClass;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

abstract class PlatformBinder
{
    abstract Root_meta_pure_executionPlan_ExecutionPlan bindPlanToPlatform(Root_meta_pure_executionPlan_ExecutionPlan plan, String planId, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions);

    // [B] leaf: node code only + this service's TypeInfoSet
    Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos bindPlansToNodeAst(List<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, List<String> planIds, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [B] root: one global union of TypeInfoSets
    Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet unionTypeInfos(List<? extends Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet> sets, PureModel pureModel)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [B] root: shared type layer generated once
    Root_meta_external_language_java_metamodel_project_Project generateSharedTypes(Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet union, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [B] root: dependencies recovered from the assembled project, resolved once
    Root_meta_external_language_java_metamodel_project_Project resolveProjectDependencies(Root_meta_external_language_java_metamodel_project_Project project, PureModel pureModel)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [D] serialize one package; independent of every other package
    RichIterable<? extends Root_meta_pure_executionPlan_JavaClass> serializeDirectory(Root_meta_external_language_java_metamodel_project_ProjectDirectory dir, PureModel pureModel)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [D] attach classes serialized elsewhere, in caller-supplied order
    RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> attachImplementationFromClasses(RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, List<? extends Root_meta_pure_executionPlan_JavaClass> classes, PureModel pureModel)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    // [A] merge two Java projects
    Root_meta_external_language_java_metamodel_project_Project mergeAsts(Root_meta_external_language_java_metamodel_project_Project a, Root_meta_external_language_java_metamodel_project_Project b, PureModel pureModel)
    {
        throw new UnsupportedOperationException("AST merge is only supported on the JAVA platform");
    }

    protected abstract PlanPlatform getPlatform();
}
