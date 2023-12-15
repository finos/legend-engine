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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestBuilderHelper
{
    public static <T extends TestSuite>  void validateNonEmptySuite(T suite)
    {
        if (suite.tests == null || suite.tests.isEmpty())
        {
            throw new EngineException("TestSuites should have at least 1 test", suite.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static <T extends AtomicTest> void validateNonEmptyTest(T test)
    {
        if (test.assertions == null || test.assertions.isEmpty())
        {
            throw new EngineException("Tests should have at least 1 assert", test.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static <T extends TestSuite>  void validateTestSuiteIdsList(List<T> suites, SourceInformation sourceInformation)
    {
        validateIds(ListIterate.collect(suites, suite -> suite.id), sourceInformation, "Multiple testSuites found with ids");
    }

    public static <T extends AtomicTest>  void validateTestIds(List<T> tests, SourceInformation sourceInformation)
    {
        validateIds(ListIterate.collect(tests, test -> test.id), sourceInformation, "Multiple tests found with ids");
    }

    public static void validateAssertionIds(List<TestAssertion> assertions, SourceInformation sourceInformation)
    {
        validateIds(ListIterate.collect(assertions, a -> a.id), sourceInformation, "Multiple assertions found with ids");
    }

    public static void validateIds(List<String> ids, SourceInformation sourceInformation, String message)
    {
        List<String> duplicateIds = ids.stream().filter(e -> Collections.frequency(ids, e) > 1).distinct().collect(Collectors.toList());
        if (!duplicateIds.isEmpty())
        {
            throw new EngineException(message + " : '" + String.join(",", duplicateIds) + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
    }
}
