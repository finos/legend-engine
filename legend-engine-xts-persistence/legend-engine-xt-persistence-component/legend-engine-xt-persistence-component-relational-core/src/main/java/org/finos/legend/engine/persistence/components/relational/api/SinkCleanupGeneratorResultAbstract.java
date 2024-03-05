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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.SqlPlanAbstract;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class SinkCleanupGeneratorResultAbstract
{

    public abstract SqlPlan preActionsSqlPlan();

    public abstract Optional<SqlPlan> initializeLockSqlPlan();

    public abstract Optional<SqlPlan> acquireLockSqlPlan();

    public abstract SqlPlan cleanupSqlPlan();


    public List<String> preActionsSql()
    {
        return preActionsSqlPlan().getSqlList();
    }

    public List<String> cleanupSql()
    {
        return cleanupSqlPlan().getSqlList();
    }

    public List<String> initializeLockSql()
    {
        return initializeLockSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> acquireLockSql()
    {
        return acquireLockSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }


}
