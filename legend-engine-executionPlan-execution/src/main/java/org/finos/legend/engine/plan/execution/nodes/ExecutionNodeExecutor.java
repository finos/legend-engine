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

package org.finos.legend.engine.plan.execution.nodes;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeGraphFetchMergeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeGraphFetchUnionSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByTargetCrossKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeSerializerHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.nodes.state.GraphExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.MultiResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.builder._class.PartialClassBuilder;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphFetchResult;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.validation.FunctionParametersParametersValidation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AggregationAwareExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AllocationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ConstantExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ErrorExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FreeMarkerConditionalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.GraphFetchM2MExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.MultiResultSequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.PureExpressionPlatformExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.XStorePropertyFetchDetails;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.SerializationConfig;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final MutableList<CommonProfile> profiles;
    private final ExecutionState executionState;

    public ExecutionNodeExecutor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        this.profiles = profiles;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        return this.executionState.extraNodeExecutors.stream().map(executor -> executor.value(executionNode, profiles, executionState)).filter(Objects::nonNull).findFirst().orElseThrow(() -> new UnsupportedOperationException("Unsupported execution node type '" + executionNode.getClass().getSimpleName() + "'"));
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        return graphFetchM2MExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.profiles, this.executionState));
    }

    @Override
    public Result visit(ErrorExecutionNode errorExecutionNode)
    {
        Result payload = (errorExecutionNode.executionNodes() == null || errorExecutionNode.executionNodes().isEmpty()) ? null : errorExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(this.profiles, this.executionState)).realizeInMemory();
        return new ErrorResult(1, errorExecutionNode.message, payload);
    }

    @Override
    public Result visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode)
    {
        Map<String, Result> subResults = Maps.mutable.empty();
        Result last = null;
        for (ExecutionNode n : multiResultSequenceExecutionNode.executionNodes())
        {
            last = n.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
            if (n instanceof AllocationExecutionNode)
            {
                subResults.put(((AllocationExecutionNode) n).varName, last);
            }
            if (last instanceof ErrorResult)
            {
                return last;
            }
        }
        if (last != null)
        {
            subResults.put("@LAST", last);
        }
        return new MultiResult(subResults);
    }

    @Override
    public Result visit(FunctionParametersValidationNode functionParametersValidationNode)
    {
        FunctionParametersParametersValidation.validate(Lists.immutable.withAll(functionParametersValidationNode.functionParameters), this.executionState);
        return new ConstantResult(true);
    }

    @Override
    public Result visit(AllocationExecutionNode allocationExecutionNode)
    {
        String varName = allocationExecutionNode.varName;
        Result result = allocationExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(this.profiles, new ExecutionState(this.executionState).varName(varName)));
