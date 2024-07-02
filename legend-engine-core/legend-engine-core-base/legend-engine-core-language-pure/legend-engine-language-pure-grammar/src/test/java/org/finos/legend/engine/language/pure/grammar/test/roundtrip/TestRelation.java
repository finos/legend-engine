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
    public void testCast()
    {
        testLambda("|test::Person.all()->meta::pure::functions::lang::cast(@Relation<(someCol:String, someCol:String)>)");
    }
}
