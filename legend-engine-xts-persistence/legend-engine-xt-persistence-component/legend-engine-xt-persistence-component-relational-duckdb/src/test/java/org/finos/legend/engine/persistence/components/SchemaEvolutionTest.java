// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.duckdb.DuckDBSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SchemaEvolutionTest extends IngestModeTest
{
    private final RelationalSink relationalSink = DuckDBSink.get();

    private final Field id = Field.builder().name("id").type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    private final Field employeesJsonCol = Field.builder().name("employees").type(FieldType.of(DataType.JSON, Optional.empty(), Optional.empty())).build();
    private final Field employeesVariantCol = Field.builder().name("employees").type(FieldType.of(DataType.VARIANT, Optional.empty(), Optional.empty())).build();

    private final SchemaDefinition baseTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(employeesJsonCol)
        .build();

    private final SchemaDefinition stagingTableImplicitDatatypeChange = SchemaDefinition.builder()
            .addFields(id)
            .addFields(employeesVariantCol)
        .build();

    @Test
    void testSchemaEvolutionCapabilityForJsonToVariant()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableImplicitDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }


}
