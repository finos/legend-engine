package org.finos.legend.engine.language.protobuf3.grammar.from;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class Protobuf3ParserException extends RuntimeException
{
    private String message;
    private SourceInformation sourceInformation;

    public Protobuf3ParserException(String message, SourceInformation sourceInformation)
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
