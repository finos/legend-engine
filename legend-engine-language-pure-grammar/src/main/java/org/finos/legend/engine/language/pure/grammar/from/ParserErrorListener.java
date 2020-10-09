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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class ParserErrorListener extends BaseErrorListener
{
    public ParseTreeWalkerSourceInformation walkerSourceInformation;

    public ParserErrorListener(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e)
    {
        if (e != null && e.getOffendingToken() != null && e instanceof InputMismatchException)
        {
            msg = "Unexpected token";
        }
        else if (e == null || e.getOffendingToken() == null)
        {
            if (e == null && offendingSymbol instanceof Token && (msg.startsWith("extraneous input") || msg.startsWith("missing ")))
            {
                // when ANTLR detects unwanted symbol, it will not result in an error, but throw
                // `null` with a message like "extraneous input ... expecting ..."
                // NOTE: this is caused by us having INVALID catch-all symbol in the lexer
                // so anytime, INVALID token is found, it should cause this error
                // but because it is a catch-all rule, it only produces a lexer token, which is a symbol
                // we have to construct the source information manually
                SourceInformation sourceInformation = new SourceInformation(
                        this.walkerSourceInformation.getSourceId(),
                        line + this.walkerSourceInformation.getLineOffset(),
                        charPositionInLine + 1 + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0),
                        line + this.walkerSourceInformation.getLineOffset(),
                        charPositionInLine + 1 + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0) + ((Token) offendingSymbol).getStopIndex() - ((Token) offendingSymbol).getStartIndex());
                // NOTE: for some reason sometimes ANTLR report the end index of the token to be smaller than the start index so we must reprocess it here
                sourceInformation.startColumn = Math.min(sourceInformation.endColumn, sourceInformation.startColumn);
                msg = "Unexpected token";
                throw new EngineException(msg, sourceInformation, EngineErrorType.PARSER);
            }
            SourceInformation sourceInformation = new SourceInformation(
                    this.walkerSourceInformation.getSourceId(),
                    line + this.walkerSourceInformation.getLineOffset(),
                    charPositionInLine + 1 + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0),
                    line + this.walkerSourceInformation.getLineOffset(),
                    charPositionInLine + 1 + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0));
            throw new EngineException(msg, sourceInformation, EngineErrorType.PARSER);
        }
        Token offendingToken = e.getOffendingToken();
        SourceInformation sourceInformation = new SourceInformation(
                this.walkerSourceInformation.getSourceId(),
                line + this.walkerSourceInformation.getLineOffset(),
                charPositionInLine + 1 + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0),
                offendingToken.getLine() + this.walkerSourceInformation.getLineOffset(),
                charPositionInLine + offendingToken.getText().length() + (line == 1 ? this.walkerSourceInformation.getColumnOffset() : 0));
        throw new EngineException(msg, sourceInformation, EngineErrorType.PARSER);
    }
}