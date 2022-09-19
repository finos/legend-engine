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
import org.finos.legend.engine.external.format.flatdata.transformation.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.transformation.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.shared.core.url.InputStreamProvider;
import org.finos.legend.engine.shared.core.url.NamedInputStream;
import org.finos.legend.engine.shared.core.url.NamedInputStreamProvider;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest.newExternalSchemaSetGrammarBuilder;

public class TestFlatDataQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        formatExtensions = Collections.singletonList(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(Compiler.compile(PureModelContextData.newPureModelContextData(), null, null).getExecutionSupport()));
    }

    @Test
    public void testInternalizeWithDynamicByteStream()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->checked()->serialize(" + personTree() + ")",
                Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithDynamicString()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:String[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->checked()->serialize(" + personTree() + ")",
                Maps.mutable.with("data", resourceAsString("queries/peopleWithExactHeadings.csv")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithStaticString()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String data = resourceAsString("queries/peopleWithExactHeadings.csv").replace("\n", "\\n").replace("'", "\\'");
        String result = runTest(generated,
                "|test::firm::model::Person->internalize(test::gen::TestBinding, '" + data + "')->checked()->serialize(" + personTree() + ")"
        );

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithDynamicUrl()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "url:String[1]|test::firm::model::Person->internalize(test::gen::TestBinding, ^Url(url=$url))->checked()->serialize(" + personTree() + ")",
                Maps.mutable.with("url", "executor:myUrl"),
                new NamedInputStreamProvider(Collections.singletonList(new NamedInputStream("myUrl", resource("queries/peopleWithExactHeadings.csv")))));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithStaticUrl()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "|test::firm::model::Person->internalize(test::gen::TestBinding, ^Url(url='executor:default'))->checked()->serialize(" + personTree() + ")",
                new InputStreamProvider(resource("queries/peopleWithExactHeadings.csv")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetch()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetch(" + personTree() + ")->serialize(" + personTree() + ")",
                Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleGraphFetchResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        try
        {
            runTest(generated,
                    "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetch(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testInternalizeWithGraphFetchChecked()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude
        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpanded()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetchUnexpanded does not expand tree scope to include constraint on latitude
        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchUnexpandedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpandedChecked()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetchUnexpanded does not expand tree scope to include constraint on latitude
        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchCheckedUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedUnexpandedResult.json")));
    }

    @Test
    public void testInternalizeWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithBadHeadings()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree() + ")->serialize(" + personTree() + ")",
                Maps.mutable.with("data", resource("queries/peopleWithBadHeadings.csv")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/peopleBadHeadingsResult.json")));
    }

    @Test
    public void testInternalizeWithEnum()
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

        String personTree = "#{test::Person {name,gender}}#";
        String result = runTest(generated,
                "data:ByteStream[1]|test::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree + ")->serialize(" + personTree + ")",
                Maps.mutable.with("data", new ByteArrayInputStream("name,gender\nJohn Doe,Male".getBytes(StandardCharsets.UTF_8))));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/internalizeWithEnumResult.json")));
    }

    @Test
    public void testInternalizeWithEnumBadValue()
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

        String personTree = "#{test::Person {name,gender}}#";
        String result = runTest(generated,
                "data:ByteStream[1]|test::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree + ")->serialize(" + personTree + ")",
                Maps.mutable.with("data", new ByteArrayInputStream("name,gender\nJohn Doe,Neuter".getBytes(StandardCharsets.UTF_8))));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/internalizeWithEnumBadValueResult.json")));
    }

    @Test
    public void testInternalizeWithGeneratedModelCheckedForMissingData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");
        PureModelContextData schemaData = PureGrammarParser.newInstance().parseModel(schemaCode);

        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                ",10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated.combine(schemaData),
                "data:ByteStream[1]|test::gen::TradeRecord->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + tradeTree + ")->serialize(" + tradeTree + ")",
                Maps.mutable.with("data", new ByteArrayInputStream(tradeData.getBytes(StandardCharsets.UTF_8))));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/internalizeWithGeneratedModelCheckedForMissingDataResult.json")));
    }

    @Test
    public void testInternalizeWithGeneratedModelCheckedForBadData()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");
        PureModelContextData schemaData = PureGrammarParser.newInstance().parseModel(schemaCode);

        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,XX,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated.combine(schemaData),
                "data:ByteStream[1]|test::gen::TradeRecord->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + tradeTree + ")->serialize(" + tradeTree + ")",
                Maps.mutable.with("data", new ByteArrayInputStream(tradeData.getBytes(StandardCharsets.UTF_8))));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/internalizeWithGeneratedModelCheckedForBadDataResult.json")));
    }

    @Test
    public void testInternalizeAndExternalizeWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding)",
                Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), result);
    }

    @Test
    public void testInternalizeAndExternalizeUncheckedCsvWithGeneratedSchema()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String result = runTest(generated,
                "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetch(" + personTree() + ")->externalize(test::gen::TestBinding)",
                Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), result);
    }

    @Test
    public void testInternalizeAndExternalizeWithGeneratedModel()
    {
        String schemaCode = tradeSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig(), true, "test::gen::TestBinding");
        PureModelContextData schemaData = PureGrammarParser.newInstance().parseModel(schemaCode);

        String tradeTree = "#{test::gen::TradeRecord {product,quantity,tradeTime,price,priceCcy,settlementCcy,settlementRate,settlementDate,confirmedAt,expiryDate,executions}}#";

        String tradeData = "Product,Quantity,Trade Time,Price,Price Ccy,Settlement Ccy,Settlement Rate,Settlement Date,Confirmed At,Expiry Date,Executions\n" +
                "P1,10,2021-06-04T15:04:21.232Z,12.32,USD,EUR,2.4,2021-06-09,2021-06-04T15:12:31.000Z,2022-06-04,5\n" +
                "P2,20,2021-06-04T15:04:21.999Z,34.7,EUR,,,2021-06-09,,,";

        String result = runTest(generated.combine(schemaData),
                "data:ByteStream[1]|test::gen::TradeRecord->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + tradeTree + ")->externalize(test::gen::TestBinding)",
                Maps.mutable.with("data", new ByteArrayInputStream(tradeData.getBytes(StandardCharsets.UTF_8))));

        Assert.assertEquals(tradeData, result);
    }

    @Test
    public void testInternalizeWithMultiSectionFlatData()
    {
        String schemaCode = multiSectionSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, fromFlatDataConfig("PriceFile"), true, "test::gen::TestBinding");
        PureModelContextData schemaData = PureGrammarParser.newInstance().parseModel(schemaCode);

        String tree = "#{test::gen::PricesRecord{accountId,synonym,synonymType,currency,closePrice,priceFile{header{closeOfBusiness}}}}#";

        String result = runTest(generated.combine(schemaData),
                "data:ByteStream[1]|test::gen::PricesRecord->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + tree + ")->serialize(" + tree + ")",
                Maps.mutable.with("data", resource("queries/prices.csv")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/internalizeWithMultiSectionFlatData.json")));
    }

    // TODO: update this to use internalize and multi expression query
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
                resource("queries/prices.csv"),
                Collections.emptyMap(),
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeAndMapMultiSectionCsvResult.json")));
    }

    // TODO: update this to use internalize and multi expression query
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
                resource("queries/prices_with_footer.csv"),
                Collections.emptyMap(),
                formatExtensions);

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/deserializeAndMapMultiSectionCsvResult.json")));
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

    private String multiSectionSchema()
    {
        return newExternalSchemaSetGrammarBuilder("test::WholeLoanPriceFileSchema", "FlatData")
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
}
