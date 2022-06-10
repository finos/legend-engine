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
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;
import org.finos.legend.engine.testable.model.DoTestsInput;
import org.finos.legend.engine.testable.model.DoTestsResult;
import org.finos.legend.engine.testable.model.DoTestsTestableInput;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_Test;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_Testable;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class TestableRunner
{
    private final ModelManager modelManager;

    public TestableRunner(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    public DoTestsResult doTests(DoTestsInput input, MutableList<CommonProfile> profiles)
    {
        Pair<PureModelContextData, PureModel> modelAndData = modelManager.loadModelAndData(input.model, input.model instanceof PureModelContextPointer ? ((PureModelContextPointer) input.model).serializer.version : null, profiles, null);
        PureModel pureModel = modelAndData.getTwo();
        PureModelContextData data = modelAndData.getOne();

        DoTestsResult doTestsResult = new DoTestsResult();
        for (DoTestsTestableInput testableInput : input.testables)
        {
            Root_meta_pure_test_Testable testable = (Root_meta_pure_test_Testable) pureModel.getPackageableElement(testableInput.testable);
            List<AtomicTestId> testIds = testableInput.unitTestIds;
            List<String> atomicTestIds = ListIterate.collect(testIds, id -> id.atomicTestId);
            Map<String, List<AtomicTestId>> testIdsBySuiteId = testIds.stream().collect(groupingBy(testId -> testId.testSuiteId));

            TestRunner testRunner = TestableRunnerExtensionLoader.forTestable(testable);
            for (Root_meta_pure_test_Test test : testable._tests())
            {
                if ((test instanceof Root_meta_pure_test_AtomicTest) && (testIds.isEmpty() || atomicTestIds.contains(test._id())))
                {
                    doTestsResult.results.add(testRunner.executeAtomicTest((Root_meta_pure_test_AtomicTest) test, pureModel, data));
                }
                if ((test instanceof Root_meta_pure_test_TestSuite) && (testIds.isEmpty() || testIdsBySuiteId.get(test._id()) != null))
                {
                    Root_meta_pure_test_TestSuite testSuite = (Root_meta_pure_test_TestSuite) test;
                    List<AtomicTestId> updatedTestIds;
                    if (testIds.isEmpty())
                    {
                        updatedTestIds = testSuite._tests().collect(pureTest ->
                        {
                            AtomicTestId id = new AtomicTestId();
                            id.testSuiteId = testSuite._id();
                            id.atomicTestId = pureTest._id();
                            return id;
                        }).toList();
                    }
                    else
                    {
                        updatedTestIds = testIdsBySuiteId.get(test._id());
                    }
                    doTestsResult.results.addAll(testRunner.executeTestSuite(testSuite, updatedTestIds, pureModel, data));
                }
            }
        }

        return doTestsResult;
    }
}
