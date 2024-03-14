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
//

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;

import java.util.Optional;

import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.DEFAULT_SINK_CLEAN_UP_AUDIT_TABLE;
import static org.immutables.value.Value.*;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface SinkCleanupAuditDatasetAbstract
{
    Optional<String> auditDatasetDatabaseName();

    Optional<String> auditDatasetGroupName();

    @Default
    default String auditDatasetName()
    {
        return DEFAULT_SINK_CLEAN_UP_AUDIT_TABLE;
    }

    @Default
    default String tableNameField()
    {
        return "table_name";
    }

    @Default
    default String requestedBy()
    {
        return "requested_by";
    }

    @Default
    default String executionTimeField()
    {
        return "execution_ts_utc";
    }

    @Default
    default String statusField()
    {
        return "status";
    }


    @Derived
    default Dataset get()
    {
        return DatasetDefinition.builder()
            .database(auditDatasetDatabaseName())
            .group(auditDatasetGroupName())
            .name(auditDatasetName())
            .schema(SchemaDefinition.builder()
                .addFields(Field.builder().name(tableNameField()).type(FieldType.of(DataType.VARCHAR, 255, null)).build())
                .addFields(Field.builder().name(executionTimeField()).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build())
                .addFields(Field.builder().name(statusField()).type(FieldType.of(DataType.VARCHAR, 32, null)).build())
                .addFields(Field.builder().name(requestedBy()).type(FieldType.of(DataType.VARCHAR, 64, null)).build())
                .build())
            .build();
    }
}
