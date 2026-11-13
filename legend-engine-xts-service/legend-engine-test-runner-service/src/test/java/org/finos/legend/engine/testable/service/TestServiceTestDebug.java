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

package org.finos.legend.engine.testable.service;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.TestableRunner;
import org.finos.legend.engine.testable.model.RunTestsTestableInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestServiceTestDebug
{
    private static final String BASE_PATH = "testable/service-new-grammar/";

    @Test
    void singleExecutionTestDebugsToItsPlan() throws IOException
    {
        List<TestDebug> debugs = debug("service-baseResolver.pure", "service::svctd::BaseResolverService");
        Assertions.assertEquals(1, debugs.size());
        TestExecutionPlanDebug debug = assertPlanDebug(debugs.get(0), "service::svctd::BaseResolverService");
        Assertions.assertInstanceOf(SingleExecutionPlan.class, debug.executionPlan);
    }

    @Test
    void multiExecutionTestDebugsToThePlansOfTheKeysItRuns() throws IOException
    {
        Map<String, TestExecutionPlanDebug> byTest = debug("service-multiExec.pure", "service::svctd::MultiExecService").stream()
                .map(d -> assertPlanDebug(d, "service::svctd::MultiExecService"))
                .collect(Collectors.toMap(d -> d.atomicTestId, Function.identity()));
        Assertions.assertEquals(Sets.mutable.with("KEY_A", "KEY_B"), ((CompositeExecutionPlan) byTest.get("test_allKeys").executionPlan).executionPlans.keySet());
        Assertions.assertEquals(Sets.mutable.with("KEY_A"), ((CompositeExecutionPlan) byTest.get("test_onlyKeyA").executionPlan).executionPlans.keySet());
    }

    private static TestExecutionPlanDebug assertPlanDebug(TestDebug debug, String servicePath)
    {
        TestExecutionPlanDebug planDebug = Assertions.assertInstanceOf(TestExecutionPlanDebug.class, debug);
        Assertions.assertNull(planDebug.error, planDebug.error);
        Assertions.assertEquals(servicePath, planDebug.testable);
        Assertions.assertEquals("testSuite_1", planDebug.testSuiteId);
        Assertions.assertNotNull(planDebug.executionPlan);
        return planDebug;
    }

    private static List<TestDebug> debug(String serviceFile, String servicePath) throws IOException
    {
        PureModelContextData data;
        try (InputStream stream = TestServiceTestDebug.class.getClassLoader().getResourceAsStream(BASE_PATH + serviceFile);
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A"))
        {
            data = PureGrammarParser.newInstance().parseModel(scanner.next());
        }
        PureModel pureModel = Compiler.compile(data, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        RunTestsTestableInput input = new RunTestsTestableInput();
        input.testable = servicePath;
        return new TestableRunner().debugTests(Collections.singletonList(input), pureModel, data).results;
    }
}
