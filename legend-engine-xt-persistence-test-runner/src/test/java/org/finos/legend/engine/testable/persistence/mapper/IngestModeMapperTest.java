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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class IngestModeMapperTest extends MapperBaseTest
{
    public static Field id = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias("id").build();
    public static Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).fieldAlias("name").build();
    public static Field income = Field.builder().name("income").type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias("income").build();

    @Test
    public void testMapperForNonTemporalSnapshot() throws Exception
    {
        IngestMode ingestMode = getNonTemporalSnapshotDateTimeAuditing();
        Persistence persistence = getPersistence(ingestMode);
        org.finos.legend.engine.persistence.components.ingestmode.IngestMode componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalSnapshot);

        NontemporalSnapshot nontemporalSnapshot = (NontemporalSnapshot) componentIngestMode;
        Assert.assertTrue(nontemporalSnapshot.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing dateTimeAuditing = (DateTimeAuditing) nontemporalSnapshot.auditing();
        Assert.assertEquals("AUDIT_TIME", dateTimeAuditing.dateTimeField());

        ingestMode = getNonTemporalSnapshotNoAuditing();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalSnapshot);

        nontemporalSnapshot = (NontemporalSnapshot) componentIngestMode;
        Assert.assertTrue(nontemporalSnapshot.auditing() instanceof NoAuditing);
    }

    @Test
    public void testMapperForAppendOnly() throws Exception
    {
        IngestMode ingestMode = getAppendOnlyNoAuditingNoFilteringDuplicates();
        Persistence persistence = getPersistence(ingestMode);
        org.finos.legend.engine.persistence.components.ingestmode.IngestMode componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof AppendOnly);

        AppendOnly appendOnly = (AppendOnly) componentIngestMode;
        Assert.assertEquals("DIGEST", appendOnly.digestField().get());
        Assert.assertTrue(appendOnly.auditing() instanceof NoAuditing);
        Assert.assertTrue(appendOnly.deduplicationStrategy() instanceof AllowDuplicates);

        ingestMode = getAppendOnlyNoAuditingWithFilteringDuplicates();
        persistence = getPersistence(ingestMode);
        componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof AppendOnly);

        appendOnly = (AppendOnly) componentIngestMode;
        Assert.assertEquals("DIGEST", appendOnly.digestField().get());
        Assert.assertTrue(appendOnly.auditing() instanceof NoAuditing);
        Assert.assertTrue(appendOnly.deduplicationStrategy() instanceof FilterDuplicates);

        ingestMode = getAppendOnlyDatetimeAuditingNoFilteringDuplicates();
        persistence = getPersistence(ingestMode);
        componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof AppendOnly);

        appendOnly = (AppendOnly) componentIngestMode;
        Assert.assertEquals("DIGEST", appendOnly.digestField().get());
        Assert.assertTrue(appendOnly.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing dateTimeAuditing = (DateTimeAuditing) appendOnly.auditing();
        Assert.assertEquals("AUDIT_TIME", dateTimeAuditing.dateTimeField());
        Assert.assertTrue(appendOnly.deduplicationStrategy() instanceof AllowDuplicates);

        ingestMode = getAppendOnlyDatetimeAuditingWithFilteringDuplicates();
        persistence = getPersistence(ingestMode);
        componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof AppendOnly);

        appendOnly = (AppendOnly) componentIngestMode;
        Assert.assertEquals("DIGEST", appendOnly.digestField().get());
        Assert.assertTrue(appendOnly.auditing() instanceof DateTimeAuditing);
        dateTimeAuditing = (DateTimeAuditing) appendOnly.auditing();
        Assert.assertEquals("AUDIT_TIME", dateTimeAuditing.dateTimeField());
        Assert.assertTrue(appendOnly.deduplicationStrategy() instanceof FilterDuplicates);
    }

    @Test
    public void testMapperForNonTemporalDelta() throws Exception
    {
        IngestMode ingestMode = getNontemporalDeltaNoAuditingNoMergeStrategy();
        Persistence persistence = getPersistence(ingestMode);
        org.finos.legend.engine.persistence.components.ingestmode.IngestMode componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalDelta);

        NontemporalDelta nontemporalDelta = (NontemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", nontemporalDelta.digestField());
        Assert.assertTrue(nontemporalDelta.auditing() instanceof NoAuditing);
        Assert.assertTrue(nontemporalDelta.mergeStrategy() instanceof NoDeletesMergeStrategy);

        ingestMode = getNontemporalDeltaNoAuditingDeleteIndMergeStrategy();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalDelta);

        nontemporalDelta = (NontemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", nontemporalDelta.digestField());
        Assert.assertTrue(nontemporalDelta.auditing() instanceof NoAuditing);
        Assert.assertTrue(nontemporalDelta.mergeStrategy() instanceof DeleteIndicatorMergeStrategy);

        ingestMode = getNontemporalDeltaWithAuditingNoMergeStrategy();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalDelta);

        nontemporalDelta = (NontemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", nontemporalDelta.digestField());
        Assert.assertTrue(nontemporalDelta.auditing() instanceof DateTimeAuditing);
        Assert.assertTrue(nontemporalDelta.mergeStrategy() instanceof NoDeletesMergeStrategy);

        ingestMode = getNontemporalDeltaWithAuditingDeleteIndMergeStrategy();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof NontemporalDelta);

        nontemporalDelta = (NontemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", nontemporalDelta.digestField());
        Assert.assertTrue(nontemporalDelta.auditing() instanceof DateTimeAuditing);
        Assert.assertTrue(nontemporalDelta.mergeStrategy() instanceof DeleteIndicatorMergeStrategy);
    }

    @Test
    public void testMapperForUnitemporalDelta() throws Exception
    {
        IngestMode ingestMode = getUnitempDeltaNoMergeBatchIdBased();
        Persistence persistence = getPersistence(ingestMode);
        org.finos.legend.engine.persistence.components.ingestmode.IngestMode componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        UnitemporalDelta unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof NoDeletesMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof BatchId);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());

        ingestMode = getUnitempDeltaNoMergeBatchIdAndTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof NoDeletesMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof BatchIdAndDateTime);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());

        ingestMode = getUnitempDeltaNoMergeTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof NoDeletesMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof TransactionDateTime);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());

        ingestMode = getUnitempDeltaDelIndMergeBatchIdBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof DeleteIndicatorMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof BatchId);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());

        ingestMode = getUnitempDeltaDelIndMergeBatchIdAndTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof DeleteIndicatorMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof BatchIdAndDateTime);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());

        ingestMode = getUnitempDeltaDelIndMergeTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalDelta);

        unitemporalDelta = (UnitemporalDelta) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalDelta.digestField());
        Assert.assertTrue(unitemporalDelta.mergeStrategy() instanceof DeleteIndicatorMergeStrategy);
        Assert.assertTrue(unitemporalDelta.transactionMilestoning() instanceof TransactionDateTime);
        Assert.assertFalse(unitemporalDelta.dataSplitField().isPresent());
    }

    @Test
    public void testMapperForUnitemporalSnapshot() throws Exception
    {
        IngestMode ingestMode = getUnitemporalSnapshotBatchIdBased();
        Persistence persistence = getPersistence(ingestMode);
        org.finos.legend.engine.persistence.components.ingestmode.IngestMode componentIngestMode =
                IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalSnapshot);

        UnitemporalSnapshot unitemporalSnapshot = (UnitemporalSnapshot) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalSnapshot.digestField());
        Assert.assertFalse(unitemporalSnapshot.partitioned());
        Assert.assertTrue(unitemporalSnapshot.transactionMilestoning() instanceof BatchId);

        ingestMode = getUnitemporalSnapshotBatchIdAndTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalSnapshot);

        unitemporalSnapshot = (UnitemporalSnapshot) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalSnapshot.digestField());
        Assert.assertFalse(unitemporalSnapshot.partitioned());
        Assert.assertTrue(unitemporalSnapshot.transactionMilestoning() instanceof BatchIdAndDateTime);

        ingestMode = getUnitemporalSnapshotTimeBased();
        persistence = getPersistence(ingestMode);
        componentIngestMode = IngestModeMapper.from(persistence);
        Assert.assertTrue(componentIngestMode instanceof UnitemporalSnapshot);

        unitemporalSnapshot = (UnitemporalSnapshot) componentIngestMode;
        Assert.assertEquals("DIGEST", unitemporalSnapshot.digestField());
        Assert.assertFalse(unitemporalSnapshot.partitioned());
        Assert.assertTrue(unitemporalSnapshot.transactionMilestoning() instanceof TransactionDateTime);
    }

    private Persistence getPersistence(IngestMode ingestMode)
    {
        Persistence persistence = new Persistence();
        BatchPersister persister = new BatchPersister();
        persister.ingestMode = ingestMode;
        persistence.persister = persister;
        return persistence;
    }

    private Dataset getMainDataset()
    {
        return DatasetDefinition.builder()
                .name("main")
                .schema(SchemaDefinition.builder()
                        .addFields(id)
                        .addFields(name)
                        .addFields(income).build()).build();
    }

    private Dataset getStagingDataset()
    {
        return DatasetDefinition.builder()
                .name("stage")
                .schema(SchemaDefinition.builder()
                        .addFields(id)
                        .addFields(name)
                        .addFields(income).build()).build();
    }

    private Dataset getMainDatasetWithoutPK()
    {
        return DatasetDefinition.builder()
                .name("main")
                .schema(SchemaDefinition.builder()
                        .addFields(name)
                        .addFields(income).build()).build();
    }

    private Dataset getStagingDatasetWithoutPK()
    {
        return DatasetDefinition.builder()
                .name("stage")
                .schema(SchemaDefinition.builder()
                        .addFields(name)
                        .addFields(income).build()).build();
    }

}
