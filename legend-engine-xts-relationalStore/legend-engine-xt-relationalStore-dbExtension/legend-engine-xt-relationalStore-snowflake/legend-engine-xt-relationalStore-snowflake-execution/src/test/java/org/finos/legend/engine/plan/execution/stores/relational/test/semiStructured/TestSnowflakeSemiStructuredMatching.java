// Copyright 2023 Goldman Sachs
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

public class TestSnowflakeSemiStructuredMatching extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "match::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "match::runtime::SnowflakeRuntime";

    @Test
    public void testSemiStructuredMatchComplexProperty()
    {
        String queryFunction = "match::semiStructuredMatchComplexProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('BillingAddress') then \"root\".CUSTOMER['customerAddress']['billAddress']::varchar when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('ShippingAddress') then \"root\".CUSTOMER['customerAddress']['shipAddress']::varchar else 'Default Address' end as \"Customer Address\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }


    @Test
    public void testSemiStructuredMatchWithMultipleProject()
    {
        String queryFunction = "match::semiStructuredMatchWithMultipleProject__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n" +
                        "      resultColumns = [(\"Customer Address\", \"\"), (\"Order Price\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('BillingAddress') then \"root\".CUSTOMER['customerAddress']['billAddress']::varchar when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('ShippingAddress') then \"root\".CUSTOMER['customerAddress']['shipAddress']::varchar else null end as \"Customer Address\", case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CashOnDeliveryPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountToBePaid']::number when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountPaid']::number else null end as \"Order Price\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredMatchWithComplexFilter()
    {
        String queryFunction = "match::semiStructuredMatchWithComplexFilter__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n" +
                        "      resultColumns = [(\"Customer Address\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('BillingAddress') then \"root\".CUSTOMER['customerAddress']['billAddress']::varchar when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('ShippingAddress') then \"root\".CUSTOMER['customerAddress']['shipAddress']::varchar else null end as \"Customer Address\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CashOnDeliveryPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountToBePaid']::number when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountPaid']::number else null end < 200\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredMatchWithVariableAccess()
    {
        String queryFunction = "match::semiStructuredMatchWithVariableAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Sequence\n" +
                "(\n" +
                "  type = TDS[(Max Amount Flag, Boolean, BIT, \"\")]\n" +
                "  (\n" +
                "    Allocation\n" +
                "    (\n" +
                "      type = Integer\n" +
                "      resultSizeRange = 1\n" +
                "      name = maxAmount\n" +
                "      value = \n" +
                "        (\n" +
                "          Constant\n" +
                "          (\n" +
                "            type = Integer\n" +
                "            resultSizeRange = 1\n" +
                "            values=[200]\n" +
                "          )\n" +
                "        )\n" +
                "    )\n" +
                "    RelationalBlockExecutionNode\n" +
                "    (\n" +
                "      type = TDS[(Max Amount Flag, Boolean, BIT, \"\")]\n" +
                "      (\n" +
                "        SQL\n" +
                "        (\n" +
                "          type = Void\n" +
                "          resultColumns = []\n" +
                "          sql = ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';\n" +
                "          connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "        )\n" +
                "        Relational\n" +
                "        (\n" +
                "          type = TDS[(Max Amount Flag, Boolean, BIT, \"\")]\n" +
                "          resultColumns = [(\"Max Amount Flag\", \"\")]\n" +
                "          sql = select case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CashOnDeliveryPayment') then case when \"root\".CUSTOMER['transactionDetails']['payment']['amountToBePaid']::number < ${maxAmount} then true else false end when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then case when \"root\".CUSTOMER['transactionDetails']['payment']['amountPaid']::number < ${maxAmount} then true else false end else null end as \"Max Amount Flag\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                "          connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "        )\n" +
                "      ) \n" +
                "      finallyExecutionNodes = \n" +
                "      (\n" +
                "        SQL\n" +
                "        (\n" +
                "          type = Void\n" +
                "          resultColumns = []\n" +
                "          sql = ALTER SESSION UNSET QUERY_TAG;\n" +
                "          connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  )\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }


    @Test
    public void testSemiStructuredMatchMultilevel()
    {
        String queryFunction = "match::semiStructuredMatchMultilevel__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Amount, Integer, BIGINT, \"\")]\n" +
                        "      resultColumns = [(\"Amount\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('WalletPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['walletTransactionAmount']::number when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CardPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['cardTransactionAmount']::number when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountPaid']::number else null end when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CashOnDeliveryPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountToBePaid']::number else null end as \"Amount\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Amount, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredMatchWithMultipleProjectUsingCol()
    {
        String queryFunction = "match::semiStructuredMatchWithMultipleProjectUsingCol__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n" +
                        "      resultColumns = [(\"Customer Address\", \"\"), (\"Order Price\", \"\")]\n" +
                        "      sql = select case when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('BillingAddress') then \"root\".CUSTOMER['customerAddress']['billAddress']::varchar when \"root\".CUSTOMER['customerAddress']['@type']::varchar in ('ShippingAddress') then \"root\".CUSTOMER['customerAddress']['shipAddress']::varchar else null end as \"Customer Address\", case when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('CashOnDeliveryPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountToBePaid']::number when \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then \"root\".CUSTOMER['transactionDetails']['payment']['amountPaid']::number else null end as \"Order Price\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredMatching.pure";
    }
}
