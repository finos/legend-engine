// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class FlatDataErrorListener extends BaseErrorListener
{
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
    {
        if (e != null && e.getOffendingToken() != null && e instanceof InputMismatchException)
        {
            throw new FlatDataSchemaParseException("Unexpected token: " + ((Token) offendingSymbol).getText(), line, charPositionInLine + 1);
        }
        else if (e == null && offendingSymbol instanceof Token && (msg.startsWith("extraneous input") || msg.startsWith("missing ")))
        {
            throw new FlatDataSchemaParseException("Unexpected token: " + ((Token) offendingSymbol).getText(), line, charPositionInLine + 1);
        }
        else
        {
            throw new FlatDataSchemaParseException(msg, line, charPositionInLine + 1);
        }
    }
}
