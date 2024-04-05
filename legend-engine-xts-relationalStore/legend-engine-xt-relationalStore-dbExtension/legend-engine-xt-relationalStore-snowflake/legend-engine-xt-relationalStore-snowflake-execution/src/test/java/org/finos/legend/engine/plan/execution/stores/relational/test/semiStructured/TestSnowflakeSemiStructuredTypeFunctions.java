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

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithFunctions()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithFunctions__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, \"\", \"\"), (Payment Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\", \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, \"\", \"\"), (Payment Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInProjectWithColSpecs()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInProjectWithColSpecs__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Customer Address Type, String, \"\", \"\"), (Payment Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Customer Address Type\", \"\"), (\"Payment Type\", \"\")]\n" +
                        "      sql = select \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\", \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar as \"Payment Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Customer Address Type, String, \"\", \"\"), (Payment Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageInFilter()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageInFilter__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Customer Address Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['customerAddress']['@type']::varchar as \"Customer Address Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" where \"root\".CUSTOMER['transactionDetails']['payment']['@type']::varchar = 'CashOnDeliveryPayment'\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Customer Address Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenFunction()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenFunction__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"ss_flatten_0\".VALUE['@type']::varchar as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterFlattenColSpec()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterFlattenColSpec__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"ss_flatten_0\".VALUE['@type']::varchar as \"Product Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\" inner join lateral flatten(input => \"root\".CUSTOMER['products'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("typeFunctions::semiStructuredTypeNameFunctionUsageAfterArrayElementAccess__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, \"\", \"\"), (Product 1 Type, String, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Order Id\", INT), (\"Product 0 Type\", \"\"), (\"Product 1 Type\", \"\")]\n" +
                        "      sql = select \"root\".ORDERID as \"Order Id\", \"root\".CUSTOMER['products'][0]['@type']::varchar as \"Product 0 Type\", \"root\".CUSTOMER['products'][1]['@type']::varchar as \"Product 1 Type\" from ORDER_SCHEMA.ORDER_TABLE as \"root\"\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Order Id, Integer, INT, \"\"), (Product 0 Type, String, \"\", \"\"), (Product 1 Type, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredTypeFunctions.pure";
    }
}
