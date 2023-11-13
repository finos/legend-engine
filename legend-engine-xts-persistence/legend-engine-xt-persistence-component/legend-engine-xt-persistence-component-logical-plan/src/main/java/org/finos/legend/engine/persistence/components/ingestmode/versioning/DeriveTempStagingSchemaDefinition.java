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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.ingestmode.deduplication.DatasetDeduplicationHandler.COUNT;

public class DeriveTempStagingSchemaDefinition implements VersioningStrategyVisitor<SchemaDefinition>
{
    DeduplicationStrategy deduplicationStrategy;
    private SchemaDefinition.Builder schemaDefBuilder;
    private List<Field> schemaFields;

    boolean anyPKInStaging;

    public DeriveTempStagingSchemaDefinition(SchemaDefinition stagingSchema, DeduplicationStrategy deduplicationStrategy)
    {
        this.deduplicationStrategy = deduplicationStrategy;
        this.schemaDefBuilder = SchemaDefinition.builder()
                .addAllIndexes(stagingSchema.indexes())
                .shardSpecification(stagingSchema.shardSpecification())
                .columnStoreSpecification(stagingSchema.columnStoreSpecification());
        anyPKInStaging = stagingSchema.fields().stream().anyMatch(field -> field.primaryKey());
        this.schemaFields = new ArrayList<>(stagingSchema.fields());
        Optional<Field> fieldToAddForDedup = deduplicationStrategy.accept(GET_FIELD_NEEDED_FOR_DEDUPLICATION);
        fieldToAddForDedup.ifPresent(this.schemaFields::add);
    }

    @Override
    public SchemaDefinition visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {

        return schemaDefBuilder.addAllFields(schemaFields).build();
    }

    @Override
    public SchemaDefinition visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        return schemaDefBuilder.addAllFields(schemaFields).build();
    }

    @Override
    public SchemaDefinition visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
    {
        if (allVersionsStrategyAbstract.performStageVersioning())
        {
            Field dataSplit = Field.builder().name(allVersionsStrategyAbstract.dataSplitFieldName())
                    .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
                    .primaryKey(anyPKInStaging)
                    .build();
            schemaFields.add(dataSplit);
        }
        return schemaDefBuilder.addAllFields(schemaFields).build();
    }

    public static final DeduplicationStrategyVisitor<Optional<Field>> GET_FIELD_NEEDED_FOR_DEDUPLICATION = new DeduplicationStrategyVisitor<Optional<Field>>()
    {
        @Override
        public Optional<Field> visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
        {
            return Optional.empty();
        }

        @Override
        public Optional<Field> visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
        {
            return getDedupField();
        }

        @Override
        public Optional<Field> visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
        {
            return getDedupField();
        }

        private Optional<Field> getDedupField()
        {
            Field count = Field.builder().name(COUNT)
                    .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
                    .primaryKey(false)
                    .build();
            return Optional.of(count);
        }

    };
}