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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestRelationFunctions extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void testFilter()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->filter(i|$i.id == 'ok')\n" +
                        "}"
        );
    }

    @Test
    public void testFilterError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->filter(i|$i.ide == 'ok')\n" +
                        "}", "COMPILATION error at [7:30-32]: The column 'ide' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testConcatenate()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->concatenate(#>{a::A.tb}#)\n" +
                        "}"
        );
    }

    @Test
    public void testConcatenateError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "Database a::B (Table tb(otherCol Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->concatenate(#>{a::B.tb}#)\n" +
                        "}", "COMPILATION error at [8:18-28]: The two relations are incompatible and can't be concatenated (id:Integer) and (otherCol:Integer)"
        );
    }

    @Test
    public void testRename()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id,~id2)\n" +
                        "}"
        );
    }

    @Test
    public void testRenameError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~idw,~id2)\n" +
                        "}", "COMPILATION error at [7:26-28]: The column 'idw' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testRenameCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id, ~newId)->filter(i|$i.newId == 'ok')\n" +
                        "}"
        );
    }

    @Test
    public void testRenameComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id, ~newId)->filter(i|$i.id == 'ok')\n" +
                        "}",
                "COMPILATION error at [7:51-52]: The column 'id' can't be found in the relation (newId:Integer)"
        );
    }

    @Test
    public void testExtend()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)\n" +
                        "}"
        );
    }

    @Test
    public void testExtendErrorDupColumns()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~id:x|$x.id->toOne() + 1)\n" +
                        "}",
                "COMPILATION error at [7:18-23]: The relation contains duplicates: [id]"
        );
    }

    @Test
    public void testExtendErrorWrongColumn()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~id:x|$x.ide->toOne() + 1)\n" +
                        "}",
                "COMPILATION error at [7:34-36]: The column 'ide' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testExtendCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)->filter(i|$i.nid < 3)\n" +
                        "}"
        );
    }

    @Test
    public void testExtendComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)->filter(i|$i.neid == 'ok')\n" +
                        "}",
                "COMPILATION error at [7:65-68]: The column 'neid' can't be found in the relation (id:Integer, nid:Integer)"
        );
    }


    @Test
    public void testSort()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(ascending(~id))\n" +
                        "}"
        );
    }

    @Test
    public void testSortErrorWrongColumn()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(ascending(~id2))\n" +
                        "}",
                "COMPILATION error at [7:34-36]: The column 'id2' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testSortErrorWrongParameter()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(~id2)\n" +
                        "}",
                "COMPILATION error at [7:18-21]: Can't find a match for function 'sort(RelationStoreAccessor[1],ColSpec[1])"
        );
    }

    @Test
    public void testSortErrorWrongParameterUnknownFunc()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(~id->desc())\n" +
                        "}",
                "COMPILATION error at [7:28-31]: Can't find a match for function 'desc(ColSpec[1])'"
        );
    }

    @Test
    public void testSortArray()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer, other VARCHAR(200)))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort([ascending(~id), descending(~other)])\n" +
                        "}"
        );
    }


    @Test
    public void testJoin()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.id == $b.id2})\n" +
                        "}"
        );
    }

    @Test
    public void testJoinErrorWrongCols()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.xid == $b.id2})\n" +
                        "}",
                "COMPILATION error at [7:95-97]: The column 'xid' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testJoinErrorWrongCols2()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.id == $b.eid2})\n" +
                        "}",
                "COMPILATION error at [7:104-107]: The column 'eid2' can't be found in the relation (id2:Integer, errr:String)"
        );
    }

    @Test
    public void testGroupBy()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())\n" +
                        "}"
        );
    }

    @Test
    public void testGroupByErrorCol()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[oteher], ~new : x|$x.id : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:28-33]: The column 'oteher' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testGroupByErrorAggMap()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.ied : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:48-50]: The column 'ied' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testGroupByErrorAggReduce()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.other : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:62-64]: Can't find a match for function 'sum(String[*])"
        );
    }

    @Test
    public void testGroupByCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())->filter(x|$x.new > 1)\n" +
                        "}"
        );
    }

    @Test
    public void testGroupByComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())->filter(x|$x.newE > 1)\n" +
                        "}",
                "COMPILATION error at [7:79-82]: The column 'newE' can't be found in the relation (other:String, new:Integer)"
        );
    }

    @Test
    public void testPivot()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())\n" +
                        "}"
        );
    }

    // TODO: @akphi casting syntax should be supported in engine as it works in Pure
//    @Test
//    public void testPivotCompose()
//    {
//        test(
//                "###Relational\n" +
//                        "Database a::A (" +
//                        "   Table tb(id Integer, other VARCHAR(200))" +
//                        ")\n" +
//                        "\n" +
//                        "###Pure\n" +
//                        "function test::f():Any[*]\n" +
//                        "{\n" +
//                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())->cast(@meta::pure::metamodel::relation::Relation<(new:Integer)>)\n" +
//                        "}"
//        );
//    }

    @Test
    public void testErrorPivotComposeWithoutCasting()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())->select(~many)\n" +
                        "}",
                "COMPILATION error at [7:73-76]: The column 'many' can't be found in the relation ()"
        );
    }

    @Test
    public void testPivotErrorCol()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[oteher], ~new : x|$x.id : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:26-31]: The column 'oteher' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testPivotErrorAggMap()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.ied : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:46-48]: The column 'ied' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testPivotErrorAggReduce()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.other : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:60-62]: Can't find a match for function 'sum(String[*])"
        );
    }

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Relational\n" +
                "Database anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }
}
