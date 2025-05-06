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


package org.finos.legend.engine.persistence.components.relational.duckdb.logicalplan.datasets;

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetProperties;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;


@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public interface DuckDBStagedFilesDatasetPropertiesAbstract extends StagedFilesDatasetProperties
{
    FileFormatType fileFormat();

    Map<String, Object> loadOptions();

    @Value.Check
    default void validate()
    {
        if (fileFormat() == FileFormatType.AVRO)
        {
            throw new IllegalStateException("Cannot build DuckDBStagedFilesDatasetProperties, AVRO file loading is not supported");
        }
    }

    default Optional<FileFormatType> fileFormatType()
    {
        return Optional.of(fileFormat());
    }
}
