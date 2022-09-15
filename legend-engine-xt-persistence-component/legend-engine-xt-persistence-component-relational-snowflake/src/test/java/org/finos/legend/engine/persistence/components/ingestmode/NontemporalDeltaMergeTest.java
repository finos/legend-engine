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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class NontemporalDeltaMergeTest extends IngestModeTest
{

    @Test
    void testGeneratePhysicalPlan()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(SnowflakeSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
            "USING \"mydb\".\"staging\" as stage " +
            "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
            "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
            "THEN UPDATE SET " +
            "sink.\"id\" = stage.\"id\"," +
            "sink.\"name\" = stage.\"name\"," +
            "sink.\"amount\" = stage.\"amount\"," +
            "sink.\"biz_date\" = stage.\"biz_date\"," +
            "sink.\"digest\" = stage.\"digest\" " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
            "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithUpperCase()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .auditing(NoAuditing.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(SnowflakeSink.get())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"MYDB\".\"MAIN\" as SINK USING \"MYDB\".\"STAGING\" as STAGE " +
            "ON (SINK.\"ID\" = STAGE.\"ID\") AND (SINK.\"NAME\" = STAGE.\"NAME\") WHEN MATCHED " +
            "AND SINK.\"DIGEST\" <> STAGE.\"DIGEST\" THEN UPDATE SET SINK.\"ID\" = STAGE.\"ID\"," +
            "SINK.\"NAME\" = STAGE.\"NAME\",SINK.\"AMOUNT\" = STAGE.\"AMOUNT\"," +
            "SINK.\"BIZ_DATE\" = STAGE.\"BIZ_DATE\",SINK.\"DIGEST\" = STAGE.\"DIGEST\" " +
            "WHEN NOT MATCHED THEN INSERT (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\") " +
            "VALUES (STAGE.\"ID\",STAGE.\"NAME\",STAGE.\"AMOUNT\",STAGE.\"BIZ_DATE\",STAGE.\"DIGEST\")";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }
}
