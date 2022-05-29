//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.protobuf3.grammar.from;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.protobuf3.grammar.from.antlr4.Protobuf3Lexer;
import org.finos.legend.engine.language.protobuf3.grammar.from.antlr4.Protobuf3Parser;
import org.finos.legend.engine.protocol.protobuf3.metamodel.BlockLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.BlockValue;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Bool;
import org.finos.legend.engine.protocol.protobuf3.metamodel.BoolLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Bytes;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Double;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Enum;
import org.finos.legend.engine.protocol.protobuf3.metamodel.EnumPtr;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Enumeration;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Field;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Fixed32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Fixed64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Float;
import org.finos.legend.engine.protocol.protobuf3.metamodel.FloatLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.IdentifierLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Int32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Int64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.IntLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Literal;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Message;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MessagePtr;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MessageType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Option;
import org.finos.legend.engine.protocol.protobuf3.metamodel.OneOf;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoBufType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoImport;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoItemDefinition;
import org.finos.legend.engine.protocol.protobuf3.metamodel.RemoteProcedureCall;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SFixed32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SFixed64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SInt32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SInt64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Service;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ServiceBodyItem;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ServiceOption;
import org.finos.legend.engine.protocol.protobuf3.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Syntax;
import org.finos.legend.engine.protocol.protobuf3.metamodel.UInt32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.UInt64;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Protobuf3GrammarParser
{
    private Protobuf3GrammarParser()
    {
    }

    public static Protobuf3GrammarParser newInstance()
    {
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
            public void syntaxError(Recognizer<?, ?> recognizer,
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
                                charPositionInLine + 1,
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
                        charPositionInLine + 1,
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

        if (protoContext.importStatement().size() > 0)
        {
            List<ProtoImport> imports = Lists.mutable.ofInitialCapacity(protoContext.importStatement().size());
            for (Protobuf3Parser.ImportStatementContext _import : protoContext.importStatement())
            {
                ProtoImport protoImport = new ProtoImport();
                protoImport.name = visitStringLiteral(_import.strLit());
                imports.add(protoImport);
            }
            protoFile.imports = imports;
        }
        if (protoContext.optionStatement().size() > 0)
        {
            protoFile.options = Lists.mutable.ofInitialCapacity(protoContext.optionStatement().size());
            for (Protobuf3Parser.OptionStatementContext optionStatementContext : protoContext.optionStatement())
            {
                protoFile.options.add(visitOptionStatementContext(optionStatementContext));
            }

        }
        if (protoContext.packageStatement().size() == 1)
        {
            protoFile._package = protoContext.packageStatement(0).fullIdent().getText();
        }
        if (protoContext.topLevelDef() != null)
        {
            List<ProtoItemDefinition> topLevelDefs = Lists.mutable.ofInitialCapacity(protoContext.topLevelDef().size());
            for (Protobuf3Parser.TopLevelDefContext topLevelDef : protoContext.topLevelDef())
            {
                topLevelDefs.add(visitTopLevelDef(topLevelDef));
            }
            protoFile.topLevelDefs = topLevelDefs;
        }
        return protoFile;
    }

    private Option visitOptionStatementContext(Protobuf3Parser.OptionStatementContext optionStatementContext)
    {
        Option option = new Option();
        option.name = optionStatementContext.optionName().fullIdent(0).getText();
        option.value = visitConstant(optionStatementContext.constant());
        return option;
    }

    private Literal visitConstant(Protobuf3Parser.ConstantContext constantContext)
    {
        if (constantContext.strLit() != null)
        {
            StringLiteral l = new StringLiteral();
            l.value = visitStringLiteral(constantContext.strLit());
            return l;
        }
        else if (constantContext.boolLit() != null)
        {
            BoolLiteral l = new BoolLiteral();
            l.value = Boolean.parseBoolean(constantContext.boolLit().BOOL_LIT().getText());
            return l;
        }
        else if (constantContext.intLit() != null)
        {
            IntLiteral l = new IntLiteral();
            l.value = Integer.parseInt(constantContext.intLit().INT_LIT().getText());
            return l;
        }
        else if (constantContext.floatLit() != null)
        {
            FloatLiteral l = new FloatLiteral();
            l.value = java.lang.Double.parseDouble(constantContext.floatLit().FLOAT_LIT().getText());
            return l;
        }
        else if (constantContext.fullIdent() != null)
        {
            IdentifierLiteral l = new IdentifierLiteral();
            l.value = constantContext.fullIdent().getText();
            return l;
        }
        else if (constantContext.blockLit() != null)
        {
            return visitBlockLiteral(constantContext.blockLit());
        }
        else
        {
            throw new RuntimeException("Unknown constant type:" + constantContext.getText());
        }
    }


    private BlockLiteral visitBlockLiteral(Protobuf3Parser.BlockLitContext blockLitContext)
    {
        BlockLiteral blockLiteral = new BlockLiteral();
        blockLiteral.values = Lists.mutable.ofInitialCapacity(blockLitContext.ident().size());
        for (int i = 0; i < blockLitContext.ident().size(); i++)
        {
            BlockValue value = new BlockValue();
            value.name = blockLitContext.ident(i).getText();
            value.value = visitConstant(blockLitContext.constant(i));
            blockLiteral.values.add(value);
        }
        return blockLiteral;
    }

    private ProtoItemDefinition visitTopLevelDef(Protobuf3Parser.TopLevelDefContext topLevelDefContext)
    {
        if (topLevelDefContext.enumDef() != null)
        {
            return visitEnumDef(topLevelDefContext.enumDef());
        }
        else if (topLevelDefContext.messageDef() != null)
        {
            return visitMessageDef(topLevelDefContext.messageDef());
        }
        else if (topLevelDefContext.serviceDef() != null)
        {
            return visitServiceDef(topLevelDefContext.serviceDef());
        }
        else
        {
            throw new RuntimeException("This top level definition is not supported yet");
        }
    }

    private Enumeration visitEnumDef(Protobuf3Parser.EnumDefContext enumDefContext)
    {
        Enumeration enumeration = new Enumeration();
        enumeration.name = enumDefContext.enumName().ident().getText();
        if (enumDefContext.enumBody().enumElement() != null)
        {
            List<Enum> enums = Lists.mutable.ofInitialCapacity(enumDefContext.enumBody().enumElement().size());
            for (Protobuf3Parser.EnumElementContext enumElement : enumDefContext.enumBody().enumElement())
            {
                Enum protoEnum = new Enum();
                protoEnum.constant = enumElement.enumField().ident().getText();
                protoEnum.constantNumber = Long.parseLong(enumElement.enumField().intLit().getText());
                enums.add(protoEnum);
            }
            enumeration.values = enums;
        }
        return enumeration;
    }

    private Message visitMessageDef(Protobuf3Parser.MessageDefContext messageDefContext)
    {
        Message message = new Message();
        message.name = messageDefContext.messageName().ident().getText();

        MutableList<ProtoItemDefinition> content = Lists.mutable.of();
        for (Protobuf3Parser.MessageElementContext elementContext : messageDefContext.messageBody().messageElement())
        {
            if (elementContext.field() != null)
            {
                Field field = new Field();
                field.type = visitProtoType(elementContext.field().type_());
                field.name = elementContext.field().fieldName().ident().getText();
                field.number = Long.parseLong(elementContext.field().fieldNumber().intLit().getText());
                field.repeated = elementContext.field().REPEATED() != null;
                content.add(field);
            }
            else if (elementContext.messageDef() != null)
            {
                Message messageNested = visitMessageDef(elementContext.messageDef());
                content.add(messageNested);
            }
            else if (elementContext.enumDef() != null)
            {
                Enumeration enumeration = visitEnumDef(elementContext.enumDef());
                content.add(enumeration);
            }
            else if (elementContext.oneof() != null )
            {
                OneOf oneOf = visitOneofContext(elementContext.oneof());
                content.add(oneOf);
            }
            else
            {
                throw new RuntimeException("Unknown message element");
            }

        }
        message.content = content;
        return message;
    }

    private OneOf visitOneofContext(Protobuf3Parser.OneofContext oneofContext) {
        OneOf oneOf = new OneOf();
        oneOf.name = oneofContext.oneofName().ident().getText();
        oneOf.field = ListIterate.collect(oneofContext.oneofField(), this::visitOneofFieldContext);
        return oneOf;
    }

    private Field visitOneofFieldContext(Protobuf3Parser.OneofFieldContext oneofFieldContext) {
        Field field = new Field();
        field.name = oneofFieldContext.fieldName().ident().getText();
        field.number = Long.parseLong(oneofFieldContext.fieldNumber().intLit().getText());
        field.type = visitProtoType(oneofFieldContext.type_());
        if( oneofFieldContext.fieldOptions() != null) {
            field.options = ListIterate.collect(oneofFieldContext.fieldOptions().fieldOption(), this::visitFieldOptionContext);
        }
        return field;
    }

    private Option visitFieldOptionContext(Protobuf3Parser.FieldOptionContext fieldOptionContext){
        Option option = new Option();
        option.name = fieldOptionContext.optionName().getText();
        option.value = visitConstant(fieldOptionContext.constant());
        return option;
    }

    private Service visitServiceDef(Protobuf3Parser.ServiceDefContext serviceDefContext)
    {
        Service service = new Service();
        service.name = serviceDefContext.serviceName().getText();
        List<ServiceBodyItem> items = Lists.mutable.of();

        for (Protobuf3Parser.ServiceElementContext element : serviceDefContext.serviceElement())
        {
            items.add(visitServiceElement(element));
        }

        service.content = items;
        return service;
    }

    private ServiceBodyItem visitServiceElement(Protobuf3Parser.ServiceElementContext element)
    {
        if (element.rpc() != null)
        {
            return visitRpc(element.rpc());
        }
        else if (element.optionStatement() != null)
        {
            ServiceOption serviceOption = new ServiceOption();
            serviceOption.option = visitOptionStatementContext(element.optionStatement());
            return serviceOption;
        }
        else
        {
            throw new RuntimeException("Unknown service element type");
        }
    }

    private RemoteProcedureCall visitRpc(Protobuf3Parser.RpcContext rpcContext)
    {
        RemoteProcedureCall rpc = new RemoteProcedureCall();
        rpc.name = rpcContext.rpcName().getText();

        List<Protobuf3Parser.MessageTypeContext> messageTypeContexts = rpcContext.messageType();
        if (messageTypeContexts.size() > 0)
        {
            Protobuf3Parser.MessageTypeContext requestTypeContext = messageTypeContexts.get(0);
            rpc.requestType = new MessageType();
            rpc.requestType.type = visitMessageType(requestTypeContext);
        }
        if (messageTypeContexts.size() > 1)
        {
            Protobuf3Parser.MessageTypeContext requestTypeContext = messageTypeContexts.get(1);
            rpc.returnType = new MessageType();
            rpc.returnType.type = visitMessageType(requestTypeContext);
        }

        if (rpcContext.optionStatement() != null)
        {
            rpc.options = Lists.mutable.ofInitialCapacity(rpcContext.optionStatement().size());
            for (Protobuf3Parser.OptionStatementContext optionStatementContext : rpcContext.optionStatement())
            {
                rpc.options.add(visitOptionStatementContext(optionStatementContext));
            }
        }

        return rpc;
    }

    private ProtoBufType visitProtoType(Protobuf3Parser.Type_Context type_context)
    {
        if (type_context.BOOL() != null)
        {
            return new Bool();
        }
        else if (type_context.BYTES() != null)
        {
            return new Bytes();
        }
        else if (type_context.DOUBLE() != null)
        {
            return new Double();
        }
        else if (type_context.FIXED32() != null)
        {
            return new Fixed32();
        }
        else if (type_context.FIXED64() != null)
        {
            return new Fixed64();
        }
        else if (type_context.FLOAT() != null)
        {
            return new Float();
        }
        else if (type_context.INT32() != null)
        {
            return new Int32();
        }
        else if (type_context.INT64() != null)
        {
            return new Int64();
        }
        else if (type_context.SFIXED32() != null)
        {
            return new SFixed32();
        }
        else if (type_context.SFIXED64() != null)
        {
            return new SFixed64();
        }
        else if (type_context.SINT32() != null)
        {
            return new SInt32();
        }
        else if (type_context.SINT64() != null)
        {
            return new SInt64();
        }
        else if (type_context.STRING() != null)
        {
            return new org.finos.legend.engine.protocol.protobuf3.metamodel.String();
        }
        else if (type_context.UINT32() != null)
        {
            return new UInt32();
        }
        else if (type_context.UINT64() != null)
        {
            return new UInt64();
        }
        else if (type_context.enumType() != null)
        {
            EnumPtr enumPtr = new EnumPtr();
            enumPtr._package = type_context.enumType().ident().isEmpty() ? null : type_context.enumType().ident().stream().map(new Function<Protobuf3Parser.IdentContext, String>()
            {
                @Override
                public String apply(Protobuf3Parser.IdentContext identContext)
                {
                    return identContext.IDENTIFIER().getText();
                }
            }).collect(Collectors.joining("."));
            enumPtr.name = type_context.enumType().enumName().ident().getText();
            return enumPtr;
        }
        else if (type_context.messageType() != null)
        {
            return visitMessageType(type_context.messageType());
        }
        else
        {
            throw new RuntimeException("Unknown type");
        }
    }

    private MessagePtr visitMessageType(Protobuf3Parser.MessageTypeContext messageTypeContext)
    {
        MessagePtr messagePtr = new MessagePtr();
        messagePtr._package = messageTypeContext.ident().isEmpty() ? null : messageTypeContext.ident().stream().map(new Function<Protobuf3Parser.IdentContext, String>()
        {
            @Override
            public String apply(Protobuf3Parser.IdentContext identContext)
            {
                return identContext.IDENTIFIER().getText();
            }
        }).collect(Collectors.joining("."));

        messagePtr.name = messageTypeContext.messageName().ident().getText();
        return messagePtr;
    }

    private String visitStringLiteral(Protobuf3Parser.StrLitContext strLitContext)
    {
        return strLitContext.STR_LIT().getText().replace("\"", "").replace("'", "");
    }
}
