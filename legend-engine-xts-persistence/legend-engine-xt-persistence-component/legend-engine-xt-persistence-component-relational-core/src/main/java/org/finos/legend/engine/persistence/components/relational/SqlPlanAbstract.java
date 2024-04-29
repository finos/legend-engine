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

package org.finos.legend.engine.persistence.components.relational;

import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;

import java.util.List;
import java.util.stream.Collectors;

import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Parameter;
import static org.immutables.value.Value.Style;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface SqlPlanAbstract extends PhysicalPlan<SqlGen>
{
    @Override
    @Parameter(order = 0)
    List<SqlGen> ops();

    default List<String> getSqlList()
    {
        return ops()
            .stream()
            .map(op ->
            {
                StringBuilder builder = new StringBuilder();
                op.genSql(builder);
                return builder.toString();
            })
            .collect(Collectors.toList());
    }

    default String getSql()
    {
        return getSqlList()
            .stream()
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    }
}
