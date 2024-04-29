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

public enum ErrorCategory
{
    TYPE_CONVERSION("Unable to type cast column"),
    CHECK_NULL_CONSTRAINT("Null values found in non-nullable column"),
    CHECK_OTHER_CONSTRAINT("Table constraints not fulfilled"),
    PARSING_ERROR("Unable to parse file"),
    FILE_NOT_FOUND("File not found in specified location"),
    UNKNOWN("Unknown error"),
    DUPLICATES("Duplicate rows found"),
    DUPLICATE_PRIMARY_KEYS("Multiple rows with duplicate primary keys found"),
    DATA_VERSION_ERROR("Data errors (same PK, same version but different data)");

    private final String defaultErrorMessage;

    ErrorCategory(String defaultErrorMessage)
    {
        this.defaultErrorMessage = defaultErrorMessage;
    }

    public String getDefaultErrorMessage()
    {
        return defaultErrorMessage;
    }
}