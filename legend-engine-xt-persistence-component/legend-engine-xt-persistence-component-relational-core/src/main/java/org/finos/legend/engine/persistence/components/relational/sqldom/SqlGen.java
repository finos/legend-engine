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

package org.finos.legend.engine.persistence.components.relational.sqldom;

import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;

import java.util.List;

public interface SqlGen extends PhysicalPlanNode
{
    static <T extends SqlGen> void genSqlList(StringBuilder builder, List<T> items, String prefix, String separator) throws SqlDomException
    {
        if (items != null && items.size() > 0)
        {
            builder.append(prefix);
            for (int ctr = 0; ctr < items.size(); ctr++)
            {
                items.get(ctr).genSql(builder);
                if (ctr < (items.size() - 1))
                {
                    builder.append(separator);
                }
            }
        }
    }

    void genSql(StringBuilder builder);
}
