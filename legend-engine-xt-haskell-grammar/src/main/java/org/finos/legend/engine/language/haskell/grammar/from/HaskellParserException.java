package org.finos.legend.engine.language.haskell.grammar.from;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class HaskellParserException extends RuntimeException
{
    private final String message;
    private final SourceInformation sourceInformation;

    public HaskellParserException(String message, SourceInformation sourceInformation)
    {
        this.message = message;
        this.sourceInformation = sourceInformation;
    }

    public String getMessage()
    {
        return message;
    }

    public SourceInformation getSourceInformation()
    {
        return sourceInformation;
    }

    @Override
    public String toString()
    {
        return "Parsing error: " + (sourceInformation == SourceInformation.getUnknownSourceInformation() || sourceInformation == null ? "" : " at " + sourceInformation.getMessage() + "") + (message == null ? "" : ": " + message);
    }
}
