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

public class TestSnowflakeExplodeSemiStructured extends AbstractTestSnowflakeSemiStructured
{
    private static final String mapping = "simple::mapping::semistructured";
    private static final String runtime = "simple::runtime::runtime";
    private static final String viewMapping = "view::mapping::semistructured";
    private static final String viewRuntime = "view::runtime::runtime";

    @Test
    public void testSimplePrimitivePropertiesProjectExplodeSource()
    {
        String queryFunction = "simple::query::getOrdersForBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Orders/Price, Float, DOUBLE, \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"Orders/Id\", VARCHAR(100)), (\"Orders/Identifier\", VARCHAR(100)), (\"Orders/Price\", DOUBLE)]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".ID as \"Orders/Id\", \"blocks_1\".IDENTIFIER as \"Orders/Identifier\", \"blocks_1\".PRICE as \"Orders/Price\" from Semistructured.Blocks as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Orders/Price, Float, DOUBLE, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        String snowflakePlanView = this.buildExecutionPlanString(queryFunction, viewMapping, viewRuntime);
        String snowflakeExpectedView =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Orders/Price, Float, DOUBLE, \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(0)), (\"Account\", VARCHAR(0)), (\"Orders/Id\", VARCHAR(100)), (\"Orders/Identifier\", VARCHAR(100)), (\"Orders/Price\", DOUBLE)]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".ID as \"Orders/Id\", \"blocks_1\".IDENTIFIER as \"Orders/Identifier\", \"blocks_1\".PRICE as \"Orders/Price\" from (select \"root\".ID as ID, \"allblocksversions_1\".ACCOUNT as ACCOUNT, \"allblocksversions_1\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_1\" on (\"allblocksversions_1\".ID = \"root\".ID and \"allblocksversions_1\".VERSION = \"root\".MAX_VERSION)) as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from (select \"root\".ID as ID, \"allblocksversions_2\".ACCOUNT as ACCOUNT, \"allblocksversions_2\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_2\" on (\"allblocksversions_2\".ID = \"root\".ID and \"allblocksversions_2\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpectedView), snowflakePlanView);
    }

    @Test
    public void testSimplePrimitivePropertiesProjectExplodeTarget()
    {
        String queryFunction = "simple::query::getBlockForTrade__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Status\", VARCHAR(100)), (\"Block/Id\", VARCHAR(100)), (\"Block/Account\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".STATUS as \"Status\", \"trades_1\".ID as \"Block/Id\", \"trades_1\".ACCOUNT as \"Block/Account\" from Semistructured.Trades as \"root\" left outer join (select \"trades_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Trades as \"trades_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_2\".ID)) as \"trades_1\" on (\"root\".ID = \"trades_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        String snowflakePlanView = this.buildExecutionPlanString(queryFunction, viewMapping, viewRuntime);
        String snowflakeExpectedView =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(0)), (\"Status\", VARCHAR(0)), (\"Block/Id\", VARCHAR(0)), (\"Block/Account\", VARCHAR(0))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".STATUS as \"Status\", \"trades_1\".ID as \"Block/Id\", \"trades_1\".ACCOUNT as \"Block/Account\" from (select \"root\".ID as ID, \"root\".STATUS as STATUS, \"root\".TRADESUMMARY as TRADESUMMARY from Semistructured.TradesTable as \"root\") as \"root\" left outer join (select \"trades_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from (select \"root\".ID as ID, \"root\".STATUS as STATUS, \"root\".TRADESUMMARY as TRADESUMMARY from Semistructured.TradesTable as \"root\") as \"trades_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from (select \"root\".ID as ID, \"allblocksversions_1\".ACCOUNT as ACCOUNT, \"allblocksversions_1\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_1\" on (\"allblocksversions_1\".ID = \"root\".ID and \"allblocksversions_1\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_2\".ID)) as \"trades_1\" on (\"root\".ID = \"trades_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpectedView), snowflakePlanView);
    }

