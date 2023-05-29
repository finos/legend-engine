// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.test;

import org.finos.legend.engine.language.pure.dsl.mastery.compiler.test.TestMasteryCompilationFromGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestMasteryGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{

    @Test
    public void masteryRoundTripFull()
    {
        testWithSectionInfoPreserved(TestMasteryCompilationFromGrammar.COMPLETE_CORRECT_MASTERY_MODEL);
    }

    @Test
    public void masteryRoundTripMinimum()
    {
        testWithSectionInfoPreserved(TestMasteryCompilationFromGrammar.MINIMUM_CORRECT_MASTERY_MODEL);
    }
}
