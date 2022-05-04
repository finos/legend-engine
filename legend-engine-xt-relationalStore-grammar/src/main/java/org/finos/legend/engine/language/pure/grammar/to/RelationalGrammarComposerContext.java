// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.apache.commons.lang3.StringUtils;

/**
 * There are 2 reasons why we have this specialized grammar composer context instead of using PureGrammarComposerContext
 * 1. Relational has its own grammar so there are things in PureGrammarComposerContext that might not be applicable to this context
 * 2. We want to do a small optimization to hide redundant database pointer (also because we don't persist `scopeInfo` in the parser
 * we have no protocol/state to get the database info from)
 */
public final class RelationalGrammarComposerContext
{
    private final String indentationString;
    private final String currentDatabase;

    private RelationalGrammarComposerContext(RelationalGrammarComposerContext.Builder builder)
    {
        this.indentationString = builder.indentationString;
        this.currentDatabase = builder.currentDatabase;
    }

    public PureGrammarComposerContext toPureGrammarComposerContext()
    {
        return PureGrammarComposerContext.Builder.newInstance().withIndentationString(this.indentationString).build();
    }

    public String getIndentationString()
    {
        return indentationString;
    }

    public String getCurrentDatabase()
    {
        return currentDatabase;
    }

    public static final class Builder
    {
        private String indentationString = "";
        private String currentDatabase;

        private Builder()
        {
            // hide constructor
        }

        public static RelationalGrammarComposerContext.Builder newInstance(PureGrammarComposerContext context)
        {
            RelationalGrammarComposerContext.Builder builder = new RelationalGrammarComposerContext.Builder();
            builder.indentationString = context.getIndentationString();
            return builder;
        }

        public static RelationalGrammarComposerContext.Builder newInstance(RelationalGrammarComposerContext context)
        {
            RelationalGrammarComposerContext.Builder builder = new RelationalGrammarComposerContext.Builder();
            builder.indentationString = context.getIndentationString();
            builder.currentDatabase = context.getCurrentDatabase();
            return builder;
        }

        public static RelationalGrammarComposerContext.Builder newInstance()
        {
            return new RelationalGrammarComposerContext.Builder();
        }

        public RelationalGrammarComposerContext build()
        {
            return new RelationalGrammarComposerContext(this);
        }

        public RelationalGrammarComposerContext.Builder withCurrentDatabase(String currentDatabase)
        {
            this.currentDatabase = currentDatabase;
            return this;
        }

        public RelationalGrammarComposerContext.Builder withIndentation(int count)
        {
            this.indentationString += StringUtils.repeat(" ", count);
            return this;
        }
    }
}
