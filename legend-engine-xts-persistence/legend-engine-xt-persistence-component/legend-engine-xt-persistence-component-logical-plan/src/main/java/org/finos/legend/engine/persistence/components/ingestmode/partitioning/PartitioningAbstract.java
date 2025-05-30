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

package org.finos.legend.engine.persistence.components.ingestmode.partitioning;

import org.finos.legend.engine.persistence.components.ingestmode.deletestrategy.DeleteStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.deletestrategy.*;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value.Immutable
@Value.Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface PartitioningAbstract extends PartitioningStrategy
{
    @Override
    default boolean isPartitioned()
    {
        return true;
    }

    @Value.Default
    default DeleteStrategy deleteStrategy()
    {
        return DeleteUpdatedStrategy.builder().build();
    }

    List<String> partitionFields();

    List<Map<String, Object>> partitionSpecList(); // [ {date: D1, Id: ID1, Name: N1}, {date: D2, Id: ID2, Name: N2}, ....]

    Map<String, Set<String>> partitionValuesByField(); // for Backward compatibility -- to be deprecated

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

    @Value.Check
    default void validate()
    {
        if (partitionFields().isEmpty())
        {
            throw new IllegalStateException("Can not build Partitioning, partitionFields cannot be empty");
        }

        if (!partitionValuesByField().isEmpty() && !partitionSpecList().isEmpty())
        {
            throw new IllegalStateException("Can not build Partitioning, Provide either partitionValuesByField or partitionSpecList, both not supported together");
        }

        // All the keys in partitionValuesByField must exactly match the fields in partitionFields
        if (!partitionValuesByField().isEmpty())
        {
            if (partitionFields().size() != partitionValuesByField().size())
            {
                throw new IllegalStateException("Can not build Partitioning, size of partitionValuesByField must be same as partitionFields");
            }
            for (String partitionKey : partitionValuesByField().keySet())
            {
                if (!partitionFields().contains(partitionKey))
                {
                    throw new IllegalStateException(String.format("Can not build Partitioning, partitionKey: [%s] not specified in partitionFields", partitionKey));
                }
            }
            int partitionKeysWithMoreThanOneValues = 0;
            for (Set<String> partitionValues : partitionValuesByField().values())
            {
                if (partitionValues.size() > 1)
                {
                    partitionKeysWithMoreThanOneValues++;
                }
            }
            if (partitionKeysWithMoreThanOneValues > 1)
            {
                throw new IllegalStateException(String.format("Can not build Partitioning, in partitionValuesByField at most one of the partition keys can have more than one value, all other partition keys must have exactly one value"));
            }
        }

        // All the keys in partitionSpecList must exactly match the fields in partitionFields
        if (!partitionSpecList().isEmpty())
        {
            for (Map<String, Object> partitionSpec : partitionSpecList())
            {
                if (partitionFields().size() != partitionSpec.size())
                {
                    throw new IllegalStateException("Can not build Partitioning, size of each partitionSpec must be same as size of partitionFields");
                }
            }
            for (Map<String, Object> partitionSpec : partitionSpecList())
            {
                for (String partitionKey : partitionSpec.keySet())
                {
                    if (partitionFields().stream().noneMatch(partitionKey::equalsIgnoreCase))
                    {
                        throw new IllegalStateException(String.format("Can not build Partitioning, partitionKey: [%s] not specified in partitionSpec", partitionKey));
                    }
                }
            }
        }
    }

    @Override
    default <T> T accept(PartitioningStrategyVisitor<T> visitor)
    {
        return visitor.visitPartitioning(this);
    }
}