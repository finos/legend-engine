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

public class TestSemiStructuredMatching extends AbstractTestSemiStructured
{
    private static final String memSQLMapping = "match::mapping::MemSQLMapping";
    private static final String memSQLRuntime = "match::runtime::MemSQLRuntime";

    private static final String h2Mapping = "match::mapping::H2Mapping";
    private static final String h2Runtime = "match::runtime::H2Runtime";

    @Test
    public void testSemiStructuredMatchComplexProperty()
    {
        String queryFunction = "match::semiStructuredMatchComplexProperty__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n" +
                        "  resultColumns = [(\"Customer Address\", \"\")]\n" +
                        "  sql = select case when `root`.CUSTOMER::customerAddress::$@type in ('BillingAddress') then `root`.CUSTOMER::customerAddress::$billAddress when `root`.CUSTOMER::customerAddress::$@type in ('ShippingAddress') then `root`.CUSTOMER::customerAddress::$shipAddress else 'Default Address' end as `Customer Address` from ORDER_SCHEMA.ORDER_TABLE as `root`\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("B1\n" +
                "B2\n" +
                "B3\n" +
                "Default Address\n" +
                "S1\n" +
                "S2\n" +
                "S3\n" +
                "S4\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredMatchWithMultipleProject()
    {
        String queryFunction = "match::semiStructuredMatchWithMultipleProject__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n" +
                        "  resultColumns = [(\"Customer Address\", \"\"), (\"Order Price\", \"\")]\n" +
                        "  sql = select case when `root`.CUSTOMER::customerAddress::$@type in ('BillingAddress') then `root`.CUSTOMER::customerAddress::$billAddress when `root`.CUSTOMER::customerAddress::$@type in ('ShippingAddress') then `root`.CUSTOMER::customerAddress::$shipAddress else null end as `Customer Address`, case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CashOnDeliveryPayment') then `root`.CUSTOMER::transactionDetails::payment::amountToBePaid !:> bigint when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::amountPaid !:> bigint else null end as `Order Price` from ORDER_SCHEMA.ORDER_TABLE as `root`\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("B1,200\n" +
                "B2,180\n" +
                "B3,290\n" +
                ",150\n" +
                "S1,185\n" +
                "S2,120\n" +
                "S3,180\n" +
                "S4,160\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredMatchWithComplexFilter()
    {
        String queryFunction = "match::semiStructuredMatchWithComplexFilter__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\")]\n" +
                        "  resultColumns = [(\"Customer Address\", \"\")]\n" +
                        "  sql = select case when `root`.CUSTOMER::customerAddress::$@type in ('BillingAddress') then `root`.CUSTOMER::customerAddress::$billAddress when `root`.CUSTOMER::customerAddress::$@type in ('ShippingAddress') then `root`.CUSTOMER::customerAddress::$shipAddress else null end as `Customer Address` from ORDER_SCHEMA.ORDER_TABLE as `root` where case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CashOnDeliveryPayment') then `root`.CUSTOMER::transactionDetails::payment::amountToBePaid !:> bigint when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::amountPaid !:> bigint else null end < 200\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("B2\n" +
                "\n" +
                "S1\n" +
                "S2\n" +
                "S3\n" +
                "S4\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredMatchWithVariableAccess()
    {
        String queryFunction = "match::semiStructuredMatchWithVariableAccess__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Sequence\n" +
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
                        "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Max Amount Flag, Boolean, BIT, \"\")]\n" +
                        "      resultColumns = [(\"Max Amount Flag\", \"\")]\n" +
                        "      sql = select case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CashOnDeliveryPayment') then case when `root`.CUSTOMER::transactionDetails::payment::amountToBePaid !:> bigint < ${maxAmount} then 'true' else 'false' end when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then case when `root`.CUSTOMER::transactionDetails::payment::amountPaid !:> bigint < ${maxAmount} then 'true' else 'false' end else null end as `Max Amount Flag` from ORDER_SCHEMA.ORDER_TABLE as `root`\n" +
                        "      connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        "    )\n" +
                        "  )\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("false\n" +
                "true\n" +
                "false\n" +
                "true\n" +
                "true\n" +
                "true\n" +
                "true\n" +
                "true\n", h2Result.replace("\r\n", "\n"));

    }


    @Test
    public void testSemiStructuredMatchMultilevel()
    {
        String queryFunction = "match::semiStructuredMatchMultilevel__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(Amount, Integer, BIGINT, \"\")]\n" +
                        "  resultColumns = [(\"Amount\", \"\")]\n" +
                        "  sql = select case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('WalletPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::walletTransactionAmount !:> bigint when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CardPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::cardTransactionAmount !:> bigint when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::amountPaid !:> bigint else null end when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CashOnDeliveryPayment') then `root`.CUSTOMER::transactionDetails::payment::amountToBePaid !:> bigint else null end as `Amount` from ORDER_SCHEMA.ORDER_TABLE as `root`\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("200\n" +
                "180\n" +
                "290\n" +
                "150\n" +
                "185\n" +
                "120\n" +
                "200\n" +
                "190\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredMatchWithMultipleProjectUsingCol()
    {
        String queryFunction = "match::semiStructuredMatchWithMultipleProjectUsingCol__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(Customer Address, String, VARCHAR(8192), \"\"), (Order Price, Integer, BIGINT, \"\")]\n" +
                        "  resultColumns = [(\"Customer Address\", \"\"), (\"Order Price\", \"\")]\n" +
                        "  sql = select case when `root`.CUSTOMER::customerAddress::$@type in ('BillingAddress') then `root`.CUSTOMER::customerAddress::$billAddress when `root`.CUSTOMER::customerAddress::$@type in ('ShippingAddress') then `root`.CUSTOMER::customerAddress::$shipAddress else null end as `Customer Address`, case when `root`.CUSTOMER::transactionDetails::payment::$@type in ('CashOnDeliveryPayment') then `root`.CUSTOMER::transactionDetails::payment::amountToBePaid !:> bigint when `root`.CUSTOMER::transactionDetails::payment::$@type in ('PrepaidPayment', 'WalletPrepaidPayment', 'CardPrepaidPayment') then `root`.CUSTOMER::transactionDetails::payment::amountPaid !:> bigint else null end as `Order Price` from ORDER_SCHEMA.ORDER_TABLE as `root`\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("B1,200\n" +
                "B2,180\n" +
                "B3,290\n" +
                ",150\n" +
                "S1,185\n" +
                "S2,120\n" +
                "S3,180\n" +
                "S4,160\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredMatching.pure";
    }
}
