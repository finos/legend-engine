// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.pure.api.test.inMemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Objects;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;
import static org.junit.Assert.assertEquals;

public class TestQueryExecutionWithParameters
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testQueryExecutionWithParameterZeroMany() throws IOException
    {
        ExecuteInput input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputZeroMany.json")), ExecuteInput.class);
        String json = TestExecutionUtility.responseAsString(runTest(input));
        assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"Age\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select top 1000 \\\"root\\\".age as \\\"Age\\\" from PersonTable as \\\"root\\\" where \\\"root\\\".age in (20,30)\"}], \"result\" : {\"columns\" : [\"Age\"], \"rows\" : [{\"values\": [20]},{\"values\": [30]}]}}", json);
    }

    @Test
    public void testQueryExecutionWithParameterEnumZeroOne() throws IOException
    {
        ExecuteInput input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputEnumZeroOne.json")), ExecuteInput.class);
        String json = TestExecutionUtility.responseAsString(runTest(input));
        assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"Inc Type\",\"type\":\"model::IncType\",\"relationalType\":\"VARCHAR(200)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select top 1000 \\\"root\\\".Inc as \\\"Inc Type\\\" from FirmTable as \\\"root\\\" where (\\\"root\\\".Inc = 'LLC')\"}], \"result\" : {\"columns\" : [\"Inc Type\"], \"rows\" : [{\"values\": [\"LLC\"]}]}}", json);
    }

    private Response runTest(ExecuteInput input)
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        RelationalStoreExecutor relationalStoreExecutor = new RelationalStoreExecutorBuilder().build();
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new java.lang.Class<?>[] {HttpServletRequest.class}, new TestExecutionUtility.ReflectiveInvocationHandler(new TestExecutionUtility.Request()));
        Response result = new Execute(modelManager, planExecutor, (PureModel pureModel) -> Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers).execute(request, input, SerializationFormat.defaultFormat, null, null);
        Assert.assertEquals(200, result.getStatus());
        return result;
    }
}

