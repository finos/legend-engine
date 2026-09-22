// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.junit.Test;

import static org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestLambdaRoundtrip.testLambda;
import static org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestLambdaRoundtrip.testLambdaPretty;

public class TestRelation
{
     @Test
    public void testRelationStoreAccessor()
    {
        testLambda("|#>{XX.OK}#");
    }

    @Test
    public void testRelationStoreAccessorAndFunction()
    {
        testLambda("|#>{path::Store.table}#->filter(c|$c.x)");
    }

    @Test
    public void testColumn()
    {
        testLambda("|#>{path::Store.table}#->rename(~a, ~b)");
    }

    @Test
    public void testColumnSpace()
    {
        testLambda("|#>{path::Store.table}#->rename(~'a space', ~b)");
    }

    @Test
    public void testColumnWithType()
    {
        testLambda("|#>{path::Store.table}#->rename(~a:Integer, ~b:String)");
    }

    @Test
    public void testColumnWithFunction()
    {
        testLambda("|#>{path::Store.table}#->extend(~a:c|'ok')");
    }

    @Test
    public void testColumnWithTwoFunctions()
    {
        testLambda("|#>{path::Store.table}#->extend(~a:c|'ok':x|'YO')");
    }

    @Test
    public void testColumnWithTwoFunctionsMultiParameters()
    {
        testLambda("|#>{path::Store.table}#->extend(~a:{p, f, r|'ok'}:x|'YO')");
    }

    @Test
    public void testColumnArray()
    {
        testLambda("|#>{path::Store.table}#->extend(~[a, b])");
    }

    @Test
    public void testSimpleProject()
    {
        testLambda("|test::Person.all()->project(~[mycol:x|$x.name])");
    }

    @Test
    public void testProject()
    {
        testLambda("|test::Person.all()->project(~[first:x|$x.name, second:x|$x.val])");
    }

    @Test
    public void testColSpecWithStereotype()
    {
        testLambda("|test::Person.all()->project(~[<<test::SampleProfile.important>> 'Person Name':x|$x.name])");
    }

    @Test
    public void testColSpecWithTaggedValue()
    {
        testLambda("|test::Person.all()->project(~[{test::SampleProfile.doc = 'model documentation'} 'Person Name':x|$x.name])");
    }

    @Test
    public void testColSpecWithStereotypeAndTaggedValue()
    {
        testLambda("|test::Person.all()->project(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'model documentation'} 'Person Name':x|$x.name])");
    }

    @Test
    public void testColSpecArrayWithAnnotations()
    {
        testLambda("|test::Person.all()->project(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'name documentation'} 'Person Name':x|$x.name, <<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'age documentation'} 'Person Age':x|$x.age])");
    }

    @Test
    public void testGroupByColSpecWithAnnotations()
    {
        testLambda("|test::Person.all()->project(~['Person Name':x|$x.name, 'Person Age Sum':x|$x.age])->groupBy(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'name documentation'} 'Person Name'], ~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'age documentation'} 'Person Age Sum':x|$x.'Person Age Sum':x|$x->sum()])");
    }

    @Test
    public void testExtendColSpecWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->extend(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'derived column'} derived:c|'ok'])");
    }

    @Test
    public void testExtendColSpecArrayWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->extend(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'first derived'} a:c|'ok', <<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'second derived'} b:c|'no'])");
    }

    @Test
    public void testSelectColSpecArrayWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->select(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'kept column'} a, <<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'other kept'} b])");
    }

    @Test
    public void testRenameColSpecWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->rename(~<<test::SampleProfile.important>> {test::SampleProfile.doc = 'old name'} a, ~<<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'new name'} b)");
    }

    @Test
    public void testSortColSpecWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->sort([~<<test::SampleProfile.important>> {test::SampleProfile.doc = 'sort key'} a->ascending()])");
    }

    @Test
    public void testPivotColSpecWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->pivot(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'pivot key'} region], ~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'pivot value'} total:x|$x.amount:y|$y->sum()])");
    }

    @Test
    public void testAggregateTwoLambdaColSpecWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->groupBy(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'grouping key'} region], ~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'sum aggregate'} total:x|$x.amount:y|$y->sum()])");
    }

    @Test
    public void testOverColSpecArrayWithAnnotations()
    {
        testLambda("|#>{path::Store.table}#->extend(over(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'partition key'} a, <<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'ordering key'} b]), ~[running:{p, f, r|1}:x|$x->sum()])");
    }

    @Test
    public void testProjectColSpecWithAnnotationsPretty()
    {
        testLambdaPretty(
                "|test::Person.all()->project(\n" +
                        "  ~[\n" +
                        "     <<test::SampleProfile.important>> {test::SampleProfile.doc = 'name documentation'} 'Person Name': x|$x.name,\n" +
                        "     <<test::SampleProfile.deprecated>> {test::SampleProfile.doc = 'age documentation'} 'Person Age': x|$x.age\n" +
                        "   ]\n" +
                        ")"
        );
    }

    @Test
    public void testCast()
    {
        testLambda("|test::Person.all()->meta::pure::functions::lang::cast(@Relation<(someCol:String, someCol:String)>)");
    }

    @Test
    public void testOver()
    {
        testLambda("|over(~a)");
        testLambda("|over(~[a, b], !c->descending())");
        testLambda("|over(~[a, b], !c->descending(), [])");
        testLambdaPretty(
                "|over(\n" +
                        "  ~a\n" +
                        ")"
        );
        testLambdaPretty(
                "|over(\n" +
                        "  ~[\n" +
                        "     a,\n" +
                        "     b\n" +
                        "   ],\n" +
                        "  !c->descending()\n" +
                        ")"
        );
        testLambdaPretty(
                "|over(\n" +
                        "  ~[\n" +
                        "     a,\n" +
                        "     b\n" +
                        "   ],\n" +
                        "  !c->descending(),\n" +
                        "  []\n" +
                        ")"
        );
    }
}
