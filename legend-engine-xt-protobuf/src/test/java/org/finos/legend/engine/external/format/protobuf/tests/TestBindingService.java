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

package org.finos.legend.engine.external.format.protobuf.tests;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.protobuf.fromModel.ModelToProtobufConfiguration;
import org.finos.legend.engine.external.shared.format.model.api.ExternalFormats;
import org.finos.legend.engine.external.shared.format.model.api.GenerateSchemaInput;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

public class TestBindingService
{
    private static final ExternalFormats externalFormats = new ExternalFormats(new ModelManager(DeploymentMode.TEST));

    @Test
    public void test()
    {
        try
        {
            test(new ModelToProtobufConfiguration(),
                    "Class test::Pierre{}",
                    Lists.mutable.with("test::Pierre"),
                    "{\n" +
                            "  \"_type\": \"data\",\n" +
                            "  \"elements\": [\n" +
                            "    {\n" +
                            "      \"_type\": \"externalFormatSchemaSet\",\n" +
                            "      \"format\": \"Protobuf\",\n" +
                            "      \"name\": \"yo\",\n" +
                            "      \"package\": \"myPack\",\n" +
                            "      \"schemas\": [\n" +
                            "        {\n" +
                            "          \"location\":\"test.proto\"," +
                            "          \"content\": \"syntax = \\\"proto3\\\";\\npackage test;\\n\\nmessage Pierre {\\n  oneof pierre {\\n\\n  }\\n}\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"_type\": \"binding\",\n" +
                            "      \"contentType\": \"application/protobuf\",\n" +
                            "      \"includedStores\": [],\n" +
                            "      \"modelUnit\": {\n" +
                            "        \"packageableElementExcludes\": [],\n" +
                            "        \"packageableElementIncludes\": [\n" +
                            "          \"test::Pierre\",\n" +
                            "          \"test::Pierre\"\n" +
                            "        ]\n" +
                            "      },\n" +
                            "      \"name\": \"yo2\",\n" +
                            "      \"package\": \"myPack\",\n" +
                            "      \"schemaSet\": \"myPack::yo\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    public void test(ModelToSchemaConfiguration configuration, String model, List<String> elements, String test) throws ParseException
    {
        GenerateSchemaInput input = new GenerateSchemaInput();
        input.config = configuration;
        input.model = PureGrammarParser.newInstance().parseModel(model);
        configuration.targetSchemaSet = "myPack::yo";
        configuration.targetBinding = "myPack::yo2";
        configuration.sourceModel = elements;
        Response response = externalFormats.generateSchema(input, null);
        Object result = response.getEntity();
        if (result instanceof ExceptionError)
        {
            System.out.println(((ExceptionError) result).status);
            System.out.println(((ExceptionError) result).getSourceInformation());
            System.out.println(((ExceptionError) result).getMessage());
            System.out.println(((ExceptionError) result).getTrace());
            Assert.fail();
        }
        JSONParser jp = new JSONParser();
        Object deserializedLeft = jp.parse(test);
        Object deserializedRight = jp.parse(result.toString());
        Assert.assertEquals(deserializedLeft, deserializedRight);
    }
}
