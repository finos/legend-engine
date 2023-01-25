package org.finos.legend.engine;

import java.sql.ParameterMetaData;
import java.sql.ResultSetMetaData;

public class DescribeResult {

    private final ResultSetMetaData fields;
    private final ParameterMetaData parameters;

    public DescribeResult(ResultSetMetaData fields, ParameterMetaData parameters) {
        this.fields = fields;
        this.parameters = parameters;
    }

    public ResultSetMetaData getFields(){
        return fields;
    }

    public ParameterMetaData getParameters(){
        return parameters;
    }
}
