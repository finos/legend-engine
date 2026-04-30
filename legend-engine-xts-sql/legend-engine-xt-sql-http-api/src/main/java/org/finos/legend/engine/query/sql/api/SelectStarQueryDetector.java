// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.Join;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.SetOperation;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.protocol.sql.visitors.BaseNodeCollectorVisitor;

/**
 * Detects if a query is a SELECT * query that can use a pre-generated plan.
 * Uses the visitor pattern to traverse the SQL AST.
 * A query qualifies as SELECT * if:
 * - It selects all columns (SELECT *)                                                    
 * - It has exactly one service/table function source (no JOINs, no multiple tables)
 * - The table function has exactly one argument (the service name, no parameters) [Version 1]
 * - It has no WHERE, ORDER BY, GROUP BY, HAVING, LIMIT, or OFFSET clauses
 * - It's not a UNION/INTERSECT/EXCEPT operation
 */
public class SelectStarQueryDetector extends BaseNodeCollectorVisitor<Boolean>
{
    private SelectStarQueryDetector()
    {
        super(values -> values.stream().allMatch(v -> v), false);
    }

    public static boolean isSelectStar(Query query)
    {
        if (query == null)
        {
            return false;
        }
        return new SelectStarQueryDetector().visit(query);
    }
 
    @Override
    public Boolean visit(QuerySpecification spec)
    {
        // Disqualifying clauses
        if (spec.where != null ||
            spec.having != null ||
            spec.limit != null ||
            spec.offset != null ||
            (spec.groupBy != null && !spec.groupBy.isEmpty()) ||
            (spec.orderBy != null && !spec.orderBy.isEmpty()))
        {
            return false;
        }

        // Must have exactly one source in FROM clause
        if (spec.from == null || spec.from.size() != 1)
        {
            return false;
        }

        // Must be SELECT * (single AllColumns item with no prefix, not DISTINCT)
        if (spec.select == null ||
            spec.select.distinct ||
            spec.select.selectItems == null ||
            spec.select.selectItems.size() != 1)
        {
            return false;
        }

        // Validate the SELECT item and FROM source
        Boolean selectValid = collect(spec.select.selectItems);
        Boolean fromValid = collect(spec.from);

        return collate(selectValid, fromValid);
    }

    @Override
    public Boolean visit(AllColumns val)
    {
        return true;
    }

    @Override
    public Boolean visit(TableFunction val)
    {
        // For iteration 1, only optimize when the table function has exactly one argument
        // (the service name), e.g. service('/myService'). Parameterized services like
        // service('/myService', businessDate => '2015-01-01') go through the standard path.
        return val.functionCall != null
                && val.functionCall.arguments != null
                && val.functionCall.arguments.size() == 1;
    }

    @Override
    public Boolean visit(Join val)
    {
        return false;
    }

    @Override
    public Boolean visit(SetOperation val)
    {
        return false;
    }
}