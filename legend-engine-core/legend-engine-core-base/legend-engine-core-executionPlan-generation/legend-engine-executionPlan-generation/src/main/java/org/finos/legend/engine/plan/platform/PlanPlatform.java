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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_ProjectDirectory;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_JavaClass;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet;
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

    // [B] leaf: node code only, plus this service's TypeInfoSet
    public Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos bindPlansToNodeAst(List<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, List<String> planIds, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return this.platformBinder.bindPlansToNodeAst(plans, planIds, pureModel, extensions);
    }

    // [B] root: one global union of all services' TypeInfoSets
    public Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet unionTypeInfos(List<? extends Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet> sets, PureModel pureModel)
    {
        return this.platformBinder.unionTypeInfos(sets, pureModel);
    }

    // [B] root: shared type layer generated once
    public Root_meta_external_language_java_metamodel_project_Project generateSharedTypes(Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet union, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return this.platformBinder.generateSharedTypes(union, pureModel, extensions);
    }

    // [B] root: dependencies recovered from the assembled project and resolved once
    public Root_meta_external_language_java_metamodel_project_Project resolveProjectDependencies(Root_meta_external_language_java_metamodel_project_Project project, PureModel pureModel)
    {
        return this.platformBinder.resolveProjectDependencies(project, pureModel);
    }

    // [A] merge two Java projects
    public Root_meta_external_language_java_metamodel_project_Project mergeAsts(Root_meta_external_language_java_metamodel_project_Project a, Root_meta_external_language_java_metamodel_project_Project b, PureModel pureModel)
    {
        return this.platformBinder.mergeAsts(a, b, pureModel);
    }

    // [D] root: serialize package by package, concurrently; reassembled in directory order
    public RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> attachMergedImplementationParallel(RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> plans, Root_meta_external_language_java_metamodel_project_Project mergedProject, PureModel pureModel, ForkJoinPool executor)
    {
        if (mergedProject == null)
        {
            return this.platformBinder.attachImplementationFromClasses(plans, Lists.mutable.empty(), pureModel);
        }

        // [D] matches Pure's allDirectories: root itself excluded
        MutableList<Root_meta_external_language_java_metamodel_project_ProjectDirectory> dirs = Lists.mutable.empty();
        if (mergedProject._root() != null)
        {
            mergedProject._root()._subdirectories().forEach(sub -> collectDirectories(sub, dirs));
        }

        // [D] invokeAll returns futures in submission order -> output stays byte-stable
        MutableList<Callable<MutableList<Root_meta_pure_executionPlan_JavaClass>>> tasks =
                dirs.collect(d -> (Callable<MutableList<Root_meta_pure_executionPlan_JavaClass>>) () -> Lists.mutable.withAll(this.platformBinder.serializeDirectory(d, pureModel)));

        ForkJoinPool pool = (executor == null) ? ForkJoinPool.commonPool() : executor;
        MutableList<Root_meta_pure_executionPlan_JavaClass> ordered = Lists.mutable.empty();
        try
        {
            for (Future<MutableList<Root_meta_pure_executionPlan_JavaClass>> f : pool.invokeAll(tasks))
            {
                ordered.addAll(f.get());
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel serialization interrupted", e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException("Parallel serialization failed", e.getCause());
        }

        return this.platformBinder.attachImplementationFromClasses(plans, ordered, pureModel);
    }

    private static void collectDirectories(Root_meta_external_language_java_metamodel_project_ProjectDirectory dir, MutableList<Root_meta_external_language_java_metamodel_project_ProjectDirectory> out)
    {
        if (dir == null)
        {
            return;
        }
        out.add(dir);
        dir._subdirectories().forEach(sub -> collectDirectories(sub, out));
    }
}
