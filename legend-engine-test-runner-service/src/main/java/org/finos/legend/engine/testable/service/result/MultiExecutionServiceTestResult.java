//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.service.result;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;

import java.util.Map;

public class MultiExecutionServiceTestResult extends TestResult
{
    private Map<String, TestResult> keyIndexedTestResults = Maps.mutable.empty();

    public void addTestResult(String key, TestResult testResult)
    {
        if (keyIndexedTestResults.get(key) != null)
        {
            throw new RuntimeException("Test Result already present for key - " + key);
        }

        keyIndexedTestResults.put(key, testResult);
    }

    public Map<String, TestResult> getKeyIndexedTestResults()
    {
        return this.keyIndexedTestResults;
    }
}