    @Test
    public void testComplexProjectFlattenedAndExplodedPropertiesInProject()
    {
        String queryFunction = "simple::query::getOrdersAndRelatedEntitiesForBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Entity Tag, String, VARCHAR(8192), \"\"), (Entity Tag Id, String, VARCHAR(8192), \"\"), (Orders/Id, String, VARCHAR(100), \"\")]\n" +
                        "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Entity Tag\", \"\"), (\"Entity Tag Id\", \"\"), (\"Orders/Id\", VARCHAR(100))]\n" +
                        "      sql = select \"root\".ID as \"Id\", \"ss_flatten_0\".VALUE['tag']::varchar as \"Entity Tag\", \"ss_flatten_0\".VALUE['tagId']::varchar as \"Entity Tag Id\", \"blocks_1\".ID as \"Orders/Id\" from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_1\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0)\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Entity Tag, String, VARCHAR(8192), \"\"), (Entity Tag Id, String, VARCHAR(8192), \"\"), (Orders/Id, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        String snowflakePlanView = this.buildExecutionPlanString(queryFunction, viewMapping, viewRuntime);
        String snowflakeExpectedView =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Entity Tag, String, VARCHAR(8192), \"\"), (Entity Tag Id, String, VARCHAR(8192), \"\"), (Orders/Id, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(0)), (\"Entity Tag\", \"\"), (\"Entity Tag Id\", \"\"), (\"Orders/Id\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"ss_flatten_0\".VALUE['tag']::varchar as \"Entity Tag\", \"ss_flatten_0\".VALUE['tagId']::varchar as \"Entity Tag Id\", \"blocks_1\".ID as \"Orders/Id\" from (select \"root\".ID as ID, \"allblocksversions_1\".ACCOUNT as ACCOUNT, \"allblocksversions_1\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_1\" on (\"allblocksversions_1\".ID = \"root\".ID and \"allblocksversions_1\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_1\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from (select \"root\".ID as ID, \"allblocksversions_2\".ACCOUNT as ACCOUNT, \"allblocksversions_2\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_2\" on (\"allblocksversions_2\".ID = \"root\".ID and \"allblocksversions_2\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpectedView), snowflakePlanView);
    }

    @Test
    public void testComplexProjectMultiplePropertiesToExplodeInProject()
    {
        String queryFunction = "simple::query::getTradesAndOrdersInBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Trades/Id, String, VARCHAR(100), \"\"), (Trades/Status, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"Orders/Id\", VARCHAR(100)), (\"Orders/Identifier\", VARCHAR(100)), (\"Trades/Id\", VARCHAR(100)), (\"Trades/Status\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".ID as \"Orders/Id\", \"blocks_1\".IDENTIFIER as \"Orders/Identifier\", \"blocks_3\".ID as \"Trades/Id\", \"blocks_3\".STATUS as \"Trades/Status\" from Semistructured.Blocks as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0) left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Trades as \"trades_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_3\" on (\"root\".ID = \"blocks_3\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Trades/Id, String, VARCHAR(100), \"\"), (Trades/Status, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        String snowflakePlanView = this.buildExecutionPlanString(queryFunction, viewMapping, viewRuntime);
        String snowflakeExpectedView =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\"), (Orders/Identifier, String, VARCHAR(100), \"\"), (Trades/Id, String, VARCHAR(100), \"\"), (Trades/Status, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(0)), (\"Account\", VARCHAR(0)), (\"Orders/Id\", VARCHAR(100)), (\"Orders/Identifier\", VARCHAR(100)), (\"Trades/Id\", VARCHAR(0)), (\"Trades/Status\", VARCHAR(0))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".ID as \"Orders/Id\", \"blocks_1\".IDENTIFIER as \"Orders/Identifier\", \"blocks_3\".ID as \"Trades/Id\", \"blocks_3\".STATUS as \"Trades/Status\" from (select \"root\".ID as ID, \"allblocksversions_1\".ACCOUNT as ACCOUNT, \"allblocksversions_1\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_1\" on (\"allblocksversions_1\".ID = \"root\".ID and \"allblocksversions_1\".VERSION = \"root\".MAX_VERSION)) as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from (select \"root\".ID as ID, \"allblocksversions_2\".ACCOUNT as ACCOUNT, \"allblocksversions_2\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_2\" on (\"allblocksversions_2\".ID = \"root\".ID and \"allblocksversions_2\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0) left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from (select \"root\".ID as ID, \"allblocksversions_2\".ACCOUNT as ACCOUNT, \"allblocksversions_2\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_2\" on (\"allblocksversions_2\".ID = \"root\".ID and \"allblocksversions_2\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join (select \"root\".ID as ID, \"root\".STATUS as STATUS, \"root\".TRADESUMMARY as TRADESUMMARY from Semistructured.TradesTable as \"root\") as \"trades_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_3\" on (\"root\".ID = \"blocks_3\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpectedView), snowflakePlanView);
    }

