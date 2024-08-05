// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.inMemory.plugin;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryPropertyGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryRootGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryRootGraphFetchMergeExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IStoreStreamReadingExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphFetchResult;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.result.graphFetch.StoreStreamReadingResult;
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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.StoreMappingGlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ClassResultType;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleStrategyHashMap;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InMemoryExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    ExecutionState executionState;
    Identity identity;

    public InMemoryExecutionNodeExecutor(Identity identity, ExecutionState executionState)
    {
        this.identity = identity;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        return ExecutionNodeJavaPlatformHelper.executeJavaImplementation(graphFetchM2MExecutionNode, GraphFetchM2MExecutionNodeContext.factory(graphFetchM2MExecutionNode), this.identity, this.executionState);
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode node)
    {
        IStoreStreamReadingExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.identity);
        StoreStreamReadingExecutionNodeContext context = (StoreStreamReadingExecutionNodeContext) StoreStreamReadingExecutionNodeContext.factory(node).create(this.executionState, null);
        StoreStreamReadingObjectsIterator<?> storeObjectsIterator = StoreStreamReadingObjectsIterator.newObjectsIterator(nodeSpecifics.streamReader(context), node.enableConstraints, node.checked);
        return new StoreStreamReadingResult<>(storeObjectsIterator);
    }

    @Override
    public Result visit(InMemoryRootGraphFetchExecutionNode node)
    {
        int batchSize = node.batchSize == null ? 1 : node.batchSize;
        boolean isLeaf = node.children == null || node.children.isEmpty();
        boolean checked = node.checked;
        ClassResultType classResultType = (ClassResultType) node.resultType;
        String _class = classResultType._class;

        Result childResult = null;
        JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) node.implementation;
        String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
        Class<?> clazz = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, executionClassName, this.executionState, this.identity);
        Span graphFetchSpan = GlobalTracer.get().buildSpan("graph fetch").withTag("rootStoreType", "inMemory").withTag("batchSizeConfig", batchSize).start();
        GlobalTracer.get().activateSpan(graphFetchSpan);

        try
        {


            if ((Arrays.asList(clazz.getInterfaces()).contains(IInMemoryRootGraphFetchMergeExecutionNodeSpecifics.class)))
            {
                return mergeInMemoryNode(node);
            }
            else
            {
                IInMemoryRootGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.identity);

                childResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.identity, this.executionState));
                Iterator<?> sourceObjectsIterator;
                if (childResult instanceof StoreStreamReadingResult)
                {
                    StoreStreamReadingResult<?> storeStreamReadingResult = (StoreStreamReadingResult) childResult;
                    sourceObjectsIterator = storeStreamReadingResult.getObjectsIterator();
                }
                else if (childResult instanceof StreamingObjectResult)
                {
                    StreamingObjectResult<?> streamingObjectResult = (StreamingObjectResult) childResult;
                    sourceObjectsIterator = streamingObjectResult.getObjectStream().iterator();
                }
                else
                {
                    throw new IllegalStateException("Unsupported result type: " + childResult.getClass().getSimpleName());
                }

                AtomicLong batchIndex = new AtomicLong(0L);
                IExecutionNodeContext context = new DefaultExecutionNodeContext(executionState, childResult);

                Spliterator<GraphObjectsBatch> graphObjectsBatchSpliterator = new Spliterators.AbstractSpliterator<GraphObjectsBatch>(Long.MAX_VALUE, Spliterator.ORDERED)
                {
                    @Override
                    public boolean tryAdvance(Consumer<? super GraphObjectsBatch> action)
                    {
                        long currentBatch = batchIndex.incrementAndGet();
                        GraphObjectsBatch inMemoryGraphObjectsBatch = new GraphObjectsBatch(currentBatch, executionState.getGraphFetchBatchMemoryLimit());
                        List<Object> resultObjects = new ArrayList<>();
                        int objectCount = 0;

                        if (checked)
                        {
                            while (sourceObjectsIterator.hasNext())
                            {
                                IChecked<?> checkedSource = (IChecked<?>) sourceObjectsIterator.next();
                                Object value = checkedSource.getValue();
                                if (value == null)
                                {
                                    resultObjects.add(newDynamicChecked(Collections.singletonList(BasicDefect.newNoInputDefect(_class)), checkedSource, null));
                                }
                                else
                                {
                                    Object targetObject = nodeSpecifics.transform(value);
                                    if (targetObject != null)
                                    {
                                        if (targetObject instanceof List)
                                        {
                                            ((List<?>) targetObject).forEach(x ->
                                            {
                                                IGraphInstance<?> target = (IGraphInstance<?>) x;
                                                inMemoryGraphObjectsBatch.addObjectMemoryUtilization(target.instanceSize());
                                                resultObjects.add(newDynamicChecked(Collections.emptyList(), checkedSource, target.getValue()));
                                            });
                                        }
                                        else
                                        {
                                            IGraphInstance<?> target = (IGraphInstance<?>) targetObject;
                                            inMemoryGraphObjectsBatch.addObjectMemoryUtilization(target.instanceSize());
                                            resultObjects.add(newDynamicChecked(Collections.emptyList(), checkedSource, target.getValue()));
                                        }
                                    }
                                }

                                objectCount += 1;
                                if (objectCount >= batchSize)
                                {
                                    break;
                                }
                            }
                        }
                        else
                        {
                            while (sourceObjectsIterator.hasNext())
                            {
                                Object targetObject = nodeSpecifics.transform(sourceObjectsIterator.next());

                                if (targetObject != null)
                                {
                                    if (targetObject instanceof List)
                                    {
                                        ((List<?>) targetObject).forEach(x ->
                                        {
                                            IGraphInstance<?> target = (IGraphInstance<?>) x;
                                            inMemoryGraphObjectsBatch.addObjectMemoryUtilization(target.instanceSize());
                                            resultObjects.add(target.getValue());
                                        });
                                    }
                                    else
                                    {
                                        IGraphInstance<?> target = (IGraphInstance<?>) targetObject;
                                        inMemoryGraphObjectsBatch.addObjectMemoryUtilization(target.instanceSize());
                                        resultObjects.add(target.getValue());
                                    }
                                }

                                objectCount += 1;
                                if (objectCount >= batchSize)
                                {
                                    break;
                                }
                            }
                        }

                        inMemoryGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, resultObjects);
                        ExecutionState newState = new ExecutionState(executionState);

                        if (!resultObjects.isEmpty() && (!isLeaf))
                        {
                            newState.graphObjectsBatch = inMemoryGraphObjectsBatch;
                            node.children.forEach(x -> x.accept(new ExecutionNodeExecutor(InMemoryExecutionNodeExecutor.this.identity, newState)));

                        }
                        if (!resultObjects.isEmpty() && node.filter != null  && node.filter)
                        {
                            List<Object> updated = resultObjects.stream().filter(f ->
                                    nodeSpecifics.filter(f, context)
                            ).filter(Objects::nonNull).collect(Collectors.toList());

                            inMemoryGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, updated);
                        }
                        action.accept(inMemoryGraphObjectsBatch);

                        return objectCount != 0;
                    }
                };

                Stream<GraphObjectsBatch> graphObjectsBatchStream = StreamSupport.stream(graphObjectsBatchSpliterator, false);


                return new GraphFetchResult(graphObjectsBatchStream, childResult).withGraphFetchSpan(graphFetchSpan);
            }
        }
        catch (Exception e)
        {
            if (childResult != null)
            {
                childResult.close();
            }
            if (graphFetchSpan != null)
            {
                graphFetchSpan.finish();
            }

            if (e instanceof RuntimeException)
            {
                throw e;
            }

            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode node)
    {
        // childObjects are the child nodes that should be processed next as part of the graph traversal
        List<Object> childObjects = new ArrayList<>();
        Result childResult = null;

        try
        {
            IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.identity);

            GraphObjectsBatch graphObjectsBatch = new GraphObjectsBatch(this.executionState.graphObjectsBatch);
            List<?> parentObjects = graphObjectsBatch.getObjectsForNodeIndex(node.parentIndex);

            if ((parentObjects != null) && !parentObjects.isEmpty())
            {
                DoubleStrategyHashMap<Object, List<Object>, Object> parentMap = new DoubleStrategyHashMap<>(InMemoryGraphFetchUtils.parentChildDoubleHashStrategy(nodeSpecifics));
                parentObjects.forEach(parentObject -> parentMap.getIfAbsentPut(parentObject, ArrayList::new).add(parentObject));

                if (node.supportsBatching)
                {
                    Map<String, List<Object>> keyValuePairs = Maps.mutable.empty();

                    nodeSpecifics.getCrossStoreKeysValueForChildren(parentObjects.get(0)).keySet().forEach(key -> keyValuePairs.put(key, Lists.mutable.empty()));
                    parentMap.keySet().forEach(parentObject -> nodeSpecifics.getCrossStoreKeysValueForChildren(parentObject).forEach((key, value) -> keyValuePairs.get(key).add(value)));

                    keyValuePairs.forEach((key, value) -> this.executionState.addResult(key, new ConstantResult(value)));
                    childResult = this.visit((InMemoryRootGraphFetchExecutionNode) node);
                    GraphFetchResult childGraphFetchResult = (GraphFetchResult) childResult;
                    Stream<GraphObjectsBatch> graphObjectsBatchStream = childGraphFetchResult.getGraphObjectsBatchStream();

                    graphObjectsBatchStream.forEach(batch ->
                    {
                        batch.getObjectsForNodeIndex(node.nodeIndex).forEach(child ->
                        {
                            IGraphInstance<?> childGraphInstance = nodeSpecifics.wrapChildInGraphInstance(child);
                            Object childObject = childGraphInstance.getValue();

                            List<Object> parentsInScope = parentMap.getWithSecondKey(childObject);

                            if (parentsInScope != null)
                            {
                                for (Object parentObject : parentsInScope)
                                {
                                    boolean isChildAdded = nodeSpecifics.attemptAddingChildToParent(parentObject, childObject);

                                    if (isChildAdded)
                                    {
                                        graphObjectsBatch.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                                        childObjects.add(childObject);
                                    }
                                }
                            }
                        });
                    });
                }
                else
                {
                    // set up to enable caching
                    boolean cachingEnabled = false;
                    ExecutionCache<GraphFetchCacheKey, List<Object>> childCrossCache = graphObjectsBatch.getXStorePropertyCacheForNodeIndex(node.nodeIndex);
                    List<Method> parentCrossKeyGettersOrderedPerTargetProperties = null;

                    if (childCrossCache != null)
                    {
                        cachingEnabled = true;
                        parentCrossKeyGettersOrderedPerTargetProperties = nodeSpecifics.parentCrossKeyGettersOrderedByTargetProperties();
                    }

                    for (Map.Entry<Object, List<Object>> entry : parentMap.entrySet())
                    {
                        // if source-level batched query, need to track the added children
                        List<Object> entryChildren = new ArrayList<>();
                        boolean cacheHit = false;
                        Object parent = entry.getKey();
                        List<Object> parentsInScope = entry.getValue();
                        if (cachingEnabled)
                        {
                            List<Object> cachedChildren = childCrossCache.getIfPresent(new InMemoryGraphFetchUtils.InMemoryCrossObjectGraphFetchCacheKey(parent, parentCrossKeyGettersOrderedPerTargetProperties));
                            if (cachedChildren != null)
                            {
                                cacheHit = true;
                                parentsInScope.forEach(parentObject -> cachedChildren.forEach(cachedChild -> nodeSpecifics.attemptAddingChildToParent(parentObject, cachedChild)));
                            }
                        }

                        // if there are any parents for which children were not found in the cache, need to visit
                        if (!cacheHit)
                        {
                            // Retrieve all foreign keys that need to be matched in the cross-store
                            Map<String, Object> keyValuePairs = nodeSpecifics.getCrossStoreKeysValueForChildren(parent);
                            // executionState is the shared memory used to pass parameters and intermediate results
                            keyValuePairs.forEach((key, value) -> this.executionState.addResult(key, new ConstantResult(value)));
                            // visit results in the REST api call
                            childResult = this.visit((InMemoryRootGraphFetchExecutionNode) node);
                            GraphFetchResult childGraphFetchResult = (GraphFetchResult) childResult;
                            Stream<GraphObjectsBatch> graphObjectsBatchStream = childGraphFetchResult.getGraphObjectsBatchStream();
                            graphObjectsBatchStream.forEach(batch ->
                                    batch.getObjectsForNodeIndex(node.nodeIndex).forEach(child ->
                                    {
                                        boolean childAdded = false;
                                        for (Object parentObject : parentsInScope)
                                        {
                                            if (nodeSpecifics.attemptAddingChildToParent(parentObject, child))
                                            {
                                                if (!childAdded)
                                                {
                                                    entryChildren.add(child);
                                                    childAdded = true;
                                                }
                                            }
                                        }
                                    }));

                            // update the cache with the results if cachingEnabled
                            if (cachingEnabled)
                            {
                                childCrossCache.put(
                                        new InMemoryGraphFetchUtils.InMemoryCrossObjectGraphFetchCacheKey(parent, parentCrossKeyGettersOrderedPerTargetProperties),
                                        entryChildren
                                );
                            }

                            for (Object entryChild : entryChildren)
                            {
                                IGraphInstance<?> crossChildGraphInstance = nodeSpecifics.wrapChildInGraphInstance(entryChild);
                                graphObjectsBatch.addObjectMemoryUtilization(crossChildGraphInstance.instanceSize());
                                childObjects.add(entryChild);
                            }
                        }
                    }
                }
                graphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);
            }

            return new ConstantResult(childObjects);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (childResult != null)
            {
                childResult.close();
            }
        }
    }

    @Override
    public Result visit(InMemoryPropertyGraphFetchExecutionNode node)
    {
        boolean isLeaf = node.children == null || node.children.isEmpty();
        IInMemoryPropertyGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.identity);

        GraphObjectsBatch graphObjectsBatch = this.executionState.graphObjectsBatch;
        List<?> parentObjects = graphObjectsBatch.getObjectsForNodeIndex(node.parentIndex).stream().map(x -> x instanceof IChecked ? ((IChecked<?>) x).getValue() : x).collect(Collectors.toList());

        Stream<IGraphInstance> childGraphInstancesStream = nodeSpecifics.transformProperty(parentObjects);
        List<Object> childObjects = childGraphInstancesStream.filter(Objects::nonNull).map(graphInstance ->
        {
            graphObjectsBatch.addObjectMemoryUtilization(graphInstance.instanceSize());
            return graphInstance.getValue();
        }).collect(Collectors.toList());

        graphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);

        if (!childObjects.isEmpty() && (!isLeaf))
        {
            node.children.forEach(x -> x.accept(new ExecutionNodeExecutor(this.identity, executionState)));
        }

        return new ConstantResult(childObjects);
    }

    private static <T> IChecked<T> newDynamicChecked(List<IDefect> defects, Object source, T value)
    {
        return new IChecked<T>()
        {

            @Override
            public List<IDefect> getDefects()
            {
                return defects;
            }

            @Override
            public Object getSource()
            {
                return source;
            }

            @Override
            public T getValue()
            {
                return value;
            }
        };
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchExecutionNode graphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(StoreMappingGlobalGraphFetchExecutionNode storeMappingGlobalGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ErrorExecutionNode errorExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(SequenceExecutionNode sequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FunctionParametersValidationNode functionParametersValidationNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AllocationExecutionNode allocationExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(PureExpressionPlatformExecutionNode pureExpressionPlatformExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ConstantExecutionNode constantExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(LocalGraphFetchExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FreeMarkerConditionalExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    private Result mergeInMemoryNode(InMemoryRootGraphFetchExecutionNode node)
    {
        IInMemoryRootGraphFetchMergeExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.identity);

        List<GraphFetchResult> results = node.executionNodes.stream().map(n -> (GraphFetchResult) n.accept(new ExecutionNodeExecutor(this.identity, this.executionState))).collect(Collectors.toList());

        List<Object> subObjects = results.stream().map(g -> g.getGraphObjectsBatchStream().findFirst().get().getObjectsForNodeIndex(0).get(0)).collect(Collectors.toList());


        Object targetObject = nodeSpecifics.transform(subObjects); //merged object

        Spliterator<GraphObjectsBatch> graphObjectsBatchSpliterator = new Spliterators.AbstractSpliterator<GraphObjectsBatch>(Long.MAX_VALUE, Spliterator.ORDERED)
        {
            AtomicLong batchIndex = new AtomicLong(0L);

            @Override
            public boolean tryAdvance(Consumer<? super GraphObjectsBatch> action)
            {
                long currentBatch = batchIndex.incrementAndGet();

                if (currentBatch > 1)  //run only once
                {
                    return false;
                }

                List<Object> resultObjects = new ArrayList<>();
                GraphObjectsBatch inMemoryGraphObjectsBatch = new GraphObjectsBatch(currentBatch, executionState.getGraphFetchBatchMemoryLimit());
                IGraphInstance<?> target = (IGraphInstance<?>) targetObject;
                inMemoryGraphObjectsBatch.addObjectMemoryUtilization(target.instanceSize());
                resultObjects.add(target.getValue());
                inMemoryGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, resultObjects);
                action.accept(inMemoryGraphObjectsBatch);


                return false;
            }
        };

        Stream<GraphObjectsBatch> graphObjectsBatchStream = StreamSupport.stream(graphObjectsBatchSpliterator, false);
        return new GraphFetchResult(graphObjectsBatchStream, new ConstantResult(targetObject));
    }
}
