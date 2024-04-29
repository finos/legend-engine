// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.ide.api.execution.test;

import org.finos.legend.pure.m3.execution.test.TestStatus;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TestResult
{
    private CoreInstance testFunction;
    private String testParameterizationId;
    private String consoleOutput;
    private TestStatus status;

    TestResult(CoreInstance testFunction, String testParameterizationId, String consoleOutput, TestStatus status)
    {
        this.testFunction = testFunction;
        this.testParameterizationId = testParameterizationId;
        this.consoleOutput = consoleOutput;
        this.status = status;
    }

    public CoreInstance getTestFunction()
    {
        return this.testFunction;
    }

    public String getConsoleOutput()
    {
        return this.consoleOutput;
    }

    public TestStatus getStatus()
    {
        return this.status;
    }

    public String getTestParameterizationId()
    {
        return this.testParameterizationId;
    }
}
