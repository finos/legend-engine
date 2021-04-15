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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestRelationalFunctionHandler
{
    @Test
    public void testJoinInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->join(test::A.all()->project(col(a|$a.name, 'Equity_Account_No')), meta::relational::metamodel::join::JoinType.INNER, { p, e | $p.getString('Account_No') == $e.getString('Equity_Account_No') })}:meta::pure::tds::TabularDataSet[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->join(test::A.all()->project(col(a|$a.name, 'Equity_Account_No')), meta::relational::metamodel::join::JoinType.INNER, { p, e | $p.getStriXng('Account_No') == $e.getString('Equity_Account_No') })}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:242-251]: Can't find property 'getStriXng' in class 'meta::pure::tds::TDSRow'");
    }
}
