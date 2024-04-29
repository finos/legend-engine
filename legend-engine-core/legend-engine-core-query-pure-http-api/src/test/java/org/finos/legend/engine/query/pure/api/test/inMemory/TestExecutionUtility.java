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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;

public class TestExecutionUtility
{
    public static String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        return baos.toString("UTF-8");
    }

    public static HttpServletRequest buildMockRequest()
    {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        HttpSession mockSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(mockSession);
        Mockito.when(mockSession.getId()).thenReturn("testSessionID");
        return mockRequest;
    }

    public static Response runTest(ExecuteInput input, HttpServletRequest request)
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        RelationalStoreExecutor relationalStoreExecutor = new RelationalStoreExecutorBuilder().build();
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
        Response result = new Execute(modelManager, planExecutor, (PureModel pureModel) -> Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers).execute(request, input, SerializationFormat.defaultFormat, null, null);
        Assert.assertEquals(200, result.getStatus());
        return result;
    }

}
