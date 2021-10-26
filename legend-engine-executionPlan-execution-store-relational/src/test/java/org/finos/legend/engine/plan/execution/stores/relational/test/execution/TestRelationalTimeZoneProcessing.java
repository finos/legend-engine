// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.execution;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.JSONTDSSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSToObjectSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;

public class TestRelationalTimeZoneProcessing extends AlloyTestServer
{
    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("drop table if exists ProductSynonymTable");
        statement.execute("create table ProductSynonymTable (prodId Integer, name VARCHAR(200), synonym VARCHAR(200), type VARCHAR(10), createdBy VARCHAR(200), testDate DATE, testDateTime TIMESTAMP(9), from_z TIMESTAMP, thru_z TIMESTAMP)");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModA','GS-Mod-S1','STOCK','harted', \'2015-6-26\', \'2014-12-04 15:22:23.123456789\', \'2015-8-26 00:00:00\', \'2015-9-26 00:00:00\')");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModB','GS-Mod-S1','STOCK','harted', \'2015-7-26\', \'2014-12-04 23:22:23.123456789\', \'2015-8-26 23:00:00\', \'2015-9-26 00:00:00\')");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModC','GS-Mod-S1','STOCK','harted', \'2015-8-26\', \'2014-12-04 08:22:23\', \'2015-8-26 23:00:00\', \'2015-9-26 00:00:00\')");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModD','GS-Mod-S1','STOCK','harted', \'2015-9-26\', \'2014-12-04 08:22:23.123\', \'2015-8-26 23:00:00\', \'2015-9-26 00:00:00\')");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModE','GS-Mod-S1','STOCK','harted', \'2015-9-26\', \'2013-12-04 10:22:23\', \'2013-8-26 23:00:00\', \'2013-9-26 00:00:00\')");
        statement.execute("insert into ProductSynonymTable values(1, 'GS-ModF','GS-Mod-S1','STOCK','harted', \'2015-9-26\', \'2013-04-04 08:22:23.123\', \'2013-8-26 23:00:00\', \'2013-4-26 00:00:00\')");

    }

    //executionPlan({dt:DateTime[1]|ProductSynonym.allVersions()->filter(p|$p.testDateTime==$dt)}, meta::relational::tests::milestoning::milestoningmap, ^Runtime(connections = ^$connection(timeZone='EST')))

    private String getTemplatePlanWithNoTz()
    {
        return "{\n" +
                "%templateFunctions%" +
                "  \"rootExecutionNode\": {\n" +
                "    \"sqlQuery\": \"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".%dateTimeParam%\",\n" +
                "    \"resultColumns\": [\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_0\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_1\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_2\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"synonym\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"type\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"testDate\\\"\",\n" +
                "        \"dataType\": \"DATE\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"testDateTime\\\"\",\n" +
                "        \"dataType\": \"TIMESTAMP\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"k_businessDate\\\"\",\n" +
                "        \"dataType\": \"DATE\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \"" + serverPort + "\"\n" +
                "       }\n" +
                "       %timeZone%" +
                "    }," +
                "    \"_type\": \"relational\",\n" +
                "    \"resultType\": {\n" +
                "      \"class\": \"meta::relational::tests::milestoning::ProductSynonym\",\n" +
                "      \"setImplementations\": [\n" +
                "        {\n" +
                "          \"class\": \"meta::relational::tests::milestoning::ProductSynonym\",\n" +
                "          \"mapping\": \"meta::relational::tests::milestoning::milestoningmap\",\n" +
                "          \"id\": \"meta_relational_tests_milestoning_ProductSynonym\",\n" +
                "          \"propertyMappings\": [\n" +
                "            {\n" +
                "              \"property\": \"synonym\",\n" +
                "              \"type\": \"String\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"type\",\n" +
                "              \"type\": \"String\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"testDate\",\n" +
                "              \"type\": \"Date\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"testDateTime\",\n" +
                "              \"type\": \"DateTime\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"_type\": \"class\"\n" +
                "    },\n" +
                "    \"resultSizeRange\": {\n" +
                "      \"lowerBound\": 0\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private static String getFreeMarkerAlloyDateFunction()
    {
        return "  \"templateFunctions\": [\n" +
                "    \"<#function GMTtoTZ tz paramDate><#return (tz+\\\" \\\"+paramDate)?date.@alloyDate></#function>\"" +
                "  ],";
    }

    @Test
    public void testPlanWitGMTTimeZoneConversionForDateTimeQueryConstant() throws IOException
    {
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"GMT\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction());
        String plan = planWithTz.replace("%dateTimeParam%", "testDateTime = '2014-12-04 15:22:23.123456789'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04 15:22:23.123456789'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModA\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-06-26\",\"2014-12-04T15:22:23.123456789+0000\",\"2015-08-26T00:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testPlanTimeZoneConversionForDateTimeQueryConstant() throws IOException
    {
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"US/Arizona\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction());
        String plan = planWithTz.replace("%dateTimeParam%", "testDateTime = '2014-12-04 15:22:23.123456789'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04 15:22:23.123456789'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModA\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-06-26\",\"2014-12-04T22:22:23.123456789+0000\",\"2015-08-26T07:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testNonDSTTTimeZonePlanWithDateTimeStringParameterPlan() throws IOException
    {
        String connectionTimeZone = "US/Arizona";
        Assert.assertFalse("Expect " + connectionTimeZone + " never to be in DST", ZoneId.of(connectionTimeZone).getRules().isDaylightSavings(Instant.now()));
        MutableMap<String, Result> vars = Maps.mutable.empty();
        String gmtTestDateTimeParam = "2014-12-04T15:22:23";
        vars.put("dt", new ConstantResult(gmtTestDateTimeParam));
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"" + connectionTimeZone + "\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${GMTtoTZ( \\\"[" + connectionTimeZone + "]\\\" dt )}'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04T08:22:23'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModC\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-08-26\",\"2014-12-04T15:22:23.000000000+0000\",\"2015-08-27T06:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }


    @Test
    public void testNonDSTTimeZonePlanWithDateTimeSpaceSeparatedStringParameterPlan() throws IOException
    {
        String connectionTimeZone = "US/Arizona";
        Assert.assertFalse("Expect " + connectionTimeZone + " never to be in DST", ZoneId.of(connectionTimeZone).getRules().isDaylightSavings(Instant.now()));
        MutableMap<String, Result> vars = Maps.mutable.empty();
        String gmtTestDateTimeParam = "2014-12-04 15:22:23";
        vars.put("dt", new ConstantResult(gmtTestDateTimeParam));
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"" + connectionTimeZone + "\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${GMTtoTZ( \\\"[" + connectionTimeZone + "]\\\" dt )}'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04 08:22:23'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModC\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-08-26\",\"2014-12-04T15:22:23.000000000+0000\",\"2015-08-27T06:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testTimeZoneRulesUsedAreAlignedWithTheDateParameter() throws IOException
    {
        String connectionTimeZone = "America/New_York";
        MutableMap<String, Result> vars = Maps.mutable.empty();

        //UTC
        String gmtTestDateTimeParam = "2013-12-04T15:22:23"; //5 hour offset
        vars.put("dt", new ConstantResult(gmtTestDateTimeParam));
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"" + connectionTimeZone + "\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${GMTtoTZ( \\\"[" + connectionTimeZone + "]\\\" dt )}'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2013-12-04T10:22:23'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModE\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-09-26\",\"2013-12-04T15:22:23.000000000+0000\",\"2013-08-27T03:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));

        //UTC-DST
        gmtTestDateTimeParam = "2013-04-04T12:22:23.123"; //4 hour offset
        vars = Maps.mutable.empty();
        vars.put("dt", new ConstantResult(gmtTestDateTimeParam));
        planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"" + connectionTimeZone + "\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${GMTtoTZ( \\\"[" + connectionTimeZone + "]\\\" dt )}'");
        executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2013-04-04T08:22:23.123'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModF\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-09-26\",\"2013-04-04T12:22:23.123000000+0000\",\"2013-08-27T03:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testESTTimeZonePlanWithDateTimeAndMillisecondStringParameter() throws IOException
    {
        String connectionTimeZone = "US/Arizona";
        Assert.assertFalse("Expect " + connectionTimeZone + " never to be in DST", ZoneId.of(connectionTimeZone).getRules().isDaylightSavings(Instant.now()));
        MutableMap<String, Result> vars = Maps.mutable.empty();
        String gmtTestDateTimeParam = "2014-12-04T15:22:23.123";
        vars.put("dt", new ConstantResult(gmtTestDateTimeParam));
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"" + connectionTimeZone + "\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${GMTtoTZ( \\\"[" + connectionTimeZone + "]\\\" dt )}'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04T08:22:23.123'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModD\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-09-26\",\"2014-12-04T15:22:23.123000000+0000\",\"2015-08-27T06:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testGMTTimeZonePlanWithDateTimeStringParameter() throws IOException
    {
        MutableMap<String, Result> vars = Maps.mutable.empty();
        vars.put("dt", new ConstantResult("2014-12-04T15:22:23.123456789"));
        String planWithTz = getTemplatePlanWithNoTz().replace("%timeZone%", ",\"timeZone\": \"GMT\"").replace("%templateFunctions%", getFreeMarkerAlloyDateFunction()).replace("%dateTimeParam%", "testDateTime = '${dt}'");
        SingleExecutionPlan executionPlan = objectMapper.readValue(planWithTz, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult)executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(vars, Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::milestoning::milestoningmap\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_milestoning_ProductSynonym\",\"properties\":[{\"property\":\"synonym\",\"type\":\"String\"},{\"property\":\"type\",\"type\":\"String\"},{\"property\":\"testDate\",\"type\":\"Date\"},{\"property\":\"testDateTime\",\"type\":\"DateTime\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}],\"class\":\"meta::relational::tests::milestoning::ProductSynonym\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".synonym as \\\"pk_1\\\", \\\"root\\\".type as \\\"pk_2\\\", \\\"root\\\".synonym as \\\"synonym\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".testDate as \\\"testDate\\\", \\\"root\\\".testDateTime as \\\"testDateTime\\\", \\\"root\\\".from_z as \\\"k_businessDate\\\" from ProductSynonymTable as \\\"root\\\" where \\\"root\\\".testDateTime = '2014-12-04T15:22:23.123456789'\"}], \"result\" : {\"columns\" : [\"pk_0\",\"pk_1\",\"pk_2\",\"synonym\",\"type\",\"testDate\",\"testDateTime\",\"k_businessDate\"], \"rows\" : [{\"values\": [\"GS-ModA\",\"GS-Mod-S1\",\"STOCK\",\"GS-Mod-S1\",\"STOCK\",\"2015-06-26\",\"2014-12-04T15:22:23.123456789+0000\",\"2015-08-26T00:00:00.000000000+0000\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testGMTTimeZonePlanWithDifferentSerializationFormats() throws IOException
    {
        String planString = "{\"_type\":\"simple\",\"authDependent\":false,\"kerberos\":null,\"serializer\":{\"name\":\"pure\",\"version\":\"vX_X_X\"},\"templateFunctions\":[\"<#function renderCollection collection separator><#return collection?join(separator)></#function>\",\"<#function collectionSize collection> <#return collection?size> </#function>\"],\"rootExecutionNode\":{\"_type\":\"relationalTdsInstantiation\",\"resultType\":{\"_type\":\"tds\",\"tdsColumns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"doc\":null,\"relationalType\":\"TIMESTAMP\",\"enumMapping\":{}}]},\"executionNodes\":[{\"_type\":\"sql\",\"resultType\":{\"_type\":\"dataType\",\"dataType\":\"meta::pure::metamodel::type::Any\"},\"executionNodes\":[],\"resultSizeRange\":null,\"implementation\":null,\"sqlQuery\":\"select \\\"root\\\".testDateTime as \\\"testDateTime\\\" from ProductSynonymTable as \\\"root\\\"\",\"onConnectionCloseCommitQuery\":null,\"onConnectionCloseRollbackQuery\":null,\"connection\":{\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"static\",\"databaseName\" : \"testDB\",\"host\":\"127.0.0.1\",\"port\" : \"" + serverPort + "\"},\"timeZone\":\"GMT\"},\"resultColumns\":[{\"label\":\"\\\"testDateTime\\\"\",\"dataType\":\"TIMESTAMP\"}]}],\"resultSizeRange\":null,\"implementation\":null},\"globalImplementationSupport\":null}";
        SingleExecutionPlan plan = objectMapper.readValue(planString, SingleExecutionPlan.class);

        RelationalResult result_default = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"relationalType\":\"TIMESTAMP\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".testDateTime as \\\"testDateTime\\\" from ProductSynonymTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"testDateTime\"], \"rows\" : [{\"values\": [\"2014-12-04T15:22:23.123456789+0000\"]},{\"values\": [\"2014-12-04T23:22:23.123456789+0000\"]},{\"values\": [\"2014-12-04T08:22:23.000000000+0000\"]},{\"values\": [\"2014-12-04T08:22:23.123000000+0000\"]},{\"values\": [\"2013-12-04T10:22:23.000000000+0000\"]},{\"values\": [\"2013-04-04T08:22:23.123000000+0000\"]}]}}", result_default.flush(new RelationalResultToJsonDefaultSerializer(result_default)));

        RelationalResult result_pureTdsObject = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("[{\"testDateTime\":\"2014-12-04T15:22:23.123456789+0000\"},{\"testDateTime\":\"2014-12-04T23:22:23.123456789+0000\"},{\"testDateTime\":\"2014-12-04T08:22:23.000000000+0000\"},{\"testDateTime\":\"2014-12-04T08:22:23.123000000+0000\"},{\"testDateTime\":\"2013-12-04T10:22:23.000000000+0000\"},{\"testDateTime\":\"2013-04-04T08:22:23.123000000+0000\"}]", result_pureTdsObject.flush(new RelationalResultToPureTDSToObjectSerializer(result_pureTdsObject)));

        RelationalResult result_pureTds = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\"}],\"rows\":[{\"values\":[\"2014-12-04T15:22:23.123456789+0000\"]},{\"values\":[\"2014-12-04T23:22:23.123456789+0000\"]},{\"values\":[\"2014-12-04T08:22:23.000000000+0000\"]},{\"values\":[\"2014-12-04T08:22:23.123000000+0000\"]},{\"values\":[\"2013-12-04T10:22:23.000000000+0000\"]},{\"values\":[\"2013-04-04T08:22:23.123000000+0000\"]}]}", result_pureTds.flush(new RelationalResultToPureTDSSerializer(result_pureTds)));

        RelationalResult result_jsonTds = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"relationalType\":\"TIMESTAMP\"}],\"rows\":[[\"2014-12-04T15:22:23.123456789+0000\"],[\"2014-12-04T23:22:23.123456789+0000\"],[\"2014-12-04T08:22:23.000000000+0000\"],[\"2014-12-04T08:22:23.123000000+0000\"],[\"2013-12-04T10:22:23.000000000+0000\"],[\"2013-04-04T08:22:23.123000000+0000\"]]}", result_jsonTds.flush(new JSONTDSSerializer(result_jsonTds, false, false)));

        RelationalResult result_csv = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("2014-12-04 15:22:23.123456789\r\n" +
                "2014-12-04 23:22:23.123456789\r\n" +
                "2014-12-04 08:22:23.0\r\n" +
                "2014-12-04 08:22:23.123\r\n" +
                "2013-12-04 10:22:23.0\r\n" +
                "2013-04-04 08:22:23.123\r\n", result_csv.flush(new RelationalResultToCSVSerializer(result_csv)));
    }

    @Test
    public void testNonGMTTimeZonePlanWithDifferentSerializationFormats() throws IOException
    {
        String planString = "{\"_type\":\"simple\",\"authDependent\":false,\"kerberos\":null,\"serializer\":{\"name\":\"pure\",\"version\":\"vX_X_X\"},\"templateFunctions\":[\"<#function renderCollection collection separator><#return collection?join(separator)></#function>\",\"<#function collectionSize collection> <#return collection?size> </#function>\"],\"rootExecutionNode\":{\"_type\":\"relationalTdsInstantiation\",\"resultType\":{\"_type\":\"tds\",\"tdsColumns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"doc\":null,\"relationalType\":\"TIMESTAMP\",\"enumMapping\":{}}]},\"executionNodes\":[{\"_type\":\"sql\",\"resultType\":{\"_type\":\"dataType\",\"dataType\":\"meta::pure::metamodel::type::Any\"},\"executionNodes\":[],\"resultSizeRange\":null,\"implementation\":null,\"sqlQuery\":\"select \\\"root\\\".testDateTime as \\\"testDateTime\\\" from ProductSynonymTable as \\\"root\\\"\",\"onConnectionCloseCommitQuery\":null,\"onConnectionCloseRollbackQuery\":null,\"connection\":{\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"static\",\"databaseName\" : \"testDB\",\"host\":\"127.0.0.1\",\"port\" : \"" + serverPort + "\"},\"timeZone\":\"US/Arizona\"},\"resultColumns\":[{\"label\":\"\\\"testDateTime\\\"\",\"dataType\":\"TIMESTAMP\"}]}],\"resultSizeRange\":null,\"implementation\":null},\"globalImplementationSupport\":null}";
        SingleExecutionPlan plan = objectMapper.readValue(planString, SingleExecutionPlan.class);

        RelationalResult result_default = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"relationalType\":\"TIMESTAMP\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".testDateTime as \\\"testDateTime\\\" from ProductSynonymTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"testDateTime\"], \"rows\" : [{\"values\": [\"2014-12-04T22:22:23.123456789+0000\"]},{\"values\": [\"2014-12-05T06:22:23.123456789+0000\"]},{\"values\": [\"2014-12-04T15:22:23.000000000+0000\"]},{\"values\": [\"2014-12-04T15:22:23.123000000+0000\"]},{\"values\": [\"2013-12-04T17:22:23.000000000+0000\"]},{\"values\": [\"2013-04-04T15:22:23.123000000+0000\"]}]}}", result_default.flush(new RelationalResultToJsonDefaultSerializer(result_default)));

        RelationalResult result_pureTdsObject = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("[{\"testDateTime\":\"2014-12-04T22:22:23.123456789+0000\"},{\"testDateTime\":\"2014-12-05T06:22:23.123456789+0000\"},{\"testDateTime\":\"2014-12-04T15:22:23.000000000+0000\"},{\"testDateTime\":\"2014-12-04T15:22:23.123000000+0000\"},{\"testDateTime\":\"2013-12-04T17:22:23.000000000+0000\"},{\"testDateTime\":\"2013-04-04T15:22:23.123000000+0000\"}]", result_pureTdsObject.flush(new RelationalResultToPureTDSToObjectSerializer(result_pureTdsObject)));

        RelationalResult result_pureTds = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\"}],\"rows\":[{\"values\":[\"2014-12-04T22:22:23.123456789+0000\"]},{\"values\":[\"2014-12-05T06:22:23.123456789+0000\"]},{\"values\":[\"2014-12-04T15:22:23.000000000+0000\"]},{\"values\":[\"2014-12-04T15:22:23.123000000+0000\"]},{\"values\":[\"2013-12-04T17:22:23.000000000+0000\"]},{\"values\":[\"2013-04-04T15:22:23.123000000+0000\"]}]}", result_pureTds.flush(new RelationalResultToPureTDSSerializer(result_pureTds)));

        RelationalResult result_jsonTds = (RelationalResult)plan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(plan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))))));
        Assert.assertEquals("{\"columns\":[{\"name\":\"testDateTime\",\"type\":\"DateTime\",\"relationalType\":\"TIMESTAMP\"}],\"rows\":[[\"2014-12-04T22:22:23.123456789+0000\"],[\"2014-12-05T06:22:23.123456789+0000\"],[\"2014-12-04T15:22:23.000000000+0000\"],[\"2014-12-04T15:22:23.123000000+0000\"],[\"2013-12-04T17:22:23.000000000+0000\"],[\"2013-04-04T15:22:23.123000000+0000\"]]}", result_jsonTds.flush(new JSONTDSSerializer(result_jsonTds, false, false)));
    }
}