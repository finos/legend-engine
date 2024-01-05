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
        Assert.assertEquals(">{a::A.", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>").getCompletion().makeString(", "));
        Assert.assertEquals(">{a::ABC., >{a::other.", new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))").complete("#>{a").getCompletion().makeString(", "));
        Assert.assertEquals(">{a::ABC.", new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))").complete("#>{a::A").getCompletion().makeString(", "));
    }

    @Test
    public void testRelationAccessor()
    {
        Assert.assertEquals("tab}#", new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))").complete("#>{a::A.t").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table tab(col VARCHAR(200)))").complete("#>{a::A.x").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))").complete("#>{a::A.tab}#").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilter()
    {
        Assert.assertEquals("name, other", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|$x.").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilterDeep()
    {
        Assert.assertEquals("name, other", new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|'x'+[1,2]->map(z|$z+$x.").getCompletion().makeString(", "));
    }

    @Test
    public void testArrowRelation()
    {
//        Assert.assertEquals("func", new Completer("Class x::A{name:String[1];other:Integer[1];}").parse("x::A.all()->").makeString(", "));
//        Assert.assertEquals("nc", new Completer("Class x::A{name:String[1];other:Integer[1];}").parse("x::A.all()->fu").makeString(", "));
        Assert.assertEquals("distinct, drop, extend, filter, from, groupBy, join, limit, rename, size, slice, sort", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->").getCompletion().makeString(", "));
        Assert.assertEquals("size, slice, sort", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->s").getCompletion().makeString(", "));
    }

    @Test
    public void testArrowDeep()
    {
        Assert.assertEquals("contains, startsWith, endsWith, toLower, toUpper, lpad, rpad", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(f|$f.col->").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilterDeepRelation()
    {
        Assert.assertEquals("col", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.").getCompletion().makeString(", "));
    }

    @Test
    public void testDotInFilterDeepRelationTypeAhead()
    {
        Assert.assertEquals("col", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.co").getCompletion().makeString(", "));
        Assert.assertEquals("", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.z").getCompletion().makeString(", "));
    }

    @Test
    public void testDeepWithCompilationError()
    {
        Assert.assertEquals("COMPILATION error at [5:26-49]: Can't find a match for function 'plus(Any[2])'", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))").complete("#>{a::A.t}#->filter(x|'p'+$x.col->startsWith('x'))->fr").getEngineException().toPretty());
    }
}
