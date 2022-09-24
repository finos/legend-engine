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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IngestModeMapper
{
    public static final String DIGEST_FIELD_DEFAULT = "DIGEST";

    /*
    Mapper from Persistence model to IngestMode object
     */
    public static org.finos.legend.engine.persistence.components.ingestmode.IngestMode from(Persistence persistence, Dataset mainDataSet,
                                                                     Dataset stagingDataset) throws Exception
    {
        IngestMode ingestMode = getIngestMode(persistence);
        IngestModeType mode = getIngestModeName(ingestMode);
        String [] pkFields = getCommonPrimaryKeys(mainDataSet, stagingDataset, ingestMode);
        switch (mode)
        {
            case NontemporalSnapshot:
                return NontemporalSnapshotMapper.from((NontemporalSnapshot) ingestMode);
            case AppendOnly:
                return AppendOnlyMapper.from((AppendOnly) ingestMode, pkFields);
            case NontemporalDelta:
                return NontemporalDeltaMapper.from((NontemporalDelta) ingestMode, pkFields);
            case UnitemporalSnapshot:
                return UnitemporalSnapshotMapper.from((UnitemporalSnapshot) ingestMode, pkFields);
            case UnitemporalDelta:
                return UnitemporalDeltaMapper.from((UnitemporalDelta) ingestMode, pkFields);
            case BitemporalSnapshot:
                return BitemporalSnapshotMapper.from((BitemporalSnapshot) ingestMode, pkFields);
            case BitemporalDelta:
                return BitemporalDeltaMapper.from((BitemporalDelta) ingestMode, pkFields);
            default:
                throw new Exception("Unsupported Ingest mode");
        }
    }

    public static Set<String> getFieldsToIgnore(Persistence persistence) throws Exception
    {
        IngestMode ingestMode = getIngestMode(persistence);
        return ingestMode.accept(IngestModeVisitors.EXTRACT_FIELDS_TO_EXCLUDE);
    }

    public static boolean isTransactionMilestoningTimeBased(Persistence persistence) throws Exception
    {
        IngestMode ingestMode = getIngestMode(persistence);
        return ingestMode.accept(IngestModeVisitors.EXTRACT_TRANSACTION_MILESTONING_TIME_BASED);
    }

    private static IngestMode getIngestMode(Persistence persistence) throws Exception
    {
        Persister persister = persistence.persister;
        if (persister instanceof BatchPersister)
        {
            BatchPersister batchPersister = (BatchPersister) persister;
            return batchPersister.ingestMode;
        }
        throw new Exception("Only BatchPersister has Ingest Mode");
    }

    public static String [] getCommonPrimaryKeys(Dataset mainDataset, Dataset stagingDataset, IngestMode mode)
    {
        Set<String> pksInMainDataset = mainDataset.schema().fields().stream()
            .filter(field -> field.primaryKey()).map(field -> field.name()).collect(Collectors.toSet());
        Set<String> pksInStagingDataset = stagingDataset.schema().fields().stream()
            .filter(field -> field.primaryKey()).map(field -> field.name()).collect(Collectors.toSet());
        List<String> commonPks = pksInMainDataset.stream()
            .filter(field -> pksInStagingDataset.contains(field)).collect(Collectors.toList());
        if (mode != null && mode instanceof BitemporalDelta)
        {
            BitemporalDelta bitemporalDelta = (BitemporalDelta) mode;
            ValidityDerivation derivation = ((DateTimeValidityMilestoning) bitemporalDelta.validityMilestoning).derivation;
            if (derivation instanceof SourceSpecifiesFromDateTime)
            {
                SourceSpecifiesFromDateTime sourceSpecifiesFromDateTime = (SourceSpecifiesFromDateTime) derivation;
                String srcDateTimeFromField = sourceSpecifiesFromDateTime.sourceDateTimeFromField;
                commonPks = commonPks.stream().filter(field -> !(field.equals(srcDateTimeFromField))).collect(Collectors.toList());
            }
        }
        return commonPks.toArray(new String[commonPks.size()]);
    }

    private static IngestModeType getIngestModeName(IngestMode ingestMode)
    {
        String clazz = ingestMode.getClass().getSimpleName();
        IngestModeType ingestModeType = IngestModeType.valueOf(clazz);
        return ingestModeType;
    }

}