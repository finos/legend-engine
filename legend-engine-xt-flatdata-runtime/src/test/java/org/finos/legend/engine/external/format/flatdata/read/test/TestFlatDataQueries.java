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
import org.finos.legend.engine.external.format.flatdata.transformation.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.transformation.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest.newExternalSchemaSetGrammarBuilder;

public class TestFlatDataQueries extends TestExternalFormatQueries
{
    List<Root_meta_pure_extension_Extension> formatExtensions = Collections.singletonList(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(Compiler.compile(PureModelContextData.newPureModelContextData(), null, null).getExecutionSupport()));

    @Test
    public void testDeserializeCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/peopleWithExactHeadings.csv"),
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testDeserializeCsvBadHeadings()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/people.csv"),
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleBadHeadingsResult.json")));
    }

    @Test
    public void testDeserializeAndReserializeCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding)",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/peopleWithExactHeadings.csv"),
                formatExtensions);

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), result);
    }

    @Test
    public void testDeserializeAndReserializeUncheckedCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = firmSelfMapping() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::gen::TestBinding");
        String result = runTest(generated,
                grammar,
                "|test::firm::model::Person.all()->graphFetch(" + personTree() + ")->externalize(test::gen::TestBinding)",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/peopleWithExactHeadings.csv"),
                formatExtensions);

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
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::Person");

        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String selfMapping = "###Mapping\n" +
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
                "name,gender\nJohn Doe,Male",
                formatExtensions);

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
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String selfMapping = "###Mapping\n" +
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
                "name,gender\nJohn Doe,Neuter",
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithEnumBadValueResult.json")));
    }

    @Test
    public void testDeserializeCsvAndReserializeWithGeneratedModel()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                grammar,
                "|test::gen::TradeRecord.all()->graphFetchChecked(" + tradeTree + ")->externalize(test::gen::TestBinding)",
                "test::trade::SelfMapping",
                "test::runtime",
                tradeData,
                formatExtensions);

        Assert.assertEquals(tradeData, result);
    }

    @Test
    public void testDeserializeCsvWithGeneratedModelCheckedForMissingData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                ",10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                grammar,
                "|test::gen::TradeRecord.all()->graphFetchChecked(" + tradeTree + ")->serialize(" + tradeTree + ")",
                "test::trade::SelfMapping",
                "test::runtime",
                tradeData,
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeCsvWithGeneratedModelCheckedForMissingDataResult.json")));
    }

    @Test
    public void testDeserializeCsvWithGeneratedModelCheckedForBadData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");

        String grammar = schemaCode + tradeSelfMapping() + urlStreamRuntime("test::trade::SelfMapping", "test::gen::TestBinding");
        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,XX,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated,
                grammar,
                "|test::gen::TradeRecord.all()->graphFetchChecked(" + tradeTree + ")->serialize(" + tradeTree + ")",
                "test::trade::SelfMapping",
                "test::runtime",
                tradeData,
                formatExtensions);

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

        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("PriceFile"), true, "test::gen::TestBinding");

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
                "|test::LoanPrice.all()->graphFetchChecked(" + tree + ")->serialize(" + tree + ")",
                "test::PriceRowToLoanPrice",
                "test::runtime",
                resourceAsString("queries/prices.csv"),
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeAndMapMultiSectionCsvResult.json")));
    }

    @Test
    public void testDeserializeAndMapMultiSectionWithImmaterialFooterCsv()
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
                        "  scope.default;\n" +
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
                        "}\n" +
                        "section footer : ImmaterialLines\n" +
                        "{\n" +
                        "  scope.forNumberOfLines: 1;\n" +
                        "}\n "
                )
                .build();

        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("PriceFile"), true, "test::gen::TestBinding");

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
                "|test::LoanPrice.all()->graphFetchChecked(" + tree + ")->serialize(" + tree + ")",
                "test::PriceRowToLoanPrice",
                "test::runtime",
                resourceAsString("queries/prices_with_footer.csv"),
                formatExtensions);

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

    private ModelToFlatDataConfiguration toFlatDataConfig()
    {
        ModelToFlatDataConfiguration config = new ModelToFlatDataConfiguration();
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.format = "FlatData";
        return config;
    }

    private FlatDataToModelConfiguration fromFlatDataConfig()
    {
        return fromFlatDataConfig(null);
    }

    private FlatDataToModelConfiguration fromFlatDataConfig(String schemaClassName)
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
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
