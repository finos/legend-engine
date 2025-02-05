// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.extension.Profile;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestDomainGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DomainParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "Class " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "}\n";
    }

    @Test
    public void testGraphFetchTreeWithSubtypeTreeAtPropertyLevel()
    {
        String nestedSubTypeTree =  "#{\n" +
                                    "    test::Firm{\n" +
                                    "      legalName,\n" +
                                    "      ->subType(@FirmSubType){\n" +
                                    "        subTypeName,\n"  +
                                    "        ->subType(@FirmSubSubType){\n" +
                                    "           subSubTypeName\n" +
                                    "        }\n" +
                                    "      }\n" +
                                    "    }\n" +
                                    "  }#\n";
        String code1 = "function my::test(): Any[*]\n{\n   " + nestedSubTypeTree.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code1,  "PARSER error at [3:50-105]: ->subType() is supported only at root level");

        String subTypeTreeInsidePropertyTre =   "#{\n" +
                                                "    test::Firm{\n" +
                                                "      legalName,\n" +
                                                "      Address{\n" +
                                                "      ->subType(@Street){\n" +
                                                "        streetName\n"  +
                                                "        }\n" +
                                                "      }\n" +
                                                "    }\n" +
                                                "  }#\n";
        String code2 = "function my::test(): Any[*]\n{\n   " + subTypeTreeInsidePropertyTre.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code2,  "PARSER error at [3:34-65]: ->subType() is supported only at root level");

        String emptySubTypeTreesAtRootLevel = "  #{\n" +
                                              "    test::Address {\n" +
                                              "      zipCode,\n" +
                                              "      ->subType(@test::Street) {\n" +
                                              "      }\n" +
                                              "    }\n" +
                                              "  }#\n";
        String code4 = "function my::test(): Any[*]\n{\n   " + emptySubTypeTreesAtRootLevel.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code4,  "PARSER error at [3:53]: Unexpected token '}'");
    }

    @Test
    public void testClass()
    {
        PureModelContextData pureModelContextData = test("Class <<temporal.businesstemporal>> {doc.doc = 'something'} A extends B\n" +
                "{\n" +
                "  <<equality.Key>> {doc.doc = 'bla'} name: e::R[*];\n" +
                "  {doc.doc = 'bla'} ok: Integer[1..2];\n" +
                "  <<devStatus.inProgress>> q(s: String[1]) {$s + 'ok'}: c::d::R[1];\n" +
                "  {doc.doc = 'bla'} xza(s: z::k::B[1]) {$s + 'ok'}: String[1];\n" +
                "}\n" +
                "\n" +
                "Class z::k::B\n" +
                "{\n" +
                "  z: String[1];\n" +
                "}\n");

        Map<String, PackageableElement> elementMap = pureModelContextData.getElements().stream().collect(Collectors.toMap(x -> x.getPath(), Function.identity()));

        Class aClass = (Class) elementMap.get("A");
        Assert.assertEquals(1, aClass.superTypes.size());
        Assert.assertEquals("B", aClass.superTypes.get(0).path);
        Assert.assertEquals(PackageableElementType.CLASS, aClass.superTypes.get(0).type);
        Assert.assertNotNull(aClass.superTypes.get(0).sourceInformation);
        Assert.assertEquals(1, aClass.superTypes.get(0).sourceInformation.startLine);
        Assert.assertEquals(71, aClass.superTypes.get(0).sourceInformation.startColumn);
    }

    @Test
    public void testProfile()
    {
        PureModelContextData pureModelContextData = test("Profile test::A\n" +
                "{\n" +
                "   tags : [tag1, tag2];\n" +
                "   stereotypes : [stereotype1, stereotype2];\n" +
                "}\n");

        Map<String, PackageableElement> elementMap = pureModelContextData.getElements().stream().collect(Collectors.toMap(x -> x.getPath(), Function.identity()));

        Profile profile = (Profile) elementMap.get("test::A");

        Assert.assertNotNull(profile.sourceInformation);
        Assert.assertEquals(1, profile.sourceInformation.startLine);
        Assert.assertEquals(5, profile.sourceInformation.endLine);
        Assert.assertEquals(1, profile.sourceInformation.startColumn);
        Assert.assertEquals(1, profile.sourceInformation.endColumn);
        Assert.assertEquals(2, profile.stereotypes.size());
        Assert.assertEquals(2, profile.tags.size());

        Assert.assertEquals("stereotype1", profile.stereotypes.get(0).value);
        Assert.assertEquals("stereotype2", profile.stereotypes.get(1).value);

        Assert.assertNotNull(profile.stereotypes.get(0).sourceInformation);
        Assert.assertEquals(4, profile.stereotypes.get(0).sourceInformation.startLine);
        Assert.assertEquals(4, profile.stereotypes.get(0).sourceInformation.endLine);
        Assert.assertEquals(19, profile.stereotypes.get(0).sourceInformation.startColumn);
        Assert.assertEquals(29, profile.stereotypes.get(0).sourceInformation.endColumn);
        Assert.assertEquals(4, profile.stereotypes.get(1).sourceInformation.startLine);
        Assert.assertEquals(4, profile.stereotypes.get(1).sourceInformation.endLine);
        Assert.assertEquals(32, profile.stereotypes.get(1).sourceInformation.startColumn);
        Assert.assertEquals(42, profile.stereotypes.get(1).sourceInformation.endColumn);

        Assert.assertEquals("tag1", profile.tags.get(0).value);
        Assert.assertEquals("tag2", profile.tags.get(1).value);

        Assert.assertNotNull(profile.tags.get(0).sourceInformation);
        Assert.assertEquals(3, profile.tags.get(0).sourceInformation.startLine);
        Assert.assertEquals(3, profile.tags.get(0).sourceInformation.endLine);
        Assert.assertEquals(12, profile.tags.get(0).sourceInformation.startColumn);
        Assert.assertEquals(15, profile.tags.get(0).sourceInformation.endColumn);
        Assert.assertEquals(3, profile.tags.get(1).sourceInformation.startLine);
        Assert.assertEquals(3, profile.tags.get(1).sourceInformation.endLine);
        Assert.assertEquals(18, profile.tags.get(1).sourceInformation.startColumn);
        Assert.assertEquals(21, profile.tags.get(1).sourceInformation.endColumn);
    }

    @Test
    public void testPreventTypeParametersInClass()
    {
        EngineException e = Assert.assertThrows(EngineException.class, () -> test(
                "Class x::X<T>{}"));
        Assert.assertEquals("PARSER error at [1:11-13]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "Class x::X<|m>{}"));
        Assert.assertEquals("PARSER error at [1:11-14]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "Class x::X<T|m>{}"));
        Assert.assertEquals("PARSER error at [1:11-15]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "Class x::X<T,U|m,n>{}"));
        Assert.assertEquals("PARSER error at [1:11-19]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());
    }

    @Test
    public void testPreventTypeParametersInFunction()
    {
        EngineException e = Assert.assertThrows(EngineException.class, () -> test(
                "function x::f<T>(x:String[1]):String[1]{'ok'}"));
        Assert.assertEquals("PARSER error at [1:14-16]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "function x::f<|m>(x:String[1]):String[1]{'ok'}"));
        Assert.assertEquals("PARSER error at [1:14-17]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "function x::f<T|m>(x:String[1]):String[1]{'ok'}"));
        Assert.assertEquals("PARSER error at [1:14-18]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());

        e = Assert.assertThrows(EngineException.class, () -> test(
                "function x::f<T,X|m,m>(x:String[1]):String[1]{'ok'}"));
        Assert.assertEquals("PARSER error at [1:14-22]: Type and/or multiplicity parameters are not authorized in Legend Engine", e.toPretty());
    }
}
