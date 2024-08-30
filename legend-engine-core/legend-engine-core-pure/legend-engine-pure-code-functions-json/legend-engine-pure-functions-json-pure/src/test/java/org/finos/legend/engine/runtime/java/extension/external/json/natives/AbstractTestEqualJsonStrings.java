// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.runtime.java.extension.external.json.natives;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestEqualJsonStrings extends PureExpressionTest
{
    @Test
    public void testParseError()
    {
        assertExpressionRaisesPureException("Failed to parse JSON string. Invalid JSON string. Unexpected character (#) at position 25.",
                "assert(equalJsonStrings('{\"key\":42,\"anotherKey\":\"foo\"}','{\"anotherKey\":\"foo\",\"key\"#: 42}'), |'')");
    }

    @Test
    public void testSemanticallyDifferentJsonStrings()
    {
        assertExpressionRaisesPureException("JSON strings don't represent semantically same object \n" +
                        " expected: {\"key\":42,\"anotherKey\":\"foo\"}\n" +
                        " actual: {\"anotherKey\":\"foo\",\"key\": 1}",
                "assert(equalJsonStrings('{\"key\":42,\"anotherKey\":\"foo\"}','{\"anotherKey\":\"foo\",\"key\": 1}'), |'JSON strings don\\'t represent semantically same object \\n expected: {\"key\":42,\"anotherKey\":\"foo\"}\\n actual: {\"anotherKey\":\"foo\",\"key\": 1}')");
    }
}
