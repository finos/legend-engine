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

package org.finos.legend.engine.persistence.components.schemaevolution;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSchemaEvolutionService;
import org.finos.legend.engine.persistence.components.relational.api.SchemaEvolutionServiceResult;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;

public class RelationalSchemaEvolutionServiceWithDerivedSchemaTest extends AbstractRelationalSchemaEvolutionServiceTest
{
    @Override
    protected SchemaEvolutionServiceResult evolve(Dataset mainDataset, Dataset stagingDataset, RelationalSchemaEvolutionService evolutionService)
    {
        return evolutionService.evolve(mainDataset.datasetReference(), stagingDataset.schema(), JdbcConnection.of(h2Sink.connection()));
    }
}