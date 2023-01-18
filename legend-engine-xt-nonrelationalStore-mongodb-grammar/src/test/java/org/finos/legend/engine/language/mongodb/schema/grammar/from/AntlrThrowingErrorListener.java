package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

// So we don't silently ignore antlr parsing errors
public class AntlrThrowingErrorListener extends BaseErrorListener
{
    public static final AntlrThrowingErrorListener INSTANCE = new AntlrThrowingErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e)
            throws ParseCancellationException
    {
        throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
    }
}