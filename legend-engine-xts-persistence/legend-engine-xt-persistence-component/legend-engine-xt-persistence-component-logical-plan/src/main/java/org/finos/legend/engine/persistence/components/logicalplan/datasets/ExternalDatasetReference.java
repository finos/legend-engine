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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

public interface ExternalDatasetReference extends DatasetReference
{
    SchemaDefinition schema();

    @Override
    ExternalDatasetReference withName(String name);

    @Override
    ExternalDatasetReference withDatabase(String database);

    @Override
    ExternalDatasetReference withGroup(String group);

    @Override
    ExternalDatasetReference withAlias(String alias);

    ExternalDatasetReference withSchema(SchemaDefinition schemaDefinition);

    default Dataset getDatasetDefinition()
    {
        return DatasetDefinition.builder()
            .name(name().orElse(null))
            .database(database())
            .group(group())
            .alias(alias().orElse(name().orElse(null)))
            .schema(schema())
            .build();
    }
}
