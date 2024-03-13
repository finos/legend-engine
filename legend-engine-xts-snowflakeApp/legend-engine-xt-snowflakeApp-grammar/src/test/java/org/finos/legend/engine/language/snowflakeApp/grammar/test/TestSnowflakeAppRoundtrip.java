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

package org.finos.legend.engine.language.snowflakeApp.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestSnowflakeAppRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSnowflakeApp()
    {
        test("###Snowflake\n" +
                "SnowflakeApp <<a::A.test>> {a::A.val = 'ok'} xx::MyApp\n" +
                "{\n" +
                "   applicationName : 'MyApp';\n" +
                "   function : zxx(Integer[1]):String[1];\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "   description : 'A super nice app!';\n" +
                "   activationConfiguration : a::b::connection;\n" +
                "}\n");
    }

    @Test
    public void testSnowflakeAppMinimal()
    {
        test("###Snowflake\n" +
                "SnowflakeApp xx::MyApp\n" +
                "{\n" +
                "   applicationName : 'MyApp';\n" +
                "   function : zxx(Integer[1]):String[1];\n" +
                "   ownership : Deployment { identifier: 'MyAppOwnership'};\n" +
                "}\n");
    }
}
