package org.finos.legend.engine.language.pure.grammar.from.authorizer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.*;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authorizer.AuthorizerLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authorizer.AuthorizerParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;

public class AuthorizerParser implements DEPRECATED_SectionGrammarParser
{
    public static final String name = "Authorizer";

    private final PureGrammarParserExtensions extensions;

    private AuthorizerParser(PureGrammarParserExtensions extensions)
    {
        this.extensions = extensions;
    }

    public static AuthorizerParser newInstance(PureGrammarParserExtensions extensions)
    {
        return new AuthorizerParser(extensions);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public SourceCodeParserInfo getParserInfo(String code, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        return this.getParserInfo(code, sourceInformation, walkerSourceInformation, true);
    }

    private SourceCodeParserInfo getParserInfo(String code, SourceInformation sectionSourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation, boolean includeRootContext)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        AuthorizerLexerGrammar lexer = new AuthorizerLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AuthorizerParserGrammar parser = new AuthorizerParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sectionSourceInformation, walkerSourceInformation, lexer, parser, includeRootContext ? parser.definition() : null);
    }

    @Override
    public Section parse(SourceCodeParserInfo sectionParserInfo, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = this.getName();
        section.sourceInformation = sectionParserInfo.sourceInformation;
        AuthorizerParseTreeWalker walker = new AuthorizerParseTreeWalker(sectionParserInfo.walkerSourceInformation, this.extensions, elementConsumer, section);
        walker.visit((AuthorizerParserGrammar.DefinitionContext) sectionParserInfo.rootContext);
        return section;
    }
}
