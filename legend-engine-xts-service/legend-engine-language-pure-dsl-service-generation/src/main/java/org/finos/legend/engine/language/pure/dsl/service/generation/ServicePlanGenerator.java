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

package org.finos.legend.engine.language.pure.dsl.service.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.generation.extension.ServiceExecutionExtension;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.plan.platform.java.JavaSourceHelper;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOption;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.core_service_service_helperFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

public class ServicePlanGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicePlanGenerator.class);
    private static final List<ServiceExecutionExtension> SERVICE_EXECUTION_EXTENSIONS = Lists.mutable.withAll(ServiceLoader.load(ServiceExecutionExtension.class));

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateServiceExecutionPlan(service, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateServiceExecutionPlan(service, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        return generateExecutionPlan(service.getPath(), service.execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, pool);
    }

    // [A] routed-but-unbound plans + what is needed to rebuild single vs composite protocol plans
    private static final class RoutedService
    {
        final MutableList<Root_meta_pure_executionPlan_ExecutionPlan> purePlans = Lists.mutable.empty();
        final MutableList<String> planIds = Lists.mutable.empty();
        final MutableList<String> keys = Lists.mutable.empty();
        String execKey = null;
        boolean multi = false;
    }

    // [B] leaf result: protocol plan, node-only Java, this service's TypeInfoSet, representative Pure plan
    public static final class ServicePlanAndAst
    {
        public final ExecutionPlan plan;
        public final Root_meta_external_language_java_metamodel_project_Project project;
        public final Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet typeInfos;
        public final Root_meta_pure_executionPlan_ExecutionPlan representativePurePlan;

        ServicePlanAndAst(ExecutionPlan plan, Root_meta_external_language_java_metamodel_project_Project project, Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet typeInfos, Root_meta_pure_executionPlan_ExecutionPlan representativePurePlan)
        {
            this.plan = plan;
            this.project = project;
            this.typeInfos = typeInfos;
            this.representativePurePlan = representativePurePlan;
        }
    }

    // [B] leaf entry point used by the SDLC build: binds node code only; shared layer is built at the root
    public static ServicePlanAndAst generateServiceExecutionPlanAndAst(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        long tRoute0 = System.nanoTime();
        RoutedService routed = routeService(service.execution, planId, context, pureModel, extensions);
        long tRoute = System.nanoTime() - tRoute0;

        long tNode0 = System.nanoTime();
        Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos nt = platform.bindPlansToNodeAst(routed.purePlans, routed.planIds, pureModel, extensions);
        long tNode = System.nanoTime() - tNode0;

        MutableList<Root_meta_pure_executionPlan_ExecutionPlan> bound = Lists.mutable.withAll(nt._plans());

        long tSer0 = System.nanoTime();
        MutableList<SingleExecutionPlan> serialized = bound.collect(p -> PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(p, clientVersion, pureModel, extensions, transformers)));
        long tSer = System.nanoTime() - tSer0;

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("[timing] LEAF {} ({} plan(s)): routing={}s nodeJavaGen={}s planSerialize={}s",
                    service.getPath(), routed.purePlans.size(), secs(tRoute), secs(tNode), secs(tSer));
        }

        ExecutionPlan protocolPlan;
        if (routed.multi)
        {
            MutableMap<String, SingleExecutionPlan> planMap = Maps.mutable.ofInitialCapacity(serialized.size());
            for (int i = 0; i < serialized.size(); i++)
            {
                planMap.put(routed.keys.get(i), serialized.get(i));
            }
            protocolPlan = new CompositeExecutionPlan(planMap, routed.execKey, routed.keys);
        }
        else
        {
            protocolPlan = serialized.get(0);
        }
        Root_meta_pure_executionPlan_ExecutionPlan representative = bound.isEmpty() ? null : bound.get(0);
        return new ServicePlanAndAst(protocolPlan, nt._project(), nt._typeInfos(), representative);
    }

    // [C] BATCH: process all services in a PMCD in one call.
    //   LEAF  - each service is bound to node code only + its TypeInfoSet (via generateServiceExecutionPlanAndAst).
    //   JOIN  - node ASTs and TypeInfoSets are merged as fork-join tasks join.
    //   ROOT  - the shared type layer is generated exactly ONCE from the union of TypeInfoSets, merged with
    //           the node ASTs, dependencies are resolved once, and the merged AST is attached to a
    //           representative pure plan and serialized once. The result is a single ExecutionPlan whose
    //           implementation Java classes carry the full merged output for JavaSourceHelper.writeJavaSourceFiles.
    //
    // Returns per-service (node-only, ready-to-write) plans aligned with the input service order plus the
    // merged-Java ExecutionPlan. mergedJavaPlan is null when no services produce any Java (e.g. all services
    // are Pure single-execution with empty TypeInfoSets).
    public static final class ServicePlanBatchResult
    {
        public final MutableList<ExecutionPlan> perServicePlans;
        public final ExecutionPlan mergedJavaPlan;

        ServicePlanBatchResult(MutableList<ExecutionPlan> perServicePlans, ExecutionPlan mergedJavaPlan)
        {
            this.perServicePlans = perServicePlans;
            this.mergedJavaPlan = mergedJavaPlan;
        }
    }

    public static ServicePlanBatchResult generateServiceExecutionPlanBatch(List<Service> services, PureModel pureModel, String clientVersion, PlanPlatform platform, String packagePrefix, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, int parallelism)
    {
        if (services == null || services.isEmpty())
        {
            return new ServicePlanBatchResult(Lists.mutable.empty(), null);
        }

        // Inherit the caller's context ClassLoader on every worker. In a Maven plugin execution the caller
        // thread's context CL is the plugin ClassRealm, which is the only CL that can see the Pure generated
        // classes (e.g. Root_meta_pure_metamodel_function_Function). ForkJoinWorkerThread's default context CL
        // is the system class loader, which under Maven cannot see plugin dependencies and produces
        // ClassNotFoundException from CompiledProcessorSupport / ClassCache on first parallel resolution.
        ClassLoader callerContextCL = Thread.currentThread().getContextClassLoader();
        ForkJoinPool pool = (parallelism > 1) ? new ForkJoinPool(parallelism, p ->
        {
            java.util.concurrent.ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            t.setContextClassLoader(callerContextCL);
            return t;
        }, null, false) : null;
        try
        {
            BatchAcc acc;
            if (pool == null)
            {
                acc = null;
                for (Service service : services)
                {
                    acc = mergeBatchAcc(acc, bindServiceLeaf(service, pureModel, clientVersion, platform, packagePrefix, extensions, transformers), pureModel);
                }
            }
            else
            {
                acc = pool.invoke(new BatchBindTask(services, pureModel, clientVersion, platform, packagePrefix, extensions, transformers));
            }

            ExecutionPlan mergedJavaPlan = null;
            if (acc != null && acc.representative != null && !acc.typeInfos.isEmpty())
            {
                long tUnion0 = System.nanoTime();
                Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet union = PlanPlatform.JAVA.unionTypeInfos(acc.typeInfos, pureModel);
                LOGGER.info("[timing] ROOT global union of {} per-service type-sets = {}s", acc.typeInfos.size(), secs(System.nanoTime() - tUnion0));

                long tTypes0 = System.nanoTime();
                Root_meta_external_language_java_metamodel_project_Project sharedTypes = PlanPlatform.JAVA.generateSharedTypes(union, pureModel, extensions);
                LOGGER.info("[timing] ROOT shared interface/enum Java generation = {}s", secs(System.nanoTime() - tTypes0));

                long tMerge0 = System.nanoTime();
                Root_meta_external_language_java_metamodel_project_Project prelim = (acc.project == null) ? sharedTypes : PlanPlatform.JAVA.mergeAsts(sharedTypes, acc.project, pureModel);
                LOGGER.info("[timing] ROOT merge shared types + node ASTs = {}s", secs(System.nanoTime() - tMerge0));

                long tDeps0 = System.nanoTime();
                Root_meta_external_language_java_metamodel_project_Project depProject = PlanPlatform.JAVA.resolveProjectDependencies(prelim, pureModel);
                LOGGER.info("[timing] ROOT dependency resolution (from assembled project) = {}s", secs(System.nanoTime() - tDeps0));

                Root_meta_external_language_java_metamodel_project_Project fullProject = (depProject == null) ? prelim : PlanPlatform.JAVA.mergeAsts(prelim, depProject, pureModel);

                long tSer0 = System.nanoTime();
                RichIterable<? extends Root_meta_pure_executionPlan_ExecutionPlan> withImpl = PlanPlatform.JAVA.attachMergedImplementationParallel(Lists.mutable.with(acc.representative), fullProject, pureModel, pool);
                mergedJavaPlan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(withImpl.getFirst(), clientVersion, pureModel, extensions, transformers));
                LOGGER.info("[timing] ROOT serialization of merged AST to protocol = {}s (across {} services)", secs(System.nanoTime() - tSer0), services.size());
            }
            return new ServicePlanBatchResult(acc == null ? Lists.mutable.empty() : acc.perServicePlans, mergedJavaPlan);
        }
        finally
        {
            if (pool != null)
            {
                pool.shutdown();
            }
        }
    }

    // sdlc-style planId derivation kept in engine so callers don't need to reimplement it.
    // Prepends a dot-to-underscore packagePrefix, converts :: to _, and sanitizes to a valid Java identifier.
    static String buildPlanId(String packagePrefix, String servicePath)
    {
        StringBuilder builder = new StringBuilder();
        if (packagePrefix != null && !packagePrefix.isEmpty())
        {
            builder.append(packagePrefix.replace('.', '_')).append('_');
        }
        builder.append(servicePath.replace("::", "_"));
        return JavaSourceHelper.toValidJavaIdentifier(builder.toString(), '$', true);
    }

    private static BatchAcc bindServiceLeaf(Service service, PureModel pureModel, String clientVersion, PlanPlatform platform, String packagePrefix, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        String planId = buildPlanId(packagePrefix, service.getPath());
        ServicePlanAndAst bound = generateServiceExecutionPlanAndAst(service, null, pureModel, clientVersion, platform, planId, extensions, transformers);
        BatchAcc acc = new BatchAcc();
        acc.perServicePlans.add(bound.plan);
        acc.project = bound.project;
        acc.typeInfos.add(bound.typeInfos);
        acc.representative = bound.representativePurePlan;
        return acc;
    }

    private static BatchAcc mergeBatchAcc(BatchAcc left, BatchAcc right, PureModel pureModel)
    {
        if (left == null)
        {
            return right;
        }
        if (right == null)
        {
            return left;
        }
        BatchAcc merged = new BatchAcc();
        merged.perServicePlans.addAll(left.perServicePlans);
        merged.perServicePlans.addAll(right.perServicePlans);
        merged.project = (left.project == null) ? right.project : ((right.project == null) ? left.project : PlanPlatform.JAVA.mergeAsts(left.project, right.project, pureModel));
        merged.typeInfos.addAll(left.typeInfos);
        merged.typeInfos.addAll(right.typeInfos);
        merged.representative = (left.representative != null) ? left.representative : right.representative;
        return merged;
    }

    // Internal accumulator carried up the fork-join JOIN tree. Per-service plans are concatenated in
    // left-then-right order at each JOIN so the final list stays aligned with the input service order.
    private static final class BatchAcc
    {
        final MutableList<ExecutionPlan> perServicePlans = Lists.mutable.empty();
        Root_meta_external_language_java_metamodel_project_Project project;
        final MutableList<Root_meta_pure_executionPlan_platformBinding_typeInfo_TypeInfoSet> typeInfos = Lists.mutable.empty();
        Root_meta_pure_executionPlan_ExecutionPlan representative;
    }

    private static final class BatchBindTask extends RecursiveTask<BatchAcc>
    {
        private static final long serialVersionUID = 1L;

        private final List<Service> services;
        private final PureModel pureModel;
        private final String clientVersion;
        private final PlanPlatform platform;
        private final String packagePrefix;
        private final RichIterable<? extends Root_meta_pure_extension_Extension> extensions;
        private final Iterable<? extends PlanTransformer> transformers;
        private final int start;
        private final int end;

        BatchBindTask(List<Service> services, PureModel pureModel, String clientVersion, PlanPlatform platform, String packagePrefix, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
        {
            this(services, pureModel, clientVersion, platform, packagePrefix, extensions, transformers, 0, services.size());
        }

        private BatchBindTask(List<Service> services, PureModel pureModel, String clientVersion, PlanPlatform platform, String packagePrefix, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, int start, int end)
        {
            this.services = services;
            this.pureModel = pureModel;
            this.clientVersion = clientVersion;
            this.platform = platform;
            this.packagePrefix = packagePrefix;
            this.extensions = extensions;
            this.transformers = transformers;
            this.start = start;
            this.end = end;
        }

        @Override
        protected BatchAcc compute()
        {
            int length = this.end - this.start;
            if (length < 1)
            {
                return null;
            }
            if (length == 1)
            {
                return bindServiceLeaf(this.services.get(this.start), this.pureModel, this.clientVersion, this.platform, this.packagePrefix, this.extensions, this.transformers);
            }
            int split = this.start + (length / 2);
            BatchBindTask left = new BatchBindTask(this.services, this.pureModel, this.clientVersion, this.platform, this.packagePrefix, this.extensions, this.transformers, this.start, split);
            BatchBindTask right = new BatchBindTask(this.services, this.pureModel, this.clientVersion, this.platform, this.packagePrefix, this.extensions, this.transformers, split, this.end);
            invokeAll(left, right);
            return mergeBatchAcc(left.getRawResult(), right.getRawResult(), this.pureModel);
        }
    }

    // [G] timing log helper
    private static String secs(long nanos)
    {
        return String.format("%.3f", nanos / 1_000_000_000.0);
    }

    // [A] route without binding
    private static RoutedService routeService(Execution execution, String planId, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        RoutedService routed = new RoutedService();
        if (execution instanceof PureSingleExecution)
        {
            PureSingleExecution se = (PureSingleExecution) execution;
            Mapping mapping = se.mapping != null ? pureModel.getMapping(se.mapping) : null;
            Root_meta_core_runtime_Runtime runtime = se.runtime != null ? HelperRuntimeBuilder.buildPureRuntime(se.runtime, pureModel.getContext()) : null;
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(se.func.body, se.func.parameters, pureModel.getContext());
            Root_meta_pure_runtime_ExecutionContext ctx = (se.executionOptions == null) ? context : getExecutionOptionContext(se.executionOptions, pureModel);
            routed.purePlans.add(PlanGenerator.generateExecutionPlanAsPure(lambda, mapping, runtime, ctx, pureModel, null, null, extensions));
            routed.planIds.add(planId);
        }
        else if (execution instanceof PureMultiExecution)
        {
            routed.multi = true;
            PureMultiExecution multi = (PureMultiExecution) execution;
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(multi.func.body, multi.func.parameters, pureModel.getContext());
            if (multi.executionParameters != null && !multi.executionParameters.isEmpty())
            {
                routed.execKey = multi.executionKey;
                int i = 0;
                for (KeyedExecutionParameter ep : multi.executionParameters)
                {
                    Mapping mapping = pureModel.getMapping(ep.mapping);
                    Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(ep.runtime, pureModel.getContext());
                    Root_meta_pure_runtime_ExecutionContext ctx = (ep.executionOptions == null) ? context : getExecutionOptionContext(ep.executionOptions, pureModel);
                    routed.purePlans.add(PlanGenerator.generateExecutionPlanAsPure(lambda, mapping, runtime, ctx, pureModel, null, null, extensions));
                    routed.planIds.add(planId != null ? planId + "_" + i : null);
                    routed.keys.add(getExecutionKey(ep));
                    i++;
                }
            }
            else
            {
                Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = core_service_service_helperFunctions.Root_meta_legend_service_getExecutionEnvironmentFromFunctionDefinition_FunctionDefinition_1__ExecutionEnvironmentInstance_1_(lambda, pureModel.getExecutionSupport());
                MutableList<Root_meta_legend_service_metamodel_ExecutionParameters> execParams = Lists.mutable.withAll(pureExecEnv._executionParameters());
                routed.execKey = core_service_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, pureModel.getExecutionSupport());
                int i = 0;
                for (Root_meta_legend_service_metamodel_ExecutionParameters ep : execParams)
                {
                    String key = getExecutionKey(ep);
                    LambdaFunction<?> keyLambda = (LambdaFunction<?>) core_service_service_helperFunctions.Root_meta_legend_service_assignValueInFunctionDefinitionForKey_FunctionDefinition_1__String_1__FunctionDefinition_1_(lambda, key, pureModel.getExecutionSupport());
                    routed.purePlans.add(PlanGenerator.generateExecutionPlanAsPure(keyLambda, null, null, context, pureModel, null, null, extensions));
                    routed.planIds.add(planId != null ? planId + "_" + i : null);
                    routed.keys.add(key);
                    i++;
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("Unsupported execution type for AST merge: " + execution.getClass().getSimpleName());
        }
        return routed;
    }

    public static ExecutionPlan generateExecutionPlan(Execution execution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateExecutionPlan(null, execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateSingleExecutionPlan(singleExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        Mapping mapping = singleExecution.mapping != null ? pureModel.getMapping(singleExecution.mapping) : null;
        Root_meta_core_runtime_Runtime runtime = singleExecution.runtime != null ? HelperRuntimeBuilder.buildPureRuntime(singleExecution.runtime, pureModel.getContext()) : null;
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(singleExecution.func.body, singleExecution.func.parameters, pureModel.getContext());
        return getSingleExecutionPlan(singleExecution.executionOptions, context, pureModel, clientVersion, platform, planId, extensions, transformers, mapping, runtime, lambda);
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateCompositeExecutionPlan(multiExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateCompositeExecutionPlan(null, multiExecution, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    // Helpers

    private static ExecutionPlan generateExecutionPlan(String servicePath, Execution execution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        if (execution instanceof PureSingleExecution)
        {
            return generateSingleExecutionPlan((PureSingleExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
        }
        if (execution instanceof PureMultiExecution)
        {
            return generateCompositeExecutionPlan(servicePath, (PureMultiExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, pool);
        }
        for (ServiceExecutionExtension serviceExecutionExtension : SERVICE_EXECUTION_EXTENSIONS)
        {
            for (ServiceExecutionExtension.ServiceExecutionPlanGenerator generator : serviceExecutionExtension.getExtraServiceExecutionPlanGenerators())
            {
                ExecutionPlan plan = generator.generate(execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
                if (plan != null)
                {
                    return plan;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported execution type: " + execution.getClass().getSimpleName());
    }

    private static SingleExecutionPlan getSingleExecutionPlan(List<ExecutionOption> executionOptions, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, Mapping mapping, Root_meta_core_runtime_Runtime runtime, LambdaFunction<?> lambda)
    {
        return PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, (executionOptions == null) ? context : getExecutionOptionContext(executionOptions, pureModel), pureModel, clientVersion, platform, planId, extensions, transformers);
    }

    private static Root_meta_pure_executionPlan_ExecutionOptionContext getExecutionOptionContext(List<ExecutionOption> executionOptions, PureModel pureModel)
    {
        return new Root_meta_pure_executionPlan_ExecutionOptionContext_Impl("", null, pureModel.getClass("meta::pure::executionPlan::ExecutionOptionContext"))
                ._executionOptions(ListIterate.collect(executionOptions, option -> processExecutionOption(option, pureModel.getContext())));
    }

    private static Root_meta_pure_executionPlan_ExecutionOption processExecutionOption(ExecutionOption executionOption, CompileContext context)
    {
        return context.getCompilerExtensions().getExtraExecutionOptionProcessors().stream()
                .map(processor -> processor.value(executionOption, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution option type '" + executionOption.getClass() + "'"));
    }

    private static CompositeExecutionPlan generateCompositeExecutionPlan(String servicePath, PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(multiExecution.func.body, multiExecution.func.parameters, pureModel.getContext());
        if (multiExecution.executionParameters != null && !multiExecution.executionParameters.isEmpty())
        {
            return generateCompositeExecutionPlan(
                    multiExecution.executionParameters,
                    ServicePlanGenerator::getExecutionKey,
                    (ep, key, i) -> getSingleExecutionPlan(ep.executionOptions, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + i : null), extensions, transformers, pureModel.getMapping(ep.mapping), HelperRuntimeBuilder.buildPureRuntime(ep.runtime, pureModel.getContext()), lambda),
                    multiExecution.executionKey,
                    servicePath,
                    pool);
        }
        else
        {
            // creating plans for execution environment in the execution plan
            Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = core_service_service_helperFunctions.Root_meta_legend_service_getExecutionEnvironmentFromFunctionDefinition_FunctionDefinition_1__ExecutionEnvironmentInstance_1_(lambda, pureModel.getExecutionSupport());
            MutableList<Root_meta_legend_service_metamodel_ExecutionParameters> execParams = Lists.mutable.withAll(pureExecEnv._executionParameters());
            String execKey = core_service_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, pureModel.getExecutionSupport());
            return generateCompositeExecutionPlan(
                    execParams,
                    ServicePlanGenerator::getExecutionKey,
                    (ep, key, i) -> getSingleExecutionPlan(null, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + i : null), extensions, transformers, null, null,
                            (LambdaFunction<?>) core_service_service_helperFunctions.Root_meta_legend_service_assignValueInFunctionDefinitionForKey_FunctionDefinition_1__String_1__FunctionDefinition_1_(lambda, key, pureModel.getExecutionSupport())),
                    execKey,
                    servicePath,
                    pool);
        }
    }

    private static <P> CompositeExecutionPlan generateCompositeExecutionPlan(List<P> execParams, Function<? super P, ? extends String> keyFn, SingleExecutionPlanGenerator<? super P> planFn, String execKey, String servicePath, ForkJoinPool pool)
    {
        if (pool != null)
        {
            return pool.invoke(new MultiExecutionRecursiveTask<>(execParams, keyFn, planFn, execKey, servicePath));
        }

        long start = System.nanoTime();
        if (servicePath != null)
        {
            LOGGER.debug("Generating {} plans for {}", execParams.size(), servicePath);
        }
        MutableMap<String, SingleExecutionPlan> plans = Maps.mutable.ofInitialCapacity(execParams.size());
        MutableList<String> keys = Lists.mutable.ofInitialCapacity(execParams.size());
        ListIterate.forEachWithIndex(execParams, (ep, i) ->
        {
            long planStart = System.nanoTime();
            if (servicePath != null)
            {
                LOGGER.debug("Generating {} plan {}", servicePath, i);
            }

            String key;
            try
            {
                key = keyFn.apply(ep);
            }
            catch (Throwable t)
            {
                if (servicePath != null)
                {
                    LOGGER.error("Error generating {} plan {} key", servicePath, i, t);
                }
                throw t;
            }
            keys.add(key);

            SingleExecutionPlan plan;
            try
            {
                plan = planFn.generate(ep, key, i);
            }
            catch (Throwable t)
            {
                if (servicePath != null)
                {
                    LOGGER.error("Error generating {} plan {}, key '{}'", servicePath, i, key, t);
                }
                throw t;
            }
            if (plans.put(key, plan) != null)
            {
                throw new IllegalStateException("Conflict for key '" + key + "' for " + servicePath);
            }

            if ((servicePath != null) && LOGGER.isDebugEnabled())
            {
                long planEnd = System.nanoTime();
                LOGGER.debug("Finished generating {} plan {}, key '{}' ({})", servicePath, i, key, formatNanoDurationForLogging(planStart, planEnd));
            }
        });

        if ((servicePath != null) && LOGGER.isDebugEnabled())
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished generating {} plans for {} ({})", execParams.size(), servicePath, formatNanoDurationForLogging(start, end));
        }
        return new CompositeExecutionPlan(plans, execKey, keys);
    }

    private static String getExecutionKey(KeyedExecutionParameter executionParameter)
    {
        return executionParameter.key;
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_ExecutionParameters executionParameter)
    {
        if (executionParameter instanceof Root_meta_legend_service_metamodel_SingleExecutionParameters)
        {
            return getExecutionKey((Root_meta_legend_service_metamodel_SingleExecutionParameters) executionParameter);
        }
        if (executionParameter instanceof Root_meta_legend_service_metamodel_MultiExecutionParameters)
        {
            return getExecutionKey((Root_meta_legend_service_metamodel_MultiExecutionParameters) executionParameter);
        }
        throw new UnsupportedOperationException("Cannot handle execution parameter: " + executionParameter);
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_SingleExecutionParameters executionParameter)
    {
        return executionParameter._key();
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_MultiExecutionParameters executionParameter)
    {
        return executionParameter._masterKey();
    }

    private static class MultiExecutionRecursiveTask<P> extends RecursiveTask<CompositeExecutionPlan>
    {
        private final List<P> executionParameters;
        private final Function<? super P, ? extends String> keyFn;
        private final SingleExecutionPlanGenerator<? super P> planFn;
        private final String execKey;
        private final String servicePath;
        private final AtomicReferenceArray<SingleExecutionPlan> plans;
        private final AtomicReferenceArray<String> keys;
        private volatile boolean terminated;

        private MultiExecutionRecursiveTask(List<P> executionParameters, Function<? super P, ? extends String> keyFn, SingleExecutionPlanGenerator<? super P> planFn, String execKey, String servicePath)
        {
            this.executionParameters = executionParameters;
            this.keyFn = keyFn;
            this.planFn = planFn;
            this.execKey = execKey;
            this.servicePath = servicePath;
            this.plans = new AtomicReferenceArray<>(this.executionParameters.size());
            this.keys = new AtomicReferenceArray<>(this.executionParameters.size());
        }

        @Override
        public void reinitialize()
        {
            this.terminated = false;
            super.reinitialize();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            this.terminated = true;
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public void completeExceptionally(Throwable ex)
        {
            this.terminated = true;
            super.completeExceptionally(ex);
        }

        @Override
        protected CompositeExecutionPlan compute()
        {
            long start = System.nanoTime();

            int size = this.executionParameters.size();
            if (shouldLog())
            {
                LOGGER.debug("Generating {} plans for {}", size, this.servicePath);
            }

            // Compute plans
            computeForRange(0, size);
            if (this.terminated)
            {
                if (isCompletedAbnormally())
                {
                    return null;
                }

                // This should never happen: terminated should only be true if there was abnormal completion (exception or cancellation)
                if (shouldLog())
                {
                    LOGGER.warn("Plan generation for {} terminated without abnormal completion", this.servicePath);
                }
                StringBuilder builder = new StringBuilder("Unknown error during multi-execution plan generation");
                if (this.servicePath != null)
                {
                    builder.append(" of ").append(this.servicePath);
                }
                throw new IllegalStateException(builder.toString());
            }

            // Assemble result
            MutableMap<String, SingleExecutionPlan> planMap = Maps.mutable.ofInitialCapacity(size);
            MutableList<String> keyList = Lists.mutable.ofInitialCapacity(size);
            for (int i = 0; i < size; i++)
            {
                String key = this.keys.get(i);
                if (key == null)
                {
                    if (shouldLog())
                    {
                        LOGGER.warn("No key for plan {} of {}", i, this.servicePath);
                    }
                    StringBuilder builder = new StringBuilder("No key generated for plan ").append(i);
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
                keyList.add(key);

                SingleExecutionPlan plan = this.plans.get(i);
                if (plan == null)
                {
                    if (shouldLog())
                    {
                        LOGGER.warn("No plan generated for key {} ({}) of {}", i, key, this.servicePath);
                    }
                    StringBuilder builder = new StringBuilder("No plan generated for key ").append(i).append(" (").append(key).append(")");
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
                if (planMap.put(key, plan) != null)
                {
                    StringBuilder builder = new StringBuilder("Conflict for key '").append(key).append("'");
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
            }
            if (shouldLog() && LOGGER.isDebugEnabled())
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished generating {} plans for {} ({})", size, this.servicePath, formatNanoDurationForLogging(start, end));
            }
            return new CompositeExecutionPlan(planMap, this.execKey, keyList);
        }

        private void computeForRange(int start, int end)
        {
            if (this.terminated)
            {
                return;
            }

            try
            {
                int length = end - start;
                if (length == 1)
                {
                    computeForIndex(start);
                }
                else
                {
                    int midPoint = start + (length / 2);
                    invokeAll(new RangeTask(start, midPoint), new RangeTask(midPoint, end));
                }
            }
            catch (Throwable t)
            {
                this.terminated = true;
                throw t;
            }
        }

        private void computeForIndex(int index)
        {
            long start = System.nanoTime();
            if (shouldLog())
            {
                LOGGER.debug("Generating {} plan {}", this.servicePath, index);
            }
            P executionParameter = this.executionParameters.get(index);

            String key;
            try
            {
                key = this.keyFn.apply(executionParameter);
            }
            catch (Throwable t)
            {
                if (shouldLog())
                {
                    LOGGER.error("Error generating {} plan key {}", this.servicePath, index, t);
                }
                throw t;
            }

            SingleExecutionPlan plan;
            try
            {
                plan = this.planFn.generate(executionParameter, key, index);
            }
            catch (Throwable t)
            {
                if (shouldLog())
                {
                    LOGGER.error("Error generating {} plan {} ({})", this.servicePath, index, key, t);
                }
                throw t;
            }

            this.keys.set(index, key);
            this.plans.set(index, plan);
            if (shouldLog() && LOGGER.isDebugEnabled())
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished generating {} plan {}, key {} ({})", this.servicePath, index, key, formatNanoDurationForLogging(start, end));
            }
        }

        private boolean shouldLog()
        {
            return this.servicePath != null;
        }

        private class RangeTask extends RecursiveAction
        {
            private final int start;
            private final int end;

            private RangeTask(int start, int end)
            {
                this.start = start;
                this.end = end;
            }

            @Override
            protected void compute()
            {
                computeForRange(this.start, this.end);
            }
        }
    }

    private interface SingleExecutionPlanGenerator<P>
    {
        SingleExecutionPlan generate(P executionParameter, String key, int index);
    }



    private static String formatNanoDurationForLogging(long startNanos, long endNanos)
    {
        return formatNanoDurationForLogging(endNanos - startNanos);
    }

    private static String formatNanoDurationForLogging(long durationNanos)
    {
        return (durationNanos == 0) ? "0s" : String.format("%.9fs", durationNanos / 1_000_000_000.0);
    }
}
