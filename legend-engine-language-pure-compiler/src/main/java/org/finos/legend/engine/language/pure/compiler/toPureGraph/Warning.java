package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class Warning
{
    public SourceInformation sourceInformation;
    public String message;

    public Warning(SourceInformation sourceInformation, String message)
    {
        this.sourceInformation = sourceInformation;
        this.message = message;
    }
}
