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

package org.finos.legend.engine.plan.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.engine.shared.core.url.InputStreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.pac4j.core.profile.CommonProfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class PlanExecutor
{
    public static final long DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT = 52_428_800L; /* 50MB - 50 * 1024 * 1024 */
    public static final long DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE = 50;
    public static final boolean DEFAULT_USE_ADAPTIVE_BATCHING = true;
    public static final String USER_ID = "userId";

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final boolean DEFAULT_IS_JAVA_COMPILATION_ALLOWED = true;

    private final boolean isJavaCompilationAllowed;
    private final ImmutableList<StoreExecutor> extraExecutors;
    private final PlanExecutorInfo planExecutorInfo;
    private long graphFetchBatchMemoryLimit;
    private long graphFetchSoftMemoryLimitPercentage;
    private boolean useAdaptiveBatching;

    private PlanExecutor(boolean isJavaCompilationAllowed, ImmutableList<StoreExecutor> extraExecutors, long graphFetchBatchMemoryLimit, long graphFetchSoftMemoryLimitPercentage, boolean useAdaptiveBatching)
    {
        EngineUrlStreamHandlerFactory.initialize();
        this.isJavaCompilationAllowed = isJavaCompilationAllowed;
        this.extraExecutors = extraExecutors;
        this.planExecutorInfo = PlanExecutorInfo.fromStoreExecutors(this.extraExecutors);
        this.graphFetchBatchMemoryLimit = graphFetchBatchMemoryLimit;
        this.graphFetchSoftMemoryLimitPercentage = graphFetchSoftMemoryLimitPercentage;
        this.useAdaptiveBatching = useAdaptiveBatching;
    }

    public PlanExecutorInfo getPlanExecutorInfo()
    {
        return this.planExecutorInfo;
    }

    public ImmutableList<StoreExecutor> getExtraExecutors()
    {
        return extraExecutors;
    }

    public Result execute(String executionPlan)
    {
        return execute(executionPlan, (InputStream) null);
    }

    public Result execute(String executionPlan, String input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    public Result execute(String executionPlan, Map<String, ?> params)
    {
        return execute(executionPlan, (InputStream) null, params);
    }

    public Result execute(String executionPlan, String input, Map<String, ?> params)
    {
        return execute(executionPlan, (input == null) ? null : new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), params);
    }

    public Result execute(String executionPlan, InputStream input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    public Result execute(String executionPlan, InputStream inputStream, Map<String, ?> params)
    {
        return execute(readExecutionPlan(executionPlan), inputStream, params);
    }

    public Result execute(ExecutionPlan executionPlan)
    {
        return execute(executionPlan, (InputStream) null);
    }

    public Result execute(ExecutionPlan executionPlan, String input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params)
    {
        return execute(executionPlan, (InputStream) null, params);
    }

    public Result execute(ExecutionPlan executionPlan, InputStream inputStream)
    {
        return execute(executionPlan, inputStream, Collections.emptyMap());
    }

    public Result execute(ExecutionPlan executionPlan, String input, Map<String, ?> params)
    {
        return execute(executionPlan, (input == null) ? null : new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), params);
    }

    public Result execute(ExecutionPlan executionPlan, InputStream inputStream, Map<String, ?> params)
    {
        return execute(executionPlan, params, (inputStream == null) ? null : new InputStreamProvider(inputStream));
    }

    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider)
    {
        return execute(executionPlan, params, inputStreamProvider, null);
    }

    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, String user, MutableList<CommonProfile> profiles, PlanExecutionContext planExecutionContext)
    {
        Map<String, Result> vars = Maps.mutable.ofInitialCapacity(params.size());
        params.forEach((key, value) -> vars.put(key, new ConstantResult(value)));
        return execute(executionPlan.getSingleExecutionPlan(params), vars, user, profiles, planExecutionContext);
    }

    // TODO: Build a user friendly API
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider, PlanExecutionContext planExecutionContext)
    {
        return execute(executionPlan, params, inputStreamProvider, null, planExecutionContext);
    }

    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider, MutableList<CommonProfile> profiles, PlanExecutionContext planExecutionContext)
    {
        SingleExecutionPlan singleExecutionPlan = executionPlan.getSingleExecutionPlan(params);
        try
        {
            if (inputStreamProvider != null)
            {
                StreamProviderHolder.streamProviderThreadLocal.set(inputStreamProvider);
            }
            Map<String, Result> vars = Maps.mutable.ofInitialCapacity(params.size());
            params.forEach((key, value) -> vars.put(key, new ConstantResult(value)));
            return execute(singleExecutionPlan, vars, (String) null, profiles, planExecutionContext);
        }
        finally
        {
            StreamProviderHolder.streamProviderThreadLocal.remove();
        }
    }

    public Result execute(SingleExecutionPlan executionPlan, Map<String, Result> vars, String user, MutableList<CommonProfile> profiles)
    {
        return execute(executionPlan, buildDefaultExecutionState(executionPlan, vars), user, profiles);
    }

    public Result execute(SingleExecutionPlan executionPlan, Map<String, Result> vars, String user, MutableList<CommonProfile> profiles, PlanExecutionContext planExecutionContext)
    {
        return execute(executionPlan, buildDefaultExecutionState(executionPlan, vars, planExecutionContext), user, profiles);
    }

    public Result execute(SingleExecutionPlan singleExecutionPlan, ExecutionState state, String user, MutableList<CommonProfile> profiles)
    {
        EngineJavaCompiler engineJavaCompiler = possiblyCompilePlan(singleExecutionPlan, state, profiles);
        try (JavaHelper.ThreadContextClassLoaderScope scope = (engineJavaCompiler == null) ? null : JavaHelper.withCurrentThreadContextClassLoader(engineJavaCompiler.getClassLoader()))
        {
            // set up the state
            if (singleExecutionPlan.authDependent)
            {
                state.setAuthUser((singleExecutionPlan.kerberos == null) ? user : singleExecutionPlan.kerberos);
            }
            if (state.authId == null)
            {
                state.setAuthUser(IdentityFactoryProvider.getInstance().makeIdentity(profiles).getName(), false);
            }
            if (singleExecutionPlan.authDependent && (state.getResult(USER_ID) == null))
            {
                state.addResult(USER_ID, new ConstantResult(state.authId));
            }
            singleExecutionPlan.getExecutionStateParams(org.eclipse.collections.api.factory.Maps.mutable.empty()).forEach(state::addParameterValue);

            // execute
            return singleExecutionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(profiles, state));
        }
    }

    public void setGraphFetchBatchMemoryLimit(long graphFetchBatchMemoryLimit)
    {
        this.graphFetchBatchMemoryLimit = graphFetchBatchMemoryLimit;
    }

    public void setGraphFetchSoftMemoryLimitPercentage(long graphFetchSoftMemoryLimitPercentage)
    {
        this.graphFetchSoftMemoryLimitPercentage = graphFetchSoftMemoryLimitPercentage;
    }

    public void setUseAdaptiveBatching(boolean useAdaptiveBatching)
    {
        this.useAdaptiveBatching = useAdaptiveBatching;
    }

    private EngineJavaCompiler possiblyCompilePlan(SingleExecutionPlan plan, ExecutionState state, MutableList<CommonProfile> profiles)
    {
        if (state.isJavaCompilationForbidden())
        {
            return null;
        }
        if (state.hasJavaCompiler())
        {
            return state.getJavaCompiler();
        }
        try
        {
            EngineJavaCompiler engineJavaCompiler = JavaHelper.compilePlan(plan, profiles);
            if (engineJavaCompiler != null)
            {
                state.setJavaCompiler(engineJavaCompiler);
            }
            return engineJavaCompiler;
        }
        catch (JavaCompileException e)
        {
            throw new RuntimeException(e);
        }
    }

    public ExecutionState buildDefaultExecutionState(SingleExecutionPlan executionPlan, Map<String, Result> vars)
    {
        return buildDefaultExecutionState(executionPlan, vars, null);
    }

    private ExecutionState buildDefaultExecutionState(SingleExecutionPlan executionPlan, Map<String, Result> vars, PlanExecutionContext planExecutionContext)
    {
        ExecutionState executionState = new ExecutionState(vars, executionPlan.templateFunctions, this.extraExecutors.collect(StoreExecutor::buildStoreExecutionState), this.isJavaCompilationAllowed, this.graphFetchBatchMemoryLimit, this.graphFetchSoftMemoryLimitPercentage, this.useAdaptiveBatching);

        if (planExecutionContext != null)
        {
            if (planExecutionContext.getExternalJavaCompiler() != null)
            {
                executionState.setJavaCompiler(planExecutionContext.getExternalJavaCompiler());
            }

            if (planExecutionContext.getGraphFetchCaches() != null)
            {
                executionState.setGraphFetchCaches(planExecutionContext.getGraphFetchCaches());
            }
        }

        return executionState;
    }

    public static ExecutionPlan readExecutionPlan(String string)
    {
        try
        {
            return objectMapper.readValue(string, ExecutionPlan.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static ExecutionPlan readExecutionPlan(Reader reader)
    {
        try
        {
            return objectMapper.readValue(reader, ExecutionPlan.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors, long graphFetchBatchMemoryLimit)
    {
        return new PlanExecutor(isJavaCompilationAllowed, Lists.immutable.withAll(storeExecutors), graphFetchBatchMemoryLimit, DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, DEFAULT_USE_ADAPTIVE_BATCHING);
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors, boolean useAdaptiveBatching)
    {
        return new PlanExecutor(isJavaCompilationAllowed, Lists.immutable.withAll(storeExecutors), DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT, DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, useAdaptiveBatching);
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors, long graphFetchBatchMemoryLimit, long graphFetchSoftMemoryLimitPercentage)
    {
        return new PlanExecutor(isJavaCompilationAllowed, Lists.immutable.withAll(storeExecutors), graphFetchBatchMemoryLimit, graphFetchSoftMemoryLimitPercentage, DEFAULT_USE_ADAPTIVE_BATCHING);
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors)
    {
        return PlanExecutor.newPlanExecutor(isJavaCompilationAllowed, storeExecutors, DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT);
    }

    public static PlanExecutor newPlanExecutor(Iterable<? extends StoreExecutor> storeExecutors)
    {
        return newPlanExecutor(DEFAULT_IS_JAVA_COMPILATION_ALLOWED, storeExecutors);
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, StoreExecutor... storeExecutors)
    {
        return new PlanExecutor(isJavaCompilationAllowed, Lists.immutable.with(storeExecutors), DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT, DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, DEFAULT_USE_ADAPTIVE_BATCHING);
    }

    public static PlanExecutor newPlanExecutor(StoreExecutor... storeExecutors)
    {
        return newPlanExecutor(DEFAULT_IS_JAVA_COMPILATION_ALLOWED, storeExecutors);
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, StoreExecutor storeExecutor)
    {
        return new PlanExecutor(isJavaCompilationAllowed, Lists.immutable.with(storeExecutor), DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT, DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, DEFAULT_USE_ADAPTIVE_BATCHING);
    }

    public static PlanExecutor newPlanExecutor(StoreExecutor storeExecutor)
    {
        return newPlanExecutor(DEFAULT_IS_JAVA_COMPILATION_ALLOWED, Lists.immutable.with(storeExecutor));
    }

    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors(boolean isJavaCompilationAllowed, long graphFetchBatchMemoryLimit)
    {
        return newPlanExecutor(isJavaCompilationAllowed, IterableIterate.collect(ServiceLoader.load(StoreExecutorBuilder.class), StoreExecutorBuilder::build, Lists.mutable.empty()), graphFetchBatchMemoryLimit);
    }

    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors(boolean isJavaCompilationAllowed)
    {
        return newPlanExecutorWithAvailableStoreExecutors(isJavaCompilationAllowed, PlanExecutor.DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT);
    }

    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors()
    {
        return newPlanExecutorWithAvailableStoreExecutors(DEFAULT_IS_JAVA_COMPILATION_ALLOWED);
    }

    public static List<StoreExecutorBuilder> loadStoreExecutorBuilders()
    {
        return Iterate.addAllTo(ServiceLoader.load(StoreExecutorBuilder.class), Lists.mutable.empty());
    }

    public static PlanExecutor newPlanExecutorWithConfigurations(StoreExecutorConfiguration... storeExecutorConfigurations)
    {

        MutableList<StoreExecutorBuilder> storeExecutorBuilders = Iterate.addAllTo(ServiceLoader.load(StoreExecutorBuilder.class), org.eclipse.collections.impl.factory.Lists.mutable.empty());
        ImmutableListMultimap<StoreType, StoreExecutorConfiguration> configurationsByType = Lists.immutable.with(storeExecutorConfigurations).groupBy(storeExecutorConfiguration -> storeExecutorConfiguration.getStoreType());
        ImmutableListMultimap<StoreType, StoreExecutorBuilder> buildersByType = Lists.immutable.withAll(storeExecutorBuilders).groupBy(storeExecutorBuilder -> storeExecutorBuilder.getStoreType());

        List<StoreExecutor> storeExecutors = Lists.immutable.with(StoreType.values())
                .stream()
                .map(storeType ->
                        {
                            ImmutableList<StoreExecutorConfiguration> configurationsForType = configurationsByType.get(storeType);
                            ImmutableList<StoreExecutorBuilder> buildersForType = buildersByType.get(storeType);
                            return buildStoreExecutor(storeType, configurationsForType, buildersForType);
                        }
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return PlanExecutor.newPlanExecutor(storeExecutors);
    }

    private static Optional<StoreExecutor> buildStoreExecutor(StoreType storeType, ImmutableList<StoreExecutorConfiguration> configurations, ImmutableList<StoreExecutorBuilder> builders)
    {
        if (builders.size() == 0)
        {
            return Optional.empty();
        }

        if (builders.size() > 1)
        {
            List<String> builderClasses = builders.stream().map(builder -> builder.getClass().getCanonicalName()).collect(Collectors.toList());
            String message = String.format("Found more than one builder for store type %s. Builders=%s", storeType, builderClasses);
            throw new RuntimeException(message);
        }

        if (configurations.size() > 1)
        {
            List<String> configurationClasses = configurations.stream().map(config -> config.getClass().getCanonicalName()).collect(Collectors.toList());
            String message = String.format("Found more than one configuration for store type %s. Configuration object types=%s", storeType, configurationClasses.size());
            throw new RuntimeException(message);
        }

        StoreExecutorBuilder builder = builders.get(0);
        StoreExecutorConfiguration configuration = configurations.size() == 1 ? configurations.get(0) : null;
        StoreExecutor storeExecutor = configuration == null ? builder.build() : builder.build(configuration);
        return Optional.of(storeExecutor);
    }
}