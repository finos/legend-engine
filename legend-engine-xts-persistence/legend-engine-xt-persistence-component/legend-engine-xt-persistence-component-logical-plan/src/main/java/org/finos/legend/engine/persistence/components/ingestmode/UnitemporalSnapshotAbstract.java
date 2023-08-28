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
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    Map<String, Set<String>> partitionValuesByField();

    @Derived
    default boolean partitioned()
    {
        return !partitionFields().isEmpty();
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
        }
    }
}
