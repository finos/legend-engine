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

package org.finos.legend.engine.pure.repl.core;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collections;
import java.util.List;

/**
 * Represents the results of running Pure tests.
 */
public class TestResults
{
    public enum Status
    {
        COMPLETED,
        ERROR
    }

    public enum TestStatus
    {
        PASS,
        FAIL,
        ERROR,
        SKIPPED
    }

    private final Status status;
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    private final long durationMs;
    private final String errorMessage;
    private final List<TestResult> testResults;

    private TestResults(Builder builder)
    {
        this.status = builder.status;
        this.totalTests = builder.totalTests;
        this.passedTests = builder.passedTests;
        this.failedTests = builder.failedTests;
        this.skippedTests = builder.skippedTests;
        this.durationMs = builder.durationMs;
        this.errorMessage = builder.errorMessage;
        this.testResults = Collections.unmodifiableList(builder.testResults);
    }

    public Status getStatus()
    {
        return status;
    }

    public int getTotalTests()
    {
        return totalTests;
    }

    public int getPassedTests()
    {
        return passedTests;
    }

    public int getFailedTests()
    {
        return failedTests;
    }

    public int getSkippedTests()
    {
        return skippedTests;
    }

    public long getDurationMs()
    {
        return durationMs;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public List<TestResult> getTestResults()
    {
        return testResults;
    }

    public boolean allPassed()
    {
        return status == Status.COMPLETED && failedTests == 0;
    }

    /**
     * Returns a formatted string representation for display.
     */
    public String toDisplayString()
    {
        StringBuilder sb = new StringBuilder();

        if (status == Status.ERROR)
        {
            sb.append("Error: ").append(errorMessage).append("\n");
            return sb.toString();
        }

        // Show individual test results
        for (TestResult result : testResults)
        {
            sb.append("  ").append(result.name);
            sb.append("... ").append(result.status.name());
            sb.append(" (").append(result.durationMs).append("ms)");
            sb.append("\n");

            if (result.status == TestStatus.FAIL && result.errorMessage != null)
            {
                sb.append("    Error: ").append(result.errorMessage).append("\n");
            }
        }

        // Summary
        sb.append("\n");
        sb.append(passedTests).append(" tests passed, ");
        sb.append(failedTests).append(" failed");
        if (skippedTests > 0)
        {
            sb.append(", ").append(skippedTests).append(" skipped");
        }
        sb.append(" (").append(durationMs).append("ms)");

        return sb.toString();
    }

    /**
     * Represents a single test result.
     */
    public static class TestResult
    {
        private final String name;
        private final TestStatus status;
        private final long durationMs;
        private final String consoleOutput;
        private final String errorMessage;

        public TestResult(String name, TestStatus status, long durationMs, String consoleOutput, String errorMessage)
        {
            this.name = name;
            this.status = status;
            this.durationMs = durationMs;
            this.consoleOutput = consoleOutput;
            this.errorMessage = errorMessage;
        }

        public String getName()
        {
            return name;
        }

        public TestStatus getStatus()
        {
            return status;
        }

        public long getDurationMs()
        {
            return durationMs;
        }

        public String getConsoleOutput()
        {
            return consoleOutput;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }
    }

    /**
     * Builder for TestResults.
     */
    public static class Builder
    {
        private Status status;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private long durationMs;
        private String errorMessage;
        private final MutableList<TestResult> testResults = Lists.mutable.empty();

        public Builder status(Status status)
        {
            this.status = status;
            return this;
        }

        public Builder totalTests(int totalTests)
        {
            this.totalTests = totalTests;
            return this;
        }

        public Builder durationMs(long durationMs)
        {
            this.durationMs = durationMs;
            return this;
        }

        public Builder errorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder addTestResult(TestResult result)
        {
            this.testResults.add(result);

            switch (result.getStatus())
            {
                case PASS:
                    passedTests++;
                    break;
                case FAIL:
                case ERROR:
                    failedTests++;
                    break;
                case SKIPPED:
                    skippedTests++;
                    break;
            }

            return this;
        }

        public TestResults build()
        {
            if (status == null)
            {
                status = Status.COMPLETED;
            }
            return new TestResults(this);
        }
    }
}
