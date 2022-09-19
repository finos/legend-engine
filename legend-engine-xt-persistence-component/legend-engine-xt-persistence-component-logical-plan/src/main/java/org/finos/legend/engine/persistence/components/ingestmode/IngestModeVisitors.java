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

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class IngestModeVisitors
{
    private IngestModeVisitors()
    {
    }

    public static final IngestModeVisitor<Boolean> DIGEST_REQUIRED = new IngestModeVisitor<Boolean>()
    {
        @Override
        public Boolean visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return appendOnly.deduplicationStrategy().accept(DeduplicationStrategyVisitors.IS_FILTER_DUPLICATES);
        }

        @Override
        public Boolean visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return false;
        }

        @Override
        public Boolean visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return true;
        }
    };

    public static final IngestModeVisitor<Optional<String>> EXTRACT_DIGEST_FIELD = new IngestModeVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return appendOnly.digestField();
        }

        @Override
        public Optional<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return Optional.of(nontemporalDelta.digestField());
        }

        @Override
        public Optional<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return Optional.of(unitemporalSnapshot.digestField());
        }

        @Override
        public Optional<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return Optional.of(unitemporalDelta.digestField());
        }

        @Override
        public Optional<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return Optional.of(bitemporalSnapshot.digestField());
        }

        @Override
        public Optional<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return Optional.of(bitemporalDelta.digestField());
        }
    };

    public static final IngestModeVisitor<Set<String>> META_FIELDS_TO_EXCLUDE_FROM_DIGEST = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            Set<String> metaFields = new HashSet<>();
            appendOnly.digestField().ifPresent(metaFields::add);
            appendOnly.dataSplitField().ifPresent(metaFields::add);
            return metaFields;
        }

        @Override
        public Set<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return Collections.singleton(nontemporalDelta.digestField());
        }

        @Override
        public Set<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return Collections.singleton(unitemporalSnapshot.digestField());
        }

        @Override
        public Set<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            Set<String> metaFields = new HashSet<>();

            metaFields.add(unitemporalDelta.digestField());
            unitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
            unitemporalDelta.dataSplitField().ifPresent(metaFields::add);

            return metaFields;
        }

        @Override
        public Set<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return Collections.singleton(bitemporalSnapshot.digestField());
        }

        @Override
        public Set<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            Set<String> metaFields = new HashSet<>();

            metaFields.add(bitemporalDelta.digestField());
            bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
            bitemporalDelta.dataSplitField().ifPresent(metaFields::add);

            return metaFields;
        }
    };

    public static final IngestModeVisitor<Boolean> IS_INGEST_MODE_TEMPORAL = new IngestModeVisitor<Boolean>()
    {
        @Override
        public Boolean visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return false;
        }

        @Override
        public Boolean visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return false;
        }

        @Override
        public Boolean visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return false;
        }

        @Override
        public Boolean visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return true;
        }
    };
}
