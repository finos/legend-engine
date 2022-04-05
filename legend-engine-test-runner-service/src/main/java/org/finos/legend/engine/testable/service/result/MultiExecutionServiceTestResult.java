package org.finos.legend.engine.testable.service.result;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;

import java.util.Map;

public class MultiExecutionServiceTestResult extends TestResult
{
    private Map<String, TestResult> keyIndexedTestResults = Maps.mutable.empty();

    public void addTestResult(String key, TestResult testResult)
    {
        if(keyIndexedTestResults.get(key) != null)
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
