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

package org.finos.legend.engine.test.runner.service;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestServiceTestRunner
{
    @Test
    public void testServiceTestRunner() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("legend-sdlc-test-services-with-tests.json"));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        PureModel pureModel = new PureModel(pureModelContextData, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        List<Service> services = pureModelContextData.streamAllElements().filter(e -> e instanceof Service).map(e -> (Service) e).collect(Collectors.toList());
        String succeedingService = "test::legend::service::execution::test::m2m::simpleJsonService";
        String failingService = "test::legend::service::execution::test::m2m::simpleFailingJsonService";
        Assert.assertEquals(Sets.mutable.with(succeedingService, failingService), Iterate.collect(services, Service::getPath, Sets.mutable.empty()));

        Map<String, List<RichServiceTestResult>> resultsByService = Maps.mutable.empty();
        services.stream().forEach(e -> runTest(e, pureModel, pureModelContextData, resultsByService));

        List<RichServiceTestResult> succeedingResults = resultsByService.get(succeedingService);
        Assert.assertNotNull(succeedingResults);
        Assert.assertEquals(1, succeedingResults.size());
        RichServiceTestResult succeedingResult = succeedingResults.get(0);
        Assert.assertEquals(succeedingService, succeedingResult.getServicePath());
        Assert.assertNull(succeedingResult.getOptionalMultiExecutionKey());
        Assert.assertEquals(Collections.emptyMap(), succeedingResult.getAssertExceptions());
        Assert.assertEquals(Collections.singletonMap("test0", org.finos.legend.engine.test.runner.shared.TestResult.SUCCESS), succeedingResult.getResults());

        List<RichServiceTestResult> failingResults = resultsByService.get(failingService);
        Assert.assertNotNull(failingResults);
        Assert.assertEquals(1, failingResults.size());
        RichServiceTestResult failingResult = failingResults.get(0);
        Assert.assertEquals(failingService, failingResult.getServicePath());
        Assert.assertNull(failingResult.getOptionalMultiExecutionKey());
        Assert.assertEquals(Collections.emptyMap(), failingResult.getAssertExceptions());
        Assert.assertEquals(Collections.singletonMap("test0", org.finos.legend.engine.test.runner.shared.TestResult.FAILURE), failingResult.getResults());
    }

    private void runTest(Service service, PureModel pureModel, PureModelContextData pureModelContextData, Map<String, List<RichServiceTestResult>> resultsByService)
    {
        ServiceTestRunner serviceTestRunner = new ServiceTestRunner(service, Tuples.pair(pureModelContextData, pureModel), PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(), Lists.immutable.empty(), LegendPlanTransformers.transformers, "vX_X_X");
        try {
            resultsByService.put(service.getPath(), serviceTestRunner.executeTests());
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}




