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

package org.finos.legend.engine.language.pure.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestTextGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testText()
    {
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  type: STRING;\n" +
                "  content: 'this is just for context';\n" +
                "}\n" +
                "\n" +
                "Text meta::pure::anotherText\n" +
                "{\n" +
                "  type: HTML;\n" +
                "  content: '<div>Inside Div</div>';\n" +
                "}\n" +
                "\n" +
                "Text meta::pure::anotherText2\n" +
                "{\n" +
                "  content: '<div>Inside Div</div>';\n" +
                "}\n"
        );
    }

    @Test
    public void testTextWithComplexContent()
    {
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  type: STRING;\n" +
                "  content: 'a more complex test. let\\'s start. the guy said \"We are going to open source! \" \\n Here we are in a new line';\n" +
                "}\n"
        );
    }
}
