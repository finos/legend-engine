package org.finos.legend.engine.language.protobuf3.grammar.from;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.protobuf.grammar.from.antlr4.Protobuf3Lexer;
import org.finos.legend.engine.language.protobuf.grammar.from.antlr4.Protobuf3Parser;
import org.finos.legend.engine.protocol.protobuf3.metamodel.*;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Double;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Enum;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Float;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.lang.String;
import java.util.BitSet;
import java.util.List;

public class Protobuf3GrammarParser
{
    private Protobuf3GrammarParser()
    {}

    public static Protobuf3GrammarParser newInstance() {
        return new Protobuf3GrammarParser();
    }

    public ProtoFile parseProto(String code)
    {
        return this.parse(code);
    }

    private ProtoFile parse(String code)
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
                        throw new Protobuf3ParserException(msg, sourceInformation);
                    }
                    SourceInformation sourceInformation = new SourceInformation(
                            "",
                            line,
                            charPositionInLine + 1,
                            line,
                            charPositionInLine + 1);
                    throw new Protobuf3ParserException(msg, sourceInformation);
                }
                Token offendingToken = e.getOffendingToken();
                SourceInformation sourceInformation = new SourceInformation(
                        "",
                        line,
                        charPositionInLine + 1 ,
                        offendingToken.getLine(),
                        charPositionInLine + offendingToken.getText().length());
                throw new Protobuf3ParserException(msg, sourceInformation);
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
        Protobuf3Lexer lexer = new Protobuf3Lexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        Protobuf3Parser parser = new Protobuf3Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return visitProto(parser.proto());
    }

    private ProtoFile visitProto(Protobuf3Parser.ProtoContext protoContext)
    {
        ProtoFile protoFile = new ProtoFile();
        protoFile.syntax = Syntax.proto3;

        if (protoContext.importStatement().size() > 0) {
            List<ProtoImport> imports = Lists.mutable.ofInitialCapacity(protoContext.importStatement().size());
            for (Protobuf3Parser.ImportStatementContext _import : protoContext.importStatement()) {
                ProtoImport protoImport = new ProtoImport();
                protoImport.name = _import.strLit().getText();
                imports.add(protoImport);
            }
            protoFile.imports = imports;
        }
        if( protoContext.optionStatement().size() > 0 ) {
            protoFile.options = visitOptionStatementContext(protoContext.optionStatement());
        }
        if (protoContext.packageStatement().size() == 1) {
            protoFile._package = protoContext.packageStatement(0).fullIdent().getText();
        }
        if (protoContext.topLevelDef() != null) {
            List<ProtoItemDefinition> topLevelDefs = Lists.mutable.ofInitialCapacity(protoContext.topLevelDef().size());
            for (Protobuf3Parser.TopLevelDefContext topLevelDef : protoContext.topLevelDef()) {
                topLevelDefs.add(visitTopLevelDef(topLevelDef));
            }
            protoFile.topLevelDefs = topLevelDefs;
        }
        return protoFile;
    }

    private Options visitOptionStatementContext(List<Protobuf3Parser.OptionStatementContext> optionStatementContexts){
        Options options = new Options();
        options.customOptions = Lists.mutable.of();
        for( Protobuf3Parser.OptionStatementContext optionStatementContext: optionStatementContexts)
        {
            String name = optionStatementContext.optionName().fullIdent(0).getText();
            String value = optionStatementContext.constant().getText();

            switch (name) {
                case "java_package":
                    options.javaPackage = value;
                    break;
                case "java_outer_classname":
                    options.javaOuterClassname = value;
                    break;
                case "java_multiple_files":
                    options.javaMultipleFiles = Boolean.parseBoolean(value);
                    break;
                case "optimize_for":
                    if (value.equals("SPEED")) {
                        options.optimizeFor = OptimizeMode.SPEED;
                    } else if (value.equals("CODE_SIZE")) {
                        options.optimizeFor = OptimizeMode.CODE_SIZE;
                    } else if (value.equals("LITE_RUNTIME")) {
                        options.optimizeFor = OptimizeMode.LITE_RUNTIME;
                    } else {
                        throw new RuntimeException("Unknown optimize_for value:" + value);
                    }
                    break;
                default:
                    Option customOption = new Option();
                    customOption.name = name;
                    customOption.value = value;
                    options.customOptions.add(customOption);
                    break;
            }
        }
        return options;
    }

    private ProtoItemDefinition visitTopLevelDef(Protobuf3Parser.TopLevelDefContext topLevelDefContext) {
        if( topLevelDefContext.enumDef() != null )
        {
            return visitEnumDef(topLevelDefContext.enumDef());
        }
        else if ( topLevelDefContext.messageDef() != null )
        {
            return visitMessageDef( topLevelDefContext.messageDef());
        }
        else {
            throw new RuntimeException("This is top level definition is not supported yet");
        }
    }

    private Enumeration visitEnumDef(Protobuf3Parser.EnumDefContext enumDefContext) {
        Enumeration enumeration = new Enumeration();
        enumeration.name = enumDefContext.enumName().ident().getText();
        if (enumDefContext.enumBody().enumElement() != null) {
            List<Enum> enums = Lists.mutable.ofInitialCapacity(enumDefContext.enumBody().enumElement().size());
            for (Protobuf3Parser.EnumElementContext enumElement : enumDefContext.enumBody().enumElement()) {
                Enum protoEnum = new Enum();
                protoEnum.constant = enumElement.enumField().ident().getText();
                protoEnum.constantNumber = Long.parseLong(enumElement.enumField().intLit().getText());
                enums.add(protoEnum);
            }
            enumeration.values = enums;
        }
        return enumeration;
    }

    private Message visitMessageDef(Protobuf3Parser.MessageDefContext messageDefContext) {
        Message message = new Message();
        message.name = messageDefContext.messageName().ident().getText();

        MutableList<ProtoItemDefinition> content = Lists.mutable.of();
        for( Protobuf3Parser.MessageElementContext elementContext : messageDefContext.messageBody().messageElement())
        {
            if(elementContext.field() != null) {
                Field field = new Field();
                field.type = visitProtoType(elementContext.field().type_());
                field.name = elementContext.field().fieldName().ident().getText();
                field.number = Long.parseLong(elementContext.field().fieldNumber().intLit().getText());
                field.repeated = elementContext.field().REPEATED() != null;
                content.add(field);
            } else if (elementContext.messageDef() != null) {
                Message messageNested = visitMessageDef(elementContext.messageDef());
                content.add(messageNested);
            } else if (elementContext.enumDef() != null) {
                Enumeration enumeration = visitEnumDef(elementContext.enumDef());
                content.add(enumeration);
            } else {
                throw new RuntimeException("Unknown message element");
            }

        }
        message.content = content;
        return message;
    }

    private ProtoBufType visitProtoType(Protobuf3Parser.Type_Context type_context)
    {
        if (type_context.BOOL() != null)
        {
            return new Bool();
        }
        else if(type_context.BYTES() != null)
        {
            return new Bytes();
        }
        else if(type_context.DOUBLE() != null)
        {
            return new Double();
        }
        else if(type_context.FIXED32() != null)
        {
            return new Fixed32();
        }
        else if(type_context.FIXED64() != null)
        {
            return new Fixed64();
        }
        else if(type_context.FLOAT() != null)
        {
            return new Float();
        }
        else if(type_context.INT32() != null)
        {
            return new Int32();
        }
        else if(type_context.INT64() != null)
        {
            return new Int64();
        }
        else if(type_context.SFIXED32() != null)
        {
            return new SFixed32();
        }
        else if(type_context.SFIXED64() != null)
        {
            return new SFixed64();
        }
        else if(type_context.SINT32() != null)
        {
            return new SInt32();
        }
        else if(type_context.SINT64() != null)
        {
            return new SInt64();
        }
        else if(type_context.STRING() != null)
        {
            return new org.finos.legend.engine.protocol.protobuf3.metamodel.String();
        }
        else if(type_context.UINT32() != null)
        {
            return new UInt32();
        }
        else if(type_context.UINT64() != null)
        {
            return new UInt64();
        }
        else if(type_context.enumType() != null)
        {
            EnumPtr enumPtr = new EnumPtr();
            //todo - sort out package
            enumPtr.name = type_context.enumType().enumName().ident().getText();
            return enumPtr;
        }
        else if(type_context.messageType() != null)
        {
            MessagePtr messagePtr = new MessagePtr();
            //todo - sort out package
            messagePtr.name = type_context.messageType().messageName().ident().getText();
            return messagePtr;
        }
        else {
            throw new RuntimeException("Unknown type");
        }
    }
}
