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

import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
            return appendOnly.digestGenStrategy().accept(DIGEST_GEN_STRATEGY_DIGEST_REQUIRED);
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

        @Override
        public Boolean visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return bulkLoad.digestGenStrategy().accept(DIGEST_GEN_STRATEGY_DIGEST_REQUIRED);
        }

        @Override
        public Boolean visitNoOp(NoOpAbstract noOpAbstract)
        {
            return false;
        }
    };

    public static final IngestModeVisitor<Optional<String>> EXTRACT_DIGEST_FIELD = new IngestModeVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return appendOnly.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_FROM_DIGEST_GEN_STRATEGY);
        }

        @Override
        public Optional<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return nontemporalDelta.digestField();
        }

        @Override
        public Optional<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return unitemporalSnapshot.digestField();
        }

        public Optional<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return unitemporalDelta.digestField();
        }

        @Override
        public Optional<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return bitemporalSnapshot.digestField();
        }

        @Override
        public Optional<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return bitemporalDelta.digestField();
        }

        @Override
        public Optional<String> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return bulkLoad.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_FROM_DIGEST_GEN_STRATEGY);
        }

        @Override
        public Optional<String> visitNoOp(NoOpAbstract noOpAbstract)
        {
            return Optional.empty();
        }
    };

    public static final IngestModeVisitor<Set<String>> META_FIELDS_TO_EXCLUDE_FROM_DIGEST = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            Set<String> metaFields = new HashSet<>();
            appendOnly.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_FROM_DIGEST_GEN_STRATEGY).ifPresent(metaFields::add);
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
            Set<String> metaFields = new HashSet<>();

            nontemporalDelta.digestField().ifPresent(metaFields::add);
            nontemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_INDICATOR_FIELD).ifPresent(metaFields::add);
            nontemporalDelta.dataSplitField().ifPresent(metaFields::add);

            return metaFields;
        }

        @Override
        public Set<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            if (unitemporalSnapshot.digestField().isPresent())
            {
                return Collections.singleton(unitemporalSnapshot.digestField().get());
            }
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            Set<String> metaFields = new HashSet<>();

            unitemporalDelta.digestField().ifPresent(metaFields::add);
            unitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_INDICATOR_FIELD).ifPresent(metaFields::add);
            unitemporalDelta.dataSplitField().ifPresent(metaFields::add);

            return metaFields;
        }

        @Override
        public Set<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            if (bitemporalSnapshot.digestField().isPresent())
            {
                return Collections.singleton(bitemporalSnapshot.digestField().get());
            }
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            Set<String> metaFields = new HashSet<>();

            bitemporalDelta.digestField().ifPresent(metaFields::add);
            bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_INDICATOR_FIELD).ifPresent(metaFields::add);
            bitemporalDelta.dataSplitField().ifPresent(metaFields::add);

            return metaFields;
        }

        @Override
        public Set<String> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            Set<String> metaFields = new HashSet<>();
            bulkLoad.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_FROM_DIGEST_GEN_STRATEGY).ifPresent(metaFields::add);
            return metaFields;
        }

        @Override
        public Set<String> visitNoOp(NoOpAbstract noOpAbstract)
        {
            return Collections.emptySet();
        }
    };

    public static final IngestModeVisitor<Boolean> NEED_TO_CHECK_STAGING_EMPTY = new IngestModeVisitor<Boolean>()
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
            return false;
        }

        @Override
        public Boolean visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return false;
        }

        @Override
        public Boolean visitNoOp(NoOpAbstract noOpAbstract)
        {
            return false;
        }
    };

    public static final IngestModeVisitor<List<OptimizationFilter>> RETRIEVE_OPTIMIZATION_FILTERS = new IngestModeVisitor<List<OptimizationFilter>>()
    {
        @Override
        public List<OptimizationFilter> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return unitemporalDelta.optimizationFilters();
        }

        @Override
        public List<OptimizationFilter> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return Collections.emptyList();
        }

        @Override
        public List<OptimizationFilter> visitNoOp(NoOpAbstract noOpAbstract)
        {
            return Collections.emptyList();
        }
    };

    private static final DigestGenStrategyVisitor<Boolean> DIGEST_GEN_STRATEGY_DIGEST_REQUIRED = new DigestGenStrategyVisitor<Boolean>()
    {
        @Override
        public Boolean visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
        {
            return false;
        }

        @Override
        public Boolean visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
        {
            return true;
        }

        @Override
        public Boolean visitUserProvidedDigestGenStrategy(UserProvidedDigestGenStrategyAbstract userProvidedDigestGenStrategy)
        {
            return true;
        }
    };

    public static final DigestGenStrategyVisitor<Optional<String>> EXTRACT_DIGEST_FIELD_FROM_DIGEST_GEN_STRATEGY = new DigestGenStrategyVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
        {
            return Optional.of(udfBasedDigestGenStrategy.digestField());
        }

        @Override
        public Optional<String> visitUserProvidedDigestGenStrategy(UserProvidedDigestGenStrategyAbstract userProvidedDigestGenStrategy)
        {
            return Optional.of(userProvidedDigestGenStrategy.digestField());
        }
    };

}
