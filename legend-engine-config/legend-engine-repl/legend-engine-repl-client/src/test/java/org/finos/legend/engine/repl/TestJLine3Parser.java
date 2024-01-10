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
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.junit.Assert;
import org.junit.Test;

public class TestJLine3Parser
{
    @Test
    public void testParser()
    {
        Assert.assertEquals("''  *  0  *  #,>{},#,", parse("#>{}#"));
        Assert.assertEquals("''  *  0  *  #,>{aaa,.,aaa},#,", parse("#>{aaa.aaa}#"));
        Assert.assertEquals("''  *  0  *  #,>{},#,->,", parse("#>{}#->"));
        Assert.assertEquals("'f'  *  1  *  #,>{},#,->,f", parse("#>{}#->f"));
        Assert.assertEquals("''  *  0  *  #,>{},#, ,->,", parse("#>{}# ->"));
        Assert.assertEquals("''  *  0  *  #,>{},#,.,", parse("#>{}#."));
        Assert.assertEquals("'gh'  *  2  *  #,>{},#,.,gh", parse("#>{}#.gh"));
        Assert.assertEquals("''  *  0  *  #,>{},#,.,gh,(,", parse("#>{}#.gh("));
        Assert.assertEquals("''  *  0  *  #,>{},#,.,gh,(,ok,),", parse("#>{}#.gh(ok)"));
        Assert.assertEquals("''  *  0  *  #,>{},#,.,gh,(,ok,),->,", parse("#>{}#.gh(ok)->"));
        Assert.assertEquals("''aaa a'  *  6  *  #,>{},#,.,'aaa a", parse("#>{}#.'aaa a"));
        Assert.assertEquals("'f'  *  1  *  #,>{},#,.,'aaa a',->,f", parse("#>{}#.'aaa a'->f"));
        Assert.assertEquals("''  *  0  *  #,>{test::TestDatabase,.,TEST0},#,->,groupBy,(,~,[,", parse("#>{test::TestDatabase.TEST0}#->groupBy(~["));
    }

    public String parse(String content)
    {
        ParsedLine line = new JLine3Parser().parse(content, content.length(), Parser.ParseContext.COMPLETE);
        return "'" + line.word() + "'  *  " + line.wordCursor() + "  *  " + Lists.mutable.withAll(line.words()).makeString(",");
    }

}
