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

package org.finos.legend.engine.repl;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.relational.autocomplete.RelationalCompleterExtension;
import org.junit.Assert;
import org.junit.Test;

public class TestCompleter
{
    @Test
    public void testRelationAccessor()
    {
        Assert.assertEquals("[a::A , {a::A]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>")));
        Assert.assertEquals("[a::other , ::other], [a::ABC , ::ABC]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a")));
        Assert.assertEquals("[a::ABC , BC]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A")));
        Assert.assertEquals("[a::ABC , ]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC")));
        Assert.assertEquals("[t , t}]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.")));
        Assert.assertEquals("[tab , ab}]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.x")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.tab}#")));
    }

    @Test
    public void testAutocompleteFunctionParameter()
    {
        Assert.assertEquals("[test::test , test::test)]", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(")));
        Assert.assertEquals("[test::test , st::test)]", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(te")));
        Assert.assertEquals("", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(zte")));
    }

    @Test
    public void testArrowOnFunction()
    {
        Assert.assertEquals("[distinct , distinct], [drop , drop], [select , select], [extend , extend], [filter , filter], [from , from], [groupBy , groupBy], [join , join], [limit , limit], [rename , rename], [size , size], [slice , slice], [sort , sort]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|$x.col == 'oo')->")));
        Assert.assertEquals("PARSER error at [6:1-23]: parsing error", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->limit(10)-").getEngineException().toPretty());
    }

    @Test
    public void testArrowRelation()
    {
        Assert.assertEquals("[distinct , distinct], [drop , drop], [select , select], [extend , extend], [filter , filter], [from , from], [groupBy , groupBy], [join , join], [limit , limit], [rename , rename], [size , size], [slice , slice], [sort , sort]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->")));
        Assert.assertEquals("[select , elect], [size , ize], [slice , lice], [sort , ort]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->s")));
    }

    @Test
    public void testArrowDeep()
    {
        Assert.assertEquals("[contains , contains], [startsWith , startsWith], [endsWith , endsWith], [toLower , toLower], [toUpper , toUpper], [lpad , lpad], [rpad , rpad], [parseInteger , parseInteger], [parseFloat , parseFloat]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(f|$f.col->")));
    }

    @Test
    public void testErrors()
    {
        Assert.assertEquals("PARSER error at [6:21]: Unexpected token", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(*").getEngineException().toPretty());
        Assert.assertEquals("PARSER error at [6:1-21]: parsing error", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(!").getEngineException().toPretty());
        Assert.assertEquals("COMPILATION error at [6:1-12]: The store 'a::As' can't be found.", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::As.t}#->select(~").getEngineException().toPretty());
    }

    @Test
    public void testDeepWithCompilationError()
    {
        Assert.assertEquals("COMPILATION error at [6:26-49]: Can't find a match for function 'plus(Any[2])'", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'p'+$x.col->startsWith('x'))->fr").getEngineException().toPretty());
        Assert.assertEquals("COMPILATION error at [6:22-24]: Can't find type 'x'", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~x:x.").getEngineException().toPretty());
    }

    @Test
    public void testArrowPostCol()
    {
        Assert.assertEquals("[ascending , ascending], [descending , descending]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->sort(~col->")));
    }

    @Test
    public void testMultiLevelFunction()
    {
        Assert.assertEquals("[select , t]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col])->join(#>{a::A.t}#->selec")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col])->join(#>{a::A.t}#->select(~")));
    }

    //--------
    // Filter
    //--------
    @Test
    public void testDotInFilterDeepRelation()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.")));
        Assert.assertEquals("[col , l]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.co")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.z")));
        Assert.assertEquals("['na col' ,  col']", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"na col\" VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|$x.'na")));
    }


    //--------
    // Rename
    //--------
    @Test
    public void testRenameFirstParam()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
        Assert.assertEquals("[col , l]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~co")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~x")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~col,~")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
    }


    //--------
    // Extend
    //--------
    @Test
    public void testDotInExtend()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~x:y|$y.")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~[x:y|$y.")));
    }

    //---------
    // GroupBy
    //---------
    @Test
    public void testGroupBy()
    {
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~[")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~[col,")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~z:x|$x.")));
        Assert.assertEquals("[val , al]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.v")));
        Assert.assertEquals("[sum , sum], [mean , mean], [average , average], [min , min], [max , max], [count , count], [percentile , percentile], [variancePopulation , variancePopulation], [varianceSample , varianceSample], [stdDevPopulation , stdDevPopulation], [stdDevSample , stdDevSample]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.val:y|$y->")));
    }


    //------
    // Join
    //------
    @Test
    public void testJoin()
    {
        Assert.assertEquals("[a::A , {a::A]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>")));
        Assert.assertEquals("[t , t}]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.")));
        Assert.assertEquals("[JoinKind , JoinKind.]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, ")));
        Assert.assertEquals("[LEFT , LEFT], [INNER , INNER]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER, {a,b|$a.")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER,")));
        Assert.assertEquals("[k , k], [o , o]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t2}#, JoinKind.INNER, {a,b|$a.col == $b.")));
        Assert.assertEquals("COMPILATION error at [6:14-17]: \"The relation contains duplicates: [val, col]\"", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER, {a,b|$a.val == $b.val})->").getEngineException().toPretty());
    }

    //--------
    // Select
    //--------
    @Test
    public void testSelect()
    {
        Assert.assertEquals("[col , ol]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~c")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col,")));
        Assert.assertEquals("[from , m]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"col space\" VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~'col space')->fro")));
        Assert.assertEquals("[from , m]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"col space\" VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~['col space'])->fro")));
    }


    private static String db = "###Relational\n" +
            "Database test::TestDatabase(Table tb(col VARCHAR(200)))\n";

    private static String connection = "###Connection\n" +
            "RelationalDatabaseConnection test::testConnection\n" +
            "{\n" +
            "   store: test::TestDatabase;" +
            "   specification: LocalH2{};" +
            "   type: H2;" +
            "   auth: DefaultH2;\n" +
            "}\n";

    private static String runtime = "###Runtime\n" +
            "Runtime test::test\n" +
            "{\n" +
            "   mappings : [];\n" +
            "   connections:\n" +
            "   [\n" +
            "       test::TestDatabase : [connection: test::testConnection]\n" +
            "   ];\n" +
            "}\n";

    private String checkResultNoException(CompletionResult completion)
    {
        Assert.assertNull(completion.getEngineException());
        return completion.getCompletion().makeString(", ");
    }
}
