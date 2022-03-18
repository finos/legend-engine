package org.finos.legend.engine.language.pure.compiler.api;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;

import java.util.List;

public class CompileResult
{
    public String message;
    public List<Warning> warnings;

    public CompileResult(String message, List<Warning> warnings)
    {
        this.message = message;
        this.warnings = warnings;
    }
}
