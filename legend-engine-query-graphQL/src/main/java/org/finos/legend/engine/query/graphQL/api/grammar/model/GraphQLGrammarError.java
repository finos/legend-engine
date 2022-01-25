package org.finos.legend.engine.query.graphQL.api.grammar.model;

import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLParserException;

public class GraphQLGrammarError
{
    public boolean _error = true;
    public int line;
    public int column;
    public String message;

    public GraphQLGrammarError(GraphQLParserException e)
    {
        this.line = e.getSourceInformation().startLine;
        this.column = e.getSourceInformation().startColumn;
        this.message = e.getMessage();
    }
}
