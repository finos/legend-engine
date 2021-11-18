package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;

public class DummyExecOptionWithParameters extends ExecutionOption
{
    public String code;

    // for jackson
    public DummyExecOptionWithParameters()
    {
    }

    public DummyExecOptionWithParameters(String code, SourceInformation sourceInformation)
    {
        this.code = code;
        this.sourceInformation = sourceInformation;
    }
}
