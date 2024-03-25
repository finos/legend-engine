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

public class TestSemiStructuredTypeFunctions extends AbstractTestSemiStructured
{
    private static final String h2Mapping = "typeFunctions::mapping::H2Mapping";
    private static final String h2Runtime = "typeFunctions::runtime::H2Runtime";

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithFunctions__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "Address,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,WalletPrepaidPayment\n" +
                "ShippingAddress,CardPrepaidPayment\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithColSpecs()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithColSpecs__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "Address,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,WalletPrepaidPayment\n" +
                "ShippingAddress,CardPrepaidPayment\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInFilter__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,BillingAddress\n" +
                "2,BillingAddress\n" +
                "3,BillingAddress\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenFunction__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,ElectronicsProduct\n" +
                "1,SportsProduct\n" +
                "2,SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenColSpec()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenColSpec__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,ElectronicsProduct\n" +
                "1,SportsProduct\n" +
                "2,SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,ElectronicsProduct,SportsProduct\n" +
                "2,SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredTypeFunctions.pure";
    }
}
