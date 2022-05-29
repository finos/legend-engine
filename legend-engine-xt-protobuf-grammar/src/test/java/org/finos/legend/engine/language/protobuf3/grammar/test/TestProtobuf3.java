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

package org.finos.legend.engine.language.protobuf3.grammar.test;

import org.finos.legend.engine.language.protobuf3.grammar.from.Protobuf3GrammarParser;
import org.finos.legend.engine.language.protobuf3.grammar.to.Protobuf3GrammarComposer;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Message;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Syntax;
import org.junit.Assert;
import org.junit.Test;

public class TestProtobuf3
{

    @Test
    public void testFile()
    {
        String tutorial = "syntax = \"proto3\";\n" +
                "package tutorial;\n" +
                "\n" +
                "import \"google/protobuf/timestamp.proto\";\n" +
                "\n" +
                "option java_multiple_files = true;\n" +
                "option java_package = \"com.example.tutorial.protos\";\n" +
                "option java_outer_classname = \"AddressBookProtos\";\n" +
                "option csharp_namespace = \"Google.Protobuf.Examples.AddressBook\";\n" +
                "option go_package = \"github.com/protocolbuffers/protobuf/examples/go/tutorialpb\";\n" +
                "\n" +
                "message AddressBook {\n" +
                "  repeated Person people = 1;\n" +
                "}\n" +
                "\n" +
                "message Person {\n" +
                "  string name = 1;\n" +
                "  int32 id = 2;\n" +
                "  string email = 3;\n" +
                "enum PhoneType {\n" +
                "  MOBILE = 0;\n" +
                "  HOME = 1;\n" +
                "  WORK = 2;\n" +
                "}\n" +
                "message PhoneNumber {\n" +
                "  string number = 1;\n" +
                "  PhoneType type = 2;\n" +
                "}\n" +
                "  repeated PhoneNumber phones = 4;\n" +
                "  google.protobuf.Timestamp last_updated = 5;\n" +
                "}";
        Protobuf3GrammarParser parser = Protobuf3GrammarParser.newInstance();
        ProtoFile proto = parser.parseProto(tutorial);

        Assert.assertEquals(Syntax.proto3, proto.syntax);
        Assert.assertEquals("tutorial", proto._package);

        Assert.assertEquals(1, proto.imports.size());
        Assert.assertEquals("google/protobuf/timestamp.proto", proto.imports.get(0).name);

        Assert.assertEquals(2, proto.topLevelDefs.size());

        Message message1 = (Message) proto.topLevelDefs.get(0);
        Assert.assertEquals("AddressBook", message1.name);
        Assert.assertEquals(1, message1.content.size());

        Message message2 = (Message) proto.topLevelDefs.get(1);
        Assert.assertEquals("Person", message2.name);
        Assert.assertEquals(7, message2.content.size());

        check(tutorial);
    }

    @Test
    public void testOneOf()
    {
        String message = "syntax = \"proto3\";\n" +
                "message SampleMessage {\n" +
                "  oneof test_oneof {\n" +
                "    string name = 4;\n" +
                "    SubMessage sub_message = 9;\n" +
                "  }\n" +
                "}";

        check(message);
    }

    @Test
    public void testReserved()
    {
        String message = "syntax = \"proto3\";\n" +
                "message Foo {\n" +
                "  reserved 2, 15, 9 to 11;\n" +
                "  reserved \"foo\", \"bar\";\n" +
                "}";

        check(message);
    }

    @Test
    public void testService() {
        String service =
                "syntax = \"proto3\";\n" +
                        "package helloworld;\n" +
                        "\n" +
                        "service Greeter {\n" +
                        "  rpc SayHello(HelloRequest) returns (HelloReply) {}\n" +
                        "  rpc SayHelloAgain(HelloRequest) returns (HelloReply) {}\n" +
                        "}\n" +
                        "\n" +
                        "message HelloRequest {\n" +
                        "  string name = 1;\n" +
                        "}\n" +
                        "\n" +
                        "message HelloReply {\n" +
                        "  string message = 1;\n" +
                        "}";
        check(service);
    }

    @Test
    public void testAnnotatedService()
    {
        String bookService = "syntax = \"proto3\";\n" +
                "package bookshelf;\n" +
                "\n" +
                "service BookShelf {\n" +
                "  rpc ListShelves(google.protobuf.Empty) returns (ListShelvesResponse) {\n" +
                "option (google.api.http) = { get: \"/v1/shelves\" };\n" +
                "}\n" +
                "}";

        check(bookService);
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
