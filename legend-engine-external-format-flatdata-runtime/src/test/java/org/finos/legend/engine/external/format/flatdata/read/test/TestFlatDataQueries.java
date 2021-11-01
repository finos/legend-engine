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

package org.finos.legend.engine.external.format.flatdata.read.test;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.flatdata.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

import static org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest.newExternalSchemaSetGrammarBuilder;

public class TestFlatDataQueries extends TestExternalFormatQueries
{
    @Test
    public void testDeserializeCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::firm::model::Person"));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                                grammar,
                                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                                "test::firm::mapping::SelfMapping",
                                "test::runtime",
                                resource("queries/peopleWithExactHeadings.csv"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testDeserializeCsvBadHeadings()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::firm::model::Person"));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                                grammar,
                                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                                "test::firm::mapping::SelfMapping",
                                "test::runtime",
                                resource("queries/people.csv"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleBadHeadingsResult.json")));
    }

    @Test
    public void testDeserializeAndReserializeCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::firm::model::Person"));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                                grammar,
                                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding)",
                                "test::firm::mapping::SelfMapping",
                                "test::runtime",
                                resource("queries/peopleWithExactHeadings.csv"));

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), result);
    }

    @Test
    public void testDeserializeAndReserializeUncheckedCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::firm::model::Person"));

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                                grammar,
                                "|test::firm::model::Person.all()->graphFetch(" + personTree() + ")->externalize(test::gen::TestBinding)",
                                "test::firm::mapping::SelfMapping",
                                "test::runtime",
                                resource("queries/peopleWithExactHeadings.csv"));

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), result);
    }

    @Test
    public void testDeserializeCsvAndReserializeWithGeneratedModel()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::tradeSchema", "FlatData")
                .withSchemaText("section trade: DelimitedWithHeadings\n" +
                                        "{\n" +
                                        "  scope.untilEof;\n" +
                                        "  delimiter: ',';\n" +
                                        "  nullString: '';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Product           : STRING;\n" +
                                        "    Quantity          : INTEGER;\n" +
                                        "    'Trade Time'      : DATETIME;\n" +
                                        "    Price             : DECIMAL;\n" +
                                        "    'Price Ccy'       : STRING;\n" +
                                        "    'Settlement Ccy'  : STRING(optional);\n" +
                                        "    'Settlement Rate' : DECIMAL(optional);\n" +
                                        "    'Settlement Date' : DATE;\n" +
                                        "    'Confirmed At'    : DATETIME(optional);\n" +
                                        "    'Expiry Date'     : DATE(optional);\n" +
                                        "    'Executions'      : INTEGER(optional);\n" +
                                        "  }\n" +
                                        "}")
                .build();

        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("test::tradeSchema"));

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::Trade {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData ="Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                                grammar,
                                "|test::gen::Trade.all()->graphFetchChecked("+tradeTree+")->externalize(test::gen::TestBinding)",
                                "test::trade::SelfMapping",
                                "test::runtime",
                                tradeData);

        Assert.assertEquals(tradeData, result);
    }

    @Test
    public void testParameterInputFlatData()
    {
        String modelGrammar = firmModel();
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::firm::model::Person"));

        String grammar = firmSelfMapping() + parameterRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding", "flatDataInput");
        String externalizeResult = runTest(generated,
                grammar,
                "{flatDataInput:String[1] | test::firm::model::Person.all()->graphFetch(" + personTree() + ")->externalize(test::gen::TestBinding)}",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                (InputStream) null,
                Maps.mutable.of("flatDataInput", resourceAsString("queries/peopleWithExactHeadings.csv")));

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), externalizeResult);

        String serializeResult = runTest(generated,
                grammar,
                "{flatDataInput:String[1] | test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")}",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                (InputStream) null,
                Maps.mutable.of("flatDataInput", resourceAsString("queries/peopleWithExactHeadings.csv")));

        MatcherAssert.assertThat(serializeResult, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    private String tradeSelfMapping()
    {
        return "###Mapping\n" +
                "\n" +
                "Mapping test::trade::SelfMapping\n" +
                "(\n" +
                "   test::gen::Trade: Pure\n" +
                "   {\n" +
                "      ~src test::gen::Trade\n" +
                "   }\n" +
                ")\n";
    }

    private ModelToFlatDataConfiguration toFlatDataConfig(String className)
    {
        ModelToFlatDataConfiguration config = new ModelToFlatDataConfiguration();
        config.targetBinding = "test::gen::TestBinding";
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.sourceModel.add(className);
        config.format = "FlatData";
        return config;
    }

    private FlatDataToModelConfiguration fromFlatDataConfig(String sourceSchemaSet)
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
        config.sourceSchemaSet = sourceSchemaSet;
        config.targetBinding = "test::gen::TestBinding";
        config.targetPackage = "test::gen";
        config.purifyNames = true;
        return config;
    }
}
