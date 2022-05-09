package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsFinCloudConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsFinCloudConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudConnection;

import java.util.Collections;

public class AwsFinCloudGrammarParserExtension implements IAwsGrammarParserExtension
{
    public static final String NAME = "Aws";
    public static final String AWS_FIN_CLOUD_CONNECTION_TYPE = "AwsFinCloudConnection";

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(AWS_FIN_CLOUD_CONNECTION_TYPE, connectionValueSourceCode ->
        {
            SourceCodeParserInfo parserInfo = getAwsFinCloudConnectionParserInfo(connectionValueSourceCode);
            AwsFinCloudConnectionParseTreeWalker walker = new AwsFinCloudConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
            FinCloudConnection finCloudConnection = new FinCloudConnection();
            finCloudConnection.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitAwsFinCloudConnectionValue((AwsFinCloudConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, finCloudConnection);
            return finCloudConnection;
        }));
    }

    private static SourceCodeParserInfo getAwsFinCloudConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        AwsFinCloudConnectionLexerGrammar lexer = new AwsFinCloudConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AwsFinCloudConnectionParserGrammar parser = new AwsFinCloudConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }


}
