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

package org.finos.legend.engine.plan.execution.nodes.state;

import io.opentracing.Span;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.concurrent.ConcurrentExecutionNodeExecutorPool;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtensionLoader;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.pac4j.core.profile.CommonProfile;

/**
 * copy method is used to create a copy of ExecutionState to be used in isolated/concurrent environment. Do update the copy method in case of any field addition.
 */
public class ExecutionState
{
    public MutableList<ExecutionActivity> activities = Lists.mutable.empty();
    public boolean inAllocation;
    public boolean inLake;
    public String allocationNodeName;
    public String authId;
    public boolean realizeInMemory;
    public Span topSpan;
    public boolean realizeAllocationResults;
    public String execID;

    private final long graphFetchBatchMemoryLimit;
    public GraphObjectsBatch graphObjectsBatch;
    public List<GraphFetchCache> graphFetchCaches;

    private EngineJavaCompiler javaCompiler;

    private ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool;

    private final Map<String, Result> res;
    private final List<? extends String> templateFunctions;
    private final boolean isJavaCompilationAllowed;
    private final Map<StoreType, StoreExecutionState> states = new EnumMap<>(StoreType.class);
    private final boolean logSQLWithParamValues;

    public final List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> extraNodeExecutors;
    public final List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> extraSequenceNodeExecutors;
    public RequestContext requestContext;
    private final CredentialProviderProvider credentialProviderProvider;

    public ExecutionState(ExecutionState state)
    {
        this.inAllocation = state.inAllocation;
        this.inLake = state.inLake;
        this.res = state.res;
        this.allocationNodeName = state.allocationNodeName;
        this.templateFunctions = state.templateFunctions;
        this.authId = state.authId;
        this.execID = state.execID;
        this.realizeInMemory = state.realizeInMemory;
        this.topSpan = state.topSpan;
        this.activities = state.activities;
        this.realizeAllocationResults = state.realizeAllocationResults;
        this.isJavaCompilationAllowed = state.isJavaCompilationAllowed;
        this.javaCompiler = state.javaCompiler;
        this.graphFetchBatchMemoryLimit = state.graphFetchBatchMemoryLimit;
        this.graphObjectsBatch = state.graphObjectsBatch;
        this.graphFetchCaches = state.graphFetchCaches;
        this.concurrentExecutionNodeExecutorPool = state.concurrentExecutionNodeExecutorPool;
        state.states.forEach((storeType, storeExecutionState) -> this.states.put(storeType, storeExecutionState.copy()));
        List<ExecutionExtension> extensions = ExecutionExtensionLoader.extensions();
        this.extraNodeExecutors = ListIterate.flatCollect(extensions, ExecutionExtension::getExtraNodeExecutors);
        this.extraSequenceNodeExecutors = ListIterate.flatCollect(extensions, ExecutionExtension::getExtraSequenceNodeExecutors);
        this.requestContext = state.requestContext;
        this.credentialProviderProvider = state.credentialProviderProvider;
        this.logSQLWithParamValues = state.logSQLWithParamValues;
    }

    public ExecutionState(Map<String, Result> res, List<? extends String> templateFunctions, Iterable<? extends StoreExecutionState> extraStates, boolean isJavaCompilationAllowed, long graphFetchBatchMemoryLimit)
    {
        this(res, templateFunctions, extraStates, isJavaCompilationAllowed, graphFetchBatchMemoryLimit, new RequestContext(), null);
    }

    public ExecutionState(Map<String, Result> res, List<? extends String> templateFunctions, Iterable<? extends StoreExecutionState> extraStates, boolean isJavaCompilationAllowed, long graphFetchBatchMemoryLimit, RequestContext requestContext,  CredentialProviderProvider credentialProviderProvider)
    {
        this(res, templateFunctions, extraStates, isJavaCompilationAllowed, graphFetchBatchMemoryLimit, requestContext, credentialProviderProvider, true);
    }

    public ExecutionState(Map<String, Result> res, List<? extends String> templateFunctions, Iterable<? extends StoreExecutionState> extraStates, boolean isJavaCompilationAllowed, long graphFetchBatchMemoryLimit, RequestContext requestContext, CredentialProviderProvider credentialProviderProvider, boolean logSQLWithParamValues)
    {
        this.inAllocation = false;
        this.inLake = false;
        this.execID = UUID.randomUUID().toString();
        this.res = res;
        this.templateFunctions = templateFunctions;
        this.realizeAllocationResults = false;
        this.isJavaCompilationAllowed = isJavaCompilationAllowed;
        this.graphFetchBatchMemoryLimit = graphFetchBatchMemoryLimit;
        extraStates.forEach(storeExecutionState -> this.states.put(storeExecutionState.getStoreState().getStoreType(), storeExecutionState));
        List<ExecutionExtension> extensions = ExecutionExtensionLoader.extensions();
        this.extraNodeExecutors = ListIterate.flatCollect(extensions, ExecutionExtension::getExtraNodeExecutors);
        this.extraSequenceNodeExecutors = ListIterate.flatCollect(extensions, ExecutionExtension::getExtraSequenceNodeExecutors);
        this.requestContext = requestContext;
        this.credentialProviderProvider = credentialProviderProvider;
        this.logSQLWithParamValues = logSQLWithParamValues;
    }

