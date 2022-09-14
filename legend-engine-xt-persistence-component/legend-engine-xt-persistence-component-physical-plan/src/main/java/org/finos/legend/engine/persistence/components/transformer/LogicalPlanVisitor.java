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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LogicalPlanVisitor<L extends LogicalPlanNode>
{
    VisitorResult visit(PhysicalPlanNode prev, L current, VisitorContext context);

    @Immutable
    @Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
    )
    interface VisitorContextAbstract
    {
        Optional<String> batchStartTimestampPattern();

        String batchStartTimestamp();

        Optional<String> batchEndTimestampPattern();

        Optional<String> batchIdPattern();

        List<Optimizer> optimizers();

        String quoteIdentifier();
    }

    class VisitorResult
    {
        private final PhysicalPlanNode returnValue;
        private final Collection<? extends LogicalPlanNode> nextItems;
        private final Collection<Operation> otherOps;

        public PhysicalPlanNode getReturnValue()
        {
            return returnValue;
        }

        public Collection<? extends LogicalPlanNode> getNextItems()
        {
            return nextItems;
        }

        public Collection<Operation> getOtherOps()
        {
            return otherOps;
        }

        public VisitorResult()
        {
            this.returnValue = null;
            this.nextItems = new ArrayList<>();
            this.otherOps = new ArrayList<>();
        }

        public VisitorResult(PhysicalPlanNode returnValue)
        {
            this.returnValue = returnValue;
            this.nextItems = new ArrayList<>();
            this.otherOps = new ArrayList<>();
        }

        public VisitorResult(PhysicalPlanNode returnValue, List<? extends LogicalPlanNode> nextItems)
        {
            this.returnValue = returnValue;
            this.nextItems = nextItems;
            this.otherOps = new ArrayList<>();
        }

        public VisitorResult(PhysicalPlanNode returnValue, LogicalPlanNode[] nextItems, Operation[] otherOps)
        {
            this.returnValue = returnValue;
            this.nextItems = Arrays.asList(nextItems);
            this.otherOps = Arrays.asList(otherOps);
        }
    }
}
