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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

/**
 * Provide information on the line/column offset of the parse tree walker as well as source ID which can be used to uniquely identify (sub) element
 */
public class ParseTreeWalkerSourceInformation
{
    /**
     * If source information like end/start line column helps to identify (sub)element in the text then source ID helps
     * to identify in the graph. This is particularly useful when we have an UI, the UI while editing the graph in
     * non-text mode (i.e. form mode) can use this source ID to identify the (sub)element
     */
    private final String sourceId;
    /**
     * Specifies how many line to add to the start/end line of the source information derived in the context of the walker
     */
    private final int lineOffset;
    /**
     * Specifies how many column to add to the start/end column of the source information derived in the context of the walker
     * NOTE: this column offset only applies to the first line in the source code, subsequent line will not be affected.
     * For example:
     * ```{Example 1}
     * token = #{ a: model::Store }#
     * ```
     * ```{Example 2}
     * token = #{
     * a: model::Store
     * }#
     * ```
     * If #{ demotes some kind of grammar island and thus, we have to send everything in between `#{` and `}#` to another parser/walker
     * we will need to compute a new walker source information. For this, in either example, the column offset is the same (i.e. 15, counting
     * from the beginning to the end of  token `#{`).
     * <p>
     * In example 1, the island grammar walker will show that `a` has column 2, but since this is on the first line of the grammar, we should add
     * the walker column offset, so `a` column is effectively 17. Whereas in example 2, token `a` column should not be affected at all by the walker
     * source information, hence it's 9.
     */
    private final int columnOffset;

    private final boolean returnSourceInfo;

    private static final ParseTreeWalkerSourceInformation defaultWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder("", 0, 0).withReturnSourceInfo(true).build();
    
    private static final ParseTreeWalkerSourceInformation defaultWalkerWithStubSourceInformation = new ParseTreeWalkerSourceInformation.Builder("", 0, 0).withReturnSourceInfo(false).build();

    private ParseTreeWalkerSourceInformation(ParseTreeWalkerSourceInformation.Builder builder)
    {
        this.sourceId = builder.sourceId;
        this.lineOffset = builder.lineOffset;
        this.columnOffset = builder.columnOffset;
        this.returnSourceInfo = builder.returnSourceInfo;
    }

    public static ParseTreeWalkerSourceInformation DEFAULT_WALKER_SOURCE_INFORMATION(boolean returnSourceInfo)
    {
        return  returnSourceInfo?  defaultWalkerSourceInformation: defaultWalkerWithStubSourceInformation;
    }

    public static ParseTreeWalkerSourceInformation DEFAULT_WALKER_SOURCE_INFORMATION()
    {
        return  DEFAULT_WALKER_SOURCE_INFORMATION(true);
    }

    public String getSourceId()
    {
        return this.sourceId;
    }

    public int getLineOffset()
    {
        return this.lineOffset;
    }

    public int getColumnOffset()
    {
        return this.columnOffset;
    }

    public boolean getReturnSourceInfo()
    {
        return this.returnSourceInfo;
    }

    public SourceInformation getSourceInformation(ParserRuleContext parserRuleContext)
    {
        return returnSourceInfo ? getSourceInformation(this.sourceId, parserRuleContext.getStart(), parserRuleContext.getStop(), this.lineOffset, this.columnOffset): null;
    }

    public SourceInformation getSourceInformation(Token token)
    {
        return getSourceInformation(this.sourceId, token, token, this.lineOffset, this.columnOffset);
    }

    public SourceInformation getSourceInformation(Token startToken, Token endToken)
    {
        return getSourceInformation(this.sourceId, startToken, endToken, this.lineOffset, this.columnOffset);
    }

    private SourceInformation getSourceInformation(String sourceId, Token startToken, Token endToken, int lineOffset, int columnOffset)
    {
        // NOTE: column offset should only apply to the first line (see the example and note in `columnOffset` attribute above)
        int startLine = startToken.getLine() + lineOffset;
        int startColumn = startToken.getCharPositionInLine() + 1 + (startToken.getLine() == 1 ? columnOffset : 0);
        int endLine = endToken.getLine() + lineOffset;
        int endColumn = endToken.getCharPositionInLine() + endToken.getText().length() + (endToken.getLine() == 1 ? columnOffset : 0);
        return new SourceInformation(sourceId, startLine, startColumn, endLine, endColumn);
    }

    public static class Builder
    {
        private String sourceId;
        private int lineOffset;
        private int columnOffset;
        private boolean returnSourceInfo;

        public Builder(String sourceId, int lineOffset, int columnOffset)
        {
            this.sourceId = sourceId;
            this.lineOffset = lineOffset;
            this.columnOffset = columnOffset;
            this.returnSourceInfo = true;
        }

        public Builder(ParseTreeWalkerSourceInformation walkerSourceInformation)
        {
            this.sourceId = walkerSourceInformation.sourceId;
            this.columnOffset = walkerSourceInformation.columnOffset;
            this.lineOffset = walkerSourceInformation.lineOffset;
            this.returnSourceInfo = walkerSourceInformation.returnSourceInfo;
        }

        public ParseTreeWalkerSourceInformation.Builder withSourceId(String sourceId)
        {
            this.sourceId = sourceId;
            return this;
        }

        public ParseTreeWalkerSourceInformation.Builder withLineOffset(int lineOffset)
        {
            this.lineOffset = lineOffset;
            return this;
        }

        public ParseTreeWalkerSourceInformation.Builder withColumnOffset(int columnOffset)
        {
            this.columnOffset = columnOffset;
            return this;
        }

      public ParseTreeWalkerSourceInformation.Builder withReturnSourceInfo(boolean returnSourceInfo)
      {
        this.returnSourceInfo = returnSourceInfo;
        return this;
      }

        public ParseTreeWalkerSourceInformation build()
        {
            return new ParseTreeWalkerSourceInformation(this);
        }
    }
}
