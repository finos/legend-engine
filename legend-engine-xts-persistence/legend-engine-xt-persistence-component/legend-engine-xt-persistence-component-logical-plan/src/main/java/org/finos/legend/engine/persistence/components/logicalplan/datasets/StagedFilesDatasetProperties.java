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

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface StagedFilesDatasetProperties
{
    List<String> filePaths();

    List<String> filePatterns();

    @Value.Derived
    default boolean validationModeSupported()
    {
        return false;
    }

    @Value.Check
    default void validate()
    {
        if (filePatterns().size() > 0 && filePaths().size() > 0)
        {
            throw new IllegalStateException("Cannot build StagedFilesDatasetProperties, Only one out of filePatterns and filePaths should be provided");
        }
    }

    Optional<FileFormatType> fileFormatType();

    Optional<Boolean> disableAvroLogicalType();
}