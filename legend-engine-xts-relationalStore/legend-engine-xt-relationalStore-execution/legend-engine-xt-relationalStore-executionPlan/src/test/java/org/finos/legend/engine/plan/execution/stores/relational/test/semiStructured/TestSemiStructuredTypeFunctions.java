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

    private static final String h2MappingWithMinimalLegendJsonSchema = "typeFunctions::mapping::H2MappingWithMinimalLegendJsonSchema";
    private static final String h2RuntimeWithMinimalLegendJsonSchema = "typeFunctions::runtime::H2RuntimeWithMinimalLegendJsonSchema";

    private static final String h2MappingWithLegendJsonSchema = "typeFunctions::mapping::H2MappingWithLegendJsonSchema";
    private static final String h2RuntimeWithLegendJsonSchema = "typeFunctions::runtime::H2RuntimeWithLegendJsonSchema";

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
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithFunctions__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "BillingAddress,CashOnDeliveryPayment\n" +
                "Address,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,PrepaidPayment\n" +
                "ShippingAddress,WalletPrepaidPayment\n" +
                "ShippingAddress,CardPrepaidPayment\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "sendBillTo,typeFunctions::model::CashOnDeliveryPayment\n" +
                "sendBillTo,typeFunctions::model::CashOnDeliveryPayment\n" +
                "sendBillTo,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::Address,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,Wallet\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::CardPrepaidPayment\n";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
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
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageInFilter__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,BillingAddress\n" +
                "2,BillingAddress\n" +
                "3,BillingAddress\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
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
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenFunction__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,ElectronicsProduct\n" +
                "1,SportsProduct\n" +
                "2,SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "1,typeFunctions::model::ElectronicsProduct\n" +
                "1,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
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

    @Test
    public void testLegendJsonSchemaSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypeNameFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,ElectronicsProduct,SportsProduct\n" +
                "2,SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "1,typeFunctions::model::ElectronicsProduct,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithFunctions__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                ",typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::WalletPrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::CardPrepaidPayment\n", h2Result.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageInProjectWithFunctions()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithFunctions__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                ",typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::WalletPrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::CardPrepaidPayment\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        Assert.assertEquals(expectedResult1, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInProjectWithColSpecs()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInProjectWithColSpecs__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                "typeFunctions::model::BillingAddress,typeFunctions::model::CashOnDeliveryPayment\n" +
                ",typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::PrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::WalletPrepaidPayment\n" +
                "typeFunctions::model::ShippingAddress,typeFunctions::model::CardPrepaidPayment\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInFilter__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,typeFunctions::model::BillingAddress\n" +
                "2,typeFunctions::model::BillingAddress\n" +
                "3,typeFunctions::model::BillingAddress\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageInFilter()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageInFilter__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,typeFunctions::model::BillingAddress\n" +
                "2,typeFunctions::model::BillingAddress\n" +
                "3,typeFunctions::model::BillingAddress\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        Assert.assertEquals(expectedResult1, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenFunction__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,typeFunctions::model::ElectronicsProduct\n" +
                "1,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageAfterFlattenFunction()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenFunction__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,typeFunctions::model::ElectronicsProduct\n" +
                "1,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "1,typeFunctions::model::ElectronicsProduct\n" +
                "1,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterFlattenColSpec()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterFlattenColSpec__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,typeFunctions::model::ElectronicsProduct\n" +
                "1,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct\n" +
                "3,\n" +
                "4,\n" +
                "5,\n" +
                "6,\n" +
                "7,\n" +
                "8,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredTypePathFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,typeFunctions::model::ElectronicsProduct,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testLegendJsonSchemaSemiStructuredTypePathFunctionUsageAfterArrayElementAccess()
    {
        String queryFunction = "typeFunctions::semiStructuredTypePathFunctionUsageAfterArrayElementAccess__TabularDataSet_1_";
        String h2Result1 = this.executeFunction(queryFunction, h2MappingWithMinimalLegendJsonSchema, h2RuntimeWithMinimalLegendJsonSchema);
        String expectedResult1 = "1,typeFunctions::model::ElectronicsProduct,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n";

        Assert.assertEquals(expectedResult1, h2Result1.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithMinimalLegendJsonSchema));

        String h2Result2 = this.executeFunction(queryFunction, h2MappingWithLegendJsonSchema, h2RuntimeWithLegendJsonSchema);
        String expectedResult2 = "1,typeFunctions::model::ElectronicsProduct,typeFunctions::model::SportsProduct\n" +
                "2,typeFunctions::model::SportsProduct,\n" +
                "3,,\n" +
                "4,,\n" +
                "5,,\n" +
                "6,,\n" +
                "7,,\n" +
                "8,,\n";
        Assert.assertEquals(expectedResult2, h2Result2.replace("\r\n", "\n"));
        Assert.assertEquals("[ORDER_TABLE.CUSTOMER <TableAliasColumn>, ORDER_TABLE.ORDERID <TableAliasColumn>]", this.scanColumns(queryFunction, h2MappingWithLegendJsonSchema));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredTypeFunctions.pure";
    }
}
