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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class TestFieldsToExclude extends MapperBaseTest
{

    @Test
    public void testFieldsToExcludeForNonTemporalSnapshot()
    {
        IngestMode ingestMode = getNonTemporalSnapshotDateTimeAuditing();
        Set<String> fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("AUDIT_TIME"));

        ingestMode = getNonTemporalSnapshotNoAuditing();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.isEmpty());
    }

    @Test
    public void testFieldsToExcludeForAppendOnly()
    {
        IngestMode ingestMode = getAppendOnlyNoAuditingNoFilteringDuplicates();
        Set<String> fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.isEmpty());

        ingestMode = getAppendOnlyNoAuditingWithFilteringDuplicates();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getAppendOnlyDatetimeAuditingNoFilteringDuplicates();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("AUDIT_TIME"));

        ingestMode = getAppendOnlyDatetimeAuditingWithFilteringDuplicates();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("AUDIT_TIME"));
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
    }

    @Test
    public void testFieldsToExcludeForNonTemporalDelta()
    {
        IngestMode ingestMode = getNontemporalDeltaNoAuditingNoMergeStrategy();
        Set<String> fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getNontemporalDeltaNoAuditingDeleteIndMergeStrategy();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getNontemporalDeltaWithAuditingNoMergeStrategy();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("AUDIT_TIME"));
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getNontemporalDeltaWithAuditingDeleteIndMergeStrategy();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("AUDIT_TIME"));
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
    }

    @Test
    public void testFieldsToExcludeForNonUnitemporalDelta()
    {
        IngestMode ingestMode = getUnitempDeltaNoMergeBatchIdBased();
        Set<String> fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getUnitempDeltaNoMergeBatchIdAndTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));

        ingestMode = getUnitempDeltaNoMergeTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));

        ingestMode = getUnitempDeltaDelIndMergeBatchIdBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getUnitempDeltaDelIndMergeBatchIdAndTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));

        ingestMode = getUnitempDeltaDelIndMergeTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));
    }

    @Test
    public void testFieldsToExcludeForNonUnitemporalSnapshot()
    {
        IngestMode ingestMode = getUnitemporalSnapshotBatchIdBased();
        Set<String> fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));

        ingestMode = getUnitemporalSnapshotBatchIdAndTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));

        ingestMode = getUnitemporalSnapshotTimeBased();
        fieldsToIgnore = ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
        Assert.assertTrue(fieldsToIgnore.contains("DIGEST"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_IN"));
        Assert.assertTrue(fieldsToIgnore.contains("BATCH_TIME_OUT"));
    }

}
