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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.jline.reader.Parser;
import org.junit.Assert;
import org.junit.Test;

public class TestJLine3Parser
{
    @Test
    public void testParser()
    {
        Assert.assertEquals("#,>{},#,", parse("#>{}#").makeString(","));
        Assert.assertEquals("#,>{},#,->,", parse("#>{}#->").makeString(","));
        Assert.assertEquals("#,>{},#,->,f", parse("#>{}#->f").makeString(","));
        Assert.assertEquals("#,>{},#, ,->,", parse("#>{}# ->").makeString(","));
        Assert.assertEquals("#,>{},#,.,", parse("#>{}#.").makeString(","));
        Assert.assertEquals("#,>{},#,.,gh", parse("#>{}#.gh").makeString(","));
        Assert.assertEquals("#,>{},#,.,gh,(,ok,),", parse("#>{}#.gh(ok)").makeString(","));
        Assert.assertEquals("#,>{},#,.,gh,(,ok,),->,", parse("#>{}#.gh(ok)->").makeString(","));
    }

    public MutableList<String> parse(String content)
    {
        return Lists.mutable.withAll(new JLine3Parser().parse(content, content.length(), Parser.ParseContext.COMPLETE).words());
    }

}
