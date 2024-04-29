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

    @Test
    public void testColumnValueDifferenceCompile()
    {
        test("Class test::Trade" +
                "{" +
                "   date : StrictDate[1];" +
                "   quantity : Float[1];" +
                "   id : Integer[1];" +
                "}" +
                "Class test::B" +
                "{" +
                "   z(){" +
                "   test::Trade.all()" +
                "       ->groupBy([x|$x.date->adjust(0, DurationUnit.DAYS)]," +
                "       [ agg(x | $x.quantity, y | $y->sum()), agg(x | $x.id, y | $y->count())]," +
                "       ['tradeDate','quantity','count'])" +
                "       ->columnValueDifference(" +
                "           test::Trade.all()" +
                "               ->groupBy([x|$x.date->adjust(0, DurationUnit.DAYS)]," +
                "               [ agg(x | $x.quantity, y | $y->sum()), agg(x | $x.id, y | $y->count())]," +
                "               ['tradeDate','quantity','count'])," +
                "           ['tradeDate'],['quantity','count'])" +
                "   }:meta::pure::tds::TabularDataSet[1];" +
                "}");
    }

    @Test
    public void testRowValueDifferenceCompile()
    {
        test("Class test::Trade" +
                "{" +
                "   date : StrictDate[1];" +
                "   quantity : Float[1];" +
                "   id : Integer[1];" +
                "}" +
                "Class test::B" +
                "{" +
                "   z(){" +
                "   test::Trade.all()" +
                "       ->groupBy([x|$x.date->adjust(0, DurationUnit.DAYS)]," +
                "       [ agg(x | $x.quantity, y | $y->sum()), agg(x | $x.id, y | $y->count())]," +
                "       ['tradeDate','quantity','count'])" +
                "       ->rowValueDifference(" +
                "           test::Trade.all()" +
                "               ->groupBy([x|$x.date->adjust(0, DurationUnit.DAYS)]," +
                "               [ agg(x | $x.quantity, y | $y->sum()), agg(x | $x.id, y | $y->count())]," +
                "               ['tradeDate','quantity','count'])," +
                "           ['tradeDate'],['quantity','count'])" +
                "   }:meta::pure::tds::TabularDataSet[1];" +
                "}");
    }

    @Test
    public void testCompile_zScore()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<String,Integer>(first = 'student1', second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])->meta::pure::tds::extensions::zScore([], ['score'], ['score zScore']);\n" +
                "   true;\n" +
                "}\n");
    }

    @Test
    public void testCompile_iqrClassify()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<String,Integer>(first = 'student1', second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])->meta::pure::tds::extensions::iqrClassify([], 'score', 'irq_classification');\n" +
                "   true;\n" +
                "}\n");
    }

    @Test
    public void testCompile_columnValueDifference1()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<Integer,Integer>(first = 1, second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])\n" +
                "               ->meta::pure::tds::extensions::columnValueDifference(\n" +
                "                       ^Pair<Integer,Integer>(first = 1, second = 6)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')]),\n" +
                "                       ['student'], ['score']);\n" +
                "   true;\n" +
                "}\n");
    }

    @Test
    public void testCompile_columnValueDifference2()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<Integer,Integer>(first = 1, second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])\n" +
                "               ->meta::pure::tds::extensions::columnValueDifference(\n" +
                "                       ^Pair<Integer,Integer>(first = 1, second = 6)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')]),\n" +
                "                       ['student'], ['score'], ['score diff']);\n" +
                "   true;\n" +
                "}\n");
    }

    @Test
    public void testCompile_extendWithDigestOnColumns1()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<Integer,Integer>(first = 1, second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])\n" +
                "               ->meta::pure::tds::extensions::extendWithDigestOnColumns('digest');\n" +
                "   true;\n" +
                "}\n");
    }

    @Test
    public void testCompile_extendWithDigestOnColumns2()
    {
        // Faulty stereotype
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "function xx::myFunc() : Boolean[1] {\n" +
                "   ^Pair<Integer,Integer>(first = 1, second = 5)->project([col(x|$x.first, 'student'), col(x|$x.second, 'score')])\n" +
                "               ->meta::pure::tds::extensions::extendWithDigestOnColumns(['student'], meta::pure::functions::hash::HashType.MD5, 'digest');\n" +
                "   true;\n" +
                "}\n");
    }

}
