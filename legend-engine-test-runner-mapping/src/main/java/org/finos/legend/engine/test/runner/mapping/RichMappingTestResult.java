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

package org.finos.legend.engine.test.runner.mapping;

import java.util.Optional;

import org.finos.legend.engine.test.runner.shared.TestResult;
import org.junit.ComparisonFailure;

public class RichMappingTestResult
{
    private String mappingPath;
    private String testName;
    private Exception exception;
    private Optional<String> expected;
    private Optional<String> actual;
    private TestResult result;

    public RichMappingTestResult()
    {
        // Jackson
    }

    public RichMappingTestResult(String mappingPath, String testName, String expected, String actual)
    {
        this.mappingPath = mappingPath;
        this.testName = testName;
        this.expected = Optional.ofNullable(expected);
        this.actual = Optional.ofNullable(actual);
        this.result = TestResult.SUCCESS;
        this.exception = null;
    }

    public RichMappingTestResult(String mappingPath, String testName, ComparisonFailure c)
    {
       this(mappingPath, testName, c.getExpected(), c.getActual());
       this.result = TestResult.FAILURE;
    }

    public RichMappingTestResult(String mappingPath, String testName, Exception e)
    {
        this(mappingPath, testName, null, null);
        this.result = TestResult.ERROR;
        this.exception = e;
    }

    public String getMappingPath()
    {
        return mappingPath;
    }

    public void setMappingPath(String mappingPath)
    {
        this.mappingPath = mappingPath;
    }

    public String getTestName()
    {
        return testName;
    }

    public void setTestName(String testName)
    {
        this.testName = testName;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
    }

    public void setResult(TestResult result)
    {
        this.result = result;
    }

    public Optional<String> getExpected()
    {
        return expected;
    }

    public void setExpected(Optional<String> expected)
    {
        this.expected = expected;
    }

    public Optional<String> getActual()
    {
        return actual;
    }

    public void setActual(Optional<String> actual)
    {
        this.actual = actual;
    }

    public TestResult getResult()
    {
        return result;
    }
}
