// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.perf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestQueryGenerator
{
    @Test
    public void testQueryHasNoFromClauseByDefault()
    {
        BenchConfig config = BenchConfig.parse(new String[]{"--query", "simple"});
        Assertions.assertFalse(new QueryGenerator(config).generate().contains("->from("));
        Assertions.assertEquals("simple@scale100/H2", config.workloadId());
    }

    @Test
    public void testFromClauseNamesTheMappingAndRuntime()
    {
        BenchConfig config = BenchConfig.parse(new String[]{"--query", "simple", "--from"});
        Assertions.assertTrue(config.fromClause);
        String query = new QueryGenerator(config).generate();
        Assertions.assertTrue(query.contains("->project([x|$x.p0, x|$x.p1, x|$x.p2], ['c0','c1','c2'])->from(test::Map, test::Runtime)}"), query);
        Assertions.assertEquals("simple@scale100+from/H2", config.workloadId());
    }

    @Test
    public void testRelationSortShape()
    {
        BenchConfig config = BenchConfig.parse(new String[]{"--query", "relsort"});
        String query = new QueryGenerator(config).generate();
        Assertions.assertTrue(query.contains("{|#>{test::DB.T0}#->sort([~p0->ascending(), ~p3->descending()])->limit(10)}"), query);
    }
}