//        if (!(r instanceof ConstantResult) && !(r instanceof RelationalResult) && !(r instanceof StreamingObjectResult))
//        {
//            r.close();
//            throw new RuntimeException("Not supported yet! " + r.getClass().getName());
//        }
        if (result instanceof ConstantResult && ((ConstantResult) result).getValue() instanceof Map && ((Map<?, ?>) ((ConstantResult) result).getValue()).get("values") != null)
        {
            result = new ConstantResult(((List<?>) ((Map<?, ?>) ((ConstantResult) result).getValue()).get("values")).get(0));
        }
        if (this.executionState.realizeAllocationResults)
        {
            result = result.realizeInMemory();
        }
        this.executionState.addResult(allocationExecutionNode.varName, result);
        return result;
    }

    @Override
    public Result visit(PureExpressionPlatformExecutionNode pureExpressionPlatformExecutionNode)
    {
        if (!(pureExpressionPlatformExecutionNode.implementation instanceof JavaPlatformImplementation))
        {
            throw new RuntimeException("Only Java implementations are currently supported, found: " + pureExpressionPlatformExecutionNode.implementation);
        }

        JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) pureExpressionPlatformExecutionNode.implementation;
        String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
        Class<?> clazz = ExecutionNodeJavaPlatformHelper.getClassToExecute(pureExpressionPlatformExecutionNode, executionClassName, this.executionState, this.profiles);
        if (Arrays.asList(clazz.getInterfaces()).contains(IPlatformPureExpressionExecutionNodeSerializeSpecifics.class))
        {
            try
            {
                org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics nodeSpecifics = (org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics) clazz.newInstance();
                Result childResult = pureExpressionPlatformExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(profiles, executionState));
                IExecutionNodeContext context = new DefaultExecutionNodeContext(this.executionState, childResult);

                AppliedFunction f = (AppliedFunction) pureExpressionPlatformExecutionNode.pure;
                SerializationConfig config = f.parameters.size() == 3 ? (SerializationConfig) f.parameters.get(2) : null;

                return ExecutionNodeSerializerHelper.executeSerialize(nodeSpecifics, config, childResult, context);
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        if (Arrays.asList(clazz.getInterfaces()).contains(IPlatformPureExpressionExecutionNodeGraphFetchUnionSpecifics.class))
        {
            List<StreamingObjectResult<?>> streamingObjectResults = ListIterate.collect(pureExpressionPlatformExecutionNode.executionNodes, node -> (StreamingObjectResult) node.accept(new ExecutionNodeExecutor(this.profiles, this.executionState)));

            Result childResult = new Result("success")
            {
                @Override
                public <T> T accept(ResultVisitor<T> resultVisitor)
                {
                    throw new RuntimeException("Not implemented");
                }

                @Override
                public void close()
                {
                    streamingObjectResults.forEach(StreamingObjectResult::close);
                }
            };

            return new StreamingObjectResult<>(streamingObjectResults.stream().flatMap(StreamingObjectResult::getObjectStream), streamingObjectResults.get(0).getResultBuilder(), childResult);
        }

        if (Arrays.asList(clazz.getInterfaces()).contains(IPlatformPureExpressionExecutionNodeGraphFetchMergeSpecifics.class))
        {
            StreamingObjectResult<?> streamResult = (StreamingObjectResult) pureExpressionPlatformExecutionNode.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));

            return streamResult;
        }

        else
        {
            return ExecutionNodeJavaPlatformHelper.executeJavaImplementation(pureExpressionPlatformExecutionNode, DefaultExecutionNodeContext.factory(), this.profiles, this.executionState);
        }
    }


    @Override
    public Result visit(ConstantExecutionNode constantExecutionNode)
    {
        return new ConstantResult(constantExecutionNode.values());
    }


    @Override
    public Result visit(FreeMarkerConditionalExecutionNode freeMarkerConditionalExecutionNode)
    {
        String conditionalExpression = freeMarkerConditionalExecutionNode.freeMarkerBooleanExpression;
        String processedBooleanValue = FreeMarkerExecutor.process(conditionalExpression, this.executionState);
        boolean isConditionSatisfied = Boolean.parseBoolean(processedBooleanValue);

        if (isConditionSatisfied)
        {
            return freeMarkerConditionalExecutionNode.trueBlock.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
        }
        else if (freeMarkerConditionalExecutionNode.falseBlock != null)
        {
            return freeMarkerConditionalExecutionNode.falseBlock.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
        }
        else
        {
            return new ConstantResult("success");
        }
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        return aggregationAwareExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.Relational).getVisitor(this.profiles, this.executionState));
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchExecutionNode graphFetchExecutionNode)
    {
        final Span topSpan = GlobalTracer.get().activeSpan();
        try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query: Execute").startActive(true))
        {
            Result rootResult;
            try (Scope ignored2 = GlobalTracer.get().buildSpan("Graph Query: Execute Root").startActive(true))
            {
                rootResult = graphFetchExecutionNode.rootExecutionNode.accept(new ExecutionNodeExecutor(profiles, executionState));
            }

            if (graphFetchExecutionNode.implementation != null)
            {
                if (!(rootResult instanceof StreamingObjectResult))
                {
                    throw new RuntimeException("Unexpected result : " + rootResult.getClass().getName());
                }

                StreamingObjectResult<?> objectResult = (StreamingObjectResult<?>) rootResult;
                try
                {
                    if (!(graphFetchExecutionNode.implementation instanceof JavaPlatformImplementation))
                    {
                        throw new RuntimeException("Only Java implementations are currently supported, found: " + graphFetchExecutionNode.implementation);
                    }
                    JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) graphFetchExecutionNode.implementation;
                    String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
                    String executionMethodName = JavaHelper.getExecutionMethodName(javaPlatformImpl);

                    Stream<?> transformedResult = ExecutionNodeJavaPlatformHelper.executeStaticJavaMethod(graphFetchExecutionNode, executionClassName, executionMethodName, Arrays.asList(StreamingObjectResult.class, ExecutionNode.class, ExecutionState.class, ProfileManager.class), Arrays.asList(objectResult, graphFetchExecutionNode, this.executionState, this.profiles), this.executionState, this.profiles);
                    return new StreamingObjectResult<>(transformedResult, objectResult.getResultBuilder(), objectResult);
                }
                catch (Exception e)
                {
                    objectResult.close();
                    throw e;
                }
            }
            else
            {
                final long maxMemoryBytesForGraph = 52_428_800L; /* 50MB - 50 * 1024 * 1024 */
                final int batchSize = graphFetchExecutionNode.batchSize;

                final GlobalGraphFetchExecutionNode rootGlobalNode = graphFetchExecutionNode.globalGraphFetchExecutionNode;
                final LocalGraphFetchExecutionNode rootLocalNode = rootGlobalNode.localGraphFetchExecutionNode;

                final AtomicInteger batchIndex = new AtomicInteger(1);
                final AtomicLong rowCount = new AtomicLong(0);

                try
                {
                    Stream<List<?>> batchedStoreLocalObjectStream = StreamSupport.stream(
                            new Spliterators.AbstractSpliterator<List<?>>(Long.MAX_VALUE, Spliterator.ORDERED)
                            {
                                @Override
                                public boolean tryAdvance(Consumer<? super List<?>> action)
                                {
                                    int currentBatch = batchIndex.getAndIncrement();
                                    try (Scope scope = GlobalTracer.get().buildSpan("Graph Query: Execute Batch " + currentBatch).startActive(true))
                                    {
                                        GraphExecutionState graphExecutionState = new GraphExecutionState(executionState, batchSize, rootResult, maxMemoryBytesForGraph);
                                        ConstantResult constantResult = (ConstantResult) rootLocalNode.accept(new ExecutionNodeExecutor(ExecutionNodeExecutor.this.profiles, graphExecutionState));
                                        List<?> objects = (List<?>) constantResult.getValue();
                                        boolean nonEmptyObjectList = !objects.isEmpty();

                                        if (rootGlobalNode.children != null && (rootGlobalNode.children.size() > 0) && nonEmptyObjectList)
                                        {
                                            try (Scope ignored3 = GlobalTracer.get().buildSpan("Graph Query: Execute Cross Store Children").startActive(true))
                                            {
                                                for (GlobalGraphFetchExecutionNode crossChild : rootGlobalNode.children)
                                                {
                                                    try (Scope ignored4 = GlobalTracer.get().buildSpan("Graph Query: Execute Cross Store Child").startActive(true))
                                                    {
                                                        graphExecutionState.setObjectsToGraphFetch(objects);
                                                        ExecutionNodeExecutor.this.executeGlobalGraphOperation(crossChild, graphExecutionState);
                                                    }
                                                }
                                            }
                                        }
                                        rowCount.addAndGet(graphExecutionState.getRowCount());
                                        action.accept(objects);
                                        if (nonEmptyObjectList)
                                        {
                                            scope.span().setTag("batchObjectCount", objects.size());
                                            scope.span().setTag("batchMemoryUtilization", graphExecutionState.getTotalObjectMemoryUtilization());
                                        }
                                        else
                                        {
                                            if (topSpan != null)
                                            {
                                                topSpan.setTag("lastQueryRowCount", rowCount);
                                            }
                                        }
                                        return nonEmptyObjectList && (objects.size() >= batchSize);
                                    }
                                }
                            },
                            false
                    );

                    Stream<?> globalObjectStream = batchedStoreLocalObjectStream.flatMap(Collection::stream);

                    return new StreamingObjectResult<>(globalObjectStream, new PartialClassBuilder(graphFetchExecutionNode), rootResult);
                }
                catch (Exception e)
                {
                    rootResult.close();
                    throw e;
                }
            }
        }
    }

    private void executeGlobalGraphOperation(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode, GraphExecutionState graphExecutionState)
    {
        List<?> parentObjects = graphExecutionState.getObjectsForNodeIndex(globalGraphFetchExecutionNode.parentIndex);
        graphExecutionState.setObjectsToGraphFetch(parentObjects);
        globalGraphFetchExecutionNode.localGraphFetchExecutionNode.accept(new ExecutionNodeExecutor(profiles, graphExecutionState));

        if (globalGraphFetchExecutionNode.children != null && (globalGraphFetchExecutionNode.children.size() > 0) && !parentObjects.isEmpty())
        {
            try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query: Execute Cross Store Children").startActive(true))
            {
                for (GlobalGraphFetchExecutionNode crossChild : globalGraphFetchExecutionNode.children)
                {
                    try (Scope ignored2 = GlobalTracer.get().buildSpan("Graph Query: Execute Cross Store Child").startActive(true))
                    {
                        this.executeGlobalGraphOperation(crossChild, graphExecutionState);
                    }
                }
            }
        }
    }

    @Override
    public Result visit(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode)
    {
        final Span topSpan = GlobalTracer.get().activeSpan();
        final boolean isGraphRoot = globalGraphFetchExecutionNode.parentIndex == null;

        if (isGraphRoot)
        {
            final boolean enableConstraints = globalGraphFetchExecutionNode.enableConstraints == null ? false : globalGraphFetchExecutionNode.enableConstraints;
            final boolean checked = globalGraphFetchExecutionNode.checked == null ? false : globalGraphFetchExecutionNode.checked;

            // Handle batching at root level
            final AtomicLong rowCount = new AtomicLong(0L);
            final AtomicLong objectCount = new AtomicLong(0L);
            final DoubleSummaryStatistics memoryStatistics = new DoubleSummaryStatistics();
            GraphFetchResult graphFetchResult = (GraphFetchResult) globalGraphFetchExecutionNode.localGraphFetchExecutionNode.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));

            Stream<?> objectStream = graphFetchResult.getGraphObjectsBatchStream().map(batch ->
            {
                List<?> parentObjects = batch.getObjectsForNodeIndex(0);
                boolean nonEmptyObjectList = !parentObjects.isEmpty();

                ExecutionState newState = new ExecutionState(this.executionState).setGraphObjectsBatch(batch);
                if (globalGraphFetchExecutionNode.children != null && !globalGraphFetchExecutionNode.children.isEmpty() && nonEmptyObjectList)
                {
                    globalGraphFetchExecutionNode.children.forEach(c -> c.accept(new ExecutionNodeExecutor(this.profiles, newState)));
                }

                rowCount.addAndGet(batch.getRowCount());

                if (this.executionState.adaptiveGraphBatchStats != null)
                {
                    this.executionState.adaptiveGraphBatchStats.batchObjectMemoryUtilization = batch.getTotalObjectMemoryUtilization();
                }


                if (nonEmptyObjectList)
                {
                    long currentObjectCount = objectCount.addAndGet(parentObjects.size());
                    memoryStatistics.accept(batch.getTotalObjectMemoryUtilization() / (parentObjects.size() * 1.0));

                    if (graphFetchResult.getGraphFetchSpan() != null)
                    {
                        Span graphFetchSpan = graphFetchResult.getGraphFetchSpan();
                        graphFetchSpan.setTag("batchCount", memoryStatistics.getCount());
                        graphFetchSpan.setTag("objectCount", currentObjectCount);
                        graphFetchSpan.setTag("avgMemoryUtilizationInBytesPerObject", memoryStatistics.getAverage());
                    }
                }

                if (!nonEmptyObjectList)
                {
                    if (topSpan != null && rowCount.get() > 0)
                    {
                        topSpan.setTag("lastQueryRowCount", rowCount);
                    }
                }

                if (checked)
                {
                    return parentObjects.stream()
                            .map(x -> (IChecked<?>) x)
                            .map(x -> x.getValue() instanceof Constrained ? ((Constrained<?>) x.getValue()).toChecked(x.getSource(), enableConstraints) : x).collect(Collectors.toList());
                }

                if (enableConstraints)
                {
                    return parentObjects.stream()
                            .map(x -> x instanceof Constrained ? ((Constrained<?>) x).withConstraintsApplied() : x).collect(Collectors.toList());
                }

                return parentObjects;

            }).flatMap(Collection::stream);
            boolean realizeAsConstant = this.executionState.inAllocation && ExecutionNodeResultHelper.isResultSizeRangeSet(globalGraphFetchExecutionNode) && ExecutionNodeResultHelper.isSingleRecordResult(globalGraphFetchExecutionNode);
            if (realizeAsConstant)
            {
                return new ConstantResult(objectStream.findFirst().orElseThrow(() -> new RuntimeException("Constant value not found")));
            }
            return new StreamingObjectResult<>(objectStream, new PartialClassBuilder(globalGraphFetchExecutionNode), graphFetchResult);
        }
        else
        {
            GraphObjectsBatch graphObjectsBatch = this.executionState.graphObjectsBatch;
            List<?> parentObjects = graphObjectsBatch.getObjectsForNodeIndex(globalGraphFetchExecutionNode.parentIndex);

            if ((parentObjects != null) && !parentObjects.isEmpty())
            {
                if (globalGraphFetchExecutionNode.xStorePropertyFetchDetails != null && globalGraphFetchExecutionNode.xStorePropertyFetchDetails.supportsCaching && this.executionState.graphFetchCaches != null)
                {
                    graphObjectsBatch.setXStorePropertyCachesForNodeIndex(globalGraphFetchExecutionNode.localGraphFetchExecutionNode.nodeIndex, findGraphFetchCacheByTargetCrossKeys(globalGraphFetchExecutionNode));
                }
                globalGraphFetchExecutionNode.localGraphFetchExecutionNode.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));

                if (globalGraphFetchExecutionNode.children != null && !globalGraphFetchExecutionNode.children.isEmpty())
                {
                    globalGraphFetchExecutionNode.children.forEach(c -> c.accept(new ExecutionNodeExecutor(this.profiles, this.executionState)));
                }
            }

            return new ConstantResult(parentObjects);
        }
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode storeStreamReadingExecutionNode)
    {
        return storeStreamReadingExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.profiles, this.executionState));
    }

    @Override
    public Result visit(InMemoryRootGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        return inMemoryRootGraphFetchExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.profiles, this.executionState));
    }

    @Override
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode inMemoryCrossStoreGraphFetchExecutionNode)
    {
        return inMemoryCrossStoreGraphFetchExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.profiles, this.executionState));
    }

    @Override
    public Result visit(InMemoryPropertyGraphFetchExecutionNode inMemoryPropertyGraphFetchExecutionNode)
    {
        return inMemoryPropertyGraphFetchExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.profiles, this.executionState));
    }

    @Override
    public Result visit(LocalGraphFetchExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(SequenceExecutionNode sequenceExecutionNode)
    {
        Result last = null;
        for (ExecutionNode node : sequenceExecutionNode.executionNodes())
        {
            Result temp = this.executionState.extraSequenceNodeExecutors.stream().map(executor -> executor.value(node, this.profiles, this.executionState)).filter(Objects::nonNull).findFirst().orElse(null);
            if (temp == null)
            {
                last = node.accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
            }
        }
        return last;
    }

    private ExecutionCache<GraphFetchCacheKey, List<Object>> findGraphFetchCacheByTargetCrossKeys(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode)
    {
        List<GraphFetchCache> graphFetchCaches = this.executionState.graphFetchCaches;
        XStorePropertyFetchDetails fetchDetails = globalGraphFetchExecutionNode.xStorePropertyFetchDetails;

        return graphFetchCaches
                .stream()
                .filter(GraphFetchCacheByTargetCrossKeys.class::isInstance)
                .map(GraphFetchCacheByTargetCrossKeys.class::cast)
                .filter(cache -> cache.getGraphFetchCrossAssociationKeys() != null)
                .filter(cache ->
                {
                    GraphFetchCrossAssociationKeys c = cache.getGraphFetchCrossAssociationKeys();
                    return c.getPropertyPath().equals(fetchDetails.propertyPath) &&
                            c.getSourceMappingId().equals(fetchDetails.sourceMappingId) &&
                            c.getSourceSetId().equals(fetchDetails.sourceSetId) &&
                            c.getTargetMappingId().equals(fetchDetails.targetMappingId) &&
                            c.getTargetSetId().equals(fetchDetails.targetSetId) &&
                            c.getTargetPropertiesOrdered().equals(fetchDetails.targetPropertiesOrdered) &&
                            c.getSubTree().equals(fetchDetails.subTree);
                })
                .map(GraphFetchCacheByTargetCrossKeys::getExecutionCache)
                .findFirst().orElse(null);
    }
}