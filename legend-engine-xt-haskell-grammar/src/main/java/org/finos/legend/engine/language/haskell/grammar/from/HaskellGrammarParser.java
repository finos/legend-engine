package org.finos.legend.engine.language.haskell.grammar.from;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
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

        return module;
    }

    private ModuleElement visitTopDecls(HaskellParser.TopdeclContext topdeclContext)
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
            data.constructors = ListIterate.flatCollect(ty_declContext.constrs().constrs1().constr(), this::visitConstrContext);
            data.derivings = ListIterate.collect(ty_declContext.derivings().deriving(), this::visitDerivingContext);
            return data;
        }

        throw new RuntimeException("Unsupported declaration");
    }

    private List<NamedConstructor> visitConstrContext(HaskellParser.ConstrContext constrContext)
    {
        List<NamedConstructor> constructors = Lists.mutable.of();
        for(HaskellParser.Constr_tyappContext constr_tyappContext : constrContext.constr_stuff().constr_tyapps().constr_tyapp())
        {
            if(constr_tyappContext.tyapp().atype() != null)
            {
                HaskellParser.AtypeContext atypeContext = constr_tyappContext.tyapp().atype();
                if(atypeContext.fielddecls() != null)
                {
                    ((RecordTypeConstructor)constructors.get(constructors.size() - 1)).fields = ListIterate.collect(atypeContext.fielddecls().fielddecl(), this::visitFieldDeclContext);
                }
                else
                {
                    RecordTypeConstructor constr = new RecordTypeConstructor();
                    constr.name = atypeContext.getText();
                    constructors.add(constr);
                }
            }
        }
        return constructors;
    }

    private Field visitFieldDeclContext(HaskellParser.FielddeclContext fielddeclContext)
    {
        Field field = new Field();
        field.name = fielddeclContext.sig_vars().getText();
        field.type = visitCTypeContext(fielddeclContext.ctype());
        return field;
    }

    private HaskellType visitATypeContext(HaskellParser.AtypeContext atypeContext)
    {
        if( atypeContext.ktype() != null) {

            HaskellType type = visitKTypeContext(atypeContext.ktype());

            //This is a list type
            if( atypeContext.OpenSquareBracket() != null)
            {
                ListType listType = new ListType();
                if( atypeContext.ktype() != null) {
                    listType.type = type;
                }
                return listType;
            }
            return type;
        }
        else if( atypeContext.ntgtycon() != null) {
            return visitNtgtyconContext(atypeContext.ntgtycon());
        }

        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitBTypeContext(HaskellParser.BtypeContext btypeContext)
    {
        if( btypeContext.tyapps() != null)
        {
            HaskellParser.TyappContext tyappContext = btypeContext.tyapps().tyapp(0);
            if( tyappContext.atype() != null) {
                return visitATypeContext(tyappContext.atype());
            }
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitCTypeContext(HaskellParser.CtypeContext ctypeContext)
    {
        if( ctypeContext.type_() != null)
        {
            return visitTypeContext(ctypeContext.type_());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitKTypeContext(HaskellParser.KtypeContext ktypeContext)
    {
        if( ktypeContext.ctype() != null)
        {
            return visitCTypeContext(ktypeContext.ctype());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitNtgtyconContext(HaskellParser.NtgtyconContext ntgtyconContext)
    {
        if( ntgtyconContext.oqtycon() != null)
        {
            return visitOqtyconContext(ntgtyconContext.oqtycon());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitOqtyconContext(HaskellParser.OqtyconContext oqtyconContext)
    {
        if( oqtyconContext.qtycon() != null)
        {
            return visitQtyconContext(oqtyconContext.qtycon());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitQtyconContext(HaskellParser.QtyconContext qtyconContext)
    {
        if( qtyconContext.tycon() != null)
        {
            return visitTyconContext(qtyconContext.tycon());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitTyconContext(HaskellParser.TyconContext tyconContext)
    {
        if( tyconContext.conid() != null)
        {
            return visitConidContext(tyconContext.conid());
        }
        throw new RuntimeException("Not supported yet");
    }

    private HaskellType visitConidContext(HaskellParser.ConidContext conidContext)
    {
        NamedType type = new NamedType();
        type.name = conidContext.CONID().getText();
        return type;
    }

    private HaskellType visitTypeContext(HaskellParser.Type_Context typeContext)
    {
        if( typeContext.btype() != null)
        {
            return visitBTypeContext(typeContext.btype());
        }
        throw new RuntimeException("Not supported yet");
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
