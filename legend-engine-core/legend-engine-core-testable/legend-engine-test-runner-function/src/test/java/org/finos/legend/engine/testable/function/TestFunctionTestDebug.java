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

package org.finos.legend.engine.testable.function;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
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
import java.util.Scanner;

public class TestFunctionTestDebug
{
    @Test
    void relationalFunctionTestsDebugToTheirPlan() throws IOException
    {
        assertEveryTestDebugsToAPlan(debug("legend-testable-function-test-model-relational.pure", "model::PersonQuery__TabularDataSet_1_"), "model::PersonQuery__TabularDataSet_1_");
    }

    @Test
    void storelessFunctionTestsDebugToTheirPlan() throws IOException
    {
        assertEveryTestDebugsToAPlan(debug("legend-testable-function-test-model.pure", "model::Simple__String_1_"), "model::Simple__String_1_");
    }

    private static void assertEveryTestDebugsToAPlan(List<TestDebug> debugs, String functionPath)
    {
        Assertions.assertFalse(debugs.isEmpty());
        for (TestDebug debug : debugs)
        {
            TestExecutionPlanDebug planDebug = Assertions.assertInstanceOf(TestExecutionPlanDebug.class, debug);
            Assertions.assertNull(planDebug.error, planDebug.error);
            Assertions.assertEquals(functionPath, planDebug.testable);
            Assertions.assertNotNull(planDebug.testSuiteId);
            Assertions.assertInstanceOf(SingleExecutionPlan.class, planDebug.executionPlan);
        }
    }

    private static List<TestDebug> debug(String grammar, String functionPath) throws IOException
    {
        PureModelContextData data;
        try (InputStream stream = TestFunctionTestDebug.class.getClassLoader().getResourceAsStream("testable/" + grammar);
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A"))
        {
            data = PureGrammarParser.newInstance().parseModel(scanner.next());
        }
        PureModel pureModel = Compiler.compile(data, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        RunTestsTestableInput input = new RunTestsTestableInput();
        input.testable = functionPath;
        return new TestableRunner().debugTests(Collections.singletonList(input), pureModel, data).results;
    }
}
