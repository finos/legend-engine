package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

import java.util.function.Function;

public interface ConnectionValueParser
{
    String getConnectionTypeName();

    Connection parse(ConnectionValueSourceCode sourceCode);

    static ConnectionValueParser newParser(String connectionTypeName, Function<ConnectionValueSourceCode, Connection> parser)
    {
        return new ConnectionValueParser()
        {
            @Override
            public String getConnectionTypeName()
            {
                return connectionTypeName;
            }

            @Override
            public Connection parse(ConnectionValueSourceCode sourceCode)
            {
                return parser.apply(sourceCode);
            }
        };
    }
}
