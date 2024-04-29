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

import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.repl.autocomplete.parser.ParserFixer.fixCode;

public class TestParserFixer
{
    @Test
    public void testArithmetic()
    {
        Assert.assertEquals("1+1", fixCode("1+1"));
        Assert.assertEquals("1+MaGiCToKeN", fixCode("1+"));
        Assert.assertEquals("1*3-MaGiCToKeN", fixCode("1*3-"));
        Assert.assertEquals("1/3*MaGiCToKeN", fixCode("1/3*"));
        Assert.assertEquals("1+3/MaGiCToKeN", fixCode("1+3/"));
        Assert.assertEquals("1+(3/MaGiCToKeN)", fixCode("1+(3/"));
        Assert.assertEquals("!MaGiCToKeN", fixCode("!"));
    }

    @Test
    public void testIsland()
    {
        Assert.assertEquals("#MaGiCToKeN{}#", fixCode("#"));
        Assert.assertEquals("#>{}#", fixCode("#>"));
        Assert.assertEquals("#>{}#", fixCode("#>{"));
        Assert.assertEquals("#>{}#", fixCode("#>{}"));
        Assert.assertEquals("#>{a::A.t}#->fil()", fixCode("#>{a::A.t}#->fil"));
        Assert.assertEquals("#>{a::A.t}#->x(#>{}#)", fixCode("#>{a::A.t}#->x(#>"));
        Assert.assertEquals("#>{a::A.t}#->x(#>{a::A.MaGiCToKeN}#)", fixCode("#>{a::A.t}#->x(#>{a::A."));
    }

    @Test
    public void testBlocks()
    {
        Assert.assertEquals("$a->eval()", fixCode("$a->eval("));
        Assert.assertEquals("$a->filter(x|$x->startsWith(x|$x))", fixCode("$a->filter(x|$x->startsWith(x|$x"));
        Assert.assertEquals("$a->filter(x|$x.value->in([1,2]))", fixCode("$a->filter(x|$x.value->in([1,2"));
        Assert.assertEquals("$a->filter({x|$x.value->in([1,2])})", fixCode("$a->filter({x|$x.value->in([1,2"));
    }

    @Test
    public void testArrow()
    {
        Assert.assertEquals("$a->MaGiCToKeN()", fixCode("$a->"));
        Assert.assertEquals("$a->fu()", fixCode("$a->fu"));
        Assert.assertEquals("$a->theFunc()", fixCode("$a->theFunc("));
        Assert.assertEquals("$a->filter(x|$x->MaGiCToKeN())", fixCode("$a->filter(x|$x->"));
    }

    @Test
    public void testDot()
    {
        Assert.assertEquals("$a->map(x|$x.MaGiCToKeN)", fixCode("$a->map(x|$x."));
        Assert.assertEquals("$a->map(x| 1 + ($x.MaGiCToKeN))", fixCode("$a->map(x| 1 + ($x."));
        Assert.assertEquals("$a->map(x|$x->filter(|$x.MaGiCToKeN))", fixCode("$a->map(x|$x->filter(|$x."));
        Assert.assertEquals("#>{test::TestDatabase.TEST0}#->join(#>{test::TestDatabase.TEST0}#,JoinKind.INNER,{a,b|$a.MaGiCToKeN})", fixCode("#>{test::TestDatabase.TEST0}#->join(#>{test::TestDatabase.TEST0}#,JoinKind.INNER,{a,b|$a.MaGiCToKeN})"));
    }

    @Test
    public void testTilde()
    {
        Assert.assertEquals("$a->rename(~[MaGiCToKeN])", fixCode("$a->rename(~["));
        Assert.assertEquals("$a->rename(~[test,MaGiCToKeN])", fixCode("$a->rename(~[test,"));
        Assert.assertEquals("$a->rename(~MaGiCToKeN)", fixCode("$a->rename(~"));
        Assert.assertEquals("$a->rename(~xd:x|MaGiCToKeN)", fixCode("$a->rename(~xd:"));
        Assert.assertEquals("$a->extend(~[x:y|$y.MaGiCToKeN])", fixCode("$a->extend(~[x:y|$y."));
    }

    @Test
    public void testQuote()
    {
        Assert.assertEquals("$a->filter(a|$a.'w')", fixCode("$a->filter(a|$a.'w"));
    }

    @Test
    public void testSilentInCaseOfError()
    {
        // Too many closing )
        Assert.assertEquals("#>{test::TestDatabase.TEST0}#->filter(x|'eee'->toLower() + $x.GENDER->startsWith('s') == 'ww'))->MaGiCToKeN()", fixCode("#>{test::TestDatabase.TEST0}#->filter(x|'eee'->toLower() + $x.GENDER->startsWith('s') == 'ww'))->"));
    }

    @Test
    public void testPipe()
    {
        Assert.assertEquals("$a->map(x|MaGiCToKeN)", fixCode("$a->map(x|"));
    }

    @Test
    public void testComma()
    {
        Assert.assertEquals("$a->map(x|[1,MaGiCToKeN])", fixCode("$a->map(x|[1,"));
    }
}