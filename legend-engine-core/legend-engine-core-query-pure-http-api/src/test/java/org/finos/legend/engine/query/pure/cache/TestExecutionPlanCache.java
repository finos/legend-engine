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

package org.finos.legend.engine.query.pure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.request.RequestContextHelper;
import org.finos.legend.engine.plan.execution.cache.executionPlan.ExecutionPlanCache;
import org.finos.legend.engine.plan.execution.cache.executionPlan.ExecutionPlanCacheBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.query.pure.api.test.inMemory.TestExecutionUtility;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;
import org.mockito.Mockito;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;
import static org.junit.Assert.assertEquals;

public class TestExecutionPlanCache
{
    @Test
    public void testCacheIsUsed() throws IOException
    {
        ModelManager modelManager;
        RelationalStoreExecutor relationalStoreExecutor;
        PlanExecutor planExecutor;
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        ExecutionPlanCache cache = ExecutionPlanCacheBuilder.buildWithDefaultCache();


        relationalStoreExecutor = new RelationalStoreExecutorBuilder().build();
        planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
        ExecuteInput input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputZeroMany.json")), ExecuteInput.class);
        ExecuteInput input2 = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputZeroMany.json")), ExecuteInput.class);

        modelManager = new ModelManager(DeploymentMode.TEST, new MockModelLoader((PureModelContextData) input.model));
        ExecuteInput inputPointer = input;
        ExecuteInput input2Pointer = input2;

        inputPointer.model = new PureModelContextPointer();
        input2Pointer.model = new PureModelContextPointer();

        HttpServletRequest request = TestExecutionUtility.buildMockRequest();
        Mockito.when(request.getHeader(RequestContextHelper.LEGEND_USE_PLAN_CACHE)).thenReturn("true");

        Execute execute = new Execute(modelManager, planExecutor, (PureModel pureModel) -> Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, null, cache);
        Response response1 = execute.execute(request, inputPointer, SerializationFormat.defaultFormat, null, null);
        Response response2 = execute.execute(request, input2Pointer, SerializationFormat.defaultFormat, null, null);

        String expected = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"Age\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select top 1000 \\\"root\\\".age as \\\"Age\\\" from PersonTable as \\\"root\\\" where \\\"root\\\".age in (20,30)\"}],\"result\":{\"columns\":[\"Age\"],\"rows\":[{\"values\":[20]},{\"values\":[30]}]}}";

        assertEquals(expected, RelationalResultToJsonDefaultSerializer.removeComment(TestExecutionUtility.responseAsString(response1)));
        assertEquals(expected, RelationalResultToJsonDefaultSerializer.removeComment(TestExecutionUtility.responseAsString(response2)));


        assert (cache.getCache().stats().requestCount() == 2);
        assert (cache.getCache().stats().missCount() == 1);
        assert (cache.getCache().stats().hitCount() == 1);

    }

    @Test
    public void testExecuteWithNoParameters() throws IOException
    {
        RelationalStoreExecutor relationalStoreExecutor;
        PlanExecutor planExecutor;
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
        ExecutionPlanCache cache = ExecutionPlanCacheBuilder.buildWithDefaultCache();
        relationalStoreExecutor = new RelationalStoreExecutorBuilder().build();
        planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
        ExecuteInput input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputNoParameters.json")), ExecuteInput.class);
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST, new MockModelLoader((PureModelContextData) input.model));
        ExecuteInput inputPointer = input;

        inputPointer.model = new PureModelContextPointer();
        HttpServletRequest request = TestExecutionUtility.buildMockRequest();
        Mockito.when(request.getHeader(RequestContextHelper.LEGEND_USE_PLAN_CACHE)).thenReturn("true");
        String expected = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"Age\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select top 1000 \\\"root\\\".age as \\\"Age\\\" from PersonTable as \\\"root\\\" where \\\"root\\\".age in (20, 30)\"}],\"result\":{\"columns\":[\"Age\"],\"rows\":[{\"values\":[20]},{\"values\":[30]}]}}";

        Execute execute = new Execute(modelManager, planExecutor, (PureModel pureModel) -> Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, null, cache);
        Response response = execute.execute(request, inputPointer, SerializationFormat.defaultFormat, null, null);
        assertEquals(expected, RelationalResultToJsonDefaultSerializer.removeComment(TestExecutionUtility.responseAsString(response)));
    }


    private class MockModelLoader implements ModelLoader
    {
        PureModelContextData data;

        public MockModelLoader(PureModelContextData data)
        {
            this.data = data;
        }

        @Override
        public boolean supports(PureModelContext context)
        {
            return true;
        }

        @Override
        public PureModelContextData load(Identity identity, PureModelContext context, String clientVersion, Span parentSpan)
        {
            return data;

        }

        @Override
        public void setModelManager(ModelManager modelManager)
        {

        }

        @Override
        public boolean shouldCache(PureModelContext context)
        {
            return this.supports(context);
        }

        @Override
        public PureModelContext cacheKey(PureModelContext context, Identity identity)
        {
            return data;
        }


    }

}

