// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.deephaven.plugin;

import org.apache.arrow.flight.FlightRuntimeException;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.deephaven.connection.DeephavenSession;
import org.finos.legend.engine.plan.execution.stores.deephaven.result.DeephavenExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.deephaven.result.DeephavenStreamingResult;
import org.finos.legend.engine.plan.execution.stores.deephaven.specifics.IDeephavenExecutionNodeSpecifics;
import org.finos.legend.engine.protocol.deephaven.metamodel.pure.DeephavenExecutionNode;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.qst.table.FriendlyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.arrow.vector.types.pojo.Schema;

import java.time.Instant;
import java.util.Spliterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DeephavenExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DeephavenExecutionNodeExecutor.class);

    private final Identity identity;
    private final ExecutionState executionState;
    private final DeephavenStoreState state;

    public DeephavenExecutionNodeExecutor(Identity identity, ExecutionState executionState, DeephavenStoreState state)
    {
        this.identity = identity;
        this.executionState = executionState;
        this.state = state;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof DeephavenExecutionNode)
        {
            return executeDeephavenNode((DeephavenExecutionNode) executionNode);
        }
        throw new IllegalStateException("DEEPHAVEN: Unexpected node type: " + executionNode.getClass().getSimpleName());
    }

    private Result executeDeephavenNode(DeephavenExecutionNode node)
    {
        DeephavenSession deephavenSession = null;
        try
        {
            deephavenSession = createDeephavenSession(node.connection);
            try (BarrageSession session = deephavenSession.getBarrageSession())
            {
                IDeephavenExecutionNodeSpecifics specifics = createSpecifics(node);
                TableHandle table = specifics.execute(session);
                FlightStream flightStream = session.stream(table.ticketId());

                Stream<Map<String, Object>> rowStream = StreamSupport.stream(new DeephavenRowSpliterator(flightStream), false);
                List<Map<String, Object>> results = rowStream.collect(Collectors.toList());
                flightStream.close();
                Stream<Map<String, Object>> resultStream = results.stream();

                String query = FriendlyString.of(specifics.getTableSpec());
                DeephavenExecutionActivity activity = new DeephavenExecutionActivity(query);
                List<org.finos.legend.engine.plan.execution.result.ExecutionActivity> activities = Collections.singletonList(activity);
                DeephavenStreamingResult deephavenResult = new DeephavenStreamingResult(activities);
                return new StreamingObjectResult<>(resultStream, deephavenResult.getResultBuilder(), deephavenResult);
            }
        }
        catch (EngineException e)
        {
            throw e;
        }
        catch (io.deephaven.client.impl.TableHandle.TableHandleException e)
        {
            throw new EngineException("Deephaven query execution failed: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new EngineException("Deephaven query interrupted: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
        }
        catch (Exception e)
        {
            throw new EngineException("Unexpected error executing Deephaven query: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
        }
        finally
        {
            if (deephavenSession != null)
            {
                try
                {
                    deephavenSession.close();
                }
                catch (Exception e)
                {
                    throw new EngineException("Failed to close Deephaven session: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
                }
            }
        }
    }

    private IDeephavenExecutionNodeSpecifics createSpecifics(DeephavenExecutionNode node) throws ReflectiveOperationException
    {
        String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
        Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, identity);
        return (IDeephavenExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();
    }

    private DeephavenSession createDeephavenSession(DeephavenConnection connection)
    {
        if (!(connection.authSpec instanceof PSKAuthenticationSpecification))
        {
            throw new EngineException("Unsupported authentication type: " + connection.authSpec.getClass().getSimpleName(), ExceptionCategory.USER_CREDENTIALS_ERROR);
        }
        PSKAuthenticationSpecification pskSpec = (PSKAuthenticationSpecification) connection.authSpec;
        if (pskSpec.psk == null || pskSpec.psk.isEmpty())
        {
            throw new EngineException("PSK secret is missing or empty for " + connection.sourceSpec.url, ExceptionCategory.USER_CREDENTIALS_ERROR);
        }
        Optional<DeephavenSession> session = this.state.getProviders().stream()
                .map(provider ->
                {
                    try
                    {
                        Optional<DeephavenSession> providedSession = provider.provide(pskSpec, connection.sourceSpec);
                        providedSession.ifPresent(s -> System.out.println("Session created successfully for " + connection.sourceSpec.url));
                        providedSession.ifPresent(s -> LOGGER.info("Session created successfully for {}" + connection.sourceSpec.url));
                        return providedSession;
                    }
                    catch (Exception e)
                    {
                        return Optional.<DeephavenSession>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        return session.orElseThrow(() -> new EngineException("Unable to create a Deephaven Session for " + connection.sourceSpec.url, ExceptionCategory.USER_CREDENTIALS_ERROR));
    }

    private static class DeephavenRowSpliterator implements Spliterator<Map<String, Object>>
    {
        private final FlightStream flightStream;
        private final Schema schema;
        private VectorSchemaRoot root;
        private List<FieldVector> vectors;
        private int currentRowIndex;
        private int rowCount;

        DeephavenRowSpliterator(FlightStream flightStream)
        {
            this.flightStream = flightStream;
            this.schema = flightStream.getSchema();
            this.currentRowIndex = 0;
            this.rowCount = 0;
        }

        @Override
        public boolean tryAdvance(java.util.function.Consumer<? super Map<String, Object>> action)
        {
            try
            {
                while (true)
                {
                    if (root == null || currentRowIndex >= rowCount)
                    {
                        if (!flightStream.next())
                        {
                            return false;
                        }
                        root = flightStream.getRoot();
                        if (root == null)
                        {
                            throw new EngineException("FlightStream returned null root, likely due to authentication failure", ExceptionCategory.SERVER_EXECUTION_ERROR);
                        }
                        vectors = root.getFieldVectors();
                        rowCount = root.getRowCount();
                        currentRowIndex = 0;
                    }
                    if (currentRowIndex < rowCount)
                    {
                        Map<String, Object> row = new HashMap<>(vectors.size());
                        for (FieldVector vector : vectors)
                        {
                            String colName = vector.getName();
                            Object value = vector.getObject(currentRowIndex);
                            Field field = schema.findField(colName);
                            ArrowType type = field.getType();
                            if (type instanceof ArrowType.Timestamp && value instanceof Long)
                            {
                                ArrowType.Timestamp timestampType = (ArrowType.Timestamp) type;
                                long nanos = (Long) value;
                                Instant instant = convertToInstant(nanos, timestampType);
                                row.put(colName, instant.toString());
                            }
                            else
                            {
                                row.put(colName, value);
                            }
                        }
                        action.accept(row);
                        currentRowIndex++;
                        return true;
                    }
                }
            }
            catch (FlightRuntimeException e)
            {
                throw new EngineException("Flight streaming error: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
            }
            catch (Exception e)
            {
                if (e instanceof NullPointerException)
                {
                    throw new EngineException("NullPointerException in streaming: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
                }
                throw new EngineException("Error streaming Deephaven results: " + e.getMessage(), e, ExceptionCategory.SERVER_EXECUTION_ERROR);
            }
        }

        private Instant convertToInstant(long nanos, ArrowType.Timestamp timestampType)
        {
            Instant instant;
            TimeUnit unit = timestampType.getUnit();
            if (unit == TimeUnit.NANOSECOND)
            {
                instant = Instant.ofEpochSecond(nanos / 1_000_000_000L, nanos % 1_000_000_000L);
            }
            else if (unit == TimeUnit.MICROSECOND)
            {
                instant = Instant.ofEpochSecond(nanos / 1_000_000L, (nanos % 1_000_000L) * 1000);
            }
            else if (unit == TimeUnit.MILLISECOND)
            {
                instant = Instant.ofEpochMilli(nanos);
            }
            else if (unit == TimeUnit.SECOND)
            {
                instant = Instant.ofEpochSecond(nanos);
            }
            else
            {
                throw new IllegalStateException("Unsupported timestamp unit: " + unit);
            }
            return instant;
        }

        @Override
        public Spliterator<Map<String, Object>> trySplit()
        {
            return null;
        }

        @Override
        public long estimateSize()
        {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics()
        {
            return Spliterator.ORDERED | Spliterator.NONNULL;
        }
    }
}