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

/**
 * Builds the query function for a named shape. Shapes vary one dimension each so that a sweep
 * isolates how a phase reacts to it: navigation depth (joinK), projection width (projW),
 * aggregate count (aggK), graph-fetch tree depth (graphK), semi-structured path depth (semiK) and
 * access width (semiwK), and alias length (longprojW).
 */
public class QueryGenerator
{
    private final BenchConfig config;

    public QueryGenerator(BenchConfig config)
    {
        this.config = config;
    }

    public String generate()
    {
        return "###Pure\nfunction test::fetch(): Any[1]\n{\n  {|" + this.body() + "}\n}\n";
    }

    private String body()
    {
        String query = this.config.query;
        String all = this.config.milestoned ? "test::domain::C0.all(%2020-01-02T00:00:00)" : "test::domain::C0.all()";

        if (query.equals("simple"))
        {
            return all + "->filter(x|$x.p0 == 'abc')->project([x|$x.p0, x|$x.p1, x|$x.p2], ['c0','c1','c2'])";
        }
        if (query.equals("m2mview"))
        {
            String tree = "#{test::view::V0{q0,q1,q2}}#";
            return "test::view::V0.all()->graphFetch(" + tree + ")->serialize(" + tree + ")";
        }
        if (query.equals("variantrel"))
        {
            return "#>{test::DB.T0}#->meta::pure::functions::relation::extend(~v:r|$r.p0->toOne()->fromJson()->get('k')->to(@String))"
                    + "->meta::pure::functions::relation::select(~[v])";
        }
        if (query.startsWith("graph"))
        {
            return this.graphFetch(Integer.parseInt(query.substring(5)), all);
        }
        if (query.startsWith("semijoin"))
        {
            return this.semiWithJoins(Integer.parseInt(query.substring(8)), all);
        }
        if (query.startsWith("semiw"))
        {
            return this.semiWide(Integer.parseInt(query.substring(5)), all);
        }
        if (query.startsWith("semi"))
        {
            return this.semiDeep(Integer.parseInt(query.substring(4)), all);
        }
        if (query.startsWith("join"))
        {
            return this.joinChain(Integer.parseInt(query.substring(4)), all);
        }
        if (query.startsWith("longproj"))
        {
            return this.projection(Integer.parseInt(query.substring(8)), all, true);
        }
        if (query.startsWith("proj"))
        {
            return this.projection(Integer.parseInt(query.substring(4)), all, false);
        }
        if (query.startsWith("agg"))
        {
            return this.aggregates(Integer.parseInt(query.substring(3)), all);
        }
        if (query.equals("groupBy"))
        {
            return all + "->groupBy([x|$x.p0], [agg(x|$x.p3, y|$y->sum())], ['k','s'])";
        }
        throw new IllegalArgumentException("Unknown query shape: " + query);
    }

    private String joinChain(int depth, String all)
    {
        StringBuilder columns = new StringBuilder("x|$x.p0");
        StringBuilder names = new StringBuilder("'c0'");
        StringBuilder navigation = new StringBuilder("$x");
        for (int i = 1; i <= depth; i++)
        {
            navigation.append(".next");
            columns.append(", x|").append(navigation).append(".p0");
            names.append(", 'c").append(i).append("'");
        }
        return all + "->filter(x|$x.p0 == 'abc')->project([" + columns + "], [" + names + "])";
    }

    private String projection(int width, String all, boolean longAliases)
    {
        StringBuilder pad = new StringBuilder();
        if (longAliases)
        {
            for (int i = 0; i < 30; i++)
            {
                pad.append("verylongalias");
            }
        }
        StringBuilder columns = new StringBuilder();
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < width; i++)
        {
            if (i > 0)
            {
                columns.append(", ");
                names.append(", ");
            }
            columns.append("x|$x.p").append(i % 4);
            names.append("'").append(pad).append(i).append("'");
        }
        return all + "->project([" + columns + "], [" + names + "])";
    }

    private String aggregates(int count, String all)
    {
        String[] functions = {"sum", "max", "min", "count", "average"};
        StringBuilder aggregations = new StringBuilder();
        StringBuilder names = new StringBuilder("'k'");
        for (int i = 0; i < count; i++)
        {
            if (i > 0)
            {
                aggregations.append(", ");
            }
            aggregations.append("agg(x|$x.p3, y|$y->").append(functions[i % functions.length]).append("())");
            names.append(", 'a").append(i).append("'");
        }
        return all + "->groupBy([x|$x.p0], [" + aggregations + "], [" + names + "])";
    }

    private String graphFetch(int depth, String all)
    {
        StringBuilder tree = new StringBuilder("p0,p1");
        for (int i = 0; i < depth; i++)
        {
            tree.insert(0, "p0,p1,next{");
        }
        for (int i = 0; i < depth; i++)
        {
            tree.append("}");
        }
        String spec = "#{test::domain::C0{" + tree + "}}#";
        return all + "->graphFetch(" + spec + ")->serialize(" + spec + ")";
    }

    private String semiDeep(int depth, String all)
    {
        StringBuilder navigation = new StringBuilder("$x.det");
        for (int d = 0; d < depth; d++)
        {
            navigation.append(".child");
        }
        return all + "->project([col(x|" + navigation + ".v, 'c0')])";
    }

    private String semiWide(int width, String all)
    {
        return all + "->project([" + this.semiColumns(width, 0) + "])";
    }

    private String semiWithJoins(int width, String all)
    {
        String joins = "col(x|$x.p0, 'j0'), col(x|$x.next.p0, 'j1'), col(x|$x.next.next.p0, 'j2')";
        return all + "->project([" + joins + ", " + this.semiColumns(width, 0) + "])";
    }

    private String semiColumns(int width, int startIndex)
    {
        if (this.config.semiDepth <= 0)
        {
            throw new IllegalArgumentException("Semi-structured query shapes require --semi <depth>");
        }
        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < width; i++)
        {
            if (i > 0)
            {
                columns.append(", ");
            }
            StringBuilder navigation = new StringBuilder("$x.det");
            for (int d = 0; d < i % this.config.semiDepth; d++)
            {
                navigation.append(".child");
            }
            columns.append("col(x|").append(navigation).append(i % 2 == 0 ? ".v" : ".w").append(", 'c").append(startIndex + i).append("')");
        }
        return columns.toString();
    }
}
