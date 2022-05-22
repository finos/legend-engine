package org.finos.legend.engine.language.haskell.grammar.from;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.haskell.grammar.from.antlr4.HaskellLexer;
import org.finos.legend.engine.language.haskell.grammar.from.antlr4.HaskellParser;
import org.finos.legend.engine.protocol.haskell.metamodel.*;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.util.BitSet;
import java.util.List;

public class HaskellGrammarParser {

    private HaskellGrammarParser()
    {
    }

    public static HaskellGrammarParser newInstance()
    {
        return new HaskellGrammarParser();
    }

    public HaskellModule parseModule(String code) {
        return parse(code);
    }

    private HaskellModule parse(String code)
    {
        ANTLRErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(            Recognizer<?, ?> recognizer,
                                                Object offendingSymbol,
                                                int line,
                                                int charPositionInLine,
                                                String msg,
                                                RecognitionException e)
            {
                if (e != null && e.getOffendingToken() != null && e instanceof InputMismatchException)
                {
                    msg = "Unexpected token";
                }
                else if (e == null || e.getOffendingToken() == null)
                {
                    if (e == null && offendingSymbol instanceof Token && (msg.startsWith("extraneous input") || msg.startsWith("missing ")))
                    {
                        // when ANTLR detects unwanted symbol, it will not result in an error, but throw
                        // `null` with a message like "extraneous input ... expecting ..."
                        // NOTE: this is caused by us having INVALID catch-all symbol in the lexer
                        // so anytime, INVALID token is found, it should cause this error
                        // but because it is a catch-all rule, it only produces a lexer token, which is a symbol
                        // we have to construct the source information manually
                        SourceInformation sourceInformation = new SourceInformation(
                                "",
                                line,
                                charPositionInLine + 1 ,
                                line,
                                charPositionInLine + 1 + ((Token) offendingSymbol).getStopIndex() - ((Token) offendingSymbol).getStartIndex());
                        // NOTE: for some reason sometimes ANTLR report the end index of the token to be smaller than the start index so we must reprocess it here
                        sourceInformation.startColumn = Math.min(sourceInformation.endColumn, sourceInformation.startColumn);
                        msg = "Unexpected token";
                        throw new HaskellParserException(msg, sourceInformation);
                    }
                    SourceInformation sourceInformation = new SourceInformation(
                            "",
                            line,
                            charPositionInLine + 1,
                            line,
                            charPositionInLine + 1);
                    throw new HaskellParserException(msg, sourceInformation);
                }
                Token offendingToken = e.getOffendingToken();
                SourceInformation sourceInformation = new SourceInformation(
                        "",
                        line,
                        charPositionInLine + 1 ,
                        offendingToken.getLine(),
                        charPositionInLine + offendingToken.getText().length());
                throw new HaskellParserException(msg, sourceInformation);
            }

            @Override
            public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet)
            {
            }
        };
        HaskellLexer lexer = new HaskellLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        HaskellParser parser = new HaskellParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return visitModule(parser.module());
    }

    private HaskellModule visitModule(HaskellParser.ModuleContext moduleContext)
    {
        HaskellModule module = new HaskellModule();

        module.id = moduleContext.module_content().modid().getText();

        module.elements = ListIterate.collect(moduleContext.module_content().where_module().module_body().body().topdecls().topdecl(),
                this::visitTopDecls);

        //moduleContext.module_content().where_module().module_body().body().topdecls()

        //document.definitions = ListIterate.collect(documentContext.definition(), this::visitDefinition);
        return module;
    }

    private TopLevelDeclaration visitTopDecls(HaskellParser.TopdeclContext topdeclContext)
    {
        if (topdeclContext.ty_decl() != null)
            return visitTypeDecl(topdeclContext.ty_decl());

        else throw new RuntimeException("Unsupported declaration");
    }

    private DataType visitTypeDecl(HaskellParser.Ty_declContext ty_declContext)
    {
        if (ty_declContext.DATA() != null)
        {
            DataType data = new DataType();
            data.name = ty_declContext.tycl_hdr().getText();
            data.constructors = ListIterate.collect(ty_declContext.constrs().constrs1().constr(), this::visitConstrContext);
            data.derivings = ListIterate.collect(ty_declContext.derivings().deriving(), this::visitDerivingContext);
            return data;
        }

        throw new RuntimeException("Unsupported declaration");
    }

    private NamedConstructor visitConstrContext(HaskellParser.ConstrContext constrContext)
    {
        return visitConstrStuffContext(constrContext.constr_stuff());
    }

    private NamedConstructor visitConstrStuffContext(HaskellParser.Constr_stuffContext constrContext)
    {
        //todo: check what type of constructor we have
        RecordTypeConstructor constr = new RecordTypeConstructor();

        if (constrContext.constr_tyapps() != null && !constrContext.constr_tyapps().isEmpty())
        {
            List<HaskellParser.Constr_tyappContext> tyapps = constrContext.constr_tyapps().constr_tyapp();
            constr.name = tyapps.get(0).tyapp().getText();

            if (tyapps.size() > 1)
            {
                for( int i=1; i<tyapps.size(); i++)
                {
                    constr.fields = ListIterate.collect(tyapps.get(i).tyapp().atype().fielddecls().fielddecl(), this::visitFieldDeclContext);
                }
            }
        }


        return constr;
    }

    private Field visitFieldDeclContext(HaskellParser.FielddeclContext fielddeclContext)
    {
        Field field = new Field();
        field.name = fielddeclContext.sig_vars().getText();
        NamedType type = new NamedType();
        type.name = fielddeclContext.ctype().getText();
        field.type = type;
        return field;
    }

    private Deriving visitDerivingContext(HaskellParser.DerivingContext derivingContext)
    {
        Deriving deriving = new Deriving();
        deriving.deriving = ListIterate.collect(derivingContext.deriv_clause_types().deriv_types().ktypedoc(), this::visitKtypedocContext);
        return deriving;
    }

    private String visitKtypedocContext(HaskellParser.KtypedocContext ktypedocContext)
    {
        return ktypedocContext.getText();
    }
}
