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
import org.finos.legend.engine.external.format.flatdata.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

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
    public void testDeserializeCsvWithEnum()
    {
        String modelGrammar = "###Pure\n" +
                "Enum test::Gender\n" +
                "{\n" +
                "  MALE, FEMALE, OTHER\n" +
                "}\n" +
                "Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  gender: test::Gender[1];\n" +
                "}\n";
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::Person"));

        String selfMapping =  "###Mapping\n" +
                    "Mapping test::SelfMapping\n" +
                    "(\n" +
                    "   test::Person: Pure\n" +
                    "   {\n" +
                    "      ~src test::Person\n" +
                    "   }\n" +
                    ")\n";

        String grammar = selfMapping + urlStreamRuntime("test::SelfMapping", "test::gen::TestBinding");
        String personTree = "#{test::Person {name,gender}}#";
        String result = runTest(generated,
                                grammar,
                                "|test::Person.all()->graphFetchChecked(" + personTree + ")->serialize(" + personTree + ")",
                                "test::SelfMapping",
                                "test::runtime",
                                "name,gender\nJohn Doe,Male");

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithEnumResult.json")));
    }

    @Test
    public void testDeserializeCsvWithEnumBadValue()
    {
        String modelGrammar = "###Pure\n" +
                "Enum test::Gender\n" +
                "{\n" +
                "  MALE, FEMALE, OTHER\n" +
                "}\n" +
                "Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  gender: test::Gender[1];\n" +
                "}\n";
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, toFlatDataConfig("test::Person"));

        String selfMapping =  "###Mapping\n" +
                "Mapping test::SelfMapping\n" +
                "(\n" +
                "   test::Person: Pure\n" +
                "   {\n" +
                "      ~src test::Person\n" +
                "   }\n" +
                ")\n";

        String grammar = selfMapping + urlStreamRuntime("test::SelfMapping", "test::gen::TestBinding");
        String personTree = "#{test::Person {name,gender}}#";
        String result = runTest(generated,
                                grammar,
                                "|test::Person.all()->graphFetchChecked(" + personTree + ")->serialize(" + personTree + ")",
                                "test::SelfMapping",
                                "test::runtime",
                                "name,gender\nJohn Doe,Neuter");

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithEnumBadValueResult.json")));
    }

    @Test
    public void testDeserializeCsvAndReserializeWithGeneratedModel()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("test::tradeSchema"));

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData ="Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                                grammar,
                                "|test::gen::TradeRecord.all()->graphFetchChecked("+tradeTree+")->externalize(test::gen::TestBinding)",
                                "test::trade::SelfMapping",
                                "test::runtime",
                                tradeData);

        Assert.assertEquals(tradeData, result);
    }

    @Test
    public void testDeserializeCsvWithGeneratedModelCheckedForMissingData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("test::tradeSchema"));

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData ="Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                ",10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                                grammar,
                                "|test::gen::TradeRecord.all()->graphFetchChecked("+tradeTree+")->serialize("+tradeTree+")",
                                "test::trade::SelfMapping",
                                "test::runtime",
                                tradeData);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithGeneratedModelCheckedForMissingDataResult.json")));
    }

    @Test
    public void testDeserializeCsvWithGeneratedModelCheckedForBadData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("test::tradeSchema"));

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData ="Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,XX,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                                grammar,
                                "|test::gen::TradeRecord.all()->graphFetchChecked("+tradeTree+")->serialize("+tradeTree+")",
                                "test::trade::SelfMapping",
                                "test::runtime",
                                tradeData);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithGeneratedModelCheckedForBadDataResult.json")));
    }

    @Test
    public void testDeserializeAndMapMultiSectionCsv()
    {
        String model = "###Pure\n" +
                "Class test::LoanPrice\n" +
                "{\n" +
                "  accountNo: Integer[0..1];\n" +
                "  productId: String[0..1];\n" +
                "  productIdType: String[0..1];\n" +
                "  eventDate: StrictDate[0..1];\n" +
                "  currency: String[0..1];\n" +
                "  closePrice: Float[0..1];\n" +
                "  askPrice: Float[0..1];\n" +
                "  bidPrice: Float[0..1];\n" +
                "}\n";

        String schemaCode = newExternalSchemaSetGrammarBuilder("test::WholeLoanPriceFileSchema", "FlatData")
                .withSchemaText("section header: DelimitedWithoutHeadings\n" +
                                        "{\n" +
                                        "  delimiter: ' ';\n" +
                                        "  scope.forNumberOfLines: 1;\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    closeOfBusiness {3}: DATE(format='yyyyMMdd');\n" +
                                        "  }\n" +
                                        "}\n" +
                                        "\n" +
                                        "section prices: DelimitedWithoutHeadings\n" +
                                        "{\n" +
                                        "  scope.untilEof;\n" +
                                        "  delimiter: '~';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Account_ID   {1}: INTEGER;\n" +
                                        "    Synonym_Type {2}: STRING;\n" +
                                        "    Synonym      {3}: STRING;\n" +
                                        "    Currency     {4}: STRING;\n" +
                                        "    Close_Price  {9}: DECIMAL;\n" +
                                        "  }\n" +
                                        "}\n")
                .build();

        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("test::WholeLoanPriceFileSchema", "PriceFile"));

        String mapping = "###Mapping\n" +
                "Mapping test::PriceRowToLoanPrice\n" +
                "(\n" +
                "  *test::LoanPrice: Pure\n" +
                "  {\n" +
                "    ~src test::gen::PricesRecord\n" +
                "    accountNo: $src.accountId,\n" +
                "    productId: $src.synonym,\n" +
                "    productIdType: $src.synonymType,\n" +
                "    eventDate: $src.priceFile.header.closeOfBusiness,\n" +
                "    currency: $src.currency,\n" +
                "    closePrice: $src.closePrice\n" +
                "  }\n" +
                ")\n";

        String grammar = model + schemaCode + mapping + urlStreamRuntime("test::PriceRowToLoanPrice", "test::gen::TestBinding");
        String tree = "#{test::LoanPrice{accountNo,productIdType,productId,eventDate,currency,closePrice}}#";

        String result = runTest(generated,
                                grammar,
                                "|test::LoanPrice.all()->graphFetchChecked("+tree+")->serialize("+tree+")",
                                "test::PriceRowToLoanPrice",
                                "test::runtime",
                                resourceAsString("queries/prices.csv"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeAndMapMultiSectionCsvResult.json")));
    }

    private String tradeSelfMapping()
    {
        return "###Mapping\n" +
                "\n" +
                "Mapping test::trade::SelfMapping\n" +
                "(\n" +
                "   test::gen::TradeRecord: Pure\n" +
                "   {\n" +
                "      ~src test::gen::TradeRecord\n" +
                "   }\n" +
                ")\n";
    }

    private String tradeSchema()
    {
        return newExternalSchemaSetGrammarBuilder("test::tradeSchema", "FlatData")
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
        return fromFlatDataConfig(sourceSchemaSet, null);
    }

    private FlatDataToModelConfiguration fromFlatDataConfig(String sourceSchemaSet, String schemaClassName)
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
        config.sourceSchemaSet = sourceSchemaSet;
        config.targetBinding = "test::gen::TestBinding";
        config.targetPackage = "test::gen";
        config.purifyNames = true;
        config.schemaClassName = schemaClassName;
        config.format = "FlatData";
        return config;
    }

    private String resourceAsString(String path)
    {
        byte[] bytes;
        try
        {
            bytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(path), "Failed to get resource " + path).toURI()));
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        String string = new String(bytes, StandardCharsets.UTF_8);
        return string.replaceAll("\\R", "\n");
    }
}
