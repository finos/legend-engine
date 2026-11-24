// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e.coverage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestErrorCategorizer
{
    private static final String ENGINE_ERROR_PREFIX =
            "ERROR: java.lang.RuntimeException: java.lang.RuntimeException: Error calling the engine (url=pure/v1/compilation/lambdaRelationType, status=400): ";

    @Test
    void unsupportedMessageBehindEngineCallWrapperIsUnsupportedSyntax()
    {
        assertEquals(ErrorCategorizer.UNSUPPORTED_SYNTAX, ErrorCategorizer.categorize("ERROR", ENGINE_ERROR_PREFIX + "Unsupported Operation: Bit String"));
    }

    @Test
    void unsupportedMessageWithoutWrapperIsUnsupportedSyntax()
    {
        assertEquals(ErrorCategorizer.UNSUPPORTED_SYNTAX, ErrorCategorizer.categorize("ERROR", "Unsupported Operation: Bit String"));
    }

    @Test
    void columnNotFoundBehindEngineCallWrapperIsAliasNotFound()
    {
        assertEquals(ErrorCategorizer.ALIAS_NOT_FOUND,
                ErrorCategorizer.categorize("ERROR", ENGINE_ERROR_PREFIX + "The column 'salary' can't be found in the relation (name:String)"));
    }

    @Test
    void nullPointerBehindEngineCallWrapperIsMisc()
    {
        assertEquals(ErrorCategorizer.MISC, ErrorCategorizer.categorize("ERROR",
                "ERROR: java.lang.RuntimeException: Error calling the engine (url=pure/v1/execution/execute, status=500): java.lang.NullPointerException"));
    }

    @Test
    void failStateIsResultMismatch()
    {
        assertEquals(ErrorCategorizer.RESULT_MISMATCH, ErrorCategorizer.categorize("FAIL", null));
    }
}
