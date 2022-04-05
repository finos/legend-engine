package org.finos.legend.engine.testable.service.extension;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_Testable;

import java.util.List;

public class ServiceTestableRunnerExtension implements TestableRunnerExtension
{
    private String pureVersion = PureClientVersions.production;

    @Override
    public TestRunner getTestRunner(Root_meta_pure_test_Testable testable)
    {
        if (testable instanceof Root_meta_legend_service_metamodel_Service)
        {
            return new ServiceTestRunner((Root_meta_legend_service_metamodel_Service) testable, pureVersion);
        }
        return null;
    }

    public List<TestResult> executeAllTest(Root_meta_pure_test_Testable testable, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        if (!(testable instanceof Root_meta_legend_service_metamodel_Service))
        {
            throw new UnsupportedOperationException("Expected Service testable. Found : " + testable.getName());
        }

        ServiceTestRunner testRunner = new ServiceTestRunner((Root_meta_legend_service_metamodel_Service) testable, pureVersion);

        return ((Root_meta_legend_service_metamodel_Service) testable)._tests().flatCollect(testSuite -> {
            List<AtomicTestId> atomicTestIds = ((Root_meta_pure_test_TestSuite) testSuite)._tests().collect(test -> {
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
