package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsS3ConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsS3ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.S3Connection;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AwsGrammarParserExtension implements IAwsGrammarParserExtension
{
    public static final String NAME = "Aws";
    public static final String AWS_S3_CONNECTION_TYPE = "AwsS3Connection";

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(AWS_S3_CONNECTION_TYPE, connectionValueSourceCode ->
        {
            SourceCodeParserInfo parserInfo = getAwsS3ConnectionParserInfo(connectionValueSourceCode);
            AwsS3ConnectionParseTreeWalker walker = new AwsS3ConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
            S3Connection s3Connection = new S3Connection();
            s3Connection.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitAwsS3ConnectionValue((AwsS3ConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, s3Connection);
            return s3Connection;
        }));
    }

    private static SourceCodeParserInfo getAwsS3ConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        AwsS3ConnectionLexerGrammar lexer = new AwsS3ConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AwsS3ConnectionParserGrammar parser = new AwsS3ConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }


}
