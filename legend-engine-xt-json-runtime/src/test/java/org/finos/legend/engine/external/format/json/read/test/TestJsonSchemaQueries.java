package org.finos.legend.engine.external.format.json.read.test;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.List;

public class TestJsonSchemaQueries extends TestExternalFormatQueries
{
    @Test
    public void testDeserializeJsonWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toJsonSchemaConfig(Lists.mutable.with("test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition")));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/peopleTestData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testDeserializeJsonWithFullTree()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toJsonSchemaConfig(Lists.mutable.with("test::firm::model::Firm", "test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition")));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Firm.all()->graphFetch(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmTreeTestData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmTreeResult.json")));
    }

    @Test
    public void testDeserializeJsonWithDefects()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toJsonSchemaConfig(Lists.mutable.with("test::firm::model::Firm", "test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition")));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmTreeDefectsData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmTreeDefectsResult.json")));
    }

    @Test
    public void testDeserializeAndSerializeJsonWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toJsonSchemaConfig(Lists.mutable.with("test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition")));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding)",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/peopleTestData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/externalizePeopleCheckedResult.json")));
    }

    @Test
    public void testDeserializeAndSerializeJsonWithFullTree()
    {
        String grammar = serializedFirmModel() + "\n\n" + jsonSchema() + "\n\n" + firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::Binding");
        String result = runTest(null,
                grammar,
                "|test::firm::model::Firm.all()->graphFetch(" + fullTree() + ")->externalize(test::Binding)",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmTreeTestData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmTreeResult.json")));
    }

    @Test
    public void testDeserializeAndSerializeJsonWithDefects()
    {
        String grammar = serializedFirmModel() + "\n\n" + jsonSchema() + "\n\n" + firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::Binding");
        String result = runTest(null,
                grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->externalize(test::Binding)",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmTreeDefectsData.json"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmTreeDefectsResult.json")));
    }

    private String serializedFirmModel()
    {
        return "###Pure\n" +
                "Enum test::firm::model::AddressType\n" +
                "{\n" +
                "   Headquarters,\n" +
                "   RegionalOffice,\n" +
                "   Home,\n" +
                "   Holiday\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Firm\n" +
                "{\n" +
                "   name      : String[1];\n" +
                "   ranking   : Integer[0..1];\n" +
                "   addresses : test::firm::model::AddressUse[1..*];\n" +
                "   employees : test::firm::model::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Address\n" +
                "{\n" +
                "   firstLine  : String[1];\n" +
                "   secondLine : String[0..1];\n" +
                "   city       : String[0..1];\n" +
                "   region     : String[0..1];\n" +
                "   country    : String[1];\n" +
                "   position   : test::firm::model::GeographicPosition[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::GeographicPosition\n" +
                "[\n" +
                "   validLatitude: ($this.latitude >= -90) && ($this.latitude <= 90),\n" +
                "   validLongitude: ($this.longitude >= -180) && ($this.longitude <= 180)\n" +
                "]\n" +
                "{\n" +
                "   latitude  : Float[1];\n" +
                "   longitude : Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::AddressUse\n" +
                "{\n" +
                "   addressType : test::firm::model::AddressType[1];\n" +
                "   address     : test::firm::model::Address[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Person\n" +
                "{\n" +
                "   firstName      : String[1];\n" +
                "   lastName       : String[1];\n" +
                "   dateOfBirth    : StrictDate[0..1];   \n" +
                "   addresses      : test::firm::model::AddressUse[*];\n" +
                "   isAlive        : Boolean[1];\n" +
                "   heightInMeters : Float[1];\n" +
                "}\n";

    }

    private String jsonSchema()
    {
        String jsonSchema = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"title\": \"test::firm::model::Firm\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"ranking\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"addresses\": {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\": {\n" +
                "        \"$ref\": \"#/definitions/test::firm::model::AddressUse\"\n" +
                "      },\n" +
                "      \"minItems\": 1\n" +
                "    },\n" +
                "    \"employees\": {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\": {\n" +
                "        \"$ref\": \"#/definitions/test::firm::model::Person\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "    \"name\",\n" +
                "    \"addresses\"\n" +
                "  ],\n" +
                "  \"definitions\": {\n" +
                "    \"test::firm::model::Address\": {\n" +
                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "      \"title\": \"test::firm::model::Address\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"firstLine\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"secondLine\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"city\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"region\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"country\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"position\": {\n" +
                "          \"$ref\": \"#/definitions/test::firm::model::GeographicPosition\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"firstLine\",\n" +
                "        \"country\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::firm::model::AddressType\": {\n" +
                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "      \"title\": \"test::firm::model::AddressType\",\n" +
                "      \"enum\": [\n" +
                "        \"Headquarters\",\n" +
                "        \"RegionalOffice\",\n" +
                "        \"Home\",\n" +
                "        \"Holiday\"\n" +
                "      ],\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"test::firm::model::AddressUse\": {\n" +
                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "      \"title\": \"test::firm::model::AddressUse\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"addressType\": {\n" +
                "          \"$ref\": \"#/definitions/test::firm::model::AddressType\"\n" +
                "        },\n" +
                "        \"address\": {\n" +
                "          \"$ref\": \"#/definitions/test::firm::model::Address\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"addressType\",\n" +
                "        \"address\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::firm::model::GeographicPosition\": {\n" +
                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "      \"title\": \"test::firm::model::GeographicPosition\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"latitude\": {\n" +
                "          \"type\": \"number\"\n" +
                "        },\n" +
                "        \"longitude\": {\n" +
                "          \"type\": \"number\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"latitude\",\n" +
                "        \"longitude\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::firm::model::Person\": {\n" +
                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "      \"title\": \"test::firm::model::Person\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"firstName\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"lastName\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"dateOfBirth\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"format\": \"date\"\n" +
                "        },\n" +
                "        \"addresses\": {\n" +
                "          \"type\": \"array\",\n" +
                "          \"items\": {\n" +
                "            \"$ref\": \"#/definitions/test::firm::model::AddressUse\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"isAlive\": {\n" +
                "          \"type\": \"boolean\"\n" +
                "        },\n" +
                "        \"heightInMeters\": {\n" +
                "          \"type\": \"number\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"firstName\",\n" +
                "        \"lastName\",\n" +
                "        \"isAlive\",\n" +
                "        \"heightInMeters\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";

        return "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::firm::model::Firm, test::firm::model::Person, test::firm::model::Address, test::firm::model::AddressUse, test::firm::model::GeographicPosition ];\n" +
                "}\n" +
                "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [ { location: 'test/firm/model/Firm.json';\n" +
                "               content: " + PureGrammarComposerUtility.convertString(jsonSchema, true) + "; } ];\n" +
                "}\n";
    }

    private ModelToJsonSchemaConfiguration toJsonSchemaConfig(List<String> classNames)
    {
        ModelToJsonSchemaConfiguration config = new ModelToJsonSchemaConfiguration();
        config.targetBinding = "test::gen::TestBinding";
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.sourceModel.addAll(classNames);
        config.format = "JSON";
        return config;
    }
}
