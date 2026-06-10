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

package org.finos.legend.engine.testable.helper;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class TestResultHelper
{
    public static TestError newTestError(String atomicTestId, Throwable t)
    {
        return newTestError(null, null, atomicTestId, t);
    }

    public static TestError newTestError(String testable, String testSuiteId, String atomicTestId, Throwable t)
    {
        TestError error = new TestError();
        error.testable = testable;
        error.testSuiteId = testSuiteId;
        error.atomicTestId = atomicTestId;
        error.error = toErrorString(t);
        return error;
    }

    public static TestExecutionPlanDebug newTestExecutionPlanDebugError(String atomicTestId, Throwable t)
    {
        return newTestExecutionPlanDebugError(null, null, atomicTestId, t);
    }

    public static TestExecutionPlanDebug newTestExecutionPlanDebugError(String testable, String testSuiteId, String atomicTestId, Throwable t)
    {
        return newTestExecutionPlanDebugError(testable, testSuiteId, atomicTestId, null, null, t);
    }

    public static TestExecutionPlanDebug newTestExecutionPlanDebugError(String testable, String testSuiteId, String atomicTestId, ExecutionPlan executionPlan, List<String> debug, Throwable t)
    {
        TestExecutionPlanDebug error = new TestExecutionPlanDebug();
        error.testable = testable;
        error.testSuiteId = testSuiteId;
        error.atomicTestId = atomicTestId;
        error.executionPlan = executionPlan;
        error.debug = debug;
        error.error = toErrorString(t);
        return error;
    }

    public static String toErrorString(Throwable t)
    {
        if (t == null)
        {
            return null;
        }

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
