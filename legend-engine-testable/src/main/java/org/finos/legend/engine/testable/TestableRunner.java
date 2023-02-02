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

package org.finos.legend.engine.testable;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;
import org.finos.legend.engine.testable.model.UniqueTestId;
import org.finos.legend.engine.testable.model.RunTestsInput;
import org.finos.legend.engine.testable.model.RunTestsResult;
import org.finos.legend.engine.testable.model.RunTestsTestableInput;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.TestAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Testable;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

public class TestableRunner
{
    private final ModelManager modelManager;

    public TestableRunner(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    public RunTestsResult doTests(RunTestsInput input, MutableList<CommonProfile> profiles)
    {
        Pair<PureModelContextData, PureModel> modelAndData = modelManager.loadModelAndData(input.model, input.model instanceof PureModelContextPointer ? ((PureModelContextPointer) input.model).serializer.version : null, profiles, null);
        PureModel pureModel = modelAndData.getTwo();
        PureModelContextData data = modelAndData.getOne();

        RunTestsResult runTestsResult = new RunTestsResult();
        for (RunTestsTestableInput testableInput : input.testables)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement(testableInput.testable);
            if (!(packageableElement instanceof Testable))
            {
                throw new UnsupportedOperationException("Element '" + testableInput.testable + "' is not a testable element");
            }
            Testable testable = (Testable) packageableElement;
            List<UniqueTestId> testIds = testableInput.unitTestIds;
            List<String> testIdStrings = ListIterate.collect(testIds, id -> id.atomicTestId);

            TestRunner testRunner = TestableRunnerExtensionLoader.forTestable(testable);
            for (Test test : testable._tests())
            {
                // We run all testIds if no `unitTestIds` are provided
                if ((test instanceof Root_meta_pure_test_AtomicTest) && (testIds.isEmpty() || testIdStrings.contains(test._id())))
                {
                    runTestsResult.results.add(testRunner.executeAtomicTest((Root_meta_pure_test_AtomicTest) test, pureModel, data));
                }

                if (test instanceof Root_meta_pure_test_TestSuite)
                {
                    List<String> testIdsForSuite = ListIterate.collectIf(testIds, testId -> test._id().equals(testId.testSuiteId), testId -> testId.atomicTestId);
                    if (testIds.isEmpty() || !testIdsForSuite.isEmpty())
                    {
                        Root_meta_pure_test_TestSuite testSuite = (Root_meta_pure_test_TestSuite) test;
                        List<String> updatedTestIds = testIds.isEmpty()
                            ? testSuite._tests().collect(TestAccessor::_id).toList()
                            : testIdsForSuite;
                        runTestsResult.results.addAll(testRunner.executeTestSuite(testSuite, updatedTestIds, pureModel, data));
                    }
                }
            }
        }
        return runTestsResult;
    }
}
