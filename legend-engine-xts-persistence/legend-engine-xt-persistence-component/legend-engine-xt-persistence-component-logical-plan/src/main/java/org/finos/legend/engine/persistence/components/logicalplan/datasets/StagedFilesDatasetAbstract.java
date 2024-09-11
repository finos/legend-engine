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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public interface StagedFilesDatasetAbstract extends Dataset
{
    StagedFilesDatasetProperties stagedFilesDatasetProperties();

    SchemaDefinition schema();

    @Value.Default
    default String alias()
    {
        return "legend_persistence_stage";
    }

    Optional<DatasetAdditionalProperties> datasetAdditionalProperties();

    @Value.Derived
    default SchemaReference schemaReference()
    {
        return SchemaReference.builder()
                .addAllFieldValues(schema().fields()
                        .stream()
                        .map(f -> FieldValue.builder()
                                .fieldName(f.name())
                                .alias(f.fieldAlias())
                                .datasetRef(datasetReference())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Value.Derived
    default DatasetReference datasetReference()
    {
        return StagedFilesDatasetReference.builder()
            .properties(this.stagedFilesDatasetProperties())
            .addAllColumns(this.schema().fields())
            .alias(alias())
            .build();
    }
}
