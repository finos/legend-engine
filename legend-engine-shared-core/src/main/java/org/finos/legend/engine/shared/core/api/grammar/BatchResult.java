package org.finos.legend.engine.shared.core.api.grammar;

import java.util.Map;

public class BatchResult<T>
{
    public Map<String, T> result;
    public Map<String, ParserError> errors;

    public BatchResult()
    {
    }

    public BatchResult(Map<String, T> result, Map<String, ParserError> errors)
    {
        this.result = result;
        this.errors = errors;
    }
}
