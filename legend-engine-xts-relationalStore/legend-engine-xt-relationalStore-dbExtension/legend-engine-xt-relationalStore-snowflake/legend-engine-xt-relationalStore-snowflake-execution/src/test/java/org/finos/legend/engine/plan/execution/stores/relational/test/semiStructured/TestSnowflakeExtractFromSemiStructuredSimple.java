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

public class TestSnowflakeExtractFromSemiStructuredSimple extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "simple::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "simple::runtime::SnowflakeRuntime";
    
    @Test
    public void testDotAndBracketNotationAccess()
    {
        String queryFunction = "simple::dotAndBracketNotationAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, Integer, INT, \"\"), (Dot Only, StrictDate, DATE, \"\"), (Bracket Only, DateTime, TIMESTAMP, \"\"), (Dot & Bracket, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Id\", INT), (\"Dot Only\", \"\"), (\"Bracket Only\", \"\"), (\"Dot & Bracket\", \"\")]\n" +
                "      sql = select \"root\".ID as \"Id\", to_date(get_path(\"root\".FIRM_DETAILS, 'dates.estDate')) as \"Dot Only\", to_timestamp(get_path(\"root\".FIRM_DETAILS, '[\"dates\"][\"last Update\"]')) as \"Bracket Only\", to_varchar(get_path(\"root\".FIRM_DETAILS, 'address.lines[1][\"details\"]')) as \"Dot & Bracket\" from FIRM_SCHEMA.FIRM_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, Integer, INT, \"\"), (Dot Only, StrictDate, DATE, \"\"), (Bracket Only, DateTime, TIMESTAMP, \"\"), (Dot & Bracket, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }
    
    @Test
    public void testArrayElementNoFlattenAccess()
    {
        String queryFunction = "simple::arrayElementNoFlattenAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, Integer, INT, \"\"), (Second Line of Address, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Id\", INT), (\"Second Line of Address\", \"\")]\n" +
                "      sql = select \"root\".ID as \"Id\", to_varchar(get_path(\"root\".FIRM_DETAILS, 'address.lines[1][\"details\"]')) as \"Second Line of Address\" from FIRM_SCHEMA.FIRM_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, Integer, INT, \"\"), (Second Line of Address, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }
    
    @Test
    public void testExtractEnumProperty()
    {
        String queryFunction = "simple::extractEnumProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, Integer, INT, \"\"), (Entity Type, simple::model::EntityType, \"\", \"\", simple_model_EntityType)]\n" +
                "      resultColumns = [(\"Id\", INT), (\"Entity Type\", \"\")]\n" +
                "      sql = select \"root\".ID as \"Id\", to_varchar(get_path(\"root\".FIRM_DETAILS, 'entity.entityType')) as \"Entity Type\" from FIRM_SCHEMA.FIRM_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, Integer, INT, \"\"), (Entity Type, simple::model::EntityType, \"\", \"\", simple_model_EntityType)]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testAllDataTypesAccess()
    {
        String queryFunction = "simple::allDataTypesAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Id, Integer, INT, \"\"), (Legal Name, String, VARCHAR(65536), \"\"), (Est Date, StrictDate, DATE, \"\"), (Mnc, Boolean, BIT, \"\"), (Employee Count, Integer, BIGINT, \"\"), (Last Update, DateTime, TIMESTAMP, \"\")]\n" +
                "      resultColumns = [(\"Id\", INT), (\"Legal Name\", \"\"), (\"Est Date\", \"\"), (\"Mnc\", \"\"), (\"Employee Count\", \"\"), (\"Last Update\", \"\")]\n" +
                "      sql = select \"root\".ID as \"Id\", to_varchar(get_path(\"root\".FIRM_DETAILS, 'legalName')) as \"Legal Name\", to_date(get_path(\"root\".FIRM_DETAILS, 'dates.estDate')) as \"Est Date\", to_boolean(get_path(\"root\".FIRM_DETAILS, 'mnc')) as \"Mnc\", to_number(get_path(\"root\".FIRM_DETAILS, 'employeeCount')) as \"Employee Count\", to_timestamp(get_path(\"root\".FIRM_DETAILS, '[\"dates\"][\"last Update\"]')) as \"Last Update\" from FIRM_SCHEMA.FIRM_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Id, Integer, INT, \"\"), (Legal Name, String, VARCHAR(65536), \"\"), (Est Date, StrictDate, DATE, \"\"), (Mnc, Boolean, BIT, \"\"), (Employee Count, Integer, BIGINT, \"\"), (Last Update, DateTime, TIMESTAMP, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/extractFromSemiStructuredMappingSimple.pure";
    }
    
}
