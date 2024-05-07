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

public class TestExtractFromSemiStructuredSimple extends AbstractTestSemiStructured
{
    private static final String memSQLMapping = "simple::mapping::MemSQLMapping";
    private static final String memSQLRuntime = "simple::runtime::MemSQLRuntime";

    private static final String h2Mapping = "simple::mapping::H2Mapping";
    private static final String h2Runtime = "simple::runtime::H2Runtime";
    
    @Test
    public void testDotAndBracketNotationAccess()
    {
        String queryFunction = "simple::dotAndBracketNotationAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Id, Integer, INT, \"\"), (Dot Only, StrictDate, DATE, \"\"), (Bracket Only, DateTime, TIMESTAMP, \"\"), (Dot & Bracket, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Id\", INT), (\"Dot Only\", \"\"), (\"Bracket Only\", \"\"), (\"Dot & Bracket\", \"\")]\n" +
                "  sql = select `root`.ID as `Id`, date(json_extract_string(`root`.FIRM_DETAILS, 'dates', 'estDate')) as `Dot Only`, timestamp(json_extract_string(`root`.FIRM_DETAILS, 'dates', 'last Update')) as `Bracket Only`, json_extract_string(`root`.FIRM_DETAILS, 'address', 'lines', '1', 'details') as `Dot & Bracket` from FIRM_SCHEMA.FIRM_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,2010-03-04,2022-01-16 01:00:00.0,D2\n" +
                "2,2012-11-13,2022-02-14 03:00:00.0,D5\n" +
                "3,2017-07-07,2022-09-01 06:00:00.0,D6\n", h2Result.replace("\r\n", "\n"));
    }
    
    @Test
    public void testArrayElementNoFlattenAccess()
    {
        String queryFunction = "simple::arrayElementNoFlattenAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Id, Integer, INT, \"\"), (Second Line of Address, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Id\", INT), (\"Second Line of Address\", \"\")]\n" +
                "  sql = select `root`.ID as `Id`, json_extract_string(`root`.FIRM_DETAILS, 'address', 'lines', '1', 'details') as `Second Line of Address` from FIRM_SCHEMA.FIRM_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,D2\n" +
                "2,D5\n" +
                "3,D6\n", h2Result.replace("\r\n", "\n"));
    }
    
    @Test
    public void testExtractEnumProperty()
    {
        String queryFunction = "simple::extractEnumProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Id, Integer, INT, \"\"), (Entity Type, simple::model::EntityType, \"\", \"\", simple_model_EntityType)]\n" +
                "  resultColumns = [(\"Id\", INT), (\"Entity Type\", \"\")]\n" +
                "  sql = select `root`.ID as `Id`, json_extract_string(`root`.FIRM_DETAILS, 'entity', 'entityType') as `Entity Type` from FIRM_SCHEMA.FIRM_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        //this needs to be fixed. works fine in studio but not here.
        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,O\n" +
                "2,O\n" +
                "3,C\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testAllDataTypesAccess()
    {
        String queryFunction = "simple::allDataTypesAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Id, Integer, INT, \"\"), (Legal Name, String, VARCHAR(8192), \"\"), (Est Date, StrictDate, DATE, \"\"), (Mnc, Boolean, BIT, \"\"), (Employee Count, Integer, BIGINT, \"\"), (Revenue, Float, FLOAT, \"\"), (Last Update, DateTime, TIMESTAMP, \"\")]\n" +
                "  resultColumns = [(\"Id\", INT), (\"Legal Name\", \"\"), (\"Est Date\", \"\"), (\"Mnc\", \"\"), (\"Employee Count\", \"\"), (\"Revenue\", \"\"), (\"Last Update\", \"\")]\n" +
                "  sql = select `root`.ID as `Id`, json_extract_string(`root`.FIRM_DETAILS, 'legalName') as `Legal Name`, date(json_extract_string(`root`.FIRM_DETAILS, 'dates', 'estDate')) as `Est Date`, json_extract_json(`root`.FIRM_DETAILS, 'mnc') as `Mnc`, json_extract_bigint(`root`.FIRM_DETAILS, 'employeeCount') as `Employee Count`, json_extract_double(`root`.FIRM_DETAILS, 'revenue') as `Revenue`, timestamp(json_extract_string(`root`.FIRM_DETAILS, 'dates', 'last Update')) as `Last Update` from FIRM_SCHEMA.FIRM_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("1,Firm X,2010-03-04,true,4,5.0,2022-01-16 01:00:00.0\n" +
                "2,Firm A,2012-11-13,false,1,2000.5,2022-02-14 03:00:00.0\n" +
                "3,Firm B,2017-07-07,true,2,0.1,2022-09-01 06:00:00.0\n", h2Result.replace("\r\n", "\n"));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/extractFromSemiStructuredMappingSimple.pure";
    }
    
}
