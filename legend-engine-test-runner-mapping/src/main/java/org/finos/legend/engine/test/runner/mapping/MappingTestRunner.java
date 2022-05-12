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

package org.finos.legend.engine.test.runner.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.ExpectedOutputMappingTestAssert;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.url.DataProtocolHandler;
import org.finos.legend.engine.test.runner.shared.ComparisonError;
import org.finos.legend.engine.test.runner.shared.JsonNodeComparator;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import javax.ws.rs.core.MediaType;

public class MappingTestRunner
{
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

    private final PureModel pureModel;
    private final PlanExecutor executor;
    private final String mappingPath;
    public final MappingTest_Legacy mappingTestLegacy;
    private final MutableList<PlanTransformer> planTransformers;
    private final RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions;
    private final Root_meta_pure_runtime_Runtime_Impl runtime;
    private final String pureVersion;

    public MappingTestRunner(PureModel pureModel, String mappingPath, MappingTest_Legacy mappingTestLegacy, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this.pureModel = pureModel;
        this.executor = executor;
        this.mappingPath = mappingPath;
        this.mappingTestLegacy = mappingTestLegacy;
        this.runtime = new Root_meta_pure_runtime_Runtime_Impl("");
        this.planTransformers = transformers;
        this.extensions = extensions;
        this.pureVersion = pureVersion;
    }

    public MappingTestRunner(PureModel pureModel, String mappingPath, MappingTest_Legacy mappingTestLegacy, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers)
    {
        this(pureModel, mappingPath, mappingTestLegacy, executor, extensions, transformers, null);
    }


    public void setupTestData()
    {
        ConnectionVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection> connectionVisitor = new ConnectionFirstPassBuilder(this.pureModel.getContext());
        this.buildTestConnection(conn -> this.runtime._connectionsAdd(conn.accept(connectionVisitor)));
    }

    private void buildTestConnection(Consumer<? super Connection> connectionRegistrar)
    {
        if (this.mappingTestLegacy.inputData.size() != 1)
        {
            throw new RuntimeException("Only tests with one input data set are currently supported; " + mappingTest.inputData.size() + " supplied");
        }
        InputData input = this.mappingTestLegacy.inputData.get(0);
        if (input instanceof ObjectInputData)
        {
            ObjectInputData objectInputData = ((ObjectInputData) input);
            if (ObjectInputType.JSON.equals(objectInputData.inputType))
            {
                JsonModelConnection jsonModelConnection = new JsonModelConnection();
                jsonModelConnection._class = objectInputData.sourceClass;
                jsonModelConnection.url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_JSON + ";base64," + Base64.getEncoder().encodeToString(objectInputData.data.getBytes(StandardCharsets.UTF_8));
                connectionRegistrar.accept(jsonModelConnection);
            }
            else if (ObjectInputType.XML.equals(objectInputData.inputType))
            {
                XmlModelConnection xmlModelConnection = new XmlModelConnection();
                xmlModelConnection._class = objectInputData.sourceClass;
                xmlModelConnection.url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_XML + ";base64," + Base64.getEncoder().encodeToString(objectInputData.data.getBytes(StandardCharsets.UTF_8));
                connectionRegistrar.accept(xmlModelConnection);
            }
            else
            {
                throw new UnsupportedOperationException("Unsupported Pure mapping test input data type '" + objectInputData.inputType + '"');
            }
        }
        else
        {
            connectionRegistrar.accept(getTestConnectionFromFactories(input));
        }
    }

    private static Connection getTestConnectionFromFactories(InputData inputData)
    {
        MutableList<ConnectionFactoryExtension> factories = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
        return factories
                .collect(f -> f.tryBuildFromInputData(inputData))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported mapping test input data type '" + inputData.getClass().getSimpleName() + '"'));
    }

    public RichMappingTestResult setupAndRunTest()
    {
        this.setupTestData();
        return this.doRunTest();
    }

    public RichMappingTestResult doRunTest()
    {
        try
        {
            Result actualResult = this.executeLegend(this.mappingTestLegacy.query, this.mappingPath);
            List<JsonNode> actual = getResultValuesAsJson(actualResult);
            String rawActualJSON = objectMapper.writeValueAsString(actual);

            List<JsonNode> expected = this.nodeToList(this.parseJsonString(getExpected().replace("\\n", "\n")));
            String rawExpectedJSON = objectMapper.writeValueAsString(expected);

            this.assertEquals(expected, actual);

            return new RichMappingTestResult(this.mappingPath, this.mappingTestLegacy.name, rawExpectedJSON, rawActualJSON);
        }
        catch (ComparisonError c)
        {
            return new RichMappingTestResult(this.mappingPath, this.mappingTestLegacy.name, c);
        }
        catch (Exception e)
        {
            return new RichMappingTestResult(this.mappingPath, this.mappingTestLegacy.name, e);
        }
    }

