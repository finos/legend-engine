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

package org.finos.legend.engine.language.pure.grammar.api.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJsonInput;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.TransformGrammarToJson;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

@Deprecated
public class TestGrammarToJsonApi
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();
    private static final TransformGrammarToJson grammarParserApi = new TransformGrammarToJson();

    @Test
    public void testProtocolGeneration()
    {
        test(getJsonString("pureParsingTest.json"), "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":21,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1},\"value\":\"test\"}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Person\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":6,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}},\"test2\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test2\",\"startColumn\":16,\"startLine\":1},\"value\":\"test2\"}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Person\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test2\",\"startColumn\":6,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":23,\"endLine\":1,\"sourceId\":\"test2\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testImportParsing()
    {
        test(getJsonString("pureImportParsingTest.json"), "{\"isolatedLambdas\":{\"lambdas\":{}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"constraints\":[],\"name\":\"Person\",\"originalMilestonedProperties\":[],\"package\":\"model\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"targetA\",\"propertyTypeSourceInformation\":{\"endColumn\":18,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":12,\"startLine\":4},\"sourceInformation\":{\"endColumn\":22,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":3,\"startLine\":4},\"stereotypes\":[],\"taggedValues\":[],\"type\":\"TargetA\"}],\"qualifiedProperties\":[],\"sourceInformation\":{\"endColumn\":1,\"endLine\":5,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":2},\"stereotypes\":[],\"superTypes\":[\"VersionClass\"],\"taggedValues\":[{\"sourceInformation\":{\"endColumn\":20,\"endLine\":2,\"sourceId\":\"\",\"startColumn\":8,\"startLine\":2},\"tag\":{\"profile\":\"doc\",\"profileSourceInformation\":{\"endColumn\":10,\"endLine\":2,\"sourceId\":\"\",\"startColumn\":8,\"startLine\":2},\"sourceInformation\":{\"endColumn\":14,\"endLine\":2,\"sourceId\":\"\",\"startColumn\":12,\"startLine\":2},\"value\":\"doc\"},\"value\":\"a\"}]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[\"model::Person\"],\"imports\":[\"projectA\"],\"parserName\":\"Pure\",\"sourceInformation\":{\"endColumn\":2,\"endLine\":7,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}}]}]},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testParsingWithNoError()
    {
        test("{\"code\": \"Class A {}\", \"isolatedLambdas\": {\"test\": \"|1\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":2,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":2,\"startLine\":1},\"value\":1}],\"parameters\":[],\"sourceInformation\":{\"endColumn\":2,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":1,\"startLine\":1}}}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"constraints\":[],\"name\":\"A\",\"originalMilestonedProperties\":[],\"package\":\"\",\"properties\":[],\"qualifiedProperties\":[],\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1},\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[\"A\"],\"imports\":[],\"parserName\":\"Pure\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":3,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}}]}]},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testLambdaParsingWithNoError()
    {
        test("{\"isolatedLambdas\": {\"test\": \"a:String[1]|'hello';\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":13,\"startLine\":1},\"value\":\"hello\"}],\"parameters\":[{\"_type\":\"var\",\"class\":\"String\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"a\",\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":3,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":20,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":12,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:String[1]|$src; \"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":18,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"String\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":14,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1]|$src+1; \"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"plus\",\"parameters\":[{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":2,\"upperBound\":2},\"sourceInformation\":{\"endColumn\":21,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":20,\"startLine\":1},\"values\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}},{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":21,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":21,\"startLine\":1},\"value\":1}]}],\"sourceInformation\":{\"endColumn\":21,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":20,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[2]|$src->first()->toOne() \"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"toOne\",\"parameters\":[{\"_type\":\"func\",\"function\":\"first\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":26,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":22,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":35,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":31,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":2,\"upperBound\":2},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":37,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Person[1] |$src.nameWithTitle('test');\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}},{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":40,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":35,\"startLine\":1},\"value\":\"test\"}],\"property\":\"nameWithTitle\",\"sourceInformation\":{\"endColumn\":33,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":21,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Person\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":42,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Person[1] |$src.name;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}],\"property\":\"name\",\"sourceInformation\":{\"endColumn\":24,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":21,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Person\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":25,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Boolean[1] | !$src;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"not\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":19,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":18,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Boolean\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":23,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1] | -$src;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"minus\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":22,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":19,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":18,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":18,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":23,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1] | add($src,minus([1,1]));\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"add\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":25,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":22,\"startLine\":1}},{\"_type\":\"func\",\"function\":\"minus\",\"parameters\":[{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":2,\"upperBound\":2},\"sourceInformation\":{\"endColumn\":37,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":33,\"startLine\":1},\"values\":[{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":34,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":34,\"startLine\":1},\"value\":1},{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":36,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":36,\"startLine\":1},\"value\":1}]}],\"sourceInformation\":{\"endColumn\":31,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":27,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":20,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":18,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":40,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1]|let a = 1;$a+1;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"letFunction\",\"parameters\":[{\"_type\":\"string\",\"value\":\"a\"},{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":24,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1},\"value\":1}],\"sourceInformation\":{\"endColumn\":24,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}},{\"_type\":\"func\",\"function\":\"plus\",\"parameters\":[{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":2,\"upperBound\":2},\"sourceInformation\":{\"endColumn\":29,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":28,\"startLine\":1},\"values\":[{\"_type\":\"var\",\"name\":\"a\",\"sourceInformation\":{\"endColumn\":27,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":26,\"startLine\":1}},{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":29,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":29,\"startLine\":1},\"value\":1}]}],\"sourceInformation\":{\"endColumn\":29,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":28,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":30,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1] | myEnum.VALUE1;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"packageableElementPtr\",\"fullPath\":\"myEnum\",\"sourceInformation\":{\"endColumn\":23,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":18,\"startLine\":1}}],\"property\":\"VALUE1\",\"sourceInformation\":{\"endColumn\":30,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":25,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":31,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        test("{\"isolatedLambdas\": {\"test\": \"src:Integer[1] | anything;\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"packageableElementPtr\",\"fullPath\":\"anything\",\"sourceInformation\":{\"endColumn\":25,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":18,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"Integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":11,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":26,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
        // a really meaningless lambda
        test("{\"isolatedLambdas\": {\"test\": \"anything\"}}", "{\"isolatedLambdas\":{\"lambdas\":{\"test\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"packageableElementPtr\",\"fullPath\":\"anything\",\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":1,\"startLine\":1}}],\"parameters\":[],\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":1,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testCodeParsingError()
    {
        test("{\"code\": \"Class A {\"}", "{\"codeError\":{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":9,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":9,\"startLine\":1}},\"isolatedLambdas\":{\"lambdas\":{}},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testLambdaParsingError()
    {
        test("{\"isolatedLambdas\": {\"test\": \"|1->toString(),\"}}", "{\"isolatedLambdas\":{\"lambdaErrors\":{\"test\":{\"message\":\"no viable alternative at input '->toString(),'\",\"sourceInformation\":{\"endColumn\":15,\"endLine\":1,\"sourceId\":\"test\",\"startColumn\":15,\"startLine\":1}}},\"lambdas\":{}},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testMixedParsingErrors()
    {
        test("{\"code\": \"Class A {,\", \"isolatedLambdas\": {\"good\": \"|'good'\", \"bad\": \"|,\"}}", "{\"codeError\":{\"message\":\"Unexpected token ','\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":10,\"startLine\":1}},\"isolatedLambdas\":{\"lambdaErrors\":{\"bad\":{\"message\":\"Unexpected token ','\",\"sourceInformation\":{\"endColumn\":2,\"endLine\":1,\"sourceId\":\"bad\",\"startColumn\":2,\"startLine\":1}}},\"lambdas\":{\"good\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":7,\"endLine\":1,\"sourceId\":\"good\",\"startColumn\":2,\"startLine\":1},\"value\":\"good\"}],\"parameters\":[],\"sourceInformation\":{\"endColumn\":7,\"endLine\":1,\"sourceId\":\"good\",\"startColumn\":1,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testMappingIncludeSerialization()
    {
        // Mapping include should yield `includedMapping` instead of `includedMappingPackage` and `includedMappingName`
        test("{\"code\": \"###Mapping\\nMapping A\\n(\\n include test::B\\n)\"}", "{\"isolatedLambdas\":{\"lambdas\":{}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"mapping\",\"associationMappings\":[],\"classMappings\":[],\"enumerationMappings\":[],\"includedMappings\":[{\"_type\":\"mappingIncludeMapping\",\"includedMapping\":\"test::B\",\"sourceInformation\":{\"endColumn\":16,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":2,\"startLine\":4}}],\"name\":\"A\",\"package\":\"\",\"sourceInformation\":{\"endColumn\":1,\"endLine\":5,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":2},\"tests\":[]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\",\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}},{\"_type\":\"importAware\",\"elements\":[\"A\"],\"imports\":[],\"parserName\":\"Mapping\",\"sourceInformation\":{\"endColumn\":1,\"endLine\":7,\"sourceId\":\"\",\"startColumn\":8,\"startLine\":2}}]}]},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testEnumerationMappingWithStructuredSourceValueSerialization()
    {
        test("{\"code\": \"###Mapping\\nMapping a::mapping\\n(test::IncType : EnumerationMapping a\\n{ CORP : [1], LLC : [2] })\"}", "{\"isolatedLambdas\":{\"lambdas\":{}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"mapping\",\"associationMappings\":[],\"classMappings\":[],\"enumerationMappings\":[{\"enumValueMappings\":[{\"enumValue\":\"CORP\",\"sourceValues\":[{\"_type\":\"integerSourceValue\",\"value\":1}]},{\"enumValue\":\"LLC\",\"sourceValues\":[{\"_type\":\"integerSourceValue\",\"value\":2}]}],\"enumeration\":\"test::IncType\",\"id\":\"a\",\"sourceInformation\":{\"endColumn\":25,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":2,\"startLine\":3}}],\"includedMappings\":[],\"name\":\"mapping\",\"package\":\"a\",\"sourceInformation\":{\"endColumn\":26,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":2},\"tests\":[]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\",\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}},{\"_type\":\"importAware\",\"elements\":[\"a::mapping\"],\"imports\":[],\"parserName\":\"Mapping\",\"sourceInformation\":{\"endColumn\":26,\"endLine\":6,\"sourceId\":\"\",\"startColumn\":8,\"startLine\":2}}]}]},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testPureInstanceClassMapping()
    {
        String code = "###Mapping\\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\\n" +
                "(\\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\\n" +
                "  {\\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\\n" +
                "  }\\n" +
                ")\\n";
        test("{\"code\": \"" + code + "\"}", "{\"isolatedLambdas\":{\"lambdas\":{}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"mapping\",\"associationMappings\":[],\"classMappings\":[{\"_type\":\"pureInstance\",\"class\":\"meta::pure::mapping::modelToModel::test::shared::dest::Person\",\"classSourceInformation\":{\"endColumn\":64,\"endLine\":4,\"sourceId\":\"\",\"startColumn\":4,\"startLine\":4},\"id\":\"meta_pure_mapping_modelToModel_test_shared_dest_Person\",\"propertyMappings\":[{\"_type\":\"purePropertyMapping\",\"explodeProperty\":false,\"property\":{\"class\":\"meta::pure::mapping::modelToModel::test::shared::dest::Person\",\"property\":\"firstName\",\"sourceInformation\":{\"endColumn\":13,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":5,\"startLine\":7}},\"source\":\"meta_pure_mapping_modelToModel_test_shared_dest_Person\",\"sourceInformation\":{\"endColumn\":71,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":5,\"startLine\":7},\"transform\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"substring\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":19,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":16,\"startLine\":7}}],\"property\":\"fullName\",\"sourceInformation\":{\"endColumn\":28,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":21,\"startLine\":7}},{\"_type\":\"integer\",\"sourceInformation\":{\"endColumn\":41,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":41,\"startLine\":7},\"value\":0},{\"_type\":\"func\",\"function\":\"indexOf\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":47,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":44,\"startLine\":7}}],\"property\":\"fullName\",\"sourceInformation\":{\"endColumn\":56,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":49,\"startLine\":7}},{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":69,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":67,\"startLine\":7},\"value\":\" \"}],\"sourceInformation\":{\"endColumn\":65,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":59,\"startLine\":7}}],\"sourceInformation\":{\"endColumn\":39,\"endLine\":7,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":31,\"startLine\":7}}],\"parameters\":[]}}],\"root\":true,\"sourceClassSourceInformation\":{\"endColumn\":72,\"endLine\":6,\"sourceId\":\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\",\"startColumn\":10,\"startLine\":6},\"sourceInformation\":{\"endColumn\":3,\"endLine\":8,\"sourceId\":\"\",\"startColumn\":3,\"startLine\":4},\"srcClass\":\"meta::pure::mapping::modelToModel::test::shared::src::_S_Person\"}],\"enumerationMappings\":[],\"includedMappings\":[],\"name\":\"simpleModelMapping\",\"package\":\"meta::pure::mapping::modelToModel::test::simple\",\"sourceInformation\":{\"endColumn\":1,\"endLine\":9,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":2},\"tests\":[]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\",\"sourceInformation\":{\"endColumn\":8,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}},{\"_type\":\"importAware\",\"elements\":[\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\"],\"imports\":[],\"parserName\":\"Mapping\",\"sourceInformation\":{\"endColumn\":2,\"endLine\":11,\"sourceId\":\"\",\"startColumn\":8,\"startLine\":2}}]}]},\"renderStyle\":\"STANDARD\"}");
    }

    @Test
    public void testPureInstanceClassMappingNoSourceInfo()
    {
        String code = "###Mapping\\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\\n" +
                "(\\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\\n" +
                "  {\\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\\n" +
                "  }\\n" +
                ")\\n";
        test("{\"code\": \"" + code + "\"}", "{\"isolatedLambdas\":{\"lambdas\":{}},\"modelDataContext\":{\"_type\":\"data\",\"elements\":[{\"_type\":\"mapping\",\"associationMappings\":[],\"classMappings\":[{\"_type\":\"pureInstance\",\"class\":\"meta::pure::mapping::modelToModel::test::shared::dest::Person\",\"id\":\"meta_pure_mapping_modelToModel_test_shared_dest_Person\",\"propertyMappings\":[{\"_type\":\"purePropertyMapping\",\"explodeProperty\":false,\"property\":{\"class\":\"meta::pure::mapping::modelToModel::test::shared::dest::Person\",\"property\":\"firstName\"},\"source\":\"meta_pure_mapping_modelToModel_test_shared_dest_Person\",\"transform\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"substring\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\"}],\"property\":\"fullName\"},{\"_type\":\"integer\",\"value\":0},{\"_type\":\"func\",\"function\":\"indexOf\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\"}],\"property\":\"fullName\"},{\"_type\":\"string\",\"value\":\" \"}]}]}],\"parameters\":[]}}],\"root\":true,\"srcClass\":\"meta::pure::mapping::modelToModel::test::shared::src::_S_Person\"}],\"enumerationMappings\":[],\"includedMappings\":[],\"name\":\"simpleModelMapping\",\"package\":\"meta::pure::mapping::modelToModel::test::simple\",\"tests\":[]},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\"},{\"_type\":\"importAware\",\"elements\":[\"meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\"],\"imports\":[],\"parserName\":\"Mapping\"}]}]},\"renderStyle\":\"STANDARD\"}", false);
    }

    private String getJsonString(String path)
    {
        return new java.util.Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), "Can't find resource '" + path + "'")).useDelimiter("\\A").next();
    }

    @Test
    public void testLambdaWithCast()
    {
        test("{\"isolatedLambdas\":{\"testLambda\":\"src:OldClass[1]|$src->cast(@newClass)\"}}",
                "{\"isolatedLambdas\":{\"lambdas\":{\"testLambda\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"cast\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":20,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":17,\"startLine\":1}},{\"_type\":\"genericTypeInstance\",\"fullPath\":\"newClass\",\"sourceInformation\":{\"endColumn\":36,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":29,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":26,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":23,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"OldClass\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":12,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":5,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":37,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":16,\"startLine\":1}}}},\"renderStyle\":\"STANDARD\"}");
    }

    private void test(String request, String expected)
    {
        test(request, expected, true);
    }

    private void test(String request, String expected, boolean returnSourceInfo)
    {
        try
        {
            GrammarToJsonInput grammarToJsonInput = objectMapper.readValue(request, GrammarToJsonInput.class);
            Response result = grammarParserApi.transformGrammarToJson(grammarToJsonInput, null, returnSourceInfo);
            String actual = result.getEntity().toString();
            assertEquals(expected, actual);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
