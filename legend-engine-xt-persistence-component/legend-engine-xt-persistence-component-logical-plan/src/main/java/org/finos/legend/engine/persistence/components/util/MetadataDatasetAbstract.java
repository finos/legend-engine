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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;

import java.util.Optional;

import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.DEFAULT_META_TABLE;
import static org.immutables.value.Value.Default;
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
public interface MetadataDatasetAbstract
{

    Optional<String> metadataDatasetDatabaseName();

    Optional<String> metadataDatasetGroupName();

    @Default
    default String metadataDatasetName()
    {
        return DEFAULT_META_TABLE;
    }

    @Default
    default String tableNameField()
    {
        return "table_name";
    }

    @Default
    default String batchStartTimeField()
    {
        return "batch_start_ts_utc";
    }

    @Default
    default String batchEndTimeField()
    {
        return "batch_end_ts_utc";
    }

    @Default
    default String batchStatusField()
    {
        return "batch_status";
    }

    @Default
    default String tableBatchIdField()
    {
        return "table_batch_id";
    }

    @Derived
    default Dataset get()
    {
        return DatasetDefinition.builder()
            .database(metadataDatasetDatabaseName())
            .group(metadataDatasetGroupName())
            .name(metadataDatasetName())
            .schema(SchemaDefinition.builder()
                .addFields(Field.builder().name(tableNameField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                .addFields(Field.builder().name(batchStartTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                .addFields(Field.builder().name(batchEndTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                .addFields(Field.builder().name(batchStatusField()).type(FieldType.of(DataType.VARCHAR, 32, null)).build())
                .addFields(Field.builder().name(tableBatchIdField()).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build())
                .build())
            .build();
    }
}
