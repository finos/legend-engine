// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.junit.Assert;
import org.junit.Test;

public class TestSnowflakeSemiStructuredTypeFunctions extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "typeFunctions::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "typeFunctions::runtime::SnowflakeRuntime";

    private static final String snowflakeMappingWithLegendJsonSchema = "typeFunctions::mapping::SnowflakeMappingWithLegendJsonSchema";
    private static final String snowflakeMappingWithMinimalLegendJsonSchema = "typeFunctions::mapping::SnowflakeMappingWithMinimalLegendJsonSchema";

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithFunctions()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithFunctions__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\", \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithFunctions__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                "      sql = select \"root\".CUSTOMER['customerAddress']['_type']::varchar as \"Customer Address Type\", \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";

        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithColSpecs()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithColSpecs__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\", \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInFilter()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInFilter__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CashOnDeliveryPayment'\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInFilter__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['customerAddress']['_type']::varchar as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'CashOnDeliveryPayment'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenFunction()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenFunction__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"ss_flatten_0\".VALUE['@type']::varchar as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenFunction__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", \"ss_flatten_0\".VALUE['_type']::varchar as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenColSpec()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenColSpec__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"ss_flatten_0\".VALUE['@type']::varchar as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterArrayElementAccess__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['products'][0]['@type']::varchar as \"Product 0 Type\", \"root\".CUSTOMER['products'][1]['@type']::varchar as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['products'][0]['_type']::varchar as \"Product 0 Type\", \"root\".CUSTOMER['products'][1]['_type']::varchar as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInProjectWithFunctions()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithFunctions__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'CustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\", case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'WalletPrepaidPayment' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithFunctions__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'CustomCustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\", case when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'Wallet' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";

        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected2 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                "      sql = select case when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::CustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\", case when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::WalletPrepaidPayment' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected2), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInProjectWithColSpecs()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithColSpecs__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'CustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\", case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'WalletPrepaidPayment' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, VARCHAR(65536), \"\"), (Payment Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInFilter()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageInFilter__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'CustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['@type']::varchar = 'ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'WalletPrepaidPayment' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end = 'CashOnDeliveryPayment'\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInFilter__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'CustomCustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where case when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'Wallet' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end = 'CashOnDeliveryPayment'\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected2 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::CustomerAddress' then 'typeFunctions::model::CustomerAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::BillingAddress' then 'typeFunctions::model::BillingAddress' when \"root\".CUSTOMER['customerAddress']['_type']::varchar = 'typeFunctions::model::ShippingAddress' then 'typeFunctions::model::ShippingAddress' else null end as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where case when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::Payment' then 'typeFunctions::model::Payment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::CashOnDeliveryPayment' then 'typeFunctions::model::CashOnDeliveryPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::PrepaidPayment' then 'typeFunctions::model::PrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::WalletPrepaidPayment' then 'typeFunctions::model::WalletPrepaidPayment' when \"root\".CUSTOMER['transactionDetails']['payment']['_type']::varchar = 'typeFunctions::model::CardPrepaidPayment' then 'typeFunctions::model::CardPrepaidPayment' else null end = 'CashOnDeliveryPayment'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected2), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterFlattenFunction()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenFunction__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"ss_flatten_0\".VALUE['@type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"ss_flatten_0\".VALUE['@type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"ss_flatten_0\".VALUE['@type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenFunction__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected1 =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"ss_flatten_0\".VALUE['_type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"ss_flatten_0\".VALUE['_type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"ss_flatten_0\".VALUE['_type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected1), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected2 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", case when \"ss_flatten_0\".VALUE['_type']::varchar = 'typeFunctions::model::Product' then 'typeFunctions::model::Product' when \"ss_flatten_0\".VALUE['_type']::varchar = 'typeFunctions::model::ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"ss_flatten_0\".VALUE['_type']::varchar = 'typeFunctions::model::SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected2), snowflakePlan2);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterFlattenColSpec()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenColSpec__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"ss_flatten_0\".VALUE['@type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"ss_flatten_0\".VALUE['@type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"ss_flatten_0\".VALUE['@type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterArrayElementAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypePathFunctionUsageAfterArrayElementAccess__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['products'][0]['@type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][0]['@type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][0]['@type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 0 Type\", case when \"root\".CUSTOMER['products'][1]['@type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][1]['@type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][1]['@type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";

        String snowflakePlan1 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 0 Type\", case when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";

        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan1);

        String snowflakePlan2 = this.buildExecutionPlanString(queryFunction, snowflakeMappingWithMinimalLegendJsonSchema, snowflakeRuntime);
        String snowflakeExpected2 =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, VARCHAR(65536), \"\"), (Product 1 Type, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                "      sql = select \"root\".ORDERID as \"Order Id\", case when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'typeFunctions::model::Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'typeFunctions::model::ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][0]['_type']::varchar = 'typeFunctions::model::SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 0 Type\", case when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'typeFunctions::model::Product' then 'typeFunctions::model::Product' when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'typeFunctions::model::ElectronicsProduct' then 'typeFunctions::model::ElectronicsProduct' when \"root\".CUSTOMER['products'][1]['_type']::varchar = 'typeFunctions::model::SportsProduct' then 'typeFunctions::model::SportsProduct' else null end as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected2), snowflakePlan2);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredTypeFunctions.pure";
    }
}
