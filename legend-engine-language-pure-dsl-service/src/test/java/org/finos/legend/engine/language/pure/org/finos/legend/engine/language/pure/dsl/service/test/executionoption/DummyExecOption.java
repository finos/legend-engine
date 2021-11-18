package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;

public class DummyExecOption extends ExecutionOption
{
    // for jackson
    public DummyExecOption()
    {
    }

    public DummyExecOption(SourceInformation sourceInformation)
    {
        this.sourceInformation = sourceInformation;
    }
}
