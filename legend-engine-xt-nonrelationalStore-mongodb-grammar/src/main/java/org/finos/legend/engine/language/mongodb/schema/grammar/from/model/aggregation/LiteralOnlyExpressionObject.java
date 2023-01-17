package org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation;

public class LiteralOnlyExpressionObject extends ArgumentExpression {

    public ArgumentExpression argument;

    public LiteralOnlyExpressionObject(ArgumentExpression argument)
    {
        this.argument = argument;
    }
}
