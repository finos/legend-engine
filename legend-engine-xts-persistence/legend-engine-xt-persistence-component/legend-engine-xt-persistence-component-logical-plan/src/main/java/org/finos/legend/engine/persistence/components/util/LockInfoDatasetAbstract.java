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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.immutables.value.Value;
import java.util.Optional;

@Value.Immutable
@Value.Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface LockInfoDatasetAbstract
{

    Optional<String> database();

    Optional<String> group();

    String name();

    @Value.Default
    default String insertTimeField()
    {
        return "insert_ts_utc";
    }

    @Value.Default
    default String lastUsedTimeField()
    {
        return "last_used_ts_utc";
    }

    @Value.Default
    default String tableNameField()
    {
        return "table_name";
    }

    @Value.Derived
    default Dataset get()
    {
        return DatasetDefinition.builder()
            .database(database())
            .group(group())
            .name(name())
            .schema(SchemaDefinition.builder()
                .addFields(Field.builder().name(insertTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                .addFields(Field.builder().name(lastUsedTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                .addFields(Field.builder().name(tableNameField()).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).unique(true).build())
                .build())
            .build();
    }
}
