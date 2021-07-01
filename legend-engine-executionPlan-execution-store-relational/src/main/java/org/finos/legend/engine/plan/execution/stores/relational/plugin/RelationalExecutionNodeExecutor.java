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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalDatabaseCommandsVisitorBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.activity.AggregationAwareActivity;
import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnection;
import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnectionContext;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;

import org.finos.legend.engine.plan.execution.stores.relational.result.PreparedTempTableResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultColumn;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtension;
import org.finos.legend.engine.plan.execution.stores.relational.result.TempTableStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.graphFetch.RelationalGraphObjectsBatch;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalResult;
import org.finos.legend.engine.plan.dependencies.store.relational.classResult.IRelationalClassInstantiationNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.*;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByEqualityKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByTargetCrossKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.nodes.state.GraphExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.builder._class.ClassBuilder;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphFetchResult;
import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.result.FunctionHelper;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ClassResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.GraphFetchTree;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.pac4j.core.profile.CommonProfile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RelationalExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final ExecutionState executionState;
    private final MutableList<CommonProfile> profiles;
    private MutableList<Function2<ExecutionState, List<Map<String, Object>>,Result>> resultInterpreterExtensions;

    public RelationalExecutionNodeExecutor(ExecutionState executionState, MutableList<CommonProfile> profiles)
    {
        this.executionState = executionState;
        this.profiles = profiles;
        this.resultInterpreterExtensions = Iterate.addAllTo(ServiceLoader.load(ResultInterpreterExtension.class), Lists.mutable.empty()).collect(ResultInterpreterExtension::additionalResultBuilder);
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof RelationalBlockExecutionNode)
        {
            RelationalBlockExecutionNode relationalBlockExecutionNode = (RelationalBlockExecutionNode) executionNode;
            ExecutionState connectionAwareState = new ExecutionState(this.executionState);
            ((RelationalStoreExecutionState) connectionAwareState.getStoreExecutionState(StoreType.Relational)).setRetainConnection(true);
            try
            {
                Result res = new ExecutionNodeExecutor(this.profiles, connectionAwareState).visit((SequenceExecutionNode) relationalBlockExecutionNode);
                ((RelationalStoreExecutionState) connectionAwareState.getStoreExecutionState(StoreType.Relational)).getBlockConnectionContext().unlockAllBlockConnections();
                return res;
            }
            catch (Exception e)
            {
                ((RelationalStoreExecutionState) connectionAwareState.getStoreExecutionState(StoreType.Relational)).getBlockConnectionContext().unlockAllBlockConnections();
                ((RelationalStoreExecutionState) connectionAwareState.getStoreExecutionState(StoreType.Relational)).getBlockConnectionContext().closeAllBlockConnections();
                throw e;
            }
        }
        else if (executionNode instanceof CreateAndPopulateTempTableExecutionNode)
        {
            CreateAndPopulateTempTableExecutionNode createAndPopulateTempTableExecutionNode = (CreateAndPopulateTempTableExecutionNode) executionNode;
            Stream<Result> results = createAndPopulateTempTableExecutionNode.inputVarNames.stream().map(this.executionState::getResult);
            Stream<?> inputStream = results.flatMap(result ->
            {
                if (result instanceof ConstantResult)
                {
                    Object value = ((ConstantResult) result).getValue();
                    if (value instanceof Map && ((Map<?, ?>) value).get("values") instanceof List)
                    {
                        return ((List<?>) ((Map<?, ?>) value).get("values")).stream().map(val -> ((List<?>) ((Map<?, ?>) val).get("values")).get(0));
                    }
                    if (value instanceof List)
                    {
                        return ((List<?>) value).stream();
                    }

                    if (ClassUtils.isPrimitiveOrWrapper(value.getClass()) || (value instanceof String))
                    {
                        return Stream.of(value);
                    }

                    if (value instanceof Stream)
                    {
                        return (Stream<?>) value;
                    }

                    throw new IllegalArgumentException("Result passed into CreateAndPopulateTempTableExecutionNode should be a stream");
                }
                if (result instanceof StreamingObjectResult)
                {
                    return ((StreamingObjectResult<?>) result).getObjectStream();
                }
                throw new IllegalArgumentException("Unexpected Result Type : " + result.getClass().getName());
            });

            if (!(createAndPopulateTempTableExecutionNode.implementation instanceof JavaPlatformImplementation))
            {
                throw new RuntimeException("Only Java implementations are currently supported, found: " + createAndPopulateTempTableExecutionNode.implementation);
            }

            JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) createAndPopulateTempTableExecutionNode.implementation;
            String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
            Class<?> clazz = ExecutionNodeJavaPlatformHelper.getClassToExecute(createAndPopulateTempTableExecutionNode, executionClassName, this.executionState, this.profiles);
            if (Arrays.asList(clazz.getInterfaces()).contains(IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics.class))
            {
                try
                {
                    IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics nodeSpecifics = (IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics) clazz.newInstance();
                    createAndPopulateTempTableExecutionNode.tempTableColumnMetaData.forEach(t -> t.identifierForGetter = nodeSpecifics.getGetterNameForProperty(t.identifierForGetter));
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
            {
                // TODO Remove once platform version supports above and existing plans mitigated
                String executionMethodName = JavaHelper.getExecutionMethodName(javaPlatformImpl);
                createAndPopulateTempTableExecutionNode.tempTableColumnMetaData.forEach(t -> t.identifierForGetter = ExecutionNodeJavaPlatformHelper.executeStaticJavaMethod(createAndPopulateTempTableExecutionNode, executionClassName,
                        executionMethodName, Collections.singletonList(Result.class), Collections.singletonList(new ConstantResult(t.identifierForGetter)), this.executionState, this.profiles));
            }

            RelationalDatabaseCommands databaseCommands = DatabaseManager.fromString(createAndPopulateTempTableExecutionNode.connection.type.name()).relationalDatabaseSupport();
            try (Connection connectionManagerConnection = this.getConnection(createAndPopulateTempTableExecutionNode, databaseCommands, this.profiles, this.executionState))
            {
                TempTableStreamingResult tempTableStreamingResult = new TempTableStreamingResult(inputStream, createAndPopulateTempTableExecutionNode);
                String databaseTimeZone = createAndPopulateTempTableExecutionNode.connection.timeZone == null ? RelationalExecutor.DEFAULT_DB_TIME_ZONE : createAndPopulateTempTableExecutionNode.connection.timeZone;
                databaseCommands.accept(RelationalDatabaseCommandsVisitorBuilder.getStreamResultToTempTableVisitor(((RelationalStoreExecutionState) this.executionState.getStoreExecutionState(StoreType.Relational)).getRelationalExecutor().getRelationalExecutionConfiguration(), connectionManagerConnection, tempTableStreamingResult, createAndPopulateTempTableExecutionNode.tempTableName, databaseTimeZone));
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
            return new ConstantResult("success");
        }
        else if (executionNode instanceof RelationalExecutionNode)
        {
            RelationalExecutionNode relationalExecutionNode = (RelationalExecutionNode) executionNode;
            Span topSpan = GlobalTracer.get().activeSpan();
            this.executionState.topSpan = topSpan;
            try (Scope scope = GlobalTracer.get().buildSpan("Relational DB Execution").startActive(true))
            {
                scope.span().setTag("databaseType", relationalExecutionNode.getDatabaseTypeName());
                scope.span().setTag("sql", relationalExecutionNode.sqlQuery());
                Result result = ((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)).getRelationalExecutor().execute(relationalExecutionNode, this.profiles, this.executionState);
                if (result instanceof RelationalResult)
                {
                    scope.span().setTag("executedSql", ((RelationalResult) result).executedSQl);
                }
                if (relationalExecutionNode.implementation != null && !(ExecutionNodeResultHelper.isResultSizeRangeSet(relationalExecutionNode) && ExecutionNodeResultHelper.isSingleRecordResult(relationalExecutionNode)))
                {
                    return executeImplementation(relationalExecutionNode, result, this.executionState, this.profiles);
                }
                return result;
            }
        }
        else if (executionNode instanceof SQLExecutionNode)
        {
            SQLExecutionNode SQLExecutionNode = (SQLExecutionNode) executionNode;
            this.executionState.topSpan = GlobalTracer.get().activeSpan();
            try (Scope scope = GlobalTracer.get().buildSpan("Relational DB Execution").startActive(true))
            {
                scope.span().setTag("databaseType", SQLExecutionNode.getDatabaseTypeName());
                scope.span().setTag("sql", SQLExecutionNode.sqlQuery());
                Result result = ((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)).getRelationalExecutor().execute(SQLExecutionNode, profiles, executionState);
                if (result instanceof SQLExecutionResult)
                {
                    scope.span().setTag("executedSql", ((SQLExecutionResult) result).getExecutedSql());
                }
                return result;
            }
        }
        else if (executionNode instanceof RelationalTdsInstantiationExecutionNode)
        {
            RelationalTdsInstantiationExecutionNode relationalTdsInstantiationExecutionNode = (RelationalTdsInstantiationExecutionNode) executionNode;
            SQLExecutionResult sqlExecutionResult = null;
            try
            {
                sqlExecutionResult = (SQLExecutionResult) this.visit((SQLExecutionNode) relationalTdsInstantiationExecutionNode.executionNodes.get(0));
                RelationalResult relationalTdsResult = new RelationalResult(sqlExecutionResult, relationalTdsInstantiationExecutionNode);

                if (this.executionState.inAllocation)
                {
                    if (!this.executionState.transformAllocation)
                    {
                        return relationalTdsResult;
                    }
                    RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) relationalTdsResult.realizeInMemory();
                    List<Map<String, Object>> rowValueMaps = realizedRelationalResult.getRowValueMaps(false);
                    Result res = RelationalExecutor.evaluateAdditionalExtractors(this.resultInterpreterExtensions, this.executionState, rowValueMaps);
                    if (res != null)
                    {
                        return res;
                    }
                    else
                    {
                        return new ConstantResult(rowValueMaps);
                    }
                }

                return relationalTdsResult;
            }
            catch (Exception e)
            {
                if (sqlExecutionResult != null)
                {
                    sqlExecutionResult.close();
                }
                throw e;
            }
        }
        else if (executionNode instanceof RelationalClassInstantiationExecutionNode)
        {
            RelationalClassInstantiationExecutionNode node = (RelationalClassInstantiationExecutionNode) executionNode;
            SQLExecutionResult sqlExecutionResult = null;
            try
            {
                SQLExecutionNode innerNode = (SQLExecutionNode) node.executionNodes.get(0);
                sqlExecutionResult = (SQLExecutionResult) this.visit(innerNode);
                RelationalResult relationalResult = new RelationalResult(sqlExecutionResult, node);

                boolean realizeAsConstant = this.executionState.inAllocation && ExecutionNodeResultHelper.isResultSizeRangeSet(node) && ExecutionNodeResultHelper.isSingleRecordResult(node);

                if (realizeAsConstant)
                {
                    RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) relationalResult.realizeInMemory();
                    List<Map<String, Object>> rowValueMaps = realizedRelationalResult.getRowValueMaps(false);
                    if (rowValueMaps.size() == 1)
                    {
                        return new ConstantResult(rowValueMaps.get(0));
                    }
                    else
                    {
                        return new ConstantResult(rowValueMaps);
                    }
                }

                return this.getStreamingObjectResultFromRelationalResult(node, relationalResult, innerNode.connection);
            }
            catch (Exception e)
            {
                if (sqlExecutionResult != null)
                {
                    sqlExecutionResult.close();
                }
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
            }
        }
        else if (executionNode instanceof RelationalRelationDataInstantiationExecutionNode)
        {
            RelationalRelationDataInstantiationExecutionNode node = (RelationalRelationDataInstantiationExecutionNode) executionNode;
            SQLExecutionResult sqlExecutionResult = null;
            try
            {
                sqlExecutionResult = (SQLExecutionResult) this.visit((SQLExecutionNode) node.executionNodes.get(0));
                return new RelationalResult(sqlExecutionResult, node);
            }
            catch (Exception e)
            {
                if (sqlExecutionResult != null)
                {
                    sqlExecutionResult.close();
                }
                throw e;
            }
        }
        else if (executionNode instanceof RelationalDataTypeInstantiationExecutionNode)
        {
            RelationalDataTypeInstantiationExecutionNode node = (RelationalDataTypeInstantiationExecutionNode) executionNode;
            SQLExecutionResult sqlExecutionResult = null;
            try
            {
                sqlExecutionResult = (SQLExecutionResult) this.visit((SQLExecutionNode) node.executionNodes.get(0));
                RelationalResult relationalPrimitiveResult = new RelationalResult(sqlExecutionResult, node);

                if (this.executionState.inAllocation)
                {
                    if ((ExecutionNodeResultHelper.isResultSizeRangeSet(node) && !ExecutionNodeResultHelper.isSingleRecordResult(node)) && !this.executionState.transformAllocation)
                    {
                        return relationalPrimitiveResult;
                    }

                    if (relationalPrimitiveResult.getResultSet().next())
                    {
                        List<org.eclipse.collections.api.block.function.Function<Object, Object>> transformers = relationalPrimitiveResult.getTransformers();
                        Object convertedValue = transformers.get(0).valueOf(relationalPrimitiveResult.getResultSet().getObject(1));
                        return new ConstantResult(convertedValue);
                    }
                    else
                    {
                        throw new RuntimeException("Result set is empty for allocation node");
                    }
                }
                return relationalPrimitiveResult;
            }
            catch (Exception e)
            {
                if (sqlExecutionResult != null)
                {
                    sqlExecutionResult.close();
                }
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
            }
        }
        else if (executionNode instanceof RelationalRootQueryTempTableGraphFetchExecutionNode)
        {
            return this.executeRelationalRootQueryTempTableGraphFetchExecutionNode((RelationalRootQueryTempTableGraphFetchExecutionNode) executionNode);
        }
        else if (executionNode instanceof RelationalCrossRootQueryTempTableGraphFetchExecutionNode)
        {
            return this.executeRelationalCrossRootQueryTempTableGraphFetchExecutionNode((RelationalCrossRootQueryTempTableGraphFetchExecutionNode) executionNode);
        }
        else if (executionNode instanceof RelationalPrimitiveQueryGraphFetchExecutionNode)
        {
            return this.executeRelationalPrimitiveQueryGraphFetchExecutionNode((RelationalPrimitiveQueryGraphFetchExecutionNode) executionNode);
        }
        else if (executionNode instanceof RelationalClassQueryTempTableGraphFetchExecutionNode)
        {
            return this.executeRelationalClassQueryTempTableGraphFetchExecutionNode((RelationalClassQueryTempTableGraphFetchExecutionNode) executionNode);
        }
        else if (executionNode instanceof RelationalRootGraphFetchExecutionNode)
        {
            RelationalRootGraphFetchExecutionNode node = (RelationalRootGraphFetchExecutionNode) executionNode;
            /* Fetch info from execution state */
            GraphExecutionState graphExecutionState = (GraphExecutionState) executionState;
            int batchSize = graphExecutionState.getBatchSize();
            SQLExecutionResult rootResult = (SQLExecutionResult) graphExecutionState.getRootResult();
            ResultSet rootResultSet = rootResult.getResultSet();

            /* Ensure all children run in the same connection */
            RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) graphExecutionState.getStoreExecutionState(StoreType.Relational);
            BlockConnectionContext oldBlockConnectionContext = relationalStoreExecutionState.getBlockConnectionContext();
            boolean oldRetainConnectionFlag = relationalStoreExecutionState.retainConnection();
            relationalStoreExecutionState.setBlockConnectionContext(new BlockConnectionContext());
            relationalStoreExecutionState.setRetainConnection(true);

            try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query Relational: Execute Relational Root").startActive(true))
            {
                String databaseTimeZone = rootResult.getDatabaseTimeZone();
                String databaseConnectionString = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(rootResult.getSQLExecutionNode().connection);

                /* Get Java executor */
                Class<?> executeClass = this.getExecuteClass(node);
                if (Arrays.asList(executeClass.getInterfaces()).contains(IRelationalRootGraphNodeExecutor.class))
                {
                    IRelationalRootGraphNodeExecutor executor = (IRelationalRootGraphNodeExecutor) executeClass.getConstructor().newInstance();
                    List<Method> primaryKeyGetters = executor.primaryKeyGetters();
                    int primaryKeyCount = primaryKeyGetters.size();

                    /* Check if caching is enabled and fetch the cache if required */
                    boolean cachingEnabledForNode = false;
                    ExecutionCache<GraphFetchCacheKey, Object> graphCache = null;
                    RelationalGraphFetchUtils.RelationalSQLResultGraphFetchCacheKey rootResultCacheKey = null;

                    if ((this.executionState.graphFetchCaches != null) && executor.supportsCaching())
                    {
                        GraphFetchCacheByEqualityKeys graphFetchCacheByEqualityKeys = RelationalGraphFetchUtils.findCacheByEqualityKeys(
                                node.graphFetchTree,
                                executor.getMappingId(rootResultSet, databaseTimeZone, databaseConnectionString),
                                executor.getInstanceSetId(rootResultSet, databaseTimeZone, databaseConnectionString),
                                this.executionState.graphFetchCaches
                        );

                        if (graphFetchCacheByEqualityKeys != null)
                        {
                            List<String> parentSQLKeyColumns = executor.primaryKeyColumns();
                            List<Integer> parentPrimaryKeyIndices = FastList.newList();
                            for (String pkCol : parentSQLKeyColumns)
                            {
                                parentPrimaryKeyIndices.add(rootResultSet.findColumn(pkCol));
                            }

                            cachingEnabledForNode = true;
                            graphCache = graphFetchCacheByEqualityKeys.getExecutionCache();
                            rootResultCacheKey = new RelationalGraphFetchUtils.RelationalSQLResultGraphFetchCacheKey(rootResult, parentPrimaryKeyIndices);
                        }
                    }

                    /* Get the next batch of root records */
                    List<Object> resultObjectsBatch = new ArrayList<>();
                    List<org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?>> instancesToDeepFetch = new ArrayList<>();

                    int objectCount = 0;

                    try (Scope ignored2 = GlobalTracer.get().buildSpan("Graph Query Relational: Read Next Batch").startActive(true))
                    {
                        while (rootResultSet.next())
                        {
                            graphExecutionState.incrementRowCount();

                            boolean shouldDeepFetchOnThisInstance = true;
                            if (cachingEnabledForNode)
                            {
                                Object cachedObject = graphCache.getIfPresent(rootResultCacheKey);
                                if (cachedObject != null)
                                {
                                    resultObjectsBatch.add(executor.deepCopy(cachedObject));
                                    shouldDeepFetchOnThisInstance = false;
                                }
                            }

                            if (shouldDeepFetchOnThisInstance)
                            {
                                org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> wrappedObject = executor.getObjectFromResultSet(rootResultSet, databaseTimeZone, databaseConnectionString);
                                instancesToDeepFetch.add(wrappedObject);
                                resultObjectsBatch.add(wrappedObject.getValue());
                            }
                            objectCount += 1;

                            if (objectCount >= batchSize)
                            {
                                break;
                            }
                        }
                    }

                    if (!instancesToDeepFetch.isEmpty())
                    {
                        boolean childrenExist = node.children != null && !node.children.isEmpty();

                        String tempTableName = node.tempTableName;
                        RealizedRelationalResult realizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.columns);

                        /* Create and populate double strategy map with key being object with its PK getters */
                        DoubleStrategyHashMap<Object, Object, SQLExecutionResult> rootMap = new DoubleStrategyHashMap<>(
                                RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters)
                        );
                        for (org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> rootGraphInstance : instancesToDeepFetch)
                        {
                            Object rootObject = rootGraphInstance.getValue();
                            rootMap.put(rootObject, rootObject);
                            graphExecutionState.addObjectMemoryUtilization(rootGraphInstance.instanceSize());
                            if (childrenExist)
                            {
                                this.addKeyRowToRealizedRelationalResult(rootObject, primaryKeyGetters, realizedRelationalResult);
                            }
                        }

                        /* Execute store local children */
                        if (childrenExist)
                        {
                            this.executeRelationalChildren(node, tempTableName, realizedRelationalResult, rootResult.getSQLExecutionNode().connection, rootResult.getDatabaseType(), databaseTimeZone, rootMap, primaryKeyGetters);
                        }
                    }

                    graphExecutionState.setObjectsForNodeIndex(node.nodeIndex, resultObjectsBatch);

                    if (cachingEnabledForNode)
                    {
                        for (org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> deepFetchedInstance : instancesToDeepFetch)
                        {
                            Object objectClone = executor.deepCopy(deepFetchedInstance.getValue());
                            graphCache.put(new RelationalGraphFetchUtils.RelationalObjectGraphFetchCacheKey(objectClone, primaryKeyGetters), objectClone);
                        }
                    }
                    return new ConstantResult(resultObjectsBatch);
                }
                else
                {
                    throw new RuntimeException("Unknown execute class " + executeClass.getCanonicalName());
                }
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
                relationalStoreExecutionState.getBlockConnectionContext().unlockAllBlockConnections();
                relationalStoreExecutionState.getBlockConnectionContext().closeAllBlockConnectionsAsync();
                relationalStoreExecutionState.setBlockConnectionContext(oldBlockConnectionContext);
                relationalStoreExecutionState.setRetainConnection(oldRetainConnectionFlag);
            }
        }
        else if (executionNode instanceof RelationalCrossRootGraphFetchExecutionNode)
        {
            RelationalCrossRootGraphFetchExecutionNode node = (RelationalCrossRootGraphFetchExecutionNode) executionNode;
            GraphExecutionState graphExecutionState = (GraphExecutionState) executionState;
            List<?> parentObjects = graphExecutionState.getObjectsToGraphFetch();

            List<Object> childObjects = FastList.newList();
            graphExecutionState.setObjectsForNodeIndex(node.nodeIndex, childObjects);

            RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) graphExecutionState.getStoreExecutionState(StoreType.Relational);
            BlockConnectionContext oldBlockConnectionContext = relationalStoreExecutionState.getBlockConnectionContext();
            boolean oldRetainConnectionFlag = relationalStoreExecutionState.retainConnection();
            relationalStoreExecutionState.setBlockConnectionContext(new BlockConnectionContext());
            relationalStoreExecutionState.setRetainConnection(true);

            SQLExecutionResult childResult = null;
            try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query Relational: Execute Relational Cross Root").startActive(true))
            {
                /* Get Java executor */
                Class<?> executeClass = this.getExecuteClass(node);
                if (Arrays.asList(executeClass.getInterfaces()).contains(IRelationalCrossRootGraphNodeExecutor.class))
                {
                    IRelationalCrossRootGraphNodeExecutor executor = (IRelationalCrossRootGraphNodeExecutor) executeClass.getConstructor().newInstance();

                    if (!parentObjects.isEmpty())
                    {
                        String parentTempTableName = node.parentTempTableName;
                        RealizedRelationalResult parentRealizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.parentTempTableColumns);

                        List<Method> crossKeyGetters = executor.parentCrossKeyGetters();
                        int parentKeyCount = crossKeyGetters.size();

                        for (Object parentObject : parentObjects)
                        {
                            this.addKeyRowToRealizedRelationalResult(parentObject, crossKeyGetters, parentRealizedRelationalResult);
                        }
                        graphExecutionState.addResult(parentTempTableName, parentRealizedRelationalResult);

                        /* Execute relational node corresponding to the cross root */
                        childResult = (SQLExecutionResult) node.relationalNode.accept(new ExecutionNodeExecutor(this.profiles, graphExecutionState));
                        ResultSet childResultSet = childResult.getResultSet();

                        boolean childrenExist = node.children != null && !node.children.isEmpty();


                        String tempTableName = childrenExist ? node.tempTableName : null;
                        RealizedRelationalResult realizedRelationalResult = childrenExist ?
                                RealizedRelationalResult.emptyRealizedRelationalResult(node.columns) :
                                null;
                        DatabaseConnection databaseConnection = childResult.getSQLExecutionNode().connection;
                        String databaseType = childResult.getDatabaseType();
                        String databaseTimeZone = childResult.getDatabaseTimeZone();


                        List<String> parentSQLKeyColumns = executor.parentSQLColumnsInResultSet(childResult.getResultColumns().stream().map(ResultColumn::getNonQuotedLabel).collect(Collectors.toList()));
                        List<Integer> parentCrossKeyIndices = FastList.newList();
                        for (String pkCol : parentSQLKeyColumns)
                        {
                            parentCrossKeyIndices.add(childResultSet.findColumn(pkCol));
                        }

                        DoubleStrategyHashMap<Object, List<Object>, SQLExecutionResult> parentMap = new DoubleStrategyHashMap<>(
                                RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategy(crossKeyGetters, parentCrossKeyIndices)
                        );

                        for (Object parentObject : parentObjects)
                        {
                            List<Object> mapped = parentMap.get(parentObject);
                            if (mapped == null)
                            {
                                parentMap.put(parentObject, FastList.newListWith(parentObject));
                            }
                            else
                            {
                                mapped.add(parentObject);
                            }
                        }

                        List<Method> primaryKeyGetters = executor.primaryKeyGetters();
                        final int primaryKeyCount = primaryKeyGetters.size();
                        DoubleStrategyHashMap<Object, Object, SQLExecutionResult> currentMap = new DoubleStrategyHashMap<>(RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters));
                        String databaseConnectionString = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(childResult.getSQLExecutionNode().connection);
                        while (childResultSet.next())
                        {
                            graphExecutionState.incrementRowCount();
                            List<Object> parents = parentMap.getWithSecondKey(childResult);
                            if (parents == null)
                            {
                                throw new RuntimeException("No parent");
                            }

                            org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> childGraphInstance = executor.getObjectFromResultSet(childResultSet, childResult.getDatabaseTimeZone(), databaseConnectionString);
                            Object child = childGraphInstance.getValue();
                            Object mapObject = currentMap.putIfAbsent(child, child);
                            if (mapObject == null)
                            {
                                mapObject = child;
                                childObjects.add(mapObject);
                                graphExecutionState.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                                if (childrenExist)
                                {
                                    this.addKeyRowToRealizedRelationalResult(child, primaryKeyGetters, realizedRelationalResult);
                                }
                            }

                            for (Object parent : parents)
                            {
                                executor.addChildToParent(parent, mapObject, DefaultExecutionNodeContext.factory().create(graphExecutionState, null));
                            }
                        }

                        childResult.close();
                        childResult = null;

                        if (childrenExist)
                        {
                            this.executeRelationalChildren(node, tempTableName, realizedRelationalResult, databaseConnection, databaseType, databaseTimeZone, currentMap, primaryKeyGetters);
                        }
                    }
                }
                else
                {
                    throw new RuntimeException("Unknown execute class " + executeClass.getCanonicalName());
                }
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

                relationalStoreExecutionState.getBlockConnectionContext().unlockAllBlockConnections();
                relationalStoreExecutionState.getBlockConnectionContext().closeAllBlockConnectionsAsync();

                relationalStoreExecutionState.setBlockConnectionContext(oldBlockConnectionContext);
                relationalStoreExecutionState.setRetainConnection(oldRetainConnectionFlag);
            }

            return new ConstantResult(childObjects);
        }
        throw new RuntimeException("Not implemented!");
    }

    private void executeRelationalChildren(RelationalGraphFetchExecutionNode node, String tempTableNameFromNode, RealizedRelationalResult realizedRelationalResult, DatabaseConnection databaseConnection, String databaseType, String databaseTimeZone, DoubleStrategyHashMap<Object, Object, SQLExecutionResult> parentMap, List<Method> parentKeyGetters)
    {
        try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query Relational: Execute Children").startActive(true))
        {
            RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) this.executionState.getStoreExecutionState(StoreType.Relational);
            String tempTableName = "temp_table_" + tempTableNameFromNode;
            DatabaseManager databaseManager = DatabaseManager.fromString(databaseType);

            try (Scope ignored2 = GlobalTracer.get().buildSpan("Graph Query Relational: Create Temp Table").startActive(true))
            {
                this.createTempTableFromRealizedRelationalResultInBlockConnection(realizedRelationalResult, tempTableName, databaseConnection, databaseType, databaseTimeZone);
            }

            this.executionState.addResult(tempTableNameFromNode, new PreparedTempTableResult(tempTableName));

            for (RelationalGraphFetchExecutionNode childNode : node.children)
            {
                try (Scope ignored3 = GlobalTracer.get().buildSpan("Graph Query Relational: Execute Child").startActive(true))
                {
                    this.executeLocalRelationalGraphOperation(childNode, parentMap, parentKeyGetters);
                }
            }
        }
    }

    private void createTempTableFromRealizedRelationalResultInBlockConnection(RealizedRelationalResult realizedRelationalResult, String tempTableName, DatabaseConnection databaseConnection, String databaseType, String databaseTimeZone)
    {
        RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) this.executionState.getStoreExecutionState(StoreType.Relational);
        DatabaseManager databaseManager = DatabaseManager.fromString(databaseType);
        BlockConnection blockConnection = relationalStoreExecutionState.getBlockConnectionContext().getBlockConnection(relationalStoreExecutionState, databaseConnection, this.profiles);
        databaseManager.relationalDatabaseSupport().accept(RelationalDatabaseCommandsVisitorBuilder.getStreamResultToTempTableVisitor(relationalStoreExecutionState.getRelationalExecutor().getRelationalExecutionConfiguration(), blockConnection, realizedRelationalResult, tempTableName, databaseTimeZone));
        blockConnection.addCommitQuery(databaseManager.relationalDatabaseSupport().dropTempTable(tempTableName));
        blockConnection.addRollbackQuery(databaseManager.relationalDatabaseSupport().dropTempTable(tempTableName));
        blockConnection.close();
    }

    private void addKeyRowToRealizedRelationalResult(Object obj, List<Method> keyGetters, RealizedRelationalResult realizedRelationalResult) throws InvocationTargetException, IllegalAccessException
    {
        int keyCount = keyGetters.size();
        List<Object> pkRowTransformed = FastList.newList(keyCount);
        List<Object> pkRowNormalized = FastList.newList(keyCount);

        for (Method keyGetter : keyGetters)
        {
            Object key = keyGetter.invoke(obj);
            pkRowTransformed.add(key);
            pkRowNormalized.add(key);
        }

        realizedRelationalResult.addRow(pkRowNormalized, pkRowTransformed);
    }

    private Class<?> getExecuteClass(ExecutionNode node)
    {
        if (!(node.implementation instanceof JavaPlatformImplementation))
        {
            throw new RuntimeException("Only Java implementations are currently supported, found: " + node.implementation);
        }
        JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) node.implementation;
        String executeClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
        return ExecutionNodeJavaPlatformHelper.getClassToExecute(node, executeClassName, this.executionState, this.profiles);
    }

    private Result getStreamingObjectResultFromRelationalResult(ExecutionNode node, RelationalResult relationalResult, DatabaseConnection databaseConnection) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, JsonProcessingException
    {
        Class<?> executeClass = this.getExecuteClass(node);
        if (Arrays.asList(executeClass.getInterfaces()).contains(IRelationalClassInstantiationNodeExecutor.class))
        {
            IRelationalClassInstantiationNodeExecutor executor = (IRelationalClassInstantiationNodeExecutor) executeClass.getConstructor().newInstance();

            final ResultSet resultSet = relationalResult.getResultSet();
            final String databaseTimeZone = relationalResult.getRelationalDatabaseTimeZone();
            final String databaseConnectionString = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(databaseConnection);

            Iterator<Object> objectIterator = new Iterator<Object>()
            {
                private boolean cursorMove;
                private boolean hasNext;

                @Override
                public boolean hasNext()
                {
                    if (!this.cursorMove)
                    {
                        try
                        {
                            this.hasNext = resultSet.next();
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                        this.cursorMove = true;
                    }
                    return this.hasNext;
                }

                @Override
                public Object next()
                {
                    if (this.hasNext())
                    {
                        cursorMove = false;
                        try
                        {
                            return executor.getObjectFromResultSet(resultSet, databaseTimeZone, databaseConnectionString);
                        }
                        catch (RuntimeException e)
                        {
                            throw e;
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }

                    throw new NoSuchElementException("End of result set reached!");
                }
            };
            Stream<Object> objectStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(objectIterator, Spliterator.ORDERED), false);
            ClassBuilder classBuilder = new ClassBuilder(node);
            return new StreamingObjectResult<>(objectStream, classBuilder, relationalResult);
        }
        else
        {
            throw new RuntimeException("Unknown execute class " + executeClass.getCanonicalName());
        }
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode storeStreamReadingExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryRootGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryPropertyGraphFetchExecutionNode inMemoryPropertyGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        Result last = null;
        for (ExecutionNode n : aggregationAwareExecutionNode.executionNodes())
        {
            ExecutionState state = new ExecutionState(this.executionState);
            state.activities.add(new AggregationAwareActivity(aggregationAwareExecutionNode.aggregationAwareActivity));
            last = n.accept(new ExecutionNodeExecutor(this.profiles, state));
        }
        return last;
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

    private Connection getConnection(CreateAndPopulateTempTableExecutionNode createAndPopulateTempTableExecutionNode, RelationalDatabaseCommands databaseCommands, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        if (((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)).retainConnection())
        {
            BlockConnection blockConnection = ((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)).getBlockConnectionContext().getBlockConnection(((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)), createAndPopulateTempTableExecutionNode.connection, profiles);
            blockConnection.addRollbackQuery(databaseCommands.dropTempTable(createAndPopulateTempTableExecutionNode.tempTableName));
            blockConnection.addCommitQuery(databaseCommands.dropTempTable(createAndPopulateTempTableExecutionNode.tempTableName));
            return blockConnection;
        }
        throw new RuntimeException("CreateAndPopulateTempTableExecutionNode should be used within RelationalBlockExecutionNode");
    }

    @JsonIgnore
    private Result executeImplementation(RelationalExecutionNode relationalExecutionNode, Result result, ExecutionState executionState, MutableList<CommonProfile> profiles)
    {
        if (!(result instanceof RelationalResult))
        {
            throw new RuntimeException("Unexpected result: " + result.getClass().getName());
        }
        RelationalResult relationalResult = (RelationalResult) result;
        try
        {
            if (!(relationalExecutionNode.implementation instanceof JavaPlatformImplementation))
            {
                throw new RuntimeException("Only Java implementations are currently supported, found: " + relationalExecutionNode.implementation);
            }
            JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) relationalExecutionNode.implementation;
            String executionClassName = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
            String executionMethodName = JavaHelper.getExecutionMethodName(javaPlatformImpl);
            String databaseConnectionString = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(relationalExecutionNode.connection);
            List<Pair<List<Class<?>>, List<Object>>> parameterTypesAndParametersAlternatives = Arrays.asList(
                    Tuples.pair(Arrays.asList(RelationalResult.class, DatabaseConnection.class), Arrays.asList(result, relationalExecutionNode.connection)),
                    Tuples.pair(Arrays.asList(RelationalResult.class, String.class), Arrays.asList(result, databaseConnectionString)),
                    Tuples.pair(Arrays.asList(IRelationalResult.class, String.class), Arrays.asList(result, databaseConnectionString)));
            Stream<?> transformedResult = ExecutionNodeJavaPlatformHelper.executeStaticJavaMethod(relationalExecutionNode, executionClassName, executionMethodName, parameterTypesAndParametersAlternatives, executionState, profiles);
            return new StreamingObjectResult<>(transformedResult, relationalResult.builder, relationalResult);
        }
        catch (Exception e)
        {
            try
            {
                return this.getStreamingObjectResultFromRelationalResult(relationalExecutionNode, relationalResult, relationalExecutionNode.connection);
            }
            catch (Exception other)
            {
                result.close();
                other.addSuppressed(e);
                throw (other instanceof RuntimeException) ? (RuntimeException) other : new RuntimeException(other);
            }
        }
    }

    private void executeLocalRelationalGraphOperation(RelationalGraphFetchExecutionNode node, DoubleStrategyHashMap<Object, Object, SQLExecutionResult> parentMap, List<Method> parentKeyGetters)
    {
        GraphExecutionState graphExecutionState = (GraphExecutionState) executionState;

        List<Object> childObjects = FastList.newList();
        graphExecutionState.setObjectsForNodeIndex(node.nodeIndex, childObjects);

        SQLExecutionResult childResult = null;
        try
        {
            /* Get Java executor */
            Class<?> executeClass = this.getExecuteClass(node);
            if (Arrays.asList(executeClass.getInterfaces()).contains(IRelationalChildGraphNodeExecutor.class))
            {
                IRelationalChildGraphNodeExecutor executor = (IRelationalChildGraphNodeExecutor) executeClass.getConstructor().newInstance();

                /* Execute relational node corresponding to the child */
                childResult = (SQLExecutionResult) node.relationalNode.accept(new ExecutionNodeExecutor(profiles, executionState));

                boolean nonPrimitiveNode = node.resultType instanceof ClassResultType;
                boolean childrenExist = node.children != null && !node.children.isEmpty();

                /* Change the second strategy to suit the primary key indices of parent PKs in the child result set*/
                List<String> parentSQLKeyColumns = executor.parentSQLColumnsInResultSet(childResult.getResultColumns().stream().map(ResultColumn::getNonQuotedLabel).collect(Collectors.toList()));
                List<Integer> parentPrimaryKeyIndices = FastList.newList();
                for (String pkCol : parentSQLKeyColumns)
                {
                    parentPrimaryKeyIndices.add(childResult.getResultSet().findColumn(pkCol));
                }
                RelationalGraphFetchUtils.switchSecondKeyHashingStrategy(parentMap, parentKeyGetters, parentPrimaryKeyIndices);
                String databaseConnectionString = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(childResult.getSQLExecutionNode().connection);

                if (nonPrimitiveNode)
                {
                    List<Method> primaryKeyGetters = executor.primaryKeyGetters();
                    int primaryKeyCount = primaryKeyGetters.size();
                    DoubleStrategyHashMap<Object, Object, SQLExecutionResult> currentMap = new DoubleStrategyHashMap<>(
                            RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters)
                    );

                    String tempTableName = childrenExist ? ((RelationalTempTableGraphFetchExecutionNode) node).tempTableName : null;
                    RealizedRelationalResult realizedRelationalResult = childrenExist ?
                            RealizedRelationalResult.emptyRealizedRelationalResult(((RelationalTempTableGraphFetchExecutionNode) node).columns) :
                            null;
                    DatabaseConnection databaseConnection = childResult.getSQLExecutionNode().connection;
                    String databaseType = childResult.getDatabaseType();
                    String databaseTimeZone = childResult.getDatabaseTimeZone();

                    ResultSet childResultSet = childResult.getResultSet();

                    try (Scope ignored1 = GlobalTracer.get().buildSpan("Graph Query Relational: Read Child Batch").startActive(true))
                    {
                        while (childResultSet.next())
                        {
                            graphExecutionState.incrementRowCount();
                            Object parent = parentMap.getWithSecondKey(childResult);
                            if (parent == null)
                            {
                                throw new RuntimeException("No parent");
                            }

                            org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> childGraphInstance = executor.getObjectFromResultSet(childResultSet, childResult.getDatabaseTimeZone(), databaseConnectionString);
                            Object child = childGraphInstance.getValue();
                            Object mapObject = currentMap.putIfAbsent(child, child);
                            if (mapObject == null)
                            {
                                mapObject = child;
                                graphExecutionState.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                                childObjects.add(mapObject);
                                if (childrenExist)
                                {
                                    this.addKeyRowToRealizedRelationalResult(child, primaryKeyGetters, realizedRelationalResult);
                                }
                            }

                            executor.addChildToParent(parent, mapObject, DefaultExecutionNodeContext.factory().create(graphExecutionState, null));
                        }
                    }

                    childResult.close();
                    childResult = null;

                    if (childrenExist)
                    {
                        this.executeRelationalChildren(node, tempTableName, realizedRelationalResult, databaseConnection, databaseType, databaseTimeZone, currentMap, primaryKeyGetters);
                    }
                }
                else
                {
                    ResultSet childResultSet = childResult.getResultSet();
                    while (childResultSet.next())
                    {
                        Object parent = parentMap.getWithSecondKey(childResult);
                        if (parent == null)
                        {
                            throw new RuntimeException("No parent");
                        }

                        org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance<?> childGraphInstance = executor.getObjectFromResultSet(childResultSet, childResult.getDatabaseTimeZone(), databaseConnectionString);
                        Object child = childGraphInstance.getValue();
                        childObjects.add(child);
                        graphExecutionState.addObjectMemoryUtilization(childGraphInstance.instanceSize());

                        executor.addChildToParent(parent, child, DefaultExecutionNodeContext.factory().create(graphExecutionState, null));
                    }

                    childResult.close();
                    childResult = null;
                }
            }
            else
            {
                throw new RuntimeException("Unknown execute class " + executeClass.getCanonicalName());
            }
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

    private Result executeRelationalRootQueryTempTableGraphFetchExecutionNode(RelationalRootQueryTempTableGraphFetchExecutionNode node)
    {
        int batchSize = node.batchSize == null ? 1000 : node.batchSize;
        boolean isLeaf = node.children == null || node.children.isEmpty();
        Result rootResult = null;

        try
        {
            rootResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
            SQLExecutionResult sqlExecutionResult = (SQLExecutionResult) rootResult;
            DatabaseConnection databaseConnection = sqlExecutionResult.getSQLExecutionNode().connection;
            ResultSet rootResultSet = ((SQLExecutionResult) rootResult).getResultSet();

            IRelationalRootQueryTempTableGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.profiles);

            List<Method> primaryKeyGetters = nodeSpecifics.primaryKeyGetters();

            /* Check if caching is enabled and fetch caches if required */
            List<Pair<String, String>> allInstanceSetImplementations = nodeSpecifics.allInstanceSetImplementations();
            int setIdCount = allInstanceSetImplementations.size();
            RelationalMultiSetExecutionCacheWrapper multiSetCache = new RelationalMultiSetExecutionCacheWrapper(setIdCount);
            boolean cachingEnabledForNode = this.checkForCachingAndPopulateCachingHelpers(allInstanceSetImplementations, nodeSpecifics.supportsCaching(), node.graphFetchTree, sqlExecutionResult, nodeSpecifics::primaryKeyColumns, multiSetCache);

            /* Prepare for reading */
            nodeSpecifics.prepare(rootResultSet, sqlExecutionResult.getDatabaseTimeZone(), ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(databaseConnection));

            boolean isUnion = setIdCount > 1;
            AtomicLong batchIndex = new AtomicLong(0L);
            Spliterator<GraphObjectsBatch> graphObjectsBatchSpliterator = new Spliterators.AbstractSpliterator<GraphObjectsBatch>(Long.MAX_VALUE, Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super GraphObjectsBatch> action) {

                    /* Ensure all children run in the same connection */
                    RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational);
                    BlockConnectionContext oldBlockConnectionContext = relationalStoreExecutionState.getBlockConnectionContext();
                    boolean oldRetainConnectionFlag = relationalStoreExecutionState.retainConnection();
                    relationalStoreExecutionState.setBlockConnectionContext(new BlockConnectionContext());
                    relationalStoreExecutionState.setRetainConnection(true);

                    try
                    {
                        long currentBatch = batchIndex.incrementAndGet();
                        RelationalGraphObjectsBatch relationalGraphObjectsBatch = new RelationalGraphObjectsBatch(currentBatch);

                        List<Object> resultObjects = new ArrayList<>();
                        List<Pair<IGraphInstance<? extends IReferencedObject>, ExecutionCache<GraphFetchCacheKey, Object>>> instancesToDeepFetchAndCache = new ArrayList<>();

                        int objectCount = 0;
                        while ((!rootResultSet.isClosed()) && rootResultSet.next())
                        {
                            relationalGraphObjectsBatch.incrementRowCount();

                            int setIndex = isUnion ? rootResultSet.getInt(1): 0;
                            Object cachedObject = RelationalExecutionNodeExecutor.this.checkAndReturnCachedObject(cachingEnabledForNode, setIndex, multiSetCache);
                            boolean shouldDeepFetchOnThisInstance = cachedObject == null;

                            if (shouldDeepFetchOnThisInstance)
                            {
                                IGraphInstance<? extends IReferencedObject> wrappedObject = nodeSpecifics.nextGraphInstance();
                                instancesToDeepFetchAndCache.add(Tuples.pair(wrappedObject, multiSetCache.setCaches.get(setIndex)));
                                resultObjects.add(wrappedObject.getValue());
                            }
                            else
                            {
                                resultObjects.add(cachedObject);
                            }

                            objectCount += 1;
                            if (objectCount >= batchSize)
                            {
                                break;
                            }
                        }

                        relationalGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, resultObjects);

                        if (!instancesToDeepFetchAndCache.isEmpty())
                        {
                            RealizedRelationalResult realizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.columns);
                            DoubleStrategyHashMap<Object, Object, SQLExecutionResult> rootMap = new DoubleStrategyHashMap<>(RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters));
                            for (Pair<IGraphInstance<? extends IReferencedObject>, ExecutionCache<GraphFetchCacheKey, Object>> instanceAndCache : instancesToDeepFetchAndCache)
                            {
                                IGraphInstance<? extends IReferencedObject> rootGraphInstance = instanceAndCache.getOne();
                                Object rootObject = rootGraphInstance.getValue();
                                rootMap.put(rootObject, rootObject);
                                relationalGraphObjectsBatch.addObjectMemoryUtilization(rootGraphInstance.instanceSize());
                                if (!isLeaf)
                                {
                                    RelationalExecutionNodeExecutor.this.addKeyRowToRealizedRelationalResult(rootObject, primaryKeyGetters, realizedRelationalResult);
                                }
                            }

                            /* Execute store local children */
                            if (!isLeaf)
                            {
                                ExecutionState newState = new ExecutionState(executionState);
                                newState.graphObjectsBatch = relationalGraphObjectsBatch;
                                RelationalExecutionNodeExecutor.this.executeTempTableNodeChildren(node, realizedRelationalResult, databaseConnection, sqlExecutionResult.getDatabaseType(), sqlExecutionResult.getDatabaseTimeZone(), rootMap, primaryKeyGetters, newState);
                            }
                        }

                        instancesToDeepFetchAndCache.stream().filter(x -> x.getTwo() != null).forEach(x -> {
                            Object object = x.getOne().getValue();
                            x.getTwo().put(new RelationalGraphFetchUtils.RelationalObjectGraphFetchCacheKey(object, primaryKeyGetters), object);
                        });

                        action.accept(relationalGraphObjectsBatch);

                        return !resultObjects.isEmpty();
                    }
                    catch (SQLException | InvocationTargetException | IllegalAccessException e)
                    {
                        throw new RuntimeException(e);
                    }
                    finally
                    {
                        relationalStoreExecutionState.getBlockConnectionContext().unlockAllBlockConnections();
                        relationalStoreExecutionState.getBlockConnectionContext().closeAllBlockConnectionsAsync();
                        relationalStoreExecutionState.setBlockConnectionContext(oldBlockConnectionContext);
                        relationalStoreExecutionState.setRetainConnection(oldRetainConnectionFlag);
                    }
                }
            };

            Stream<GraphObjectsBatch> graphObjectsBatchStream = StreamSupport.stream(graphObjectsBatchSpliterator, false);
            return new GraphFetchResult(graphObjectsBatchStream, rootResult);
        }
        catch (RuntimeException e)
        {
            if (rootResult != null)
            {
                rootResult.close();
            }
            throw e;
        }
        catch (Exception e)
        {
            if (rootResult != null)
            {
                rootResult.close();
            }
            throw new RuntimeException(e);
        }
    }

    private Result executeRelationalPrimitiveQueryGraphFetchExecutionNode(RelationalPrimitiveQueryGraphFetchExecutionNode node)
    {
        List<Object> childObjects = new ArrayList<>();
        Result childResult = null;

        try
        {
            RelationalGraphObjectsBatch relationalGraphObjectsBatch = (RelationalGraphObjectsBatch) this.executionState.graphObjectsBatch;

            childResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
            SQLExecutionResult childSqlResult = (SQLExecutionResult) childResult;
            ResultSet childResultSet = childSqlResult.getResultSet();

            IRelationalPrimitiveQueryGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.profiles);

            DoubleStrategyHashMap<Object, Object, SQLExecutionResult> parentMap = this.switchedParentHashMapPerChildResult(
                    relationalGraphObjectsBatch, node.parentIndex, childResultSet,
                    () -> nodeSpecifics.parentPrimaryKeyColumns(childSqlResult.getResultColumns().stream().map(ResultColumn::getNonQuotedLabel).collect(Collectors.toList()))
            );

            /* Prepare for reading */
            nodeSpecifics.prepare(childResultSet, childSqlResult.getDatabaseTimeZone(), ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(childSqlResult.getSQLExecutionNode().connection));

            while (childResultSet.next())
            {
                Object parent = parentMap.getWithSecondKey(childSqlResult);
                if (parent == null)
                {
                    throw new RuntimeException("Cannot find the parent for child");
                }

                IGraphInstance<?> childGraphInstance = nodeSpecifics.nextGraphInstance();
                Object child = childGraphInstance.getValue();
                childObjects.add(child);
                relationalGraphObjectsBatch.addObjectMemoryUtilization(childGraphInstance.instanceSize());

                nodeSpecifics.addChildToParent(parent, child, DefaultExecutionNodeContext.factory().create(this.executionState, null));
            }

            childResult.close();
            childResult = null;

            relationalGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);

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

    private Result executeRelationalClassQueryTempTableGraphFetchExecutionNode(RelationalClassQueryTempTableGraphFetchExecutionNode node)
    {
        boolean isLeaf = node.children == null || node.children.isEmpty();
        List<Object> childObjects = new ArrayList<>();
        List<Pair<IGraphInstance<? extends IReferencedObject>, ExecutionCache<GraphFetchCacheKey, Object>>> childInstancesToDeepFetchAndCache = new ArrayList<>();
        Result childResult = null;

        try
        {
            RelationalGraphObjectsBatch relationalGraphObjectsBatch = (RelationalGraphObjectsBatch) this.executionState.graphObjectsBatch;

            childResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
            SQLExecutionResult childSqlResult = (SQLExecutionResult) childResult;
            DatabaseConnection databaseConnection = childSqlResult.getSQLExecutionNode().connection;
            ResultSet childResultSet = childSqlResult.getResultSet();

            IRelationalClassQueryTempTableGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.profiles);

            /* Check if caching is enabled and fetch caches if required */
            List<Pair<String, String>> allInstanceSetImplementations = nodeSpecifics.allInstanceSetImplementations();
            int setIdCount = allInstanceSetImplementations.size();
            boolean isUnion = setIdCount > 1;
            RelationalMultiSetExecutionCacheWrapper multiSetCache = new RelationalMultiSetExecutionCacheWrapper(setIdCount);
            boolean cachingEnabledForNode = this.checkForCachingAndPopulateCachingHelpers(allInstanceSetImplementations, nodeSpecifics.supportsCaching(), node.graphFetchTree, childSqlResult, nodeSpecifics::primaryKeyColumns, multiSetCache);

            DoubleStrategyHashMap<Object, Object, SQLExecutionResult> parentMap = this.switchedParentHashMapPerChildResult(
                    relationalGraphObjectsBatch, node.parentIndex, childResultSet,
                    () -> nodeSpecifics.parentPrimaryKeyColumns(childSqlResult.getResultColumns().stream().map(ResultColumn::getNonQuotedLabel).collect(Collectors.toList()))
            );

            List<Method> primaryKeyGetters = nodeSpecifics.primaryKeyGetters();
            DoubleStrategyHashMap<Object, Object, SQLExecutionResult> currentMap = new DoubleStrategyHashMap<>(RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters));
            RealizedRelationalResult realizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.columns);

            /* Prepare for reading */
            nodeSpecifics.prepare(childResultSet, childSqlResult.getDatabaseTimeZone(), ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(databaseConnection));

            while (childResultSet.next())
            {
                relationalGraphObjectsBatch.incrementRowCount();

                Object child;
                int setIndex = isUnion ? childResultSet.getInt(1): 0;
                Object cachedObject = RelationalExecutionNodeExecutor.this.checkAndReturnCachedObject(cachingEnabledForNode, setIndex, multiSetCache);
                boolean shouldDeepFetchOnThisInstance = cachedObject == null;

                if (shouldDeepFetchOnThisInstance)
                {
                    IGraphInstance<? extends IReferencedObject> wrappedObject = nodeSpecifics.nextGraphInstance();
                    Object wrappedValue = wrappedObject.getValue();

                    Object mapObject = currentMap.putIfAbsent(wrappedValue, wrappedValue);

                    if (mapObject == null)
                    {
                        child = wrappedValue;
                        childInstancesToDeepFetchAndCache.add(Tuples.pair(wrappedObject, multiSetCache.setCaches.get(setIndex)));
                        relationalGraphObjectsBatch.addObjectMemoryUtilization(wrappedObject.instanceSize());
                        childObjects.add(child);
                        if (!isLeaf)
                        {
                            this.addKeyRowToRealizedRelationalResult(child, primaryKeyGetters, realizedRelationalResult);
                        }
                    }
                    else
                    {
                        child = mapObject;
                    }
                }
                else
                {
                    childObjects.add(cachedObject);
                    child = cachedObject;
                }

                Object parent = parentMap.getWithSecondKey(childSqlResult);
                if (parent == null)
                {
                    throw new RuntimeException("Cannot find the parent for child");
                }

                nodeSpecifics.addChildToParent(parent, child, DefaultExecutionNodeContext.factory().create(this.executionState, null));
            }

            relationalGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);

            childResult.close();
            childResult = null;

            /* Execute store local children */
            if (!isLeaf)
            {
                this.executeTempTableNodeChildren(node, realizedRelationalResult, databaseConnection, childSqlResult.getDatabaseType(), childSqlResult.getDatabaseTimeZone(), currentMap, primaryKeyGetters, this.executionState);
            }

            childInstancesToDeepFetchAndCache.stream().filter(x -> x.getTwo() != null).forEach(x -> {
                Object object = x.getOne().getValue();
                x.getTwo().put(new RelationalGraphFetchUtils.RelationalObjectGraphFetchCacheKey(object, primaryKeyGetters), object);
            });

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

    private Result executeRelationalCrossRootQueryTempTableGraphFetchExecutionNode(RelationalCrossRootQueryTempTableGraphFetchExecutionNode node)
    {
        boolean isLeaf = node.children == null || node.children.isEmpty();
        List<Object> childObjects = new ArrayList<>();
        Result childResult = null;

        RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) this.executionState.getStoreExecutionState(StoreType.Relational);
        BlockConnectionContext oldBlockConnectionContext = relationalStoreExecutionState.getBlockConnectionContext();
        boolean oldRetainConnectionFlag = relationalStoreExecutionState.retainConnection();
        relationalStoreExecutionState.setBlockConnectionContext(new BlockConnectionContext());
        relationalStoreExecutionState.setRetainConnection(true);

        try
        {
            IRelationalCrossRootQueryTempTableGraphFetchExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.profiles);

            RelationalGraphObjectsBatch relationalGraphObjectsBatch = new RelationalGraphObjectsBatch(this.executionState.graphObjectsBatch);
            List<?> parentObjects = relationalGraphObjectsBatch.getObjectsForNodeIndex(node.parentIndex);

            if ((parentObjects != null) && !parentObjects.isEmpty())
            {
                GraphFetchTree nodeSubTree = node.graphFetchTree;

                boolean cachingEnabled = false;
                ExecutionCache<GraphFetchCacheKey, List<Object>> crossCache = null;
                List<Method> parentCrossKeyGettersOrderedPerTargetProperties = null;
                if ((this.executionState.graphFetchCaches != null) && nodeSpecifics.supportsCrossCaching())
                {
                    GraphFetchCacheByTargetCrossKeys c = RelationalGraphFetchUtils.findCacheByCrossKeys(nodeSubTree, nodeSpecifics.mappingId(), nodeSpecifics.sourceInstanceSetId(), nodeSpecifics.targetInstanceSetId(), nodeSpecifics.targetPropertiesOrdered(), this.executionState.graphFetchCaches);
                    if (c != null)
                    {
                        cachingEnabled = true;
                        crossCache = c.getExecutionCache();
                        parentCrossKeyGettersOrderedPerTargetProperties = nodeSpecifics.parentCrossKeyGettersOrderedByTargetProperties();
                    }
                }

                List<Object> parentsToDeepFetch = new ArrayList<>();
                for (Object parent : parentObjects)
                {
                    if (cachingEnabled)
                    {
                        List<Object> children = crossCache.getIfPresent(new RelationalGraphFetchUtils.RelationalCrossObjectGraphFetchCacheKey(parent, parentCrossKeyGettersOrderedPerTargetProperties));
                        if (children == null)
                        {
                            parentsToDeepFetch.add(parent);
                        }
                        else
                        {
                            for (Object child : children)
                            {
                                childObjects.add(child);
                                nodeSpecifics.addChildToParent(parent, child, DefaultExecutionNodeContext.factory().create(this.executionState, null));
                            }
                        }
                    }
                    else
                    {
                        parentsToDeepFetch.add(parent);
                    }
                }

                if (!parentsToDeepFetch.isEmpty())
                {
                    Map<Object, List<Object>> parentToChildMap = new HashMap<>();

                    RealizedRelationalResult parentRealizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.parentTempTableColumns);
                    List<Method> crossKeyGetters = nodeSpecifics.parentCrossKeyGetters();

                    for (Object parentObject : parentsToDeepFetch)
                    {
                        this.addKeyRowToRealizedRelationalResult(parentObject, crossKeyGetters, parentRealizedRelationalResult);
                        parentToChildMap.put(parentObject, new ArrayList<>());
                    }

                    this.executionState.addResult(node.parentTempTableName, parentRealizedRelationalResult);

                    childResult = node.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));
                    SQLExecutionResult childSqlResult = (SQLExecutionResult) childResult;
                    DatabaseConnection databaseConnection = childSqlResult.getSQLExecutionNode().connection;
                    ResultSet childResultSet = childSqlResult.getResultSet();

                    List<String> parentSQLKeyColumns = nodeSpecifics.parentCrossKeyColumns(childSqlResult.getResultColumns().stream().map(ResultColumn::getNonQuotedLabel).collect(Collectors.toList()));
                    List<Integer> parentCrossKeyIndices = parentSQLKeyColumns.stream().map(FunctionHelper.unchecked(childResultSet::findColumn)).collect(Collectors.toList());

                    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                    DoubleStrategyHashMap<Object, List<Object>, SQLExecutionResult> parentMap = new DoubleStrategyHashMap<>(
                            RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategy(crossKeyGetters, parentCrossKeyIndices)
                    );
                    parentsToDeepFetch.forEach((o) -> parentMap.getIfAbsentPut(o, ArrayList::new).add(o));

                    RealizedRelationalResult realizedRelationalResult = RealizedRelationalResult.emptyRealizedRelationalResult(node.columns);

                    List<Method> primaryKeyGetters = nodeSpecifics.primaryKeyGetters();
                    DoubleStrategyHashMap<Object, Object, SQLExecutionResult> currentMap = new DoubleStrategyHashMap<>(
                            RelationalGraphFetchUtils.objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(primaryKeyGetters)
                    );

                    /* Prepare for reading */
                    nodeSpecifics.prepare(childResultSet, childSqlResult.getDatabaseTimeZone(), ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(databaseConnection));

                    while (childResultSet.next())
                    {
                        relationalGraphObjectsBatch.incrementRowCount();

                        List<Object> parents = parentMap.getWithSecondKey(childSqlResult);
                        if (parents == null)
                        {
                            throw new RuntimeException("Cannot find the parent for child");
                        }

                        IGraphInstance<? extends IReferencedObject> childGraphInstance = nodeSpecifics.nextGraphInstance();
                        Object child = childGraphInstance.getValue();
                        Object mapObject = currentMap.putIfAbsent(child, child);
                        if (mapObject == null)
                        {
                            mapObject = child;
                            childObjects.add(mapObject);
                            relationalGraphObjectsBatch.addObjectMemoryUtilization(childGraphInstance.instanceSize());
                            if (!isLeaf)
                            {
                                this.addKeyRowToRealizedRelationalResult(child, primaryKeyGetters, realizedRelationalResult);
                            }
                        }

                        for (Object parent : parents)
                        {
                            if (parentToChildMap.containsKey(parent))
                            {
                                parentToChildMap.get(parent).add(mapObject);
                            }
                            else
                            {
                                parentToChildMap.put(parent, new ArrayList<>());
                                parentToChildMap.get(parent).add(mapObject);
                            }
                            nodeSpecifics.addChildToParent(parent, mapObject, DefaultExecutionNodeContext.factory().create(this.executionState, null));
                        }
                    }

                    relationalGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, childObjects);

                    childResult.close();
                    childResult = null;

                    /* Execute store local children */
                    if (!isLeaf)
                    {
                        ExecutionState newState = new ExecutionState(executionState);
                        newState.graphObjectsBatch = relationalGraphObjectsBatch;
                        this.executeTempTableNodeChildren(node, realizedRelationalResult, databaseConnection, childSqlResult.getDatabaseType(), childSqlResult.getDatabaseTimeZone(), currentMap, primaryKeyGetters, newState);
                    }

                    if (cachingEnabled)
                    {
                        ExecutionCache<GraphFetchCacheKey, List<Object>> cache = crossCache;
                        List<Method> getters = parentCrossKeyGettersOrderedPerTargetProperties;
                        parentToChildMap.forEach((p, cs) -> {
                            cache.put(
                                    new RelationalGraphFetchUtils.RelationalCrossObjectGraphFetchCacheKey(p, getters),
                                    cs
                            );
                        });
                    }
                }
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

            relationalStoreExecutionState.getBlockConnectionContext().unlockAllBlockConnections();
            relationalStoreExecutionState.getBlockConnectionContext().closeAllBlockConnectionsAsync();
            relationalStoreExecutionState.setBlockConnectionContext(oldBlockConnectionContext);
            relationalStoreExecutionState.setRetainConnection(oldRetainConnectionFlag);
        }
    }

    private void executeTempTableNodeChildren(RelationalTempTableGraphFetchExecutionNode node, RealizedRelationalResult realizedRelationalResult, DatabaseConnection databaseConnection, String databaseType, String databaseTimeZone, DoubleStrategyHashMap<Object, Object, SQLExecutionResult> nodeObjectsMap, List<Method> nodePrimaryKeyGetters, ExecutionState state)
    {
        RelationalGraphObjectsBatch relationalGraphObjectsBatch = (RelationalGraphObjectsBatch) state.graphObjectsBatch;

        if (realizedRelationalResult.resultSetRows.isEmpty())
        {
            node.children.forEach(x -> this.recursivelyPopulateEmptyResultsInGraphObjectsBatch(x, relationalGraphObjectsBatch));
        }
        else
        {
            String tempTableName = DatabaseManager.fromString(databaseType).relationalDatabaseSupport().processTempTableName(node.tempTableName);
            RelationalExecutionNodeExecutor.this.createTempTableFromRealizedRelationalResultInBlockConnection(realizedRelationalResult, tempTableName, databaseConnection, databaseType, databaseTimeZone);
            state.addResult(node.tempTableName, new PreparedTempTableResult(tempTableName));

            relationalGraphObjectsBatch.setNodeObjectsHashMap(node.nodeIndex, nodeObjectsMap);
            relationalGraphObjectsBatch.setNodePrimaryKeyGetters(node.nodeIndex, nodePrimaryKeyGetters);

            node.children.forEach(x -> x.accept(new ExecutionNodeExecutor(this.profiles, state)));
        }
    }

    private void recursivelyPopulateEmptyResultsInGraphObjectsBatch(RelationalGraphFetchExecutionNode node, RelationalGraphObjectsBatch relationalGraphObjectsBatch)
    {
        relationalGraphObjectsBatch.setObjectsForNodeIndex(node.nodeIndex, Collections.emptyList());
        if (node.children != null && !node.children.isEmpty())
        {
            node.children.forEach(x -> this.recursivelyPopulateEmptyResultsInGraphObjectsBatch(x, relationalGraphObjectsBatch));
        }
    }

    private DoubleStrategyHashMap<Object, Object, SQLExecutionResult> switchedParentHashMapPerChildResult(RelationalGraphObjectsBatch relationalGraphObjectsBatch, int parentIndex, ResultSet childResultSet, Supplier<List<String>> parentPrimaryKeyColumnsSupplier)
    {
        List<Integer> parentPrimaryKeyIndices = parentPrimaryKeyColumnsSupplier.get().stream().map(FunctionHelper.unchecked(childResultSet::findColumn)).collect(Collectors.toList());
        DoubleStrategyHashMap<Object, Object, SQLExecutionResult> parentMap = relationalGraphObjectsBatch.getNodeObjectsHashMap(parentIndex);
        RelationalGraphFetchUtils.switchSecondKeyHashingStrategy(parentMap, relationalGraphObjectsBatch.getNodePrimaryKeyGetters(parentIndex), parentPrimaryKeyIndices);
        return parentMap;
    }

    private boolean checkForCachingAndPopulateCachingHelpers(List<Pair<String, String>> allInstanceSetImplementations, boolean nodeSupportsCaching, GraphFetchTree nodeSubTree, SQLExecutionResult sqlExecutionResult, Function<Integer, List<String>> pkColumnsFunction, RelationalMultiSetExecutionCacheWrapper multiSetCaches)
    {
        boolean cachingEnabledForNode = (this.executionState.graphFetchCaches != null) && nodeSupportsCaching && RelationalGraphFetchUtils.subTreeValidForCaching(nodeSubTree);
        ResultSet sqlResultSet = sqlExecutionResult.getResultSet();

        if (cachingEnabledForNode)
        {
            int i = 0;
            for (Pair<String, String> setImpl : allInstanceSetImplementations)
            {
                GraphFetchCacheByEqualityKeys cache = RelationalGraphFetchUtils.findCacheByEqualityKeys(nodeSubTree, setImpl.getOne(), setImpl.getTwo(), this.executionState.graphFetchCaches);
                if (cache != null)
                {
                    List<Integer> primaryKeyIndices = pkColumnsFunction.apply(i).stream().map(FunctionHelper.unchecked(sqlResultSet::findColumn)).collect(Collectors.toList());
                    multiSetCaches.addNextValidCache(cache.getExecutionCache(), new RelationalGraphFetchUtils.RelationalSQLResultGraphFetchCacheKey(sqlExecutionResult, primaryKeyIndices));
                }
                else
                {
                    multiSetCaches.addNextEmptyCache();
                }
                i += 1;
            }
        }
        else
        {
            allInstanceSetImplementations.forEach((x) -> multiSetCaches.addNextEmptyCache());
        }

        return cachingEnabledForNode;
    }

    private Object checkAndReturnCachedObject(boolean cachingEnabledForNode, int setIndex, RelationalMultiSetExecutionCacheWrapper multiSetCache)
    {
        if (cachingEnabledForNode && multiSetCache.setCachingEnabled.get(setIndex))
        {
            return multiSetCache.setCaches.get(setIndex).getIfPresent(multiSetCache.sqlResultCacheKeys.get(setIndex));
        }
        return null;
    }

    private static class RelationalMultiSetExecutionCacheWrapper
    {
        List<Boolean> setCachingEnabled;
        List<ExecutionCache<GraphFetchCacheKey, Object>> setCaches;
        List<RelationalGraphFetchUtils.RelationalSQLResultGraphFetchCacheKey> sqlResultCacheKeys;

        RelationalMultiSetExecutionCacheWrapper(int setIdCount)
        {
            this.setCachingEnabled = new ArrayList<>(setIdCount);
            this.setCaches = new ArrayList<>(setIdCount);
            this.sqlResultCacheKeys = new ArrayList<>(setIdCount);
        }

        void addNextValidCache(ExecutionCache<GraphFetchCacheKey, Object> cache, RelationalGraphFetchUtils.RelationalSQLResultGraphFetchCacheKey cacheKey)
        {
            this.setCachingEnabled.add(true);
            this.setCaches.add(cache);
            this.sqlResultCacheKeys.add(cacheKey);
        }

        void addNextEmptyCache()
        {
            this.setCachingEnabled.add(false);
            this.setCaches.add(null);
            this.sqlResultCacheKeys.add(null);
        }
    }
}
