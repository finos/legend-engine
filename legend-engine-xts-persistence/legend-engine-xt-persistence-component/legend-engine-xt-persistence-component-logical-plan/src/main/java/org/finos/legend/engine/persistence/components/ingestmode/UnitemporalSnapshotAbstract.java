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

import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetData;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.EmptyDatasetHandling;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoned;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MergeDataVersionResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolverAbstract;
import org.immutables.value.Value;

import java.util.*;

import static org.immutables.value.Value.Derived;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface UnitemporalSnapshotAbstract extends IngestMode, TransactionMilestoned
{
    String digestField();

    @Override
    TransactionMilestoning transactionMilestoning();

    List<String> partitionFields();

    List<Map<String, Object>> partitionSpecList(); // [ {date: D1, Id: ID1, Name: N1}, {date: D2, Id: ID2, Name: N2}, ....]

    Map<String, Set<String>> partitionValuesByField(); // for Backward compatibility -- to be deprecated

    @Derived
    default boolean partitioned()
    {
        return !partitionFields().isEmpty();
    }

    @Value.Default
    default boolean derivePartitionSpec()
    {
        return false;
    }

    @Value.Default
    default Long maxPartitionSpecFilters()
    {
        return 1000L;
    }

    @Value.Default
    default EmptyDatasetHandling emptyDatasetHandling()
    {
        return DeleteTargetData.builder().build();
    }

    @Override
    default <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visitUnitemporalSnapshot(this);
    }

    @Value.Check
    default void validate()
    {

        if (!partitionValuesByField().isEmpty() && !partitionSpecList().isEmpty())
        {
            throw new IllegalStateException("Can not build UnitemporalSnapshot, Provide either partitionValuesByField or partitionSpecList, both not supported together");
        }

        // All the keys in partitionValuesByField must exactly match the fields in partitionFields
        if (!partitionValuesByField().isEmpty())
        {
            if (partitionFields().size() != partitionValuesByField().size())
            {
                throw new IllegalStateException("Can not build UnitemporalSnapshot, size of partitionValuesByField must be same as partitionFields");
            }
            for (String partitionKey: partitionValuesByField().keySet())
            {
                if (!partitionFields().contains(partitionKey))
                {
                    throw new IllegalStateException(String.format("Can not build UnitemporalSnapshot, partitionKey: [%s] not specified in partitionFields", partitionKey));
                }
            }
            int partitionKeysWithMoreThanOneValues = 0;
            for (Set<String> partitionValues: partitionValuesByField().values())
            {
                if (partitionValues.size() > 1)
                {
                    partitionKeysWithMoreThanOneValues++;
                }
            }
            if (partitionKeysWithMoreThanOneValues > 1)
            {
                throw new IllegalStateException(String.format("Can not build UnitemporalSnapshot, in partitionValuesByField at most one of the partition keys can have more than one value, all other partition keys must have exactly one value"));
            }
        }

        if (!partitionSpecList().isEmpty())
        {
            for (Map<String, Object> partitionSpec : partitionSpecList())
            {
                if (partitionFields().size() != partitionSpec.size())
                {
                    throw new IllegalStateException("Can not build UnitemporalSnapshot, size of each partitionSpec must be same as size of partitionFields");
                }
            }
            for (Map<String, Object> partitionSpec : partitionSpecList())
            {
                for (String partitionKey: partitionSpec.keySet())
                {
                    if (!partitionFields().contains(partitionKey))
                    {
                        throw new IllegalStateException(String.format("Can not build UnitemporalSnapshot, partitionKey: [%s] not specified in partitionSpec", partitionKey));
                    }
                }
            }
        }

        // Allowed Versioning Strategy - NoVersioning, MaxVersioining
        this.versioningStrategy().accept(new VersioningStrategyVisitor<Void>()
        {
            @Override
            public Void visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
            {
                return null;
            }

            @Override
            public Void visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
            {
                Optional<MergeDataVersionResolver> versionResolver = maxVersionStrategy.mergeDataVersionResolver();
                if (!versionResolver.isPresent())
                {
                    throw new IllegalStateException("Cannot build UnitemporalSnapshot, MergeDataVersionResolver is mandatory for MaxVersionStrategy");
                }
                if (!(versionResolver.orElseThrow(IllegalStateException::new) instanceof DigestBasedResolverAbstract))
                {
                    throw new IllegalStateException("Cannot build UnitemporalSnapshot, Only DIGEST_BASED VersioningResolver allowed for this ingest mode");
                }
                return null;
            }

            @Override
            public Void visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
            {
                throw new IllegalStateException("Cannot build UnitemporalSnapshot, AllVersionsStrategy not supported");
            }
        });

    }
}
