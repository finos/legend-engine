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

package org.finos.legend.engine.language.sql.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractSqlBaseLexer extends Lexer
{
    protected final Deque<String> tags = new ArrayDeque<>();

    public AbstractSqlBaseLexer(CharStream input)
    {
        super(input);
    }

    public void pushTag()
    {
        tags.push(getText());
    }

    public boolean isTag()
    {
        return getText().equals(tags.peek());
    }

    public void popTag()
    {
        tags.pop();
    }
}
