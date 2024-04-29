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

package org.finos.legend.engine.test.runner.service;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.test.runner.shared.TestResult;

import java.util.Map;

public class RichServiceTestResult
{
    private final String servicePath;
    private final Map<String, TestResult> results;
    private final Map<String, Exception> assertExceptions;
    public final ExecutionPlan executionPlan;
    public final String javaCodeString;
    private String optionalMultiExecutionKey;


    /**
     * @param results A Map keyed by the generated service assert name based on indices;
     *                Values as the boolean assertion result.
     *                May design as Pair< Result, Boolean > in the future, now dropping Result due to serialization unsupportness.
     */
    public RichServiceTestResult(String servicePath, Map<String, TestResult> results, Map<String, Exception> assertExceptions, String optionalMultiExecutionKey,
                                 ExecutionPlan executionPlan, String javaCodeString)
    {
        this.servicePath = servicePath;
        this.optionalMultiExecutionKey = optionalMultiExecutionKey;
        this.results = results;
        this.assertExceptions = assertExceptions;
        this.executionPlan = executionPlan;
        this.javaCodeString = javaCodeString;
    }


    public String getServicePath()
    {
        return this.servicePath;
    }

    public String getOptionalMultiExecutionKey()
    {
        return this.optionalMultiExecutionKey;
    }

    public Map<String, TestResult> getResults()
    {
        return this.results;
    }

    public Map<String, Exception> getAssertExceptions()
    {
        return this.assertExceptions;
    }

    public String getJavaCodeString()
    {
        return javaCodeString;
    }

    public ExecutionPlan getExecutionPlan()
    {
        return executionPlan;
    }

    public void setOptionalMultiExecutionKey(String optionalMultiExecutionKey)
    {
        this.optionalMultiExecutionKey = optionalMultiExecutionKey;
    }

}