    @Test
    public void testSimplePrimitivePropertiesProjectWithFilterOnSource()
    {
        String queryFunction = "simple::query::getTradesForNonCancelledBlocks__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Trades/Id, String, VARCHAR(100), \"\"), (Trades/Status, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"Trades/Id\", VARCHAR(100)), (\"Trades/Status\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".ID as \"Trades/Id\", \"blocks_1\".STATUS as \"Trades/Status\" from Semistructured.Blocks as \"root\" left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Trades as \"trades_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0) where (\"root\".BLOCKDATA['status']::varchar <> 'cancelled' OR \"root\".BLOCKDATA['status']::varchar is null)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Trades/Id, String, VARCHAR(100), \"\"), (Trades/Status, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSimplePrimitivePropertiesProjectWithFilterOnTarget()
    {
        String queryFunction = "simple::query::getNonCancelledBlocksForTrades__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Status\", VARCHAR(100)), (\"Block/Id\", VARCHAR(100)), (\"Block/Account\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".STATUS as \"Status\", \"trades_1\".ID as \"Block/Id\", \"trades_1\".ACCOUNT as \"Block/Account\" from Semistructured.Trades as \"root\" left outer join (select \"trades_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Trades as \"trades_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_2\".ID)) as \"trades_1\" on (\"root\".ID = \"trades_1\".leftJoinKey_0) where (\"trades_1\".BLOCKDATA['status']::varchar <> 'cancelled' OR \"trades_1\".BLOCKDATA['status']::varchar is null)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testProjectWithExplodedPropertyAccessOnlyInFilter()
    {
        String queryFunction = "simple::query::getNonCancelledBlocksForTradesNoProject__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Status\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".STATUS as \"Status\" from Semistructured.Trades as \"root\" left outer join (select \"trades_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Trades as \"trades_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_2\".ID)) as \"trades_1\" on (\"root\".ID = \"trades_1\".leftJoinKey_0) where (\"trades_1\".BLOCKDATA['status']::varchar <> 'cancelled' OR \"trades_1\".BLOCKDATA['status']::varchar is null)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Status, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterOnExplodedPropertyFilteringInsideProject()
    {
        String queryFunction = "simple::query::getBigOrdersInBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\"), (Big Buy Orders, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\")]\n" +
                        "      resultColumns = [(\"Block/Id\", VARCHAR(100)), (\"Block/Account\", VARCHAR(100)), (\"Big Buy Orders\", VARCHAR(100)), (\"Orders/Id\", VARCHAR(100))]\n" +
                        "      sql = select \"root\".ID as \"Block/Id\", \"root\".ACCOUNT as \"Block/Account\", \"blocks_1\".ID as \"Big Buy Orders\", \"blocks_3\".ID as \"Orders/Id\" from Semistructured.Blocks as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0 and (\"blocks_1\".QUANTITY >= 100 and \"blocks_1\".SIDE = 'BUY')) left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_3\" on (\"root\".ID = \"blocks_3\".leftJoinKey_0)\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Block/Id, String, VARCHAR(100), \"\"), (Block/Account, String, VARCHAR(100), \"\"), (Big Buy Orders, String, VARCHAR(100), \"\"), (Orders/Id, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testAggregationAggregateExplodedPropertyUsingGroupBy()
    {
        String queryFunction = "simple::query::getTradeVolumeInBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (quantity, Integer, INT, \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"quantity\", \"\")]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", sum(\"blocks_1\".TRADESUMMARY['execQuantity']::number) as \"quantity\" from Semistructured.Blocks as \"root\" left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Trades as \"trades_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0) group by \"Id\",\"Account\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (quantity, Integer, INT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testAggregationAggregateExplodedPropertyInsideProject()
    {
        String queryFunction = "simple::query::getTotalBuyOrderVolumeInBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Buy Order, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"Buy Order\", INT)]\n" +
                "      sql = select \"root\".ID as \"Id\", \"root\".ACCOUNT as \"Account\", \"blocks_1\".aggCol as \"Buy Order\" from Semistructured.Blocks as \"root\" left outer join (select \"blocks_2\".ID as ID, sum(\"blocks_3\".QUANTITY) as aggCol from Semistructured.Blocks as \"blocks_2\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_4\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_4\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_4\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_4\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_3\" on (\"blocks_2\".ID = \"blocks_3\".leftJoinKey_0) where \"blocks_3\".SIDE = 'BUY' group by \"blocks_2\".ID) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Buy Order, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSimpleJoinChainOneJoin()
    {
        String queryFunction = "simple::query::getAccountForOrders__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Account\", VARCHAR(100)), (\"Block/Id\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"orders_1\".ACCOUNT as \"Account\", \"orders_1\".ID as \"Block/Id\" from Semistructured.Orders as \"root\" left outer join (select \"orders_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Orders as \"orders_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"orders_2\".ID)) as \"orders_1\" on (\"root\".ID = \"orders_1\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Account, String, VARCHAR(100), \"\"), (Block/Id, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testJoinChainMultipleJoinsSingleExplode()
    {
        String queryFunction = "simple::query::getProductsForOrdersInBlock__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Product, String, VARCHAR(100), \"\"), (Id, String, VARCHAR(100), \"\")]\n" +
                        "      resultColumns = [(\"Product\", VARCHAR(100)), (\"Id\", VARCHAR(100))]\n" +
                        "      sql = select \"product_0\".PRODUCT as \"Product\", \"root\".ID as \"Id\" from Semistructured.Blocks as \"root\" left outer join (select \"orders_0\".ID, \"orders_0\".IDENTIFIER, \"orders_0\".QUANTITY, \"orders_0\".SIDE, \"orders_0\".PRICE, \"blocks_2\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_2\" inner join Semistructured.Orders as \"orders_0\" on (to_varchar(get_path(\"blocks_2\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_2\".flattened_prop, 'tagId')) = \"orders_0\".ID)) as \"blocks_1\" on (\"root\".ID = \"blocks_1\".leftJoinKey_0) left outer join Semistructured.Product as \"product_0\" on (\"blocks_1\".IDENTIFIER = \"product_0\".IDENTIFIER)\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Product, String, VARCHAR(100), \"\"), (Id, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testJoinChainMultipleJoinsMultipleExplode()
    {
        String queryFunction = "simple::query::getRelatedTradesForOrder__TabularDataSet_1_";

        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Trade Id, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Trade Id\", VARCHAR(100))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"blocks_2\".ID as \"Trade Id\" from Semistructured.Orders as \"root\" left outer join (select \"orders_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Orders as \"orders_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"orders_2\".ID)) as \"orders_1\" on (\"root\".ID = \"orders_1\".leftJoinKey_0) left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_0\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from Semistructured.Blocks as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities']['trades'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" inner join Semistructured.Trades as \"trades_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_2\" on (\"orders_1\".ID = \"blocks_2\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, String, VARCHAR(100), \"\"), (Trade Id, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        String snowflakePlanView = this.buildExecutionPlanString(queryFunction, viewMapping, viewRuntime);
        String snowflakeExpectedView =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, String, VARCHAR(100), \"\"), (Trade Id, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"Id\", VARCHAR(100)), (\"Trade Id\", VARCHAR(0))]\n" +
                "      sql = select \"root\".ID as \"Id\", \"blocks_2\".ID as \"Trade Id\" from Semistructured.Orders as \"root\" left outer join (select \"orders_2\".ID as leftJoinKey_0, \"blocks_0\".ID, \"blocks_0\".ACCOUNT, \"blocks_0\".BLOCKDATA from Semistructured.Orders as \"orders_2\" inner join (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID, \"root\".ACCOUNT, \"root\".BLOCKDATA from (select \"root\".ID as ID, \"allblocksversions_1\".ACCOUNT as ACCOUNT, \"allblocksversions_1\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_1\" on (\"allblocksversions_1\".ID = \"root\".ID and \"allblocksversions_1\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'order' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"orders_2\".ID)) as \"orders_1\" on (\"root\".ID = \"orders_1\".leftJoinKey_0) left outer join (select \"trades_0\".ID, \"trades_0\".STATUS, \"trades_0\".TRADESUMMARY, \"blocks_0\".leftJoinKey_0 as leftJoinKey_0 from (select \"ss_flatten_0\".VALUE as flattened_prop, \"root\".ID as leftJoinKey_0 from (select \"root\".ID as ID, \"allblocksversions_2\".ACCOUNT as ACCOUNT, \"allblocksversions_2\".BLOCKDATA as BLOCKDATA from (select \"root\".ID as ID, max(\"root\".VERSION) as MAX_VERSION from Semistructured.AllBlocksVersions as \"root\" group by \"root\".ID) as \"root\" left outer join Semistructured.AllBlocksVersions as \"allblocksversions_2\" on (\"allblocksversions_2\".ID = \"root\".ID and \"allblocksversions_2\".VERSION = \"root\".MAX_VERSION)) as \"root\" inner join lateral flatten(input => \"root\".BLOCKDATA['relatedEntities'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\") as \"blocks_0\" inner join (select \"root\".ID as ID, \"root\".STATUS as STATUS, \"root\".TRADESUMMARY as TRADESUMMARY from Semistructured.TradesTable as \"root\") as \"trades_0\" on (to_varchar(get_path(\"blocks_0\".flattened_prop, 'tag')) = 'trade' and to_varchar(get_path(\"blocks_0\".flattened_prop, 'tagId')) = \"trades_0\".ID)) as \"blocks_2\" on (\"orders_1\".ID = \"blocks_2\".leftJoinKey_0)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpectedView), snowflakePlanView);
    }

    @Test
    public void testSemiStructuredQueryWithMultipleFieldTypes()
    {
        String queryFunction = "simple::query::semiStructuredWithDifferentJsonTypes__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, mapping, runtime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(Manufacturer Id, Integer, INT, \"\"), (Is Tax Exempt, Boolean, BIT, \"\"), (Date Foudned, StrictDate, DATE, \"\"), (Name, String, VARCHAR(8192), \"\"), (Annual Tax Rate, Float, FLOAT, \"\"), (Hourly Rate, Decimal, \"\", \"\")]\n" +
                        "      resultColumns = [(\"Manufacturer Id\", INT), (\"Is Tax Exempt\", \"\"), (\"Date Foudned\", \"\"), (\"Name\", \"\"), (\"Annual Tax Rate\", \"\"), (\"Hourly Rate\", \"\")]\n" +
                        "      sql = select \"root\".MANUFACTURER_ID as \"Manufacturer Id\", \"root\".MANUFACTURER_DETAILS['isTaxExempt']::boolean as \"Is Tax Exempt\", \"root\".MANUFACTURER_DETAILS['dateFounded']::date as \"Date Foudned\", \"root\".MANUFACTURER_DETAILS['name']::varchar as \"Name\", \"root\".MANUFACTURER_DETAILS['annualTaxRate']::float as \"Annual Tax Rate\", \"root\".MANUFACTURER_DETAILS['hourlyRate'] as \"Hourly Rate\" from Semistructured.Manufacturers as \"root\" where \"root\".MANUFACTURER_DETAILS['isTaxExempt']::boolean\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(Manufacturer Id, Integer, INT, \"\"), (Is Tax Exempt, Boolean, BIT, \"\"), (Date Foudned, StrictDate, DATE, \"\"), (Name, String, VARCHAR(8192), \"\"), (Annual Tax Rate, Float, FLOAT, \"\"), (Hourly Rate, Decimal, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Override
    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/explodeSemiStructuredMapping.pure";
    }
}
