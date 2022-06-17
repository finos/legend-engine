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

package org.finos.legend.engine.testable.service.extension;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.ServiceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Testable;

import java.util.List;

public class ServiceTestableRunnerExtension implements TestableRunnerExtension
{
    private String pureVersion = PureClientVersions.production;

    @Override
    public String getSupportedClassifierPath()
    {
        return ServiceProtocolExtension.SERVICE_CLASSIFIER_PATH;
    }

    @Override
    public TestRunner getTestRunner(Testable testable)
    {
        if (testable instanceof Root_meta_legend_service_metamodel_Service)
        {
            return new ServiceTestRunner((Root_meta_legend_service_metamodel_Service) testable, pureVersion);
        }
        return null;
    }

    public List<TestResult> executeAllTest(Testable testable, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        if (!(testable instanceof Root_meta_legend_service_metamodel_Service))
        {
            throw new UnsupportedOperationException("Expected Service testable. Found : " + testable.getName());
        }

        ServiceTestRunner testRunner = new ServiceTestRunner((Root_meta_legend_service_metamodel_Service) testable, pureVersion);

        return ((Root_meta_legend_service_metamodel_Service) testable)._tests().flatCollect(testSuite ->
        {
            List<AtomicTestId> atomicTestIds = ((Root_meta_pure_test_TestSuite) testSuite)._tests().collect(test ->
            {
                AtomicTestId id = new AtomicTestId();
                id.testSuiteId = testSuite._id();
                id.atomicTestId = test._id();
                return id;
            }).toList();
            return testRunner.executeTestSuite((Root_meta_pure_test_TestSuite) testSuite, atomicTestIds, pureModel, pureModelContextData);
        }).toList();
    }

    public void setPureVersion(String pureVersion)
    {
        this.pureVersion = pureVersion;
    }
}
