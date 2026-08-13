// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestDataQualityExecuteReconciliation
{
    @Mock
    private ModelManager modelManager;

    @Mock
    private DataQualityPlanLoader planLoader;

    @Mock
    private PureModel pureModel;

    @Test
    public void testReconciliation_UsesPreGeneratedPlanWhenDefaultsMatch() throws Exception
    {
        SingleExecutionPlan cachedPlan = new SingleExecutionPlan();
        when(planLoader.fetchReconPlanFromSDLC(any(Identity.class), eq("meta::dataquality::TestRelationComparison"), any())).thenReturn(cachedPlan);

        AtomicReference<SingleExecutionPlan> executedPlan = new AtomicReference<>();
        DataQualityExecute execute = newTestExecute(modelManager, planLoader, executedPlan);

        Response response = execute.reconciliationUsingPreGeneratedPlan(mock(HttpServletRequest.class), defaultInput(), SerializationFormat.defaultFormat, null);

        assertNotNull(response);
        assertSame("Pre-generated recon plan must be executed as-is", cachedPlan, executedPlan.get());
        verify(planLoader).fetchReconPlanFromSDLC(any(Identity.class), eq("meta::dataquality::TestRelationComparison"), any());
        verify(modelManager, never()).loadModel(any(), any(), any(), any());
    }

    @Test
    public void testReconciliation_FallsBackToOnTheFlyPlanWhenCachedPlanNotFound() throws Exception
    {
        when(modelManager.loadModel(any(), any(), any(), any())).thenReturn(pureModel);
        SingleExecutionPlan generatedPlan = new SingleExecutionPlan();
        AtomicReference<SingleExecutionPlan> executedPlan = new AtomicReference<>();
        DataQualityExecute execute = new DataQualityExecute(modelManager, mock(PlanExecutor.class), null, null, dummyMetaDataConfig(), identity -> null)
        {
            @Override
            protected Pair<SingleExecutionPlan, MutableMap<String, Object>> generateDataReconciliationPlan(PureModel model, DataQualityReconInput input, long start, Identity identity)
            {
                assertSame(pureModel, model);
                return Tuples.pair(generatedPlan, Maps.mutable.empty());
            }

            @Override
            protected Result executePlanToResult(HttpServletRequest request, Identity identity, SingleExecutionPlan plan, java.util.Map<String, Object> params)
            {
                executedPlan.set(plan);
                return mock(Result.class);
            }

            @Override
            protected Response wrapInResponse(Identity identity, SerializationFormat format, long start, Result result)
            {
                return Response.ok().build();
            }
        };
        injectPlanLoader(execute, planLoader);

        DataQualityReconCachedPlanInput input = defaultInput();
        when(planLoader.fetchReconPlanFromSDLC(any(), any(), any())).thenThrow(new RuntimeException("Cached plan not found"));

        Response response = execute.reconciliationUsingPreGeneratedPlan(mock(HttpServletRequest.class), input, SerializationFormat.defaultFormat, null);

        assertNotNull(response);
        assertSame("Freshly generated plan must be executed", generatedPlan, executedPlan.get());
        verify(modelManager).loadModel(any(), any(), any(), any());
    }

    private static DataQualityExecute newTestExecute(ModelManager modelManager, DataQualityPlanLoader planLoader, AtomicReference<SingleExecutionPlan> executedPlanSink) throws Exception
    {
        DataQualityExecute execute = new DataQualityExecute(modelManager, mock(PlanExecutor.class), null, null, dummyMetaDataConfig(), identity -> null)
        {
            @Override
            protected Pair<SingleExecutionPlan, MutableMap<String, Object>> generateDataReconciliationPlan(PureModel pureModel, DataQualityReconInput input, long start, Identity identity)
            {
                throw new AssertionError("On-the-fly plan generation must be skipped when the pre-generated plan is available");
            }

            @Override
            protected Result executePlanToResult(HttpServletRequest request, Identity identity, SingleExecutionPlan plan, java.util.Map<String, Object> params)
            {
                executedPlanSink.set(plan);
                return mock(Result.class);
            }

            @Override
            protected Response wrapInResponse(Identity identity, SerializationFormat format, long start, Result result)
            {
                return Response.ok().build();
            }
        };
        injectPlanLoader(execute, planLoader);
        return execute;
    }

    private static void injectPlanLoader(DataQualityExecute execute, DataQualityPlanLoader loader) throws Exception
    {
        Field f = DataQualityExecute.class.getDeclaredField("dataQualityPlanLoader");
        f.setAccessible(true);
        f.set(execute, loader);
    }

    private static MetaDataServerConfiguration dummyMetaDataConfig()
    {
        MetaDataServerConfiguration cfg = new MetaDataServerConfiguration();
        cfg.sdlc = new ServerConnectionConfiguration();
        cfg.sdlc.host = "localhost";
        cfg.sdlc.port = 0;
        cfg.sdlc.prefix = "/api";
        return cfg;
    }

    private static DataQualityReconCachedPlanInput defaultInput()
    {
        DataQualityReconCachedPlanInput input = new DataQualityReconCachedPlanInput();
        input.packagePath = "meta::dataquality::TestRelationComparison";
        input.model = new PureModelContextPointer();
        return input;
    }
}