    public ExecutionState(Map<String, Result> res, List<? extends String> templateFunctions, Iterable<? extends StoreExecutionState> extraStates, boolean isJavaCompilationAllowed)
    {
        this(res, templateFunctions, extraStates, isJavaCompilationAllowed, PlanExecutor.DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT);
    }

    public ExecutionState(Map<String, Result> res, List<? extends String> templateFunctions, Iterable<? extends StoreExecutionState> extraStates)
    {
        this(res, templateFunctions, extraStates, true);
    }

    public ExecutionState copy()
    {
        Map<String, Result> resCopy = Maps.mutable.ofMap(this.res);
        List<? extends String> templateFunctionsCopy = Lists.mutable.ofAll(this.templateFunctions);
        List<? extends StoreExecutionState> extraStatesCopy = this.states.values().stream().map(StoreExecutionState::copy).collect(Collectors.toList());
        ExecutionState copy = new ExecutionState(resCopy, templateFunctionsCopy, extraStatesCopy, this.isJavaCompilationAllowed, this.graphFetchBatchMemoryLimit, this.requestContext, this.credentialProviderProvider, this.logSQLWithParamValues);

        copy.activities = Lists.mutable.withAll(this.activities);
        copy.allocationNodeName = this.allocationNodeName;
        copy.authId = this.authId;
        copy.concurrentExecutionNodeExecutorPool = this.concurrentExecutionNodeExecutorPool;
        copy.inAllocation = this.inAllocation;
        copy.inLake = this.inLake;
        copy.realizeAllocationResults = this.realizeAllocationResults;
        copy.realizeInMemory = this.realizeInMemory;
        copy.execID = this.execID;
        copy.graphObjectsBatch = null;  // Explicitly making this null, to prevent conflicts during concurrent executions
        copy.graphFetchCaches = null;   // Explicitly making this null, to prevent conflicts during concurrent executions
        copy.javaCompiler = this.javaCompiler;
        copy.topSpan = this.topSpan;
        copy.requestContext = this.requestContext;
        return copy;
    }

    public ExecutionState inLake(boolean inLake)
    {
        this.inLake = inLake;
        this.realizeInMemory = true;
        return this;
    }

    public ExecutionState varName(String var)
    {
        this.inAllocation = true;
        this.allocationNodeName = var;
        return this;
    }

    public ExecutionState setAuthUser(String user)
    {
        return setAuthUser(user, true);
    }

    public ExecutionState setAuthUser(String user, boolean setRealizeInMemory)
    {
        this.authId = user;
        this.realizeInMemory = setRealizeInMemory;
        return this;
    }

    public ExecutionState setRealizeAllocationResults(boolean realize)
    {
        this.realizeAllocationResults = realize;
        return this;
    }

    public ExecutionState setJavaCompiler(EngineJavaCompiler javaCompiler)
    {
        if (isJavaCompilationForbidden())
        {
            throw new IllegalStateException("Java compilation is not allowed");
        }
        this.javaCompiler = javaCompiler;
        return this;
    }

    public EngineJavaCompiler getJavaCompiler()
    {
        return this.javaCompiler;
    }

    public boolean hasJavaCompiler()
    {
        return this.javaCompiler != null;
    }

    public boolean isJavaCompilationAllowed()
    {
        return this.isJavaCompilationAllowed;
    }

    public boolean isJavaCompilationForbidden()
    {
        return !isJavaCompilationAllowed();
    }

    public ConcurrentExecutionNodeExecutorPool getConcurrentExecutionNodeExecutorPool()
    {
        return this.concurrentExecutionNodeExecutorPool;
    }

    public void setConcurrentExecutionNodeExecutorPool(ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool)
    {
        this.concurrentExecutionNodeExecutorPool = concurrentExecutionNodeExecutorPool;
    }

    public long getGraphFetchBatchMemoryLimit()
    {
        return this.graphFetchBatchMemoryLimit;
    }

    public ExecutionState setGraphObjectsBatch(GraphObjectsBatch graphObjectsBatch)
    {
        this.graphObjectsBatch = graphObjectsBatch;
        return this;
    }

    public ExecutionState setGraphFetchCaches(List<GraphFetchCache> graphFetchCaches)
    {
        this.graphFetchCaches = graphFetchCaches;
        return this;
    }

    public ExecutionState setRealizeInMemory(boolean realizeInMemory)
    {
        if (!this.realizeInMemory)
        {
            this.realizeInMemory = realizeInMemory;
        }
        return this;
    }

    public Result getUserSuppliedVector()
    {
        return this.res.get("userSuppliedVector");
    }

    public Result getResult(String key)
    {
        return this.res.get(key);
    }

    public Map<String, Result> getResults()
    {
        return Collections.unmodifiableMap(this.res);
    }

    public void addResult(String key, Result result)
    {
        this.res.put(key, result);
    }

    public void addParameterValue(String parameter, Object value)
    {
        addResult(parameter, (value instanceof Result) ? (Result) value : new ConstantResult(value));
    }

    public StoreExecutionState getStoreExecutionState(StoreType type)
    {
        return this.states.get(type);
    }

    public List<? extends String> getTemplateFunctions()
    {
        return Collections.unmodifiableList(this.templateFunctions);
    }

    public RequestContext getRequestContext()
    {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext)
    {
        this.requestContext = requestContext;
    }

    public CredentialProviderProvider getCredentialProviderProvider()
    {
        return this.credentialProviderProvider;
    }

    public boolean logSQLWithParamValues()
    {
        return this.logSQLWithParamValues;
    }
}