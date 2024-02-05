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

package org.finos.legend.engine.repl.client.jline3;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.regex.Pattern;

import static org.finos.legend.engine.repl.client.Client.drawCommand;
import static org.finos.legend.engine.repl.client.Client.drawPath;

public class JLine3Highlighter implements Highlighter
{
    @Override
    public AttributedString highlight(LineReader lineReader, String s)
    {
        if (s.startsWith("show"))
        {
            MutableList<String> split = Lists.mutable.with(s.split(" "));
            if (split.size() > 1)
            {
                AttributedStringBuilder ab = new AttributedStringBuilder();
                drawCommand(ab, "show");
                ab.append(" ");
                drawPath(ab, split.get(1));
                return ab.toAttributedString();
            }
        }

        AttributedStringBuilder ab = new AttributedStringBuilder();
        drawCommand(ab, s);
        return ab.toAttributedString();
    }

    @Override
    public void setErrorPattern(Pattern pattern)
    {

    }

    @Override
    public void setErrorIndex(int i)
    {

    }
}
