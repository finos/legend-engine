//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.test.full.tdsJoin;

import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.test.full.functions.in.TestPlanExecutionForIn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

public class TestTDSJoinRealizedResults extends AlloyTestServer
{
    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists PERSON;");
        statement.execute("Create Table PERSON(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL,birthTime TIMESTAMP NULL, PRIMARY KEY(fullName));");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P1','F1','A1','2020-12-12 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P2','F2','A2','2020-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P3',null,null,'2020-12-14 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P4',null,'A3','2020-12-15 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P5','F1','A1','2020-12-16 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P10','F1','A1','2020-12-17 20:00:00');");

        statement.execute("Drop table if exists PersonTable;");
        statement.execute("Create Table PersonTable(id INT, firstName VARCHAR(200), lastName VARCHAR(200), age INT, addressId INT, firmId INT, managerId INT);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (1, \'Peter\', \'Smith\',23, 1,1,2);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (2, \'John\', \'Johnson\',22, 2,1,4);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (3, \'John\', \'Hill\',12, 3,1,2);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (4, \'Anthony\', \'Allen\',22, 4,1,null);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (5, \'Fabrice\', \'Roberts\',34, 5,2,null);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (6, \'Oliver\', \'Hill\',32, 6,3,null);");
        statement.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (7, \'David\', \'Harris\',35, 7,4,null);");

        statement.execute("Drop table if exists FirmTable");
        statement.execute("Create Table FirmTable(id INT, legalName VARCHAR(200), addressId INT, ceoId INT);");
        statement.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (1, \'Firm X\', 8, 1);");
        statement.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (2, \'Firm A\', 9, 5);");
        statement.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (3, \'Firm B\', 10, 3);");
        statement.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (4, \'Firm C\', 11, 7);");

    }

    @Test
    public void TDSJoin()
    {
        try
        {
            InputStream executionPlanJson = TestPlanExecutionForIn.class.getClassLoader().getResourceAsStream("org/finos/legend/engine/plan/execution/stores/relational/test/full/tdsJoin/TestTDSJoin.json");
            String executionPlanJsonString = IOUtils.toString(executionPlanJson);
            SingleExecutionPlan plan = objectMapper.readValue(executionPlanJsonString, SingleExecutionPlan.class);

            String expectedResult = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"firstName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(200)\"},{\"name\":\"eID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"managerID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"fID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"legalName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(200)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".FIRSTNAME as \\\"firstName\\\", \\\"firmtable_0\\\".ID as \\\"eID\\\", case when \\\"root\\\".MANAGERID = 0 then 0 else \\\"root\\\".MANAGERID end as \\\"managerID\\\" from personTable as \\\"root\\\" left outer join firmTable as \\\"firmtable_0\\\" on (\\\"firmtable_0\\\".ID = \\\"root\\\".FIRMID)\"},{\"_type\":\"relational\",\"sql\":\"select \\\"tdsvar0_0_0\\\".firstName as \\\"firstName\\\", \\\"tdsvar0_0_0\\\".eID as \\\"eID\\\", \\\"tdsvar0_0_0\\\".managerID as \\\"managerID\\\", \\\"tdsvar0_0_0\\\".\\\"fID\\\" as \\\"fID\\\", \\\"tdsvar0_0_0\\\".\\\"legalName\\\" as \\\"legalName\\\" from (select * from tdsVar0_0 as \\\"tdsvar0_0_1\\\" inner join (select \\\"root\\\".ID as \\\"fID\\\", \\\"root\\\".LEGALNAME as \\\"legalName\\\" from firmTable as \\\"root\\\") as \\\"firmtable_0\\\" on (\\\"tdsvar0_0_1\\\".eID = \\\"firmtable_0\\\".\\\"fID\\\")) as \\\"tdsvar0_0_0\\\"\"}], \"result\" : {\"columns\" : [\"firstName\",\"eID\",\"managerID\",\"fID\",\"legalName\"], \"rows\" : [{\"values\": [\"Peter\",1,2,1,\"Firm X\"]},{\"values\": [\"John\",1,4,1,\"Firm X\"]},{\"values\": [\"John\",1,2,1,\"Firm X\"]},{\"values\": [\"Anthony\",1,null,1,\"Firm X\"]},{\"values\": [\"Fabrice\",2,null,2,\"Firm A\"]},{\"values\": [\"Oliver\",3,null,3,\"Firm B\"]},{\"values\": [\"David\",4,null,4,\"Firm C\"]}]}}";
            Assert.assertEquals(expectedResult, executePlan(plan, "user01"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // test will throw runtime exception due to realized relational result memory exceeding max default rows
    @Test (expected = RuntimeException.class)
    public void TDSJoinThrowsFailToRealizeInMemory() throws Exception
    {
        try
        {
            InputStream executionPlanJson = TestPlanExecutionForIn.class.getClassLoader().getResourceAsStream("org/finos/legend/engine/plan/execution/stores/relational/test/full/tdsJoin/TestTDSJoinExpectedFail.json");
            String executionPlanJsonString = IOUtils.toString(executionPlanJson);
            System.setProperty(RealizedRelationalResult.ROW_LIMIT_PROPERTY_NAME, "3");
            SingleExecutionPlan plan = objectMapper.readValue(executionPlanJsonString, SingleExecutionPlan.class);

            String expectedResult = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"firstName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(200)\"},{\"name\":\"eID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"managerID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"fID\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"},{\"name\":\"legalName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(200)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".FIRSTNAME as \\\"firstName\\\", \\\"firmtable_0\\\".ID as \\\"eID\\\", case when \\\"root\\\".MANAGERID = 0 then 0 else \\\"root\\\".MANAGERID end as \\\"managerID\\\" from personTable as \\\"root\\\" left outer join firmTable as \\\"firmtable_0\\\" on (\\\"firmtable_0\\\".ID = \\\"root\\\".FIRMID)\"},{\"_type\":\"relational\",\"sql\":\"select \\\"tdsvar0_0_0\\\".firstName as \\\"firstName\\\", \\\"tdsvar0_0_0\\\".eID as \\\"eID\\\", \\\"tdsvar0_0_0\\\".managerID as \\\"managerID\\\", \\\"tdsvar0_0_0\\\".\\\"fID\\\" as \\\"fID\\\", \\\"tdsvar0_0_0\\\".\\\"legalName\\\" as \\\"legalName\\\" from (select * from tdsVar0_0 as \\\"tdsvar0_0_1\\\" inner join (select \\\"root\\\".ID as \\\"fID\\\", \\\"root\\\".LEGALNAME as \\\"legalName\\\" from firmTable as \\\"root\\\") as \\\"firmtable_0\\\" on (\\\"tdsvar0_0_1\\\".eID = \\\"firmtable_0\\\".\\\"fID\\\")) as \\\"tdsvar0_0_0\\\"\"}], \"result\" : {\"columns\" : [\"firstName\",\"eID\",\"managerID\",\"fID\",\"legalName\"], \"rows\" : [{\"values\": [\"Peter\",1,2,1,\"Firm X\"]},{\"values\": [\"John\",1,4,1,\"Firm X\"]},{\"values\": [\"John\",1,2,1,\"Firm X\"]},{\"values\": [\"Anthony\",1,null,1,\"Firm X\"]},{\"values\": [\"Fabrice\",2,null,2,\"Firm A\"]},{\"values\": [\"Oliver\",3,null,3,\"Firm B\"]},{\"values\": [\"David\",4,null,4,\"Firm C\"]}]}}";
            Assert.assertEquals(expectedResult, executePlan(plan, "user01"));

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }



}