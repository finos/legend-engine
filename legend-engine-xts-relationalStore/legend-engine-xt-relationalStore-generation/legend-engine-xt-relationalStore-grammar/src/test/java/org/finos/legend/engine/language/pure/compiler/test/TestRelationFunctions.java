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
                        "}","COMPILATION error at [7:30-32]: The column 'ide' can't be found in the relation (id:String)"
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
                        "}","COMPILATION error at [8:18-28]: The two relations are incompatible and can't be concatenated (id:String) and (otherCol:String"
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
                    "}","COMPILATION error at [7:26-28]: The column 'idw' can't be found in the relation (id:String)"
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
                "COMPILATION error at [7:51-52]: The column 'id' can't be found in the relation (newId:String)"
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
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + '1')\n" +
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
                        "   #>{a::A.tb}#->extend(~id:x|$x.id->toOne() + '1')\n" +
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
                        "   #>{a::A.tb}#->extend(~id:x|$x.ide->toOne() + '1')\n" +
                        "}",
                "COMPILATION error at [7:34-36]: The column 'ide' can't be found in the relation (id:String)"
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
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + '1')->filter(i|$i.nid == 'ok')\n" +
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
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + '1')->filter(i|$i.neid == 'ok')\n" +
                        "}",
                "COMPILATION error at [7:67-70]: The column 'neid' can't be found in the relation (id:String, nid:String)"
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
