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

package org.finos.legend.engine.persistence.components.sink;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.util.Optional;
import java.util.Set;

public interface Sink
{
    Set<Capability> capabilities();

    boolean supportsImplicitMapping(DataType source, DataType target);

    boolean supportsExplicitMapping(DataType source, DataType target);

    String quoteIdentifier();

    <L extends LogicalPlanNode> LogicalPlanVisitor<L> visitorForClass(Class<?> clazz);

    Field evolveFieldLength(Field evolveFrom, Field evolveTo);

    Optional<Integer> getEvolveToLength(String columnName, Optional<Integer> mainLength, Optional<Integer> stagingLength, DataType mainType, DataType stagingType, SchemaEvolution.DataTypeEvolutionType dataTypeEvolutionType, boolean failOnDecrement);

    Optional<Integer> getEvolveToScale(String columnName, Optional<Integer> mainScale, Optional<Integer> stagingScale, DataType mainType, DataType stagingType, SchemaEvolution.DataTypeEvolutionType dataTypeEvolutionType, boolean failOnDecrement);

    Field createNewField(Field evolveTo, Field evolveFrom, Optional<Integer> length, Optional<Integer> scale);
}
