// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.protobuf3.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.protobuf3.metamodel.BlockLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Bool;
import org.finos.legend.engine.protocol.protobuf3.metamodel.BoolLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Bytes;
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
import org.finos.legend.engine.protocol.protobuf3.metamodel.LiteralVisitor;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MapType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Message;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MessageOption;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MessagePtr;
import org.finos.legend.engine.protocol.protobuf3.metamodel.MessageType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.OneOf;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Option;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoBufType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoBufTypeVisitor;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoItemDefinition;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoItemDefinitionVisitor;
import org.finos.legend.engine.protocol.protobuf3.metamodel.RemoteProcedureCall;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ReservedFieldNames;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ReservedFieldRanges;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SFixed32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SFixed64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SInt32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.SInt64;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ScalarType;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Service;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ServiceBodyItem;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ServiceBodyItemVisitor;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ServiceOption;
import org.finos.legend.engine.protocol.protobuf3.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.protobuf3.metamodel.UInt32;
import org.finos.legend.engine.protocol.protobuf3.metamodel.UInt64;

import java.util.Locale;

public class Protobuf3GrammarComposer
{
    private Protobuf3GrammarComposer()
    {
    }

    public static Protobuf3GrammarComposer newInstance()
    {
        return new Protobuf3GrammarComposer();
    }

    public String renderProto(ProtoFile file)
    {
        String packageName = file._package == null ? "" : file._package;
        return "syntax = \"" + file.syntax.name() + "\";\n" +
                (file._package == null || file._package.isEmpty() ? "" : "package " + packageName + ";\n\n") +
                (file.imports == null || file.imports.isEmpty() ? "" : ListIterate.collect(file.imports, i -> "import \"" + (i.importType == null ? "" : i.importType.name()) + i.name + "\";\n").makeString("", "", "\n")) +
                (file.options == null || file.options.isEmpty() ? "" : ListIterate.collect(file.options, this::renderOption).makeString("") + "\n") +
                (file.topLevelDefs == null || file.topLevelDefs.isEmpty() ? "" : ListIterate.collect(file.topLevelDefs, t -> renderProtoItemDefinition(t, packageName)).makeString("\n\n"));
    }

    public String renderOption(Option option)
    {
        return "option " + (option.name.contains(".") ? "(" + option.name + ")" : option.name) + " = " + renderLiteral(option.value) + ";\n";
    }

    public String renderProtoItemDefinition(ProtoItemDefinition protoItemDefinition, String currentPackage)
    {
        return protoItemDefinition.accept(new ProtoItemDefinitionVisitor<String>()
        {
            @Override
            public String visit(Enumeration val)
            {
                return renderEnumeration(val, currentPackage);
            }

            @Override
            public String visit(Field val)
            {
                return renderField(val, currentPackage);
            }

            @Override
            public String visit(Message val)
            {
                return renderMessage(val, currentPackage);
            }

            @Override
            public String visit(MessageOption val)
            {
                return renderProtoItemDefinition(val, currentPackage);
            }

            @Override
            public String visit(OneOf val)
            {
                return "  oneof " + val.name + "{\n" + ListIterate.collect(val.field, f -> renderField(f, currentPackage)).makeString("\n") + "\n }";
            }

            @Override
            public String visit(Service val)
            {
                return renderService(val, currentPackage);
            }

            @Override
            public String visit(ReservedFieldNames val)
            {
                throw new RuntimeException("Not supported yet");
            }

            @Override
            public String visit(ReservedFieldRanges val)
            {
                throw new RuntimeException("Not supported yet");
            }
        });
    }

    public String renderMessage(Message message, String currentPackage)
    {
        return "message " + message.name + " {\n" + ListIterate.collect(message.content, c -> renderProtoItemDefinition(c, currentPackage)).makeString("\n") + "\n}";
    }

    public String renderService(Service service, String currentPackage)
    {
        return "service " + service.name + " {\n" + ListIterate.collect(service.content, c -> renderServiceBodyItem(c, currentPackage)).makeString("\n") + "\n}";
    }

    public String renderMessageType(MessageType messageType, String currentPackage)
    {
        return messageType.stream != null && messageType.stream ? "stream" : renderProtoBufType(messageType.type, currentPackage);
    }

    public String renderProtoBufType(ProtoBufType protoBufType, String currentPackage)
    {
        return protoBufType.accept(new ProtoBufTypeVisitor<String>()
        {
            @Override
            public String visit(MessagePtr val)
            {
                return (val._package == null || currentPackage.equals(val._package) ? "" : val._package + ".") + val.name;
            }

            @Override
            public String visit(EnumPtr val)
            {
                return (val._package == null || currentPackage.equals(val._package) ? "" : val._package + ".") + val.name;
            }

            @Override
            public String visit(MapType val)
            {
                return "map<" + renderProtoBufType(val.keyType, currentPackage) + "," + renderProtoBufType(val.valueType, currentPackage) + ">";
            }

            @Override
            public String visit(Bool val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Bytes val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(org.finos.legend.engine.protocol.protobuf3.metamodel.Double val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Fixed32 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Fixed64 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Float val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Int32 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(Int64 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(SFixed32 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(SFixed64 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(SInt32 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(SInt64 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(ScalarType val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(org.finos.legend.engine.protocol.protobuf3.metamodel.String val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(UInt32 val)
            {
                return renderScalarType(val);
            }

            @Override
            public String visit(UInt64 val)
            {
                return renderScalarType(val);
            }
        });
    }

    public String renderScalarType(ScalarType scalarType)
    {
        return scalarType.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    public String renderServiceBodyItem(ServiceBodyItem serviceBodyItem, String currentPackage)
    {
        return serviceBodyItem.accept(new ServiceBodyItemVisitor<String>()
        {
            @Override
            public String visit(RemoteProcedureCall val)
            {
                return "  rpc " + val.name + "(" + renderMessageType(val.requestType, currentPackage) + ") returns (" + renderMessageType(val.returnType, currentPackage) + ") {" + (val.options == null || val.options.isEmpty() ? "" : ListIterate.collect(val.options, v -> renderOption(v)).makeString("\n", "\n", "")) + "}";
            }

            @Override
            public String visit(ServiceOption val)
            {
                return "  " + renderOption(val.option);
            }
        });
    }

    public String renderField(Field field, String currentPackage)
    {
        return "  " + (field.repeated ? "repeated " : "") + renderProtoBufType(field.type, currentPackage) + " " + field.name + " = " + field.number + ";";
    }

    public String renderEnumeration(Enumeration enumeration, String currentPackage)
    {
        return "enum " + enumeration.name + " {\n" + ListIterate.collect(enumeration.values, v -> "  " + v.constant + " = " + v.constantNumber).makeString(";\n") + ";\n}";
    }

    public String renderLiteral(Literal literal)
    {
        return literal.accept(new LiteralVisitor<String>()
        {
            @Override
            public String visit(BlockLiteral val)
            {
                return "{ " + ListIterate.collect(val.values, v -> v.name + ": " + renderLiteral(v.value)).makeString(",") + " }";
            }

            @Override
            public String visit(BoolLiteral val)
            {
                return Boolean.toString(val.value);
            }

            @Override
            public String visit(FloatLiteral val)
            {
                return Double.toString(val.value);
            }

            @Override
            public String visit(IdentifierLiteral val)
            {
                return val.value;
            }

            @Override
            public String visit(IntLiteral val)
            {
                return Long.toString(val.value);
            }

            @Override
            public String visit(StringLiteral val)
            {
                return "\"" + val.value + "\"";
            }
        });
    }

}
