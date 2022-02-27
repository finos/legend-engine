package org.finos.legend.engine.language.protobuf3.grammar.test;

import org.finos.legend.engine.language.protobuf3.grammar.from.Protobuf3GrammarParser;
import org.finos.legend.engine.language.protobuf3.grammar.to.Protobuf3GrammarComposer;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Message;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Syntax;
import org.junit.Assert;
import org.junit.Test;

public class TestProtobuf3 {

    @Test
    public void testFile()
    {
        String tutorial = "\n" +
                "   \n" +
                "// See README.txt for information and build instructions.\n" +
                "//\n" +
                "// Note: START and END tags are used in comments to define sections used in\n" +
                "// tutorials.  They are not part of the syntax for Protocol Buffers.\n" +
                "//\n" +
                "// To get an in-depth walkthrough of this file and the related examples, see:\n" +
                "// https://developers.google.com/protocol-buffers/docs/tutorials\n" +
                "\n" +
                "// [START declaration]\n" +
                "syntax = \"proto3\";\n" +
                "package tutorial;\n" +
                "\n" +
                "import \"google/protobuf/timestamp.proto\";\n" +
                "// [END declaration]\n" +
                "\n" +
                "// [START java_declaration]\n" +
                "option java_multiple_files = true;\n" +
                "option java_package = \"com.example.tutorial.protos\";\n" +
                "option java_outer_classname = \"AddressBookProtos\";\n" +
                "// [END java_declaration]\n" +
                "\n" +
                "// [START csharp_declaration]\n" +
                "option csharp_namespace = \"Google.Protobuf.Examples.AddressBook\";\n" +
                "// [END csharp_declaration]\n" +
                "\n" +
                "// [START go_declaration]\n" +
                "option go_package = \"github.com/protocolbuffers/protobuf/examples/go/tutorialpb\";\n" +
                "// [END go_declaration]\n" +
                "\n" +
                "// [START messages]\n" +
                "message Person {\n" +
                "  string name = 1;\n" +
                "  int32 id = 2;  // Unique ID number for this person.\n" +
                "  string email = 3;\n" +
                "\n" +
                "  enum PhoneType {\n" +
                "    MOBILE = 0;\n" +
                "    HOME = 1;\n" +
                "    WORK = 2;\n" +
                "  }\n" +
                "\n" +
                "  message PhoneNumber {\n" +
                "    string number = 1;\n" +
                "    PhoneType type = 2;\n" +
                "  }\n" +
                "\n" +
                "  repeated PhoneNumber phones = 4;\n" +
                "\n" +
                "  google.protobuf.Timestamp last_updated = 5;\n" +
                "}\n" +
                "\n" +
                "// Our address book file is just one of these.\n" +
                "message AddressBook {\n" +
                "  repeated Person people = 1;\n" +
                "}";
        Protobuf3GrammarParser parser = Protobuf3GrammarParser.newInstance();
        ProtoFile proto = parser.parseProto(tutorial);

        Assert.assertEquals(Syntax.proto3, proto.syntax);
        Assert.assertEquals("tutorial", proto._package);

        Assert.assertEquals(1, proto.imports.size());
        //Assert.assertEquals("google/protobuf/timestamp.proto",proto.imports.get(0));

        Assert.assertEquals(2, proto.topLevelDefs.size());
        Message message1 = (Message) proto.topLevelDefs.get(0);
        Assert.assertEquals("Person", message1.name);
        Assert.assertEquals(5, message1.content.size());
        Message message2 = (Message) proto.topLevelDefs.get(1);
        Assert.assertEquals("AddressBook", message2.name);
        Assert.assertEquals(1, message2.content.size());
    }
    protected void check(String value)
    {
        Protobuf3GrammarParser parser = Protobuf3GrammarParser.newInstance();
        ProtoFile proto = parser.parseProto(value);
        Protobuf3GrammarComposer composer = Protobuf3GrammarComposer.newInstance();
        String result = composer.renderProto(proto);
        Assert.assertEquals(value, result);
    }
}
