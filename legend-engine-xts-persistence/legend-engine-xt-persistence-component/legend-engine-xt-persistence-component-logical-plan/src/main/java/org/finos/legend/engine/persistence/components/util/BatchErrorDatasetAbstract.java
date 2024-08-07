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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
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
public interface BatchErrorDatasetAbstract
{
    Optional<String> database();

    Optional<String> group();

    String name();

    @Value.Default
    default String requestIdField()
    {
        return "ingest_request_id";
    }

    @Value.Default
    default String tableNameField()
    {
        return "table_name";
    }

    @Value.Default
    default String errorMessageField()
    {
        return "error_message";
    }

    @Value.Default
    default String errorCategoryField()
    {
        return "error_category";
    }

    @Value.Default
    default String createdOnField()
    {
        return "created_on";
    }

    @Value.Derived
    default Dataset get()
    {
        return DatasetDefinition.builder()
                .database(database())
                .group(group())
                .name(name())
                .schema(SchemaDefinition.builder()
                        .addFields(Field.builder().name(requestIdField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                        .addFields(Field.builder().name(tableNameField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                        .addFields(Field.builder().name(errorMessageField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                        .addFields(Field.builder().name(errorCategoryField()).type(FieldType.of(DataType.VARCHAR, 32, null)).build())
                        .addFields(Field.builder().name(createdOnField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                        .build())
                .build();
    }
}
