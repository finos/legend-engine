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

package org.finos.legend.engine.language.pure.compiler.api.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.language.pure.compiler.api.Compile;
import org.finos.legend.engine.language.pure.compiler.api.LambdaReturnTypeInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.relation.RelationType;
import org.finos.legend.engine.protocol.pure.m3.relation.Column;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class TestCompileApi
{
    private static final Compile compileApi = new Compile(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testEnumerationMappingWithMixedFormatSourceValues()
    {
        testWithProtocolPath("faultyEnumerationMappingWithMixedFormatSourceValues.json",
                "{\n" +
                        "  \"errorType\" : \"COMPILATION\",\n" +
                        "  \"code\" : -1,\n" +
                        "  \"status\" : \"error\",\n" +
                        "  \"message\" : \"Error in 'meta::sMapping::tests::simpleMapping1': Mixed formats for enum value mapping source values\"\n" +
                        "}");
    }

    @Test
    public void testResolutionOfAutoImportsWhenNoSectionInfoIsProvided()
    {
        testWithProtocolPath("enumerationWithSystemProfileButNoSection.json");
    }

    public void testWithProtocolPath(String protocolPath)
    {
        testWithProtocolPath(protocolPath, null);
    }

    public void testWithProtocolPath(String protocolPath, String compilationResult)
    {
        String jsonString = new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(protocolPath), "Can't find resource '" + protocolPath + "'"), "UTF-8").useDelimiter("\\A").next();
        testWithJson(jsonString, compilationResult);
    }

    // NOTE: since if compilation failed we throw an EngineException which inherits many properties from the general Exception
    // comparing the JSON is not a good option, so we have to search fragment of the error response string instead
    // We can fix this method when we properly serialize the error response
    public void testWithJson(String pureModelContextDataJsonStr, String compilationResult)
    {
        String actual;
        try
        {
            PureModelContextData pureModelContextData = objectMapper.readValue(pureModelContextDataJsonStr, PureModelContextData.class);
            Object response = compileApi.compile(pureModelContextData, null, null).getEntity();
            actual = objectMapper.writeValueAsString(response);
            if (compilationResult != null)
            {
                JsonAssert.assertJsonEquals(compilationResult, actual,
                        JsonAssert.whenIgnoringPaths("trace")
                );
            }
            else
            {
                assertEquals("{\"message\":\"OK\",\"defects\":[]}", actual);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRelationType() throws JsonProcessingException
    {
        String model = "Class model::Person {\n" +
                "name: String[1];\n" +
                "}\n";
        PureModelContextText text = new PureModelContextText();
        text.code = model;
        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|model::Person.all()->project(~['Person Name':x|$x.name])", "", 0, 0, false);
        LambdaReturnTypeInput lambdaReturnTypeInput = new LambdaReturnTypeInput();
        lambdaReturnTypeInput.model = text;
        lambdaReturnTypeInput.lambda = lambda;
        String stringResult = objectMapper.writeValueAsString(compileApi.lambdaRelationType(lambdaReturnTypeInput, null, null).getEntity());
        RelationType relationType = objectMapper.readValue(stringResult, RelationType.class);
        Assert.assertEquals(1, relationType.columns.size());
        Column column = relationType.columns.get(0);
        Assert.assertEquals("Person Name", column.name);
        Assert.assertEquals("String", ((PackageableType) column.genericType.rawType).fullPath);
    }

    @Test
    public void testRelationTypeWithStereotypeAndTaggedValue() throws JsonProcessingException
    {
        String model = "Profile test::SampleProfile\n" +
                "{\n" +
                "  stereotypes: [important];\n" +
                "  tags: [doc];\n" +
                "}\n" +
                "\n" +
                "Class model::Person {\n" +
                "name: String[1];\n" +
                "}\n";
        PureModelContextText text = new PureModelContextText();
        text.code = model;
        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|model::Person.all()->project(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'model documentation'} 'Person Name':x|$x.name])", "", 0, 0, false);
        LambdaReturnTypeInput lambdaReturnTypeInput = new LambdaReturnTypeInput();
        lambdaReturnTypeInput.model = text;
        lambdaReturnTypeInput.lambda = lambda;
        String stringResult = objectMapper.writeValueAsString(compileApi.lambdaRelationType(lambdaReturnTypeInput, null, null).getEntity());
        RelationType relationType = objectMapper.readValue(stringResult, RelationType.class);
        Assert.assertEquals(1, relationType.columns.size());
        Column column = relationType.columns.get(0);
        Assert.assertEquals("Person Name", column.name);
        Assert.assertEquals("String", ((PackageableType) column.genericType.rawType).fullPath);
        Assert.assertEquals("test::SampleProfile", column.stereotypes.get(0).profile);
        Assert.assertEquals("important", column.stereotypes.get(0).value);
        Assert.assertEquals("test::SampleProfile", column.taggedValues.get(0).tag.profile);
        Assert.assertEquals("doc", column.taggedValues.get(0).tag.value);
        Assert.assertEquals("model documentation", column.taggedValues.get(0).value);
    }

    @Test
    public void testRelationTypeWithStereotypeAndTaggedValueAggregateColumn() throws JsonProcessingException
    {
        String model = "Profile test::SampleProfile\n" +
                "{\n" +
                "  stereotypes: [important];\n" +
                "  tags: [doc];\n" +
                "}\n" +
                "\n" +
                "Class model::Person {\n" +
                "name: String[1];\n" +
                "age: Integer[1];\n" +
                "}\n";
        PureModelContextText text = new PureModelContextText();
        text.code = model;
        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|model::Person.all()->project(~['Person Name':x|$x.name, 'Person Age Sum': x|$x.age])->groupBy(~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'name documentation'} 'Person Name'], ~[<<test::SampleProfile.important>> {test::SampleProfile.doc = 'age documentation'} 'Person Age Sum': x|$x.'Person Age Sum':x|$x->sum()])", "", 0, 0, false);
        LambdaReturnTypeInput lambdaReturnTypeInput = new LambdaReturnTypeInput();
        lambdaReturnTypeInput.model = text;
        lambdaReturnTypeInput.lambda = lambda;
        String stringResult = objectMapper.writeValueAsString(compileApi.lambdaRelationType(lambdaReturnTypeInput, null, null).getEntity());
        RelationType relationType = objectMapper.readValue(stringResult, RelationType.class);
        Assert.assertEquals(2, relationType.columns.size());
        Column nameColumn = relationType.columns.get(0);
        Assert.assertEquals("Person Name", nameColumn.name);
        Assert.assertEquals("String", ((PackageableType) nameColumn.genericType.rawType).fullPath);
        Assert.assertEquals("test::SampleProfile", nameColumn.stereotypes.get(0).profile);
        Assert.assertEquals("important", nameColumn.stereotypes.get(0).value);
        Assert.assertEquals("test::SampleProfile", nameColumn.taggedValues.get(0).tag.profile);
        Assert.assertEquals("doc", nameColumn.taggedValues.get(0).tag.value);
        Assert.assertEquals("name documentation", nameColumn.taggedValues.get(0).value);
        Column ageColumn = relationType.columns.get(1);
        Assert.assertEquals("Person Age Sum", ageColumn.name);
        Assert.assertEquals("Integer", ((PackageableType) ageColumn.genericType.rawType).fullPath);
        Assert.assertEquals("test::SampleProfile", ageColumn.stereotypes.get(0).profile);
        Assert.assertEquals("important", ageColumn.stereotypes.get(0).value);
        Assert.assertEquals("test::SampleProfile", ageColumn.taggedValues.get(0).tag.profile);
        Assert.assertEquals("doc", ageColumn.taggedValues.get(0).tag.value);
        Assert.assertEquals("age documentation", ageColumn.taggedValues.get(0).value);
    }

}

