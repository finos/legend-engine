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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Index;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DatasetCaseConverterTest extends IngestModeTest
{

    @Test
    public void testDatasetCaseConverter()
    {
        DatasetCaseConverter converter = new DatasetCaseConverter();
        Dataset dataset = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaComplete)
            .build();

        // Convert everything to upper case
        Dataset convertedDataset = converter.applyCaseOnDataset(dataset, String::toUpperCase);
        assertConvertedDataset(convertedDataset, String::toUpperCase);

        // Convert everything back to lower case
        convertedDataset = converter.applyCaseOnDataset(convertedDataset, String::toLowerCase);
        assertConvertedDataset(convertedDataset, String::toLowerCase);
    }

    private void assertConvertedDataset(Dataset convertedDataset, Function<String, String> desiredCasing)
    {
        // Check table name, schema name, database name
        Assertions.assertEquals(desiredCasing.apply(mainTableName), convertedDataset.datasetReference().name().orElse(null));
        Assertions.assertEquals(null, convertedDataset.datasetReference().group().orElse(null));
        Assertions.assertEquals(desiredCasing.apply(mainDbName), convertedDataset.datasetReference().database().orElse(null));

        // Check column names
        Set<String> desiredColumnNames = new HashSet<>(Arrays.asList(desiredCasing.apply(id.name()), desiredCasing.apply(name.name()), desiredCasing.apply(amount.name()), desiredCasing.apply(bizDate.name())));
        Set<String> actualColumnNames = convertedDataset.schema().fields().stream().map(field -> field.name()).collect(Collectors.toSet());
        Assertions.assertEquals(desiredColumnNames, actualColumnNames);

        // Check index 1
        Index someIndex = convertedDataset.schema().indexes().stream().filter(index -> index.indexName().equals(desiredCasing.apply(someIndexName))).findFirst().orElse(null);
        if (someIndex == null)
        {
            Assertions.fail();
        }
        Set<String> desiredIndexColumnNames = new HashSet<>(Arrays.asList(desiredCasing.apply(id.name()), desiredCasing.apply(bizDate.name())));
        Set<String> actualIndexColumnNames = new HashSet<>(someIndex.columns());
        Assertions.assertEquals(desiredIndexColumnNames, actualIndexColumnNames);

        // Check index 2
        Index anotherIndex = convertedDataset.schema().indexes().stream().filter(index -> index.indexName().equals(desiredCasing.apply(anotherIndexName))).findFirst().orElse(null);
        if (anotherIndex == null)
        {
            Assertions.fail();
        }
        desiredIndexColumnNames = new HashSet<>(Arrays.asList(desiredCasing.apply(amount.name())));
        actualIndexColumnNames = new HashSet<>(anotherIndex.columns());
        Assertions.assertEquals(desiredIndexColumnNames, actualIndexColumnNames);

        // Check column store information
        Set<String> desiredColumnStoreKeys = new HashSet<>(Arrays.asList(desiredCasing.apply(name.name()), desiredCasing.apply(amount.name())));
        Set<String> actualColumnStoreKeys = convertedDataset.schema().columnStoreSpecification().get().columnStoreKeys().stream().map(key -> key.name()).collect(Collectors.toSet());
        Assertions.assertEquals(desiredColumnStoreKeys, actualColumnStoreKeys);

        // Check shard store information
        Set<String> desiredShardKeys = new HashSet<>(Arrays.asList(desiredCasing.apply(bizDate.name())));
        Set<String> actualShardKeys = convertedDataset.schema().shardSpecification().get().shardKeys().stream().map(key -> key.name()).collect(Collectors.toSet());
        Assertions.assertEquals(desiredShardKeys, actualShardKeys);
    }
}
