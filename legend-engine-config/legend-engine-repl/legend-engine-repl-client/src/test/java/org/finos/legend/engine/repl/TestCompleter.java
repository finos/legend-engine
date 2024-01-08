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

import org.finos.legend.engine.repl.autocomplete.Completer;
import org.junit.Assert;
import org.junit.Test;

public class TestCompleter
{
    @Test
    public void testNonFunction()
    {
        Assert.assertEquals("[a::A , >{a::A.]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>").getCompletion().makeString(", "));
        Assert.assertEquals("[a::other , >{a::other.], [a::ABC , >{a::ABC.]", new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))").complete("#>{a").getCompletion().makeString(", "));
        Assert.assertEquals("[a::ABC , >{a::ABC.]", new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))").complete("#>{a::A").getCompletion().makeString(", "));
    }

    @Test
    public void testRelationAccessor()
    {
        Assert.assertEquals("[tab , tab}#]", new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))").complete("#>{a::A.t").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table tab(col VARCHAR(200)))").complete("#>{a::A.x").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))").complete("#>{a::A.tab}#").getCompletion().makeString(", "));
    }

    @Test
    public void testAutocompleteFunctionParameter()
    {
        Assert.assertEquals("[test::test , test::test)]", new Completer(db + connection + runtime).complete("#>{test::TestDatabase.tb}#->from(").getCompletion().makeString(", "));
        Assert.assertEquals("[test::test , test::test)]", new Completer(db + connection + runtime).complete("#>{test::TestDatabase.tb}#->from(te").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer(db + connection + runtime).complete("#>{test::TestDatabase.tb}#->from(zte").getCompletion().makeString(", "));
    }

    @Test
    public void testArrowRelation()
    {
        Assert.assertEquals("[distinct , distinct(], [drop , drop(], [extend , extend(], [filter , filter(], [from , from(], [groupBy , groupBy(], [join , join(], [limit , limit(], [rename , rename(], [size , size(], [slice , slice(], [sort , sort(]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->").getCompletion().makeString(", "));
        Assert.assertEquals("[size , size(], [slice , slice(], [sort , sort(]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->s").getCompletion().makeString(", "));
    }

    @Test
    public void testArrowOnType()
    {
        Assert.assertEquals("[project , project(]", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->fu").getCompletion().makeString(", "));
    }

    @Test
    public void testArrowDeep()
    {
        Assert.assertEquals("[contains , contains(], [startsWith , startsWith(], [endsWith , endsWith(], [toLower , toLower(], [toUpper , toUpper(], [lpad , lpad(], [rpad , rpad(], [parseInteger , parseInteger(], [parseFloat , parseFloat(]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(f|$f.col->").getCompletion().makeString(", "));
    }

    @Test
    public void testDeepWithCompilationError()
    {
        Assert.assertEquals("COMPILATION error at [6:26-49]: Can't find a match for function 'plus(Any[2])'", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'p'+$x.col->startsWith('x'))->fr").getEngineException().toPretty());
    }

    @Test
    public void testArrowPostCol()
    {
        Assert.assertEquals("[ascending , ascending(],[descending , descending(]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->sort(~col->").getCompletion().makeString(","));
    }


    //--------
    // Filter
    //--------
    @Test
    public void testDotInFilter()
    {
        Assert.assertEquals("[name , name], [other , other]", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|$x.").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilterDeepClass()
    {
        Assert.assertEquals("[name , name], [other , other]", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|'x'+[1,2]->map(z|$z+$x.").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilterDeepRelation()
    {
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.").getCompletion().makeString(", "));
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.co").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.z").getCompletion().makeString(", "));
    }


    //--------
    // Rename
    //--------
    @Test
    public void testRenameFirstParam()
    {
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->rename(~").getCompletion().makeString(", "));
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->rename(~co").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->rename(~x").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->rename(~col,~").getCompletion().makeString(", "));
    }



    //--------
    // Extend
    //--------
    @Test
    public void testDotInExtend()
    {
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->extend(~x:y|$y.").getCompletion().makeString(", "));
        Assert.assertEquals("[col , col]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->extend(~[x:y|$y.").getCompletion().makeString(", "));
    }

    //---------
    // GroupBy
    //---------
    @Test
    public void testGroupBy()
    {
        Assert.assertEquals("[col , col], [val , val]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->groupBy(~").getCompletion().makeString(", "));
        Assert.assertEquals("[col , col], [val , val]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->groupBy(~col, ~z:x|$x.").getCompletion().makeString(", "));
        Assert.assertEquals("[val , val]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.v").getCompletion().makeString(", "));
        Assert.assertEquals("[sum , sum(], [count , count(]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.val:y|$y->").getCompletion().makeString(", "));
    }


    //------
    // Join
    //------
    @Test
    public void testJoin()
    {
        Assert.assertEquals("[a::A , >{a::A.]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>").getCompletion().makeString(", "));
        Assert.assertEquals("[t , t}#]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.").getCompletion().makeString(", "));
        Assert.assertEquals("[JoinKind , JoinKind.]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.t}#, ").getCompletion().makeString(", "));
        Assert.assertEquals("[LEFT , LEFT], [INNER , INNER]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.").getCompletion().makeString(", "));
        Assert.assertEquals("[col , col], [val , val]", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER, {a,b|$a.").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER,").getCompletion().makeString(", "));
        Assert.assertEquals("[k , k], [o , o]", new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))").complete("#>{a::A.t}#->join(#>{a::A.t2}#, JoinKind.INNER, {a,b|$a.col == $b.").getCompletion().makeString(", "));
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

}
