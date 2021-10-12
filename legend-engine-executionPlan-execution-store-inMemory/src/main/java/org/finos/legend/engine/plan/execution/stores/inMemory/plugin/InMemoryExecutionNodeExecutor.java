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

package org.finos.legend.engine.plan.execution.stores.inMemory.plugin;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryPropertyGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryRootGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IStoreStreamReadingExecutionNodeSpecifics;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphFetchResult;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.result.graphFetch.StoreStreamReadingResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.utils.InMemoryGraphFetchUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AggregationAwareExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AllocationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ConstantExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ErrorExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FreeMarkerConditionalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.GraphFetchM2MExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.MultiResultSequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.PureExpressionPlatformExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ClassResultType;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleStrategyHashMap;
import org.pac4j.core.profile.CommonProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InMemoryExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    MutableList<CommonProfile> pm;
    ExecutionState executionState;

    public InMemoryExecutionNodeExecutor(MutableList<CommonProfile> pm, ExecutionState executionState)
    {
        this.pm = pm;
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
        return ExecutionNodeJavaPlatformHelper.executeJavaImplementation(graphFetchM2MExecutionNode, GraphFetchM2MExecutionNodeContext.factory(graphFetchM2MExecutionNode), this.pm, this.executionState);
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode node)
    {
        IStoreStreamReadingExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.pm);
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

        try
        {
            IInMemoryRootGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.pm);

            childResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.pm, this.executionState));
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
                                        ((List<?>) targetObject).forEach(x -> {
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
                            if (objectCount >= batchSize) break;
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
                                    ((List<?>) targetObject).forEach(x -> {
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
                            if (objectCount >= batchSize) break;
                        }
                    }

                    inMemoryGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, resultObjects);

                    if (!resultObjects.isEmpty() && (!isLeaf))
                    {
                        ExecutionState newState = new ExecutionState(executionState);
                        newState.graphObjectsBatch = inMemoryGraphObjectsBatch;
                        node.children.forEach(x -> x.accept(new ExecutionNodeExecutor(InMemoryExecutionNodeExecutor.this.pm, newState)));
                    }

                    action.accept(inMemoryGraphObjectsBatch);

                    return objectCount != 0;
                }
            };

            Stream<GraphObjectsBatch> graphObjectsBatchStream = StreamSupport.stream(graphObjectsBatchSpliterator, false);

            return new GraphFetchResult(graphObjectsBatchStream, childResult);
        }
        catch (Exception e)
        {
            if (childResult != null)
            {
                childResult.close();
            }

            if (e instanceof RuntimeException)
            {
                throw e;
            }

            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode node)
    {
        List<Object> childObjects = new ArrayList<>();
        Result childResult = null;

        try
        {
            IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.pm);

            GraphObjectsBatch graphObjectsBatch = new GraphObjectsBatch(this.executionState.graphObjectsBatch);
            List<?> parentObjects = graphObjectsBatch.getObjectsForNodeIndex(node.parentIndex);

            if ((parentObjects != null) && !parentObjects.isEmpty())
            {
                if (node.supportsBatching)
                {
                    DoubleStrategyHashMap<Object, List<Object>, Object> parentMap = new DoubleStrategyHashMap<>(InMemoryGraphFetchUtils.parentChildDoubleHashStrategy(nodeSpecifics));
                    Map<String, List<Object>> keyValuePairs = Maps.mutable.empty();

                    nodeSpecifics.getCrossStoreKeysValueForChildren(parentObjects.get(0)).keySet().forEach(key -> keyValuePairs.put(key, Lists.mutable.empty()));
                    parentObjects.forEach(parentObject -> {
                        parentMap.getIfAbsentPut(parentObject, ArrayList::new).add(parentObject);
                        nodeSpecifics.getCrossStoreKeysValueForChildren(parentObject).forEach((key, value) -> keyValuePairs.get(key).add(value));
                    });

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

                            if(parentsInScope == null)
                            {
                                throw new RuntimeException("No parent was found for a child object");
                            }
                            for (Object parentObject : parentsInScope)
                            {
                                boolean isChildAdded = nodeSpecifics.attemptAddingChildToParent(parentObject, childObject);

                                if (isChildAdded)
                                {
                                    graphObjectsBatch.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                                    childObjects.add(childObject);
                                }
                            }
                        });
                    });
                }
                else
                {
                    for (Object parentObject : parentObjects)
                    {
                        Map<String, Object> keyValuePairs = nodeSpecifics.getCrossStoreKeysValueForChildren(parentObject);

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
                                boolean isChildAdded = nodeSpecifics.attemptAddingChildToParent(parentObject, childObject);

                                if (isChildAdded)
                                {
                                    graphObjectsBatch.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                                    childObjects.add(childObject);
                                }
                            });
                        });
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
        IInMemoryPropertyGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.pm);

        GraphObjectsBatch graphObjectsBatch = this.executionState.graphObjectsBatch;
        List<?> parentObjects = graphObjectsBatch.getObjectsForNodeIndex(node.parentIndex).stream().map(x -> x instanceof IChecked ? ((IChecked<?>) x).getValue() : x).collect(Collectors.toList());

        Stream<IGraphInstance> childGraphInstancesStream = nodeSpecifics.transformProperty(parentObjects);
        List<Object> childObjects = childGraphInstancesStream.filter(Objects::nonNull).map(graphInstance -> {
            graphObjectsBatch.addObjectMemoryUtilization(graphInstance.instanceSize());
            return graphInstance.getValue();
        }).collect(Collectors.toList());

        graphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);

        if (!childObjects.isEmpty() && (!isLeaf))
        {
            node.children.forEach(x -> x.accept(new ExecutionNodeExecutor(this.pm, executionState)));
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
    public Result visit(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode)
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
}
