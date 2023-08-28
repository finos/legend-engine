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

package org.finos.legend.engine.persistence.components.e2e;

import com.google.cloud.bigquery.BigQuery;
import org.finos.legend.engine.persistence.components.BaseTestUtils;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.TabularValues;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.IncompatibleSchemaChangeException;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumns;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumnsFromDb;

@Disabled
public class SchemaEvolutionTest extends BigQueryEndToEndTest
{
    protected final String datasetName = "demo";

    @Test
    public void testSchemaValidation() throws IOException
    {
        String tableName = "test_data_types_supported";
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name(tableName)
                .alias(tableName)
                .schema(schemaWithAllColumns)
                .build();
        BigQuery bigquery = getBigQueryConnection();
        BigQueryHelper bigQueryHelper = BigQueryHelper.of(bigquery);
        RelationalSink relationalSink = BigQuerySink.get();
        Executor<SqlGen, TabularData, SqlPlan> relationalExecutor = relationalSink.getRelationalExecutor(BigQueryConnection.of(bigquery));
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());

        dropTable(relationalExecutor, transformer, dataset);
        createTable(relationalExecutor, transformer, dataset);

        relationalSink.validateMainDatasetSchemaFn().execute(relationalExecutor, bigQueryHelper, dataset);
        Dataset datasetConstructedFromDb = relationalSink.constructDatasetFromDatabaseFn().execute(relationalExecutor, bigQueryHelper, dataset);
        relationalSink.validateMainDatasetSchemaFn().execute(relationalExecutor, bigQueryHelper, datasetConstructedFromDb);
        Assertions.assertEquals(dataset.withSchema(schemaWithAllColumnsFromDb), datasetConstructedFromDb);
    }

    @Test
    public void testSchemaEvolution() throws IOException
    {
        List<DatasetDefinition> list = Arrays.asList(
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_int64")
                        .alias("tsm_int64")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colInt.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_numeric")
                        .alias("tsm_numeric")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumeric.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_numeric_with_precision")
                        .alias("tsm_numeric_with_precision")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithPrecision.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_numeric_with_scale")
                        .alias("tsm_numeric_with_scale")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_numeric_with_less_scale")
                        .alias("tsm_numeric_with_less_scale")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col").withType(BaseTestUtils.colNumericWithScale.type().withLength(32).withScale(3))).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_float64")
                        .alias("tsm_float64")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colFloat.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_string")
                        .alias("tsm_string")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colString.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_string_with_length")
                        .alias("tsm_string_with_length")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colStringWithLength.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_string_with_big_length")
                        .alias("tsm_string_with_big_length")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colStringWithLength.withName("col").withType(BaseTestUtils.colStringWithLength.type().withLength(100))).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_date")
                        .alias("tsm_date")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colDate.withName("col").withPrimaryKey(false)).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_datetime")
                        .alias("tsm_datetime")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colDatetime.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_time")
                        .alias("tsm_time")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colTime.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_timestamp")
                        .alias("tsm_timestamp")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colTimestamp.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_bool")
                        .alias("tsm_bool")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colBoolean.withName("col")).build())
                        .build(),
                DatasetDefinition.builder()
                        .database(projectId)
                        .group(datasetName)
                        .name("tsm_json")
                        .alias("tsm_json")
                        .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colJson.withName("col")).build())
                        .build());

        List<List<String>> alterSqls = Arrays.asList(
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_precision` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_scale` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_less_scale` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_int64` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_precision` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_scale` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_less_scale` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_int64` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_int64` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_precision` ALTER COLUMN `col` SET DATA TYPE NUMERIC(33,4)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_less_scale` ALTER COLUMN `col` SET DATA TYPE NUMERIC(33,4)"),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_int64` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_precision` ALTER COLUMN `col` SET DATA TYPE NUMERIC(32,3)"),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_int64` ALTER COLUMN `col` SET DATA TYPE FLOAT64"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric` ALTER COLUMN `col` SET DATA TYPE FLOAT64"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_precision` ALTER COLUMN `col` SET DATA TYPE FLOAT64"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_scale` ALTER COLUMN `col` SET DATA TYPE FLOAT64"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_with_less_scale` ALTER COLUMN `col` SET DATA TYPE FLOAT64"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_string_with_length` ALTER COLUMN `col` SET DATA TYPE STRING"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_string_with_big_length` ALTER COLUMN `col` SET DATA TYPE STRING"),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_string_with_length` ALTER COLUMN `col` SET DATA TYPE STRING(100)"),
                Arrays.asList()
        );

        List<String> exceptionMessages = Arrays.asList(
                "Breaking schema change from datatype \"STRING\" to \"INT64\"",
                "Breaking schema change from datatype \"STRING\" to \"INT64\"",
                "Breaking schema change from datatype \"STRING\" to \"INT64\"",
                "Breaking schema change from datatype \"DATE\" to \"INT64\"",
                "Breaking schema change from datatype \"DATETIME\" to \"INT64\"",
                "Breaking schema change from datatype \"TIME\" to \"INT64\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"INT64\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"INT64\"",
                "Breaking schema change from datatype \"JSON\" to \"INT64\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATE\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATETIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"JSON\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATE\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATETIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"JSON\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATE\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATETIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"JSON\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATE\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"DATETIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIME\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"JSON\" to \"NUMERIC\"",
                "Breaking schema change from datatype \"STRING\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"STRING\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"STRING\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"DATE\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"DATETIME\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"TIME\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"JSON\" to \"FLOAT64\"",
                "Breaking schema change from datatype \"INTEGER\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"FLOAT\" to \"STRING\"",
                "Breaking schema change from datatype \"DATE\" to \"STRING\"",
                "Breaking schema change from datatype \"DATETIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"STRING\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"STRING\"",
                "Breaking schema change from datatype \"JSON\" to \"STRING\"",
                "Breaking schema change from datatype \"INTEGER\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"FLOAT\" to \"STRING\"",
                "Breaking schema change from datatype \"DATE\" to \"STRING\"",
                "Breaking schema change from datatype \"DATETIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"STRING\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"STRING\"",
                "Breaking schema change from datatype \"JSON\" to \"STRING\"",
                "Breaking schema change from datatype \"INTEGER\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"STRING\"",
                "Breaking schema change from datatype \"FLOAT\" to \"STRING\"",
                "Breaking schema change from datatype \"DATE\" to \"STRING\"",
                "Breaking schema change from datatype \"DATETIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIME\" to \"STRING\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"STRING\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"STRING\"",
                "Breaking schema change from datatype \"JSON\" to \"STRING\"",
                "Breaking schema change from datatype \"INTEGER\" to \"DATE\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATE\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATE\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATE\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATE\"",
                "Breaking schema change from datatype \"FLOAT\" to \"DATE\"",
                "Breaking schema change from datatype \"STRING\" to \"DATE\"",
                "Breaking schema change from datatype \"STRING\" to \"DATE\"",
                "Breaking schema change from datatype \"STRING\" to \"DATE\"",
                "Breaking schema change from datatype \"TIME\" to \"DATE\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"DATE\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"DATE\"",
                "Breaking schema change from datatype \"JSON\" to \"DATE\"",
                "Breaking schema change from datatype \"INTEGER\" to \"DATETIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATETIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATETIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATETIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"DATETIME\"",
                "Breaking schema change from datatype \"FLOAT\" to \"DATETIME\"",
                "Breaking schema change from datatype \"STRING\" to \"DATETIME\"",
                "Breaking schema change from datatype \"STRING\" to \"DATETIME\"",
                "Breaking schema change from datatype \"STRING\" to \"DATETIME\"",
                "Breaking schema change from datatype \"DATE\" to \"DATETIME\"",
                "Breaking schema change from datatype \"TIME\" to \"DATETIME\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"DATETIME\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"DATETIME\"",
                "Breaking schema change from datatype \"JSON\" to \"DATETIME\"",
                "Breaking schema change from datatype \"INTEGER\" to \"TIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIME\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIME\"",
                "Breaking schema change from datatype \"FLOAT\" to \"TIME\"",
                "Breaking schema change from datatype \"STRING\" to \"TIME\"",
                "Breaking schema change from datatype \"STRING\" to \"TIME\"",
                "Breaking schema change from datatype \"STRING\" to \"TIME\"",
                "Breaking schema change from datatype \"DATE\" to \"TIME\"",
                "Breaking schema change from datatype \"DATETIME\" to \"TIME\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"TIME\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"TIME\"",
                "Breaking schema change from datatype \"JSON\" to \"TIME\"",
                "Breaking schema change from datatype \"INTEGER\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"FLOAT\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"STRING\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"STRING\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"STRING\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"DATE\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"DATETIME\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"TIME\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"JSON\" to \"TIMESTAMP\"",
                "Breaking schema change from datatype \"INTEGER\" to \"BOOL\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"BOOL\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"BOOL\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"BOOL\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"BOOL\"",
                "Breaking schema change from datatype \"FLOAT\" to \"BOOL\"",
                "Breaking schema change from datatype \"STRING\" to \"BOOL\"",
                "Breaking schema change from datatype \"STRING\" to \"BOOL\"",
                "Breaking schema change from datatype \"STRING\" to \"BOOL\"",
                "Breaking schema change from datatype \"DATE\" to \"BOOL\"",
                "Breaking schema change from datatype \"DATETIME\" to \"BOOL\"",
                "Breaking schema change from datatype \"TIME\" to \"BOOL\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"BOOL\"",
                "Breaking schema change from datatype \"JSON\" to \"BOOL\"",
                "Breaking schema change from datatype \"INTEGER\" to \"JSON\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"JSON\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"JSON\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"JSON\"",
                "Breaking schema change from datatype \"NUMERIC\" to \"JSON\"",
                "Breaking schema change from datatype \"FLOAT\" to \"JSON\"",
                "Breaking schema change from datatype \"STRING\" to \"JSON\"",
                "Breaking schema change from datatype \"STRING\" to \"JSON\"",
                "Breaking schema change from datatype \"STRING\" to \"JSON\"",
                "Breaking schema change from datatype \"DATE\" to \"JSON\"",
                "Breaking schema change from datatype \"DATETIME\" to \"JSON\"",
                "Breaking schema change from datatype \"TIME\" to \"JSON\"",
                "Breaking schema change from datatype \"TIMESTAMP\" to \"JSON\"",
                "Breaking schema change from datatype \"BOOLEAN\" to \"JSON\""
        );

        BigQuery bigquery = getBigQueryConnection();
        RelationalSink relationalSink = BigQuerySink.get();
        BigQueryHelper bigQueryHelper = BigQueryHelper.of(bigquery);
        Executor<SqlGen, TabularData, SqlPlan> relationalExecutor = relationalSink.getRelationalExecutor(BigQueryConnection.of(bigquery));
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);

        int size = list.size();
        int alterCallIndex = 0;
        int exceptionMessageIndex = 0;
        for (int stage = 0; stage < size; stage++)
        {
            for (int main = 0; main < size; main++)
            {
                if (stage == main)
                {
                    continue;
                }
                SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build(), Arrays.stream(SchemaEvolutionCapability.values()).collect(Collectors.toSet()));
                DatasetDefinition datasetDefinitionStage = list.get(stage);
                DatasetDefinition datasetDefinitionMain = list.get(main);
                refreshDataset(relationalExecutor, transformer, datasetDefinitionMain, null);
                Dataset datasetMain = relationalSink.constructDatasetFromDatabaseFn().execute(relationalExecutor, bigQueryHelper, datasetDefinitionMain);
                FieldType typeStage = datasetDefinitionStage.schema().fields().get(0).type();
                FieldType typeMain = datasetMain.schema().fields().get(0).type();
                DataType dataTypeStage = typeStage.dataType();
                DataType dataTypeMain = typeMain.dataType();
                if (typeMain.equals(typeStage))
                {
                    //assert no change
                    schemaEvolve(relationalExecutor, transformer, schemaEvolution, datasetMain, datasetDefinitionStage, datasetMain, alterSqls.get(alterCallIndex++));
                }
                else
                {
                    FieldType typeToAssert;
                    Optional<Integer> lengthToAssert = typeMain.length();
                    if (!typeStage.length().isPresent() || lengthToAssert.isPresent() && typeStage.length().get() > lengthToAssert.get())
                    {
                        lengthToAssert = typeStage.length();
                    }

                    Optional<Integer> scaleToAssert = typeMain.scale();
                    if (typeStage.scale().isPresent() && (!scaleToAssert.isPresent() || typeStage.scale().get() > scaleToAssert.get()))
                    {
                        scaleToAssert = typeStage.scale();
                    }
                    if (!lengthToAssert.isPresent())
                    {
                        scaleToAssert = Optional.empty();
                    }

                    if (relationalSink.supportsImplicitMapping(dataTypeMain, dataTypeStage))
                    {
                        //assert no changes
                        typeToAssert = typeMain.withLength(lengthToAssert).withScale(scaleToAssert);
                        Dataset datasetToAssert = datasetMain.withSchema(datasetMain.schema().withFields(datasetMain.schema().fields().get(0).withType(typeToAssert)));
                        schemaEvolve(relationalExecutor, transformer, schemaEvolution, datasetMain, datasetDefinitionStage, datasetToAssert, alterSqls.get(alterCallIndex++));
                    }
                    else if (relationalSink.supportsExplicitMapping(dataTypeMain, dataTypeStage))
                    {
                        //assert stage schema
                        typeToAssert = typeStage.withLength(lengthToAssert).withScale(scaleToAssert);
                        Dataset datasetToAssert = datasetMain.withSchema(datasetDefinitionStage.schema().withFields(datasetDefinitionStage.schema().fields().get(0).withType(typeToAssert)));
                        schemaEvolve(relationalExecutor, transformer, schemaEvolution, datasetMain, datasetDefinitionStage, datasetToAssert, alterSqls.get(alterCallIndex++));
                    }
                    else
                    {
                        try
                        {
                            schemaEvolve(relationalExecutor, transformer, schemaEvolution, datasetMain, datasetDefinitionStage, null, null);
                        }
                        catch (Exception e)
                        {
                            Assertions.assertTrue(e instanceof IncompatibleSchemaChangeException);
                            Assertions.assertEquals(exceptionMessages.get(exceptionMessageIndex++), e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testLengthEvolution() throws IOException
    {
        DatasetDefinition numeric1 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_1")
                .alias("tsm_numeric_1")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")
                        .withType(BaseTestUtils.colNumericWithScale.type().withLength(20).withScale(3))).build())
                .build();
        DatasetDefinition numeric2 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_2")
                .alias("tsm_numeric_2")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")
                        .withType(BaseTestUtils.colNumericWithScale.type().withLength(20).withScale(5))).build())
                .build();
        DatasetDefinition numeric3 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_3")
                .alias("tsm_numeric_3")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")
                        .withType(BaseTestUtils.colNumericWithScale.type().withLength(22).withScale(5))).build())
                .build();
        DatasetDefinition numeric4 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_4")
                .alias("tsm_numeric_4")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithPrecision.withName("col")).build())
                .build();
        DatasetDefinition numeric5 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_5")
                .alias("tsm_numeric_5")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")
                        .withType(BaseTestUtils.colNumericWithScale.type().withLength(32).withScale(3))).build())
                .build();
        DatasetDefinition numeric6 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_6")
                .alias("tsm_numeric_6")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumericWithScale.withName("col")
                        .withType(BaseTestUtils.colNumericWithScale.type().withLength(34).withScale(5))).build())
                .build();
        DatasetDefinition numeric7 = DatasetDefinition.builder()
                .database(projectId)
                .group(datasetName)
                .name("tsm_numeric_7")
                .alias("tsm_numeric_7")
                .schema(SchemaDefinition.builder().addFields(BaseTestUtils.colNumeric.withName("col")).build())
                .build();

        List<DatasetDefinition> stageDefinition = Arrays.asList(
                numeric1, numeric2, numeric3, numeric3, numeric1, numeric4, numeric7, numeric7, numeric7);
        List<DatasetDefinition> mainDefinition = Arrays.asList(
                numeric2, numeric1, numeric1, numeric2, numeric4, numeric2, numeric1, numeric4, numeric6);
        List<DatasetDefinition> assertionDefinition = Arrays.asList(
                numeric3, numeric3, numeric3, numeric3, numeric5, numeric6, numeric7, numeric7, numeric7);
        List<List<String>> alterSqlToAssert = Arrays.asList(
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_2` ALTER COLUMN `col` SET DATA TYPE NUMERIC(22,5)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_1` ALTER COLUMN `col` SET DATA TYPE NUMERIC(22,5)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_1` ALTER COLUMN `col` SET DATA TYPE NUMERIC(22,5)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_2` ALTER COLUMN `col` SET DATA TYPE NUMERIC(22,5)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_4` ALTER COLUMN `col` SET DATA TYPE NUMERIC(32,3)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_2` ALTER COLUMN `col` SET DATA TYPE NUMERIC(34,5)"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_1` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_4` ALTER COLUMN `col` SET DATA TYPE NUMERIC"),
                Arrays.asList("ALTER TABLE `" + projectId + "`.`" + datasetName + "`.`tsm_numeric_6` ALTER COLUMN `col` SET DATA TYPE NUMERIC"));

        int size = stageDefinition.size();
        for (int i = 0; i < size; i++)
        {
            BigQuery bigquery = getBigQueryConnection();
            RelationalSink relationalSink = BigQuerySink.get();
            Executor<SqlGen, TabularData, SqlPlan> relationalExecutor = relationalSink.getRelationalExecutor(BigQueryConnection.of(bigquery));
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build(), Arrays.stream(SchemaEvolutionCapability.values()).collect(Collectors.toSet()));
            DatasetDefinition datasetDefinitionStage = stageDefinition.get(i);
            DatasetDefinition datasetDefinitionMain = mainDefinition.get(i);
            DatasetDefinition datasetDefinitionAssert = assertionDefinition.get(i);
            refreshDataset(relationalExecutor, transformer, datasetDefinitionMain, null);
            schemaEvolve(relationalExecutor, transformer, schemaEvolution, datasetDefinitionMain, datasetDefinitionStage, datasetDefinitionAssert.withName(datasetDefinitionMain.name()).withAlias(datasetDefinitionMain.alias()), alterSqlToAssert.get(i));
        }
    }

    private static void schemaEvolve(Executor<SqlGen, TabularData, SqlPlan> relationalExecutor, RelationalTransformer transformer, SchemaEvolution schemaEvolution, Dataset datasetMain, Dataset datasetStage, Dataset datasetToAssert, List<String> alterSqlsToAssert)
    {
        SchemaEvolutionResult schemaEvolutionResult = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasetMain, datasetStage);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(schemaEvolutionResult.logicalPlan());
        Assertions.assertEquals(alterSqlsToAssert, physicalPlan.getSqlList());
        relationalExecutor.executePhysicalPlan(physicalPlan);
        Assertions.assertEquals(datasetToAssert, schemaEvolutionResult.evolvedDataset());
    }

    private static void refreshDataset(Executor<SqlGen, TabularData, SqlPlan> relationalExecutor, RelationalTransformer transformer, DatasetDefinition datasetDefinition, Value value)
    {
        dropTable(relationalExecutor, transformer, datasetDefinition);
        createTable(relationalExecutor, transformer, datasetDefinition);
        if (value != null)
        {
            insertValues(relationalExecutor, transformer, datasetDefinition, value);
        }
    }

    private static void insertValues(Executor<SqlGen, TabularData, SqlPlan> relationalExecutor, RelationalTransformer transformer, DatasetDefinition datasetDefinition, Value value)
    {
        Dataset values = TabularValues.builder().addValues(Collections.singletonList(value)).columnCount(1).build();
        List<Value> fields = Collections.singletonList(FieldValue.builder().fieldName(datasetDefinition.schema().fields().get(0).name()).build());
        LogicalPlan insertValuesLogicalPlan = LogicalPlan.builder().addOps(Insert.of(datasetDefinition, values, fields)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(insertValuesLogicalPlan);
        relationalExecutor.executePhysicalPlan(physicalPlan);
    }

    private static void createTable(Executor<SqlGen, TabularData, SqlPlan> relationalExecutor, RelationalTransformer transformer, DatasetDefinition datasetDefinition)
    {
        LogicalPlan createTableLogicalPlan = LogicalPlan.builder().addOps(Create.of(false, datasetDefinition)).build();
        relationalExecutor.executePhysicalPlan(transformer.generatePhysicalPlan(createTableLogicalPlan));
    }

    private static void dropTable(Executor<SqlGen, TabularData, SqlPlan> relationalExecutor, RelationalTransformer transformer, DatasetDefinition datasetDefinition)
    {
        LogicalPlan dropTableLogicalPlan = LogicalPlan.builder().addOps(Drop.of(true, datasetDefinition, false)).build();
        relationalExecutor.executePhysicalPlan(transformer.generatePhysicalPlan(dropTableLogicalPlan));
    }
}
