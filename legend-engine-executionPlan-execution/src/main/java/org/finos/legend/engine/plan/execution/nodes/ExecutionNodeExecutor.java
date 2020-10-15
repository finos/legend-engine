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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
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
import org.finos.legend.engine.plan.execution.result.builder._class.PartialClassBuilder;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.SerializationConfig;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final Subject subject;
    private final ExecutionState executionState;

    public ExecutionNodeExecutor(Subject subject, ExecutionState executionState)
    {
        this.subject = subject;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        return this.executionState.extraNodeExecutors.stream().map(executor -> executor.value(executionNode, subject, executionState)).filter(Objects::nonNull).findFirst().orElseThrow(() -> new UnsupportedOperationException("Unsupported execution node type '" + executionNode.getClass().getSimpleName() + "'"));
    }

    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        return graphFetchM2MExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.InMemory).getVisitor(this.subject, this.executionState));
    }

    @Override
    public Result visit(ErrorExecutionNode errorExecutionNode)
    {
        Result payload = (errorExecutionNode.executionNodes() == null || errorExecutionNode.executionNodes().isEmpty()) ? null : errorExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(this.subject, this.executionState)).realizeInMemory();
        return new ErrorResult(1, errorExecutionNode.message, payload);
    }

    @Override
    public Result visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode)
    {
        Map<String, Result> subResults = Maps.mutable.empty();
        Result last = null;
        for (ExecutionNode n : multiResultSequenceExecutionNode.executionNodes())
        {
            last = n.accept(new ExecutionNodeExecutor(this.subject, this.executionState));
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
        Result result = allocationExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(this.subject, new ExecutionState(this.executionState).varName(varName)));
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
        Class<?> clazz = ExecutionNodeJavaPlatformHelper.getClassToExecute(pureExpressionPlatformExecutionNode, executionClassName, this.executionState, this.subject);
        if (Arrays.asList(clazz.getInterfaces()).contains(IPlatformPureExpressionExecutionNodeSerializeSpecifics.class))
        {
            try
            {
                org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics nodeSpecifics = (org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics) clazz.newInstance();
                Result childResult = pureExpressionPlatformExecutionNode.executionNodes().getFirst().accept(new ExecutionNodeExecutor(subject, executionState));
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
        else
        {
            return ExecutionNodeJavaPlatformHelper.executeJavaImplementation(pureExpressionPlatformExecutionNode, DefaultExecutionNodeContext.factory(), this.subject, this.executionState);
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
            return freeMarkerConditionalExecutionNode.trueBlock.accept(new ExecutionNodeExecutor(this.subject, this.executionState));
        }
        else if (freeMarkerConditionalExecutionNode.falseBlock != null)
        {
            return freeMarkerConditionalExecutionNode.falseBlock.accept(new ExecutionNodeExecutor(this.subject, this.executionState));
        }
        else
        {
            return new ConstantResult("success");
        }
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        return aggregationAwareExecutionNode.accept(this.executionState.getStoreExecutionState(StoreType.Relational).getVisitor(this.subject, this.executionState));
    }

    @Override
    public Result visit(GraphFetchExecutionNode graphFetchExecutionNode)
    {
        final Span topSpan = GlobalTracer.get().activeSpan();
        try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query: Execute").startActive(true))
        {
            Result rootResult;
            try (Scope ignored2 = GlobalTracer.get().buildSpan("Graph Query: Execute Root").startActive(true))
            {
                rootResult = graphFetchExecutionNode.rootExecutionNode.accept(new ExecutionNodeExecutor(subject, executionState));
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

                    Stream<?> transformedResult = ExecutionNodeJavaPlatformHelper.executeStaticJavaMethod(graphFetchExecutionNode, executionClassName, executionMethodName, Arrays.asList(StreamingObjectResult.class, ExecutionNode.class, ExecutionState.class, Subject.class), Arrays.asList(objectResult, graphFetchExecutionNode, this.executionState, this.subject), this.executionState, this.subject);
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
                                        ConstantResult constantResult = (ConstantResult) rootLocalNode.accept(new ExecutionNodeExecutor(ExecutionNodeExecutor.this.subject, graphExecutionState));
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
        globalGraphFetchExecutionNode.localGraphFetchExecutionNode.accept(new ExecutionNodeExecutor(subject, graphExecutionState));

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
        throw new RuntimeException("Not implemented!");
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
            Result temp = this.executionState.extraSequenceNodeExecutors.stream().map(executor -> executor.value(node, this.subject, this.executionState)).filter(Objects::nonNull).findFirst().orElse(null);
            if (temp == null)
            {
                last = node.accept(new ExecutionNodeExecutor(this.subject, this.executionState));
            }
        }
        return last;
    }
}