// Copyright 2022 Goldman Sachs
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

public class TestSimpleSemiStructuredMapping extends AbstractTestSemiStructured
{
    private static final String memSQLMapping = "simple::mapping::MemSQLMapping";
    private static final String memSQLRuntime = "simple::runtime::MemSQLRuntime";

    private static final String h2Mapping = "simple::mapping::H2Mapping";
    private static final String h2Runtime = "simple::runtime::H2Runtime";

    @Test
    public void testSingleSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::singleSemiStructuredPropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name\", \"\")]\n" +
                "  sql = select `root`.FIRM_DETAILS::$legalName as `Firm Legal Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Firm X\n" +
                "Firm X\n" +
                "Firm X\n" +
                "Firm X\n" +
                "Firm A\n" +
                "Firm B\n" +
                "Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccessParallel()
    {
        String queryFunction = "simple::combinedPrimitiveAndSemiStructuredPropertyAccessParallel__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Legal Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X\n" +
                "John,Firm X\n" +
                "John,Firm X\n" +
                "Anthony,Firm X\n" +
                "Fabrice,Firm A\n" +
                "Oliver,Firm B\n" +
                "David,Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::combinedPrimitiveAndSemiStructuredPropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Out Col, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Out Col\", \"\")]\n" +
                "  sql = select concat(`root`.FIRSTNAME, ' : ', `root`.FIRM_DETAILS::$legalName) as `Out Col` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter : Firm X\n" +
                "John : Firm X\n" +
                "John : Firm X\n" +
                "Anthony : Firm X\n" +
                "Fabrice : Firm A\n" +
                "Oliver : Firm B\n" +
                "David : Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccessParallel()
    {
        String queryFunction = "simple::combinedComplexAndSemiStructuredPropertyAccessParallel__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Manager First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Manager First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "  sql = select `person_table_1`.FIRSTNAME as `Manager First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Legal Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("John,Firm X\n" +
                "Anthony,Firm X\n" +
                "John,Firm X\n" +
                ",Firm X\n" +
                ",Firm A\n" +
                "David,Firm B\n" +
                ",Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::combinedComplexAndSemiStructuredPropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Out Col, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Out Col\", \"\")]\n" +
                "  sql = select concat(case when `person_table_1`.FIRSTNAME is null then 'NULL' else `person_table_1`.FIRSTNAME end, ' : ', `root`.FIRM_DETAILS::$legalName) as `Out Col` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("John : Firm X\n" +
                "Anthony : Firm X\n" +
                "John : Firm X\n" +
                "NULL : Firm X\n" +
                "NULL : Firm A\n" +
                "David : Firm B\n" +
                "NULL : Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testNestedSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::nestedSemiStructuredPropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Address Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Firm Address Name\", \"\")]\n" +
                "  sql = select `root`.FIRM_DETAILS::address::$name as `Firm Address Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("A1\n" +
                "A1\n" +
                "A1\n" +
                "A1\n" +
                "A2\n" +
                "A3\n" +
                "A3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::multipleSemiStructuredPropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name, String, VARCHAR(8192), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name\", \"\"), (\"Firm Address Name\", \"\")]\n" +
                "  sql = select `root`.FIRM_DETAILS::$legalName as `Firm Legal Name`, `root`.FIRM_DETAILS::address::$name as `Firm Address Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm A,A2\n" +
                "Firm B,A3\n" +
                "Firm B,A3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccessCombined()
    {
        String queryFunction = "simple::multipleSemiStructuredPropertyAccessCombined__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name And Address Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name And Address Name\", \"\")]\n" +
                "  sql = select concat(`root`.FIRM_DETAILS::$legalName, `root`.FIRM_DETAILS::address::$name) as `Firm Legal Name And Address Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Firm XA1\n" +
                "Firm XA1\n" +
                "Firm XA1\n" +
                "Firm XA1\n" +
                "Firm AA2\n" +
                "Firm BA3\n" +
                "Firm BA3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterWithSemiStructuredProperty()
    {
        String queryFunction = "simple::filterWithSemiStructuredProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` where `root`.FIRM_DETAILS::$legalName = 'Firm X'\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testGroupByWithSemiStructuredProperty()
    {
        String queryFunction = "simple::groupByWithSemiStructuredProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Address, String, VARCHAR(8192), \"\"), (Names, String, VARCHAR(1024), \"\")]\n" +
                "  resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "  sql = select `root`.FIRM_DETAILS::address::$name as `Address`, group_concat(`root`.FIRSTNAME separator ';') as `Names` from PERSON_SCHEMA.PERSON_TABLE as `root` group by `Address`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("A1,Peter;John;John;Anthony\n" +
                "A2,Fabrice\n" +
                "A3,Oliver;David\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSortByWithSemiStructuredProperty()
    {
        String queryFunction = "simple::sortByWithSemiStructuredProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` order by `root`.FIRM_DETAILS::$legalName\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Fabrice\n" +
                "Oliver\n" +
                "David\n" +
                "Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPrimitivePropertyAccess()
    {
        String queryFunction = "simple::isEmptyCheckOnSemiStructuredPrimitivePropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Street, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Street\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, case when `root`.FIRM_DETAILS::address::$street is null then 'NULL' else `root`.FIRM_DETAILS::address::$street end as `First Address Street` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,S1\n" +
                "John,S1\n" +
                "John,S1\n" +
                "Anthony,S1\n" +
                "Fabrice,NULL\n" +
                "Oliver,S2\n" +
                "David,NULL\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPropertyAccessAfterAt()
    {
        String queryFunction = "simple::isEmptyCheckOnSemiStructuredPropertyAccessAfterAt__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Line, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Line\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, case when `root`.FIRM_DETAILS::address::`lines`::`2`::$details is null then 'NULL' else `root`.FIRM_DETAILS::address::`lines`::`2`::$details end as `First Address Line` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,D3\n" +
                "John,D3\n" +
                "John,D3\n" +
                "Anthony,D3\n" +
                "Fabrice,NULL\n" +
                "Oliver,NULL\n" +
                "David,NULL\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredDifferentDataTypePropertyAccess()
    {
        String queryFunction = "simple::semiStructuredDifferentDataTypePropertyAccess__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(8192), \"\"), (Firm Employee Count, Integer, BIGINT, \"\"), (Firm Revenue, Float, FLOAT, \"\"), (Firm MNC, Boolean, BIT, \"\"), (Firm Est Date, StrictDate, DATE, \"\"), (Firm Last Update, DateTime, TIMESTAMP, \"\"), (Firm Address Street, String, VARCHAR(8192), \"\"), (Firm Entity Type, simple::model::EntityType, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Firm Employee Count\", \"\"), (\"Firm Revenue\", \"\"), (\"Firm MNC\", \"\"), (\"Firm Est Date\", \"\"), (\"Firm Last Update\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Entity Type\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Legal Name`, `root`.FIRM_DETAILS::employeeCount !:> bigint as `Firm Employee Count`, `root`.FIRM_DETAILS::%revenue as `Firm Revenue`, `root`.FIRM_DETAILS::mnc as `Firm MNC`, date(`root`.FIRM_DETAILS::$estDate) as `Firm Est Date`, timestamp(`root`.FIRM_DETAILS::$lastUpdate) as `Firm Last Update`, `root`.FIRM_DETAILS::address::$street as `Firm Address Street`, `root`.FIRM_DETAILS::$entityType as `Firm Entity Type` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,4,0.5,true,2010-03-04,2022-01-16 01:00:00.0,S1,Organization\n" +
                "John,Firm X,4,5.5,true,2010-03-04,2022-01-16 01:00:00.0,S1,Organization\n" +
                "John,Firm X,4,55.5,true,2010-03-04,2022-01-16 01:00:00.0,S1,Organization\n" +
                "Anthony,Firm X,4,5555.5,true,2010-03-04,2022-01-16 01:00:00.0,S1,Organization\n" +
                "Fabrice,Firm A,1,0.5,false,2012-11-13,2022-02-14 03:00:00.0,,\n" +
                "Oliver,Firm B,2,5.5,true,2017-07-07,2022-09-01 06:00:00.0,S2,Company\n" +
                "David,Firm B,2,55.5,true,2017-07-07,2022-09-01 06:00:00.0,,Company\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[PERSON_TABLE.FIRM_DETAILS <TableAliasColumn>, PERSON_TABLE.FIRSTNAME <TableAliasColumn>]", this.scanColumns(queryFunction, h2Mapping));
    }

    @Test
    public void testSemiStructuredArrayElementAccessPrimitive()
    {
        String queryFunction = "simple::semiStructuredArrayElementAccessPrimitive__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Other Name 0, String, VARCHAR(8192), \"\"), (Firm Other Name 1, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Other Name 0\", \"\"), (\"Firm Other Name 1\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::otherNames::$`0` as `Firm Other Name 0`, `root`.FIRM_DETAILS::otherNames::$`1` as `Firm Other Name 1` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,O1,O2\n" +
                "John,O1,O2\n" +
                "John,O1,O2\n" +
                "Anthony,O1,O2\n" +
                "Fabrice,O3,O4\n" +
                "Oliver,O5,O6\n" +
                "David,O5,O6\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredArrayElementAccessComplex()
    {
        String queryFunction = "simple::semiStructuredArrayElementAccessComplex__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Line 0, String, VARCHAR(8192), \"\"), (Firm Address Line 1, String, VARCHAR(8192), \"\"), (Firm Address Line 2, String, VARCHAR(8192), \"\"), (Firm Address Line 3, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Line 0\", \"\"), (\"Firm Address Line 1\", \"\"), (\"Firm Address Line 2\", \"\"), (\"Firm Address Line 3\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::address::`lines`::`0`::$details as `Firm Address Line 0`, `root`.FIRM_DETAILS::address::`lines`::`1`::$details as `Firm Address Line 1`, `root`.FIRM_DETAILS::address::`lines`::`2`::$details as `Firm Address Line 2`, `root`.FIRM_DETAILS::address::`lines`::`3`::$details as `Firm Address Line 3` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,D1,D2,D3,\n" +
                "John,D1,D2,D3,\n" +
                "John,D1,D2,D3,\n" +
                "Anthony,D1,D2,D3,\n" +
                "Fabrice,D4,D5,,\n" +
                "Oliver,D5,D6,,\n" +
                "David,D5,D6,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessAtNestedProperty()
    {
        String queryFunction = "simple::semiStructuredPropertyAccessAtNestedProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Manager Firm Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Name, String, VARCHAR(8192), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(8192), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Manager Firm Name\", \"\"), (\"Manager Manager Firm Name\", \"\"), (\"Manager Manager Manager Firm Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Name`, `person_table_1`.FIRM_DETAILS::$legalName as `Manager Firm Name`, `person_table_2`.FIRM_DETAILS::$legalName as `Manager Manager Firm Name`, `person_table_3`.FIRM_DETAILS::$legalName as `Manager Manager Manager Firm Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_2` on (`person_table_1`.MANAGERID = `person_table_2`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_3` on (`person_table_2`.MANAGERID = `person_table_3`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,Firm X,Firm X,\n" +
                "John,Firm X,Firm X,,\n" +
                "John,Firm X,Firm X,Firm X,\n" +
                "Anthony,Firm X,,,\n" +
                "Fabrice,Firm A,,,\n" +
                "Oliver,Firm B,Firm B,,\n" +
                "David,Firm B,,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessAtNestedPropertyWithProjectFunctions()
    {
        String queryFunction = "simple::semiStructuredPropertyAccessAtNestedPropertyWithProjectFunctions__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Manager Firm Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Name, String, VARCHAR(8192), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(8192), \"\")]\n" +
                        "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Manager Firm Name\", \"\"), (\"Manager Manager Firm Name\", \"\"), (\"Manager Manager Manager Firm Name\", \"\")]\n" +
                        "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Name`, `person_table_1`.FIRM_DETAILS::$legalName as `Manager Firm Name`, `person_table_2`.FIRM_DETAILS::$legalName as `Manager Manager Firm Name`, `person_table_3`.FIRM_DETAILS::$legalName as `Manager Manager Manager Firm Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_2` on (`person_table_1`.MANAGERID = `person_table_2`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_3` on (`person_table_2`.MANAGERID = `person_table_3`.ID)\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,Firm X,Firm X,\n" +
                "John,Firm X,Firm X,,\n" +
                "John,Firm X,Firm X,Firm X,\n" +
                "Anthony,Firm X,,,\n" +
                "Fabrice,Firm A,,,\n" +
                "Oliver,Firm B,Firm B,,\n" +
                "David,Firm B,,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterWithSemiStructuredPropertyAccessAtNestedProperty()
    {
        String queryFunction = "simple::filterWithSemiStructuredPropertyAccessAtNestedProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_2` on (`person_table_1`.MANAGERID = `person_table_2`.ID) where `person_table_2`.FIRM_DETAILS::$legalName = 'Firm X'\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIfElseLogicOnEnumProperties()
    {
        String queryFunction = "simple::ifElseLogicOnEnumProperties__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Enum Return, simple::model::EntityType, \"\", \"\")]\n" +
                "  resultColumns = [(\"Enum Return\", \"\")]\n" +
                "  sql = select case when `root`.FIRSTNAME = 'John' then `root`.FIRM_DETAILS::$entityType else `root`.FIRM_DETAILS::$entityType end as `Enum Return` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Organization\n" +
                "Organization\n" +
                "Organization\n" +
                "Organization\n" +
                "\n" +
                "Company\n" +
                "Company\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnEnumPropertyWithEnumConst()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithEnumConst__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` where `root`.FIRM_DETAILS::$entityType = 'Organization'\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnEnumPropertyWithStringConst()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithStringConst__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                        "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                        "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` where `root`.FIRM_DETAILS::$entityType = 'Organization'\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnEnumPropertyWithWithIfElseLogicEnum()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithIfElseLogicEnum__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                        "(\n" +
                        "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                        "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                        "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` where case when `root`.FIRSTNAME = 'John' then `root`.FIRM_DETAILS::$entityType else `root`.FIRM_DETAILS::$entityType end = 'Organization'\n" +
                        "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                        ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testGroupByOnEnumProperty()
    {
        String queryFunction = "simple::groupByOnEnumProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(Address, simple::model::EntityType, \"\", \"\"), (Names, String, VARCHAR(1024), \"\")]\n" +
                "  resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "  sql = select `root`.FIRM_DETAILS::$entityType as `Address`, group_concat(`root`.FIRSTNAME separator ';') as `Names` from PERSON_SCHEMA.PERSON_TABLE as `root` group by `Address`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(",Fabrice\n" +
                "Company,Oliver;David\n" +
                "Organization,Peter;John;John;Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSortByOnEnumProperty()
    {
        String queryFunction = "simple::sortByOnEnumProperty__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name` from PERSON_SCHEMA.PERSON_TABLE as `root` order by `root`.FIRM_DETAILS::$entityType\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Fabrice\n" +
                "Oliver\n" +
                "David\n" +
                "Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/simpleSemiStructuredMapping.pure";
    }
}
