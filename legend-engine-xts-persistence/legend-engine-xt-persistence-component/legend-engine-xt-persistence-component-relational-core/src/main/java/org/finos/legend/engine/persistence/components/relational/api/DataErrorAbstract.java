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

package org.finos.legend.engine.persistence.components.relational.api;

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
public interface DataErrorAbstract
{

    public static final String FILE_NAME = "file";
    public static final String LINE_NUMBER = "line_number";
    public static final String RECORD_NUMBER = "record_number";
    public static final String COLUMN_NAME = "column_name";
    public static final String CHARACTER_POSITION = "character_position";
    public static final String NUM_DUPLICATES = "num_duplicates";
    public static final String NUM_PK_DUPLICATES = "num_pk_duplicates";
    public static final String NUM_DATA_VERSION_ERRORS = "num_data_version_errors";

    String errorMessage();

    ErrorCategory errorCategory();

    Optional<String> errorRecord();

    Map<String, Object> errorDetails();
}