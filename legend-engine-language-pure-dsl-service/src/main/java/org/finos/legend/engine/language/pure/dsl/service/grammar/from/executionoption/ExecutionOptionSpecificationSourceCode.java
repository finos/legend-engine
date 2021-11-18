package org.finos.legend.engine.language.pure.dsl.service.grammar.from.executionoption;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class ExecutionOptionSpecificationSourceCode
{
    private final String code;
    private final String type;
    private final SourceInformation sourceInformation;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public ExecutionOptionSpecificationSourceCode(String code, String type, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation) {
        this.code = code;
        this.type = type;
        this.sourceInformation = sourceInformation;
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public String getCode()
    {
        return code;
    }

    public String getType()
    {
        return type;
    }

    public SourceInformation getSourceInformation()
    {
        return sourceInformation;
    }

    public ParseTreeWalkerSourceInformation getWalkerSourceInformation()
    {
        return walkerSourceInformation;
    }
}
