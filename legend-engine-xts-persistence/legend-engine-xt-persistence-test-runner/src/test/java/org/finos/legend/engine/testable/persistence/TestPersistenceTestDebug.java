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

package org.finos.legend.engine.testable.persistence;

import org.apache.commons.io.FileUtils;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.TestableRunner;
import org.finos.legend.engine.testable.model.RunTestsTestableInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class TestPersistenceTestDebug
{
    private static final String SPEC = "src/test/resources/append-only/allow_duplicates/persistence_no_audit_success.txt";

    @Test
    void persistenceTestDebugsToTheStatementsItWouldIssue() throws IOException
    {
        List<TestDebug> debugs = debug();
        Assertions.assertEquals(1, debugs.size());
        TestExecutionPlanDebug debug = Assertions.assertInstanceOf(TestExecutionPlanDebug.class, debugs.get(0));
        Assertions.assertNull(debug.error, debug.error);
        Assertions.assertEquals("test::TestPersistence", debug.testable);
        Assertions.assertNull(debug.executionPlan);
        Assertions.assertTrue(debug.debug.stream().anyMatch(statement -> statement.startsWith("INSERT INTO")), () -> String.join("\n", debug.debug));
    }

    @Test
    void persistenceTestDebugIsTheSameOnEveryRun() throws IOException
    {
        Assertions.assertEquals(((TestExecutionPlanDebug) debug().get(0)).debug, ((TestExecutionPlanDebug) debug().get(0)).debug);
    }

    private static List<TestDebug> debug() throws IOException
    {
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(FileUtils.readFileToString(new File(SPEC), StandardCharsets.UTF_8));
        PureModel pureModel = Compiler.compile(data, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        RunTestsTestableInput input = new RunTestsTestableInput();
        input.testable = "test::TestPersistence";
        return new TestableRunner().debugTests(Collections.singletonList(input), pureModel, data).results;
    }
}
