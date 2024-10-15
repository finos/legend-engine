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
import org.finos.legend.engine.plan.execution.concurrent.ConcurrentExecutionNodeExecutorPool;
import org.finos.legend.engine.plan.execution.concurrent.ParallelGraphFetchExecutionExecutorPool;
import org.finos.legend.engine.plan.execution.graphFetch.GraphFetchExecutionConfiguration;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilderLoader;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.engine.shared.core.url.InputStreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PlanExecutor
{
    public static final String USER_ID = "userId";
    public static final String EXEC_ID = "execID";
    public static final String REFERER = "referer";

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final boolean DEFAULT_IS_JAVA_COMPILATION_ALLOWED = true;

    private final boolean isJavaCompilationAllowed;
    private final ImmutableList<StoreExecutor> extraExecutors;
    private final PlanExecutorInfo planExecutorInfo;
    private ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool;
    private ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool;
    private GraphFetchExecutionConfiguration graphFetchExecutionConfiguration;
    private BiFunction<Identity, ExecutionState, ExecutionNodeExecutor> executionNodeExecutorBuilder;
    private final boolean logSQLWithParamValues;


    private PlanExecutor(boolean isJavaCompilationAllowed, ImmutableList<StoreExecutor> extraExecutors, GraphFetchExecutionConfiguration graphFetchExecutionConfiguration, boolean logSQLWithParamValues)
    {
        EngineUrlStreamHandlerFactory.initialize();
        this.isJavaCompilationAllowed = isJavaCompilationAllowed;
        this.extraExecutors = extraExecutors;
        this.planExecutorInfo = PlanExecutorInfo.fromStoreExecutors(this.extraExecutors);
        this.graphFetchExecutionConfiguration = graphFetchExecutionConfiguration;
        this.logSQLWithParamValues = logSQLWithParamValues;
    }

    public PlanExecutorInfo getPlanExecutorInfo()
    {
        return this.planExecutorInfo;
    }

    public void setGraphFetchExecutionConfiguration(GraphFetchExecutionConfiguration graphFetchExecutionConfiguration)
    {
        this.graphFetchExecutionConfiguration = graphFetchExecutionConfiguration;
    }

    public ImmutableList<StoreExecutor> getExtraExecutors()
    {
        return extraExecutors;
    }

    public ImmutableList<StoreExecutor> getExecutorsOfType(StoreType type)
    {
        return this.extraExecutors.select(e -> e.getStoreState().getStoreType() == type);
    }

    @Deprecated
    public Result execute(String executionPlan)
    {
        return execute(executionPlan, (InputStream) null);
    }

    @Deprecated
    public Result execute(String executionPlan, String input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    @Deprecated
    public Result execute(String executionPlan, Map<String, ?> params)
    {
        return execute(executionPlan, (InputStream) null, params);
    }

    @Deprecated
    public Result execute(String executionPlan, String input, Map<String, ?> params)
    {
        return execute(executionPlan, (input == null) ? null : new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), params);
    }

    @Deprecated
    public Result execute(String executionPlan, InputStream input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    @Deprecated
    public Result execute(String executionPlan, InputStream inputStream, Map<String, ?> params)
    {
        return execute(readExecutionPlan(executionPlan), inputStream, params);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan)
    {
        return execute(executionPlan, (InputStream) null);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, String input)
    {
        return execute(executionPlan, input, Collections.emptyMap());
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params)
    {
        return execute(executionPlan, (InputStream) null, params);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, InputStream inputStream)
    {
        return execute(executionPlan, inputStream, Collections.emptyMap());
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, String input, Map<String, ?> params)
    {
        return execute(executionPlan, (input == null) ? null : new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), params);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, InputStream inputStream, Map<String, ?> params)
    {
        return execute(executionPlan, params, (inputStream == null) ? null : new InputStreamProvider(inputStream));
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider)
    {
        return execute(executionPlan, params, inputStreamProvider, null);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, String user, Identity identity, PlanExecutionContext planExecutionContext)
    {
        Map<String, Result> vars = Maps.mutable.ofInitialCapacity(params.size());
        params.forEach((key, value) -> vars.put(key, new ConstantResult(value)));
        return execute(executionPlan.getSingleExecutionPlan(params), vars, user, identity, planExecutionContext);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider, PlanExecutionContext planExecutionContext)
    {
        return execute(executionPlan, params, inputStreamProvider, Identity.getAnonymousIdentity(), planExecutionContext);
    }

    @Deprecated
    public Result execute(ExecutionPlan executionPlan, Map<String, ?> params, StreamProvider inputStreamProvider, Identity identity, PlanExecutionContext planExecutionContext)
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
            return execute(singleExecutionPlan, vars, (String) null, identity, planExecutionContext);
        }
        finally
        {
            StreamProviderHolder.streamProviderThreadLocal.remove();
        }
    }

    public Result execute(SingleExecutionPlan executionPlan, Map<String, Result> vars, String user, Identity identity)
    {
        return execute(executionPlan, buildDefaultExecutionState(executionPlan, vars), user, identity);
    }

    public Result execute(SingleExecutionPlan executionPlan, Map<String, Result> vars, String user,Identity identity, PlanExecutionContext planExecutionContext)
    {
        return execute(executionPlan, buildDefaultExecutionState(executionPlan, vars, planExecutionContext), user, identity);
    }

    public Result execute(SingleExecutionPlan executionPlan, Map<String, Result> vars, String user, Identity identity, PlanExecutionContext planExecutionContext, RequestContext requestContext)
    {
        ExecutionState state = buildDefaultExecutionState(executionPlan, vars, planExecutionContext);
        if (requestContext != null)
        {
            state.setRequestContext(requestContext);
        }

        return execute(executionPlan, state, user, identity);
    }

    public Result execute(SingleExecutionPlan singleExecutionPlan, ExecutionState state, String user, Identity identity)
    {
        EngineJavaCompiler engineJavaCompiler = possiblyCompilePlan(singleExecutionPlan, state, identity);
        try (JavaHelper.ThreadContextClassLoaderScope scope = (engineJavaCompiler == null) ? null : JavaHelper.withCurrentThreadContextClassLoader(engineJavaCompiler.getClassLoader()))
        {
            // set up the state
            setUpState(singleExecutionPlan, state, identity, user);
            singleExecutionPlan.getExecutionStateParams(org.eclipse.collections.api.factory.Maps.mutable.empty()).forEach(state::addParameterValue);

            // execute
            return singleExecutionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(identity, state));
        }
    }

    public Result executeWithArgs(ExecuteArgs executeArgs)
    {
        SingleExecutionPlan singleExecutionPlan = executeArgs.executionPlan.getSingleExecutionPlan(executeArgs.params);
        try
        {
            StreamProvider inputStreamProvider = executeArgs.inputStreamProvider;
            if (inputStreamProvider != null)
            {
                StreamProviderHolder.streamProviderThreadLocal.set(inputStreamProvider);
            }

            final ExecutionState state = (executeArgs.executionState != null) ?
                    executeArgs.executionState :
                    buildDefaultExecutionState(singleExecutionPlan, executeArgs.vars, executeArgs.planExecutionContext);

            executeArgs.storeRuntimeContexts.forEach(((storeType, runtimeContext) ->
            {
                state.getStoreExecutionState(storeType).setRuntimeContext(runtimeContext);
            }));

            EngineJavaCompiler engineJavaCompiler = possiblyCompilePlan(singleExecutionPlan, state, executeArgs.identity);
            try (JavaHelper.ThreadContextClassLoaderScope scope = (engineJavaCompiler == null) ? null : JavaHelper.withCurrentThreadContextClassLoader(engineJavaCompiler.getClassLoader()))
            {
                // set up the state
                if (executeArgs.requestContext != null)
                {
                    state.setRequestContext(executeArgs.requestContext);
                }
                setUpState(singleExecutionPlan, state, executeArgs.identity, executeArgs.user);

                singleExecutionPlan.getExecutionStateParams(org.eclipse.collections.api.factory.Maps.mutable.empty()).forEach(state::addParameterValue);
                // execute
                ExecutionNodeExecutor executionNodeExecutor = this.buildExecutionNodeExecutor(executeArgs.identity, state);
                return singleExecutionPlan.rootExecutionNode.accept(executionNodeExecutor);
            }
        }
        finally
        {
            StreamProviderHolder.streamProviderThreadLocal.remove();
        }
    }

    protected static void setUpState(SingleExecutionPlan singleExecutionPlan, ExecutionState state, Identity identity, String user)
    {
        if (singleExecutionPlan.authDependent)
        {
            state.setAuthUser((singleExecutionPlan.kerberos == null) ? user : singleExecutionPlan.kerberos);
        }
        if (state.authId == null)
        {
            state.setAuthUser(identity.getName(), false);
        }
        if ((state.getResult(USER_ID) == null))
        {
            state.addResult(USER_ID, new ConstantResult(state.authId));
        }
        if (state.getResult(EXEC_ID) == null)
        {
            state.addResult(EXEC_ID, new ConstantResult(state.execID));
        }
        state.addResult(REFERER, new ConstantResult(String.valueOf(RequestContext.getReferral(state.requestContext)).replace("'", "''")));
    }

    public void injectConcurrentExecutionNodeExecutorPool(ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool)
    {
        if (this.concurrentExecutionNodeExecutorPool != null)
        {
            throw new IllegalStateException("PlanExecutor already contains a ConcurrentExecutionNodeExecutorPool");
        }
        this.concurrentExecutionNodeExecutorPool = concurrentExecutionNodeExecutorPool;
    }

    public void injectGraphFetchExecutionNodeExecutorPool(ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool)
    {
        if (this.graphFetchExecutionNodeExecutorPool != null)
        {
            throw new IllegalStateException("PlanExecutor already contains a graphFetchExecutionNodeExecutorPool");
        }
        this.graphFetchExecutionNodeExecutorPool = graphFetchExecutionNodeExecutorPool;
    }

    private EngineJavaCompiler possiblyCompilePlan(SingleExecutionPlan plan, ExecutionState state, Identity identity)
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
            EngineJavaCompiler engineJavaCompiler = JavaHelper.compilePlan(plan, identity);
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

    public ExecutionState buildDefaultExecutionState(SingleExecutionPlan executionPlan, Map<String, Result> vars, PlanExecutionContext planExecutionContext)
    {
        ExecutionState executionState = new ExecutionState(vars, executionPlan.templateFunctions, this.extraExecutors.collect(StoreExecutor::buildStoreExecutionState), this.isJavaCompilationAllowed, null, this.graphFetchExecutionConfiguration, this.logSQLWithParamValues);

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

        if (this.concurrentExecutionNodeExecutorPool != null)
        {
            executionState.setConcurrentExecutionNodeExecutorPool(this.concurrentExecutionNodeExecutorPool);
        }

        if (this.graphFetchExecutionNodeExecutorPool != null)
        {
            executionState.setGraphFetchExecutionNodeExecutorPool(this.graphFetchExecutionNodeExecutorPool);
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

    public static PlanExecutor.Builder newPlanExecutorBuilder()
    {
        return new PlanExecutor.Builder();
    }

    public static class Builder
    {
        private boolean isJavaCompilationAllowed = DEFAULT_IS_JAVA_COMPILATION_ALLOWED;
        private final MutableList<StoreExecutor> storeExecutors = Lists.mutable.empty();
        private GraphFetchExecutionConfiguration graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration();
        private boolean logSQLWithParamValues = true;

        private Builder()
        {

        }

        public Builder isJavaCompilationAllowed(boolean isJavaCompilationAllowed)
        {
            this.isJavaCompilationAllowed = isJavaCompilationAllowed;
            return this;
        }

        @Deprecated
        public Builder withGraphFetchBatchMemoryLimit(long graphFetchBatchMemoryLimit)
        {
            this.graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration(graphFetchBatchMemoryLimit);
            return this;
        }

        public Builder withGraphFetchExecutionConfiguration(GraphFetchExecutionConfiguration graphFetchExecutionConfiguration)
        {
            this.graphFetchExecutionConfiguration = graphFetchExecutionConfiguration;
            return this;
        }

        public Builder withStoreExecutors(StoreExecutor... storeExecutors)
        {
            Collections.addAll(this.storeExecutors, storeExecutors);
            return this;
        }

        public Builder withAvailableStoreExecutors()
        {
            IterableIterate.collect(StoreExecutorBuilderLoader.extensions(), StoreExecutorBuilder::build, this.storeExecutors);
            return this;
        }

        public Builder logSQLWithParamValues(boolean value)
        {
            this.logSQLWithParamValues = value;
            return this;
        }

        public PlanExecutor build()
        {
            return new PlanExecutor(this.isJavaCompilationAllowed, this.storeExecutors.toImmutable(), this.graphFetchExecutionConfiguration, this.logSQLWithParamValues);
        }
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors, long graphFetchBatchMemoryLimit)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withStoreExecutors(Iterate.toArray(storeExecutors, new StoreExecutor[0]))
                .withGraphFetchBatchMemoryLimit(graphFetchBatchMemoryLimit)
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, Iterable<? extends StoreExecutor> storeExecutors)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withStoreExecutors(Iterate.toArray(storeExecutors, new StoreExecutor[0]))
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(Iterable<? extends StoreExecutor> storeExecutors)
    {
        return newPlanExecutorBuilder()
                .withStoreExecutors(Iterate.toArray(storeExecutors, new StoreExecutor[0]))
                .build();
    }

    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, StoreExecutor... storeExecutors)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withStoreExecutors(storeExecutors)
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(StoreExecutor... storeExecutors)
    {
        return newPlanExecutorBuilder()
                .withStoreExecutors(storeExecutors)
                .build();
    }

    public static PlanExecutor newPlanExecutor(GraphFetchExecutionConfiguration graphFetchExecutionConfiguration, StoreExecutor... storeExecutors)
    {
        return newPlanExecutorBuilder()
                .withStoreExecutors(storeExecutors)
                .withGraphFetchExecutionConfiguration(graphFetchExecutionConfiguration)
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(boolean isJavaCompilationAllowed, StoreExecutor storeExecutor)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withStoreExecutors(storeExecutor)
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutor(StoreExecutor storeExecutor)
    {
        return newPlanExecutorBuilder()
                .withStoreExecutors(storeExecutor)
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors(boolean isJavaCompilationAllowed, long graphFetchBatchMemoryLimit)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withGraphFetchBatchMemoryLimit(graphFetchBatchMemoryLimit)
                .withAvailableStoreExecutors()
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors(boolean isJavaCompilationAllowed, GraphFetchExecutionConfiguration graphFetchExecutionConfiguration)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withGraphFetchExecutionConfiguration(graphFetchExecutionConfiguration)
                .withAvailableStoreExecutors()
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors(boolean isJavaCompilationAllowed)
    {
        return newPlanExecutorBuilder()
                .isJavaCompilationAllowed(isJavaCompilationAllowed)
                .withAvailableStoreExecutors()
                .build();
    }

    @Deprecated
    public static PlanExecutor newPlanExecutorWithAvailableStoreExecutors()
    {
        return newPlanExecutorBuilder()
                .withAvailableStoreExecutors()
                .build();
    }

    public static List<StoreExecutorBuilder> loadStoreExecutorBuilders()
    {
        return Iterate.addAllTo(StoreExecutorBuilderLoader.extensions(), Lists.mutable.empty());
    }

    public static PlanExecutor newPlanExecutorWithConfigurations(StoreExecutorConfiguration... storeExecutorConfigurations)
    {
        MutableList<StoreExecutorBuilder> storeExecutorBuilders = Iterate.addAllTo(StoreExecutorBuilderLoader.extensions(), org.eclipse.collections.impl.factory.Lists.mutable.empty());
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

    public static ExecuteArgsBuilder withArgs()
    {
        return new ExecuteArgsBuilder();
    }

    public static class ExecuteArgs
    {
        private ExecutionPlan executionPlan;
        private ExecutionState executionState;
        private StreamProvider inputStreamProvider;
        private PlanExecutionContext planExecutionContext;
        private Map<StoreType, StoreExecutionState.RuntimeContext> storeRuntimeContexts = Maps.mutable.empty();
        private Map<String, Result> vars = Maps.mutable.empty();
        private Map<String, Object> params = Maps.mutable.empty();
        private Identity identity;
        private String user;
        private RequestContext requestContext;


        private ExecuteArgs(ExecuteArgsBuilder builder)
        {
            this.executionPlan = builder.executionPlan;
            this.executionState = builder.executionState;
            this.inputStreamProvider = builder.inputStreamProvider;
            this.planExecutionContext = builder.planExecutionContext;
            this.storeRuntimeContexts.putAll(builder.storeRuntimeContexts);
            this.executionState = builder.executionState;
            this.vars.putAll(builder.vars);
            this.params.putAll(builder.params);
            this.identity = builder.identity == null ? Identity.getAnonymousIdentity() : builder.identity; // write a test case and merge it into engine
            this.user = builder.user;
            this.requestContext = builder.requestContext;
        }

        public static ExecuteArgsBuilder newArgs()
        {
            return new ExecuteArgsBuilder();
        }
    }

    public static class ExecuteArgsBuilder
    {
        private ExecutionPlan executionPlan;
        private ExecutionState executionState;
        private StreamProvider inputStreamProvider;
        private PlanExecutionContext planExecutionContext;
        private Map<StoreType, StoreExecutionState.RuntimeContext> storeRuntimeContexts = Maps.mutable.empty();
        private Map<String, Result> vars = Maps.mutable.empty();
        private Map<String, Object> params = Maps.mutable.empty();
        private Identity identity = Identity.getAnonymousIdentity();
        private String user;
        private RequestContext requestContext;


        private ExecuteArgsBuilder()
        {

        }

        public ExecuteArgsBuilder withPlan(ExecutionPlan executionPlan)
        {
            this.executionPlan = executionPlan;
            return this;
        }

        public ExecuteArgsBuilder withPlanAsString(String executionPlanString)
        {
            this.executionPlan = PlanExecutor.readExecutionPlan(executionPlanString);
            return this;
        }

        public ExecuteArgsBuilder withState(ExecutionState state)
        {
            this.executionState = state;
            return this;
        }

        public ExecuteArgsBuilder withParams(Map<String, ?> params)
        {
            this.params.clear();
            this.vars.clear();

            if (params != null)
            {
                this.params.putAll(params);
                params.forEach((key, value) -> this.vars.put(key, new ConstantResult(value)));
            }

            return this;
        }

        public ExecuteArgsBuilder withParamsAsResults(Map<String, Result> vars)
        {
            this.params.clear();
            this.vars.clear();

            if (vars != null)
            {
                vars.forEach((key, value) -> this.params.put(key, value));
                this.vars.putAll(vars);
            }

            return this;
        }

        public ExecuteArgsBuilder withInputAsString(String input)
        {
            this.inputStreamProvider = new InputStreamProvider(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        public ExecuteArgsBuilder withInputAsStream(InputStream inputStream)
        {
            this.inputStreamProvider = new InputStreamProvider(inputStream);
            return this;
        }

        public ExecuteArgsBuilder withInputAsStreamProvider(StreamProvider inputStreamProvider)
        {
            this.inputStreamProvider = inputStreamProvider;
            return this;
        }

        public ExecuteArgsBuilder withIdentity(Identity identity)
        {
           this.identity = identity;
            return this;
        }

        public ExecuteArgsBuilder withUser(String user)
        {
            this.user = user;
            return this;
        }

        public ExecuteArgsBuilder withPlanExecutionContext(PlanExecutionContext planExecutionContext)
        {
            this.planExecutionContext = planExecutionContext;
            return this;
        }

        public ExecuteArgsBuilder withStoreRuntimeContexts(Map<StoreType, StoreExecutionState.RuntimeContext> storeRuntimeContexts)
        {
            this.storeRuntimeContexts = storeRuntimeContexts;
            return this;
        }

        public ExecuteArgsBuilder withStoreRuntimeContext(StoreType storeType, StoreExecutionState.RuntimeContext storeRuntimeContext)
        {
            this.storeRuntimeContexts.put(storeType, storeRuntimeContext);
            return this;
        }

        public ExecuteArgsBuilder withRequestContext(RequestContext requestContext)
        {
            this.requestContext = requestContext;
            return this;
        }

        public ExecutionState getExecutionState()
        {
            return executionState;
        }

        public ExecuteArgs build()
        {
            ExecuteArgs executeArgs = new ExecuteArgs(this);
            return executeArgs;
        }
    }

    public void setExecutionNodeExecutorBuilder(BiFunction<Identity, ExecutionState, ExecutionNodeExecutor> supplier)
    {
        this.executionNodeExecutorBuilder = supplier;
    }

    private ExecutionNodeExecutor buildExecutionNodeExecutor(Identity identity, ExecutionState executionState)
    {
        if (this.executionNodeExecutorBuilder == null)
        {
            return new ExecutionNodeExecutor(identity, executionState);
        }
        else
        {
            return executionNodeExecutorBuilder.apply(identity, executionState);
        }
    }
}
