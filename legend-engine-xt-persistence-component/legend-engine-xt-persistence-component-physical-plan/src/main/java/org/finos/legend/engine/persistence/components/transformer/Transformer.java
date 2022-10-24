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

package org.finos.legend.engine.persistence.components.transformer;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public interface Transformer<C extends PhysicalPlanNode, P extends PhysicalPlan<C>>
{
    @Immutable
    @Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
    )
    abstract class TransformOptionsAbstract
    {

        public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Default
        public Clock executionTimestampClock()
        {
            return Clock.systemUTC();
        }

        public abstract Optional<String> batchStartTimestampPattern();

        public abstract Optional<String> batchEndTimestampPattern();

        public abstract Optional<String> batchIdPattern();

        public abstract List<Optimizer> optimizers();

        @Default
        public String batchStartTimestampValue()
        {
            return LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER);
        }
    }

    TransformOptions options();

    P generatePhysicalPlan(LogicalPlan plan);
}