    private String getExpected()
    {
        if (this.mappingTestLegacy._assert instanceof ExpectedOutputMappingTestAssert)
        {
            return ((ExpectedOutputMappingTestAssert) this.mappingTestLegacy._assert).expectedOutput;
        }
        throw new RuntimeException("Unsupported type of MappingTestAssert: " + this.mappingTestLegacy._assert.getClass().getName());
    }

    protected Result executeLegend(Lambda lambda, String mappingPath)
    {
        LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(lambda, new CompileContext.Builder(this.pureModel).withElement(mappingPath).build());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = this.pureModel.getMapping(mappingPath);
        SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(pureLambda, pureMapping, this.runtime, null, this.pureModel, this.pureVersion, PlanPlatform.JAVA, null, this.extensions, this.planTransformers);
        return this.executor.execute(executionPlan);
    }


    private void assertEquals(String expected, String actual)
    {
        if (!Objects.equals(expected, actual))
        {
            throw new ComparisonError(expected, actual);
        }
    }

    protected void assertEquals(JsonNode expected, JsonNode actual)
    {
        assertEquals(expected, actual, false);
    }

    protected void assertEquals(JsonNode expected, JsonNode actual, boolean nullEqualsMissing)
    {
        if (notEqual(expected, actual, nullEqualsMissing))
        {
            assertEquals(serializeForFailureMessage(expected), serializeForFailureMessage(actual));
        }
    }

    protected void assertEquals(List<? extends JsonNode> expected, List<? extends JsonNode> actual)
    {
        assertEquals(expected, actual, false);
    }

    protected void assertEquals(List<? extends JsonNode> expected, List<? extends JsonNode> actual, boolean nullEqualsMissing)
    {
        if (notEqual(expected, actual, nullEqualsMissing))
        {
            assertEquals(serializeForFailureMessage(expected), serializeForFailureMessage(actual));
        }
    }

    private boolean notEqual(Collection<? extends JsonNode> expected, Collection<? extends JsonNode> actual, boolean nullEqualsMissing)
    {
        if (expected.size() != actual.size())
        {
            return true;
        }
        Iterator<? extends JsonNode> expectedIter = expected.iterator();
        Iterator<? extends JsonNode> actualIter = actual.iterator();
        while (expectedIter.hasNext())
        {
            if (notEqual(expectedIter.next(), actualIter.next(), nullEqualsMissing))
            {
                return true;
            }
        }
        return false;
    }

    private boolean notEqual(JsonNode expected, JsonNode actual, boolean nullEqualsMissing)
    {
        return getComparator(nullEqualsMissing).compare(expected, actual) != 0;
    }

    private JsonNodeComparator getComparator(boolean nullEqualsMissing)
    {
        return nullEqualsMissing ? JsonNodeComparator.NULL_MISSING_EQUIVALENT : JsonNodeComparator.MISSING_BEFORE_NULL;
    }

    private String serializeForFailureMessage(Collection<? extends JsonNode> nodes)
    {
        try
        {
            List<Object> values = new ArrayList<>(nodes.size());
            for (JsonNode node : nodes)
            {
                values.add(objectMapper.treeToValue(node, Object.class));
            }
            return objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(values);
        }
        catch (JsonProcessingException e)
        {
            return nodes.toString();
        }
    }

    private String serializeForFailureMessage(JsonNode node)
    {
        try
        {
            Object value = objectMapper.treeToValue(node, Object.class);
            return objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            return node.toString();
        }
    }


    private JsonNode parseJsonString(String rawExpected)
    {
        try
        {
            return objectMapper.readTree(rawExpected);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting expected value from: " + rawExpected, e);
        }
    }

    private List<JsonNode> getResultValuesAsJson(Result result)
    {
        if (result instanceof ErrorResult)
        {
            throw new RuntimeException(((ErrorResult) result).getMessage());
        }
        JsonNode jsonResult = getResultAsJson(result);
        JsonNode values = jsonResult.get("values");
        if (values != null)
        {
            return nodeToList(values);
        }
        values = jsonResult.get("result");
        if (values != null)
        {
            return nodeToList(values.get("rows"));
        }
        throw new RuntimeException("The system was not extract values from the Result");
    }

    private List<JsonNode> nodeToList(JsonNode node)
    {
        if ((node == null) || node.isMissingNode())
        {
            return Collections.emptyList();
        }
        if (node.isArray())
        {
            List<JsonNode> list = new ArrayList<>(node.size());
            node.forEach(list::add);
            return list;
        }
        return Collections.singletonList(node);
    }

    private JsonNode getResultAsJson(Result result)
    {
        if (result instanceof StreamingResult)
        {
            try
            {
                Serializer serializer = ((StreamingResult) result).getSerializer(SerializationFormat.DEFAULT);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
                serializer.stream(byteStream);
                return objectMapper.readTree(byteStream.toByteArray());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        if (result instanceof ConstantResult)
        {
            Object value = ((ConstantResult) result).getValue();
            return objectMapper.valueToTree(value);
        }
        throw new RuntimeException("Unhandled result type: " + result.getClass().getSimpleName());
    }


}
