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

package org.finos.legend.engine.persistence.components.logicalplan.operations;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;

public class SchemaEvolutionForMetadataDatestTest extends BaseTest
{
    @Test
    void testSchemaEvolutionNoChanges()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        RelationalIngestor ingestor = getRelationalIngestor(CaseConversion.NONE);

        List<String> schemaEvolutionSql = performSchemaEvolutionOfMetadataDataset(datasets, ingestor);
        Assertions.assertTrue(schemaEvolutionSql.isEmpty());
    }

    @Test
    void testSchemaEvolutionBatchSourceInfoMissing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        RelationalIngestor ingestor = getRelationalIngestor(CaseConversion.NONE);

        // Pre create metadata table
        DatasetDefinition meta = getMetadataDatasetDefWithoutBatchSourceInfo(MetadataDataset.builder().build());
        createTempTable(meta);

        // Perform Schema Evolution
        List<String> schemaEvolutionSql = performSchemaEvolutionOfMetadataDataset(datasets, ingestor);
        String expectedSql = "ALTER TABLE \"batch_metadata\" ADD COLUMN \"batch_source_info\" JSON";
        Assertions.assertEquals(1, schemaEvolutionSql.size());
        Assertions.assertEquals(expectedSql, schemaEvolutionSql.get(0));

        // Perform Schema Evolution again - nothing should happen now
        schemaEvolutionSql = performSchemaEvolutionOfMetadataDataset(datasets, ingestor);
        Assertions.assertTrue(schemaEvolutionSql.isEmpty());
    }

    @Test
    void testSchemaEvolutionBatchSourceInfoAndAdditionalMetaMissing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        RelationalIngestor ingestor = getRelationalIngestor(CaseConversion.TO_UPPER);

        // Pre create metadata table
        MetadataDataset metadataDataset = new DatasetCaseConverter().applyCaseOnMetadataDataset(MetadataDataset.builder().build(), String::toUpperCase);
        DatasetDefinition meta = getMetadataDatasetDefWithoutBatchSourceInfoAndAdditionalMetadata(metadataDataset);
        createTempTable(meta);

        // Perform Schema Evolution
        List<String> schemaEvolutionSql = performSchemaEvolutionOfMetadataDataset(datasets, ingestor);
        String expectedSql1 = "ALTER TABLE \"BATCH_METADATA\" ADD COLUMN \"BATCH_SOURCE_INFO\" JSON";
        String expectedSql2 = "ALTER TABLE \"BATCH_METADATA\" ADD COLUMN \"ADDITIONAL_METADATA\" JSON";
        Assertions.assertEquals(2, schemaEvolutionSql.size());
        Assertions.assertEquals(expectedSql1, schemaEvolutionSql.get(0));
        Assertions.assertEquals(expectedSql2, schemaEvolutionSql.get(1));

        // Perform Schema Evolution again - nothing should happen now
        schemaEvolutionSql = performSchemaEvolutionOfMetadataDataset(datasets, ingestor);
        Assertions.assertTrue(schemaEvolutionSql.isEmpty());
    }

    private static RelationalIngestor getRelationalIngestor(CaseConversion caseConversion)
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
                .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .enableSchemaEvolution(false)
                .enableSchemaEvolutionForMetadataDatasets(true)
                .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
                .caseConversion(caseConversion)
                .build();
        return ingestor;
    }

    private DatasetDefinition getMetadataDatasetDefWithoutBatchSourceInfo(MetadataDataset metadataDataset)
    {
        return DatasetDefinition.builder()
                .database(metadataDataset.metadataDatasetDatabaseName())
                .group(metadataDataset.metadataDatasetGroupName())
                .name(metadataDataset.metadataDatasetName())
                .schema(SchemaDefinition.builder()
                        .addFields(Field.builder().name(metadataDataset.tableNameField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                        .addFields(Field.builder().name(metadataDataset.batchStartTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                        .addFields(Field.builder().name(metadataDataset.batchEndTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                        .addFields(Field.builder().name(metadataDataset.batchStatusField()).type(FieldType.of(DataType.VARCHAR, 32, null)).build())
                        .addFields(Field.builder().name(metadataDataset.tableBatchIdField()).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build())
                        .addFields(Field.builder().name(metadataDataset.additionalMetadataField()).type(FieldType.of(DataType.JSON, Optional.empty(), Optional.empty())).build())
                        .build())
                .build();
    }

    private DatasetDefinition getMetadataDatasetDefWithoutBatchSourceInfoAndAdditionalMetadata(MetadataDataset metadataDataset)
    {
        return DatasetDefinition.builder()
                .database(metadataDataset.metadataDatasetDatabaseName())
                .group(metadataDataset.metadataDatasetGroupName())
                .name(metadataDataset.metadataDatasetName())
                .schema(SchemaDefinition.builder()
                        .addFields(Field.builder().name(metadataDataset.tableNameField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                        .addFields(Field.builder().name(metadataDataset.batchStartTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                        .addFields(Field.builder().name(metadataDataset.batchEndTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                        .addFields(Field.builder().name(metadataDataset.batchStatusField()).type(FieldType.of(DataType.VARCHAR, 32, null)).build())
                        .addFields(Field.builder().name(metadataDataset.tableBatchIdField()).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build())
                        .build())
                .build();
    }

    private static List<String> performSchemaEvolutionOfMetadataDataset(Datasets datasets, RelationalIngestor ingestor)
    {
        ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        ingestor.create();
        List<String> schemaEvolutionSql = ingestor.evolve().schemaEvolutionSql();
        return schemaEvolutionSql;
    }


}
