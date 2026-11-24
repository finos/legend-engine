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
 * access width (semiwK), alias length (longprojW), and the number of root filter conjuncts copied into
 * aggregating view subqueries (conjK, with --aggviews).
 *
 * The SQL optimizer's cost grows with the size of the query tree it walks, so three shapes build that tree directly:
 * towerN stacks N wrapper selects, each re-filtering the one below, over --width columns and no joins; towerstarN puts the same
 * tower over a star of --star left joins; andK is one filter of K conjuncts over --width columns. Four more are taken from the
 * query mix the optimizer was slowest on: orandK (a filter of K OR-ed pairs of conjuncts, one flat predicate of tens of thousands of
 * nodes), caseK (K columns each a conditional over OR-ed comparisons), inK (a filter on membership of a list of K values),
 * winK (K nested olapGroupBy window layers) and trimW (a limited subselect of W columns of which the outer select keeps one, the
 * case the optimizer shrinks by dropping columns).
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
        return "###Pure\nfunction test::fetch(): Any[1]\n{\n  {|" + this.body() + this.sqlOptimizerFlags() + "}\n}\n";
    }

    private String sqlOptimizerFlags()
    {
        String flag = "meta::relational::sqlOptimizer::DisableSqlOptimization.";
        switch (this.config.sqlOptimizer)
        {
            case "all":
                return "";
            case "none":
                return "->meta::pure::executionPlan::featureFlag::withFeatureFlags([" + flag + "ALL])";
            case "nojoinrules":
                return "->meta::pure::executionPlan::featureFlag::withFeatureFlags([" + flag + "UNREAD_LEFT_JOIN_ELIMINATION, " + flag + "DUPLICATE_LEFT_JOIN_MERGE])";
            default:
                throw new IllegalArgumentException("Unknown --sqlopt value (all, nojoinrules, none): " + this.config.sqlOptimizer);
        }
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
        if (query.startsWith("trim"))
        {
            return this.restrictedWideSubSelect(Integer.parseInt(query.substring(4)), all);
        }
        if (query.startsWith("orand"))
        {
            return this.orOfConjunctPairs(Integer.parseInt(query.substring(5)), all);
        }
        if (query.startsWith("case"))
        {
            return this.conditionalColumns(Integer.parseInt(query.substring(4)), all);
        }
        if (query.startsWith("in"))
        {
            return this.membership(Integer.parseInt(query.substring(2)), all);
        }
        if (query.startsWith("win"))
        {
            return this.windowLayers(Integer.parseInt(query.substring(3)), all);
        }
        if (query.startsWith("towerstar"))
        {
            return this.tower(Integer.parseInt(query.substring(9)), this.starJoins(this.config.starJoins, all, false));
        }
        if (query.startsWith("tower"))
        {
            return this.tower(Integer.parseInt(query.substring(5)), this.widthProjection(all));
        }
        if (query.startsWith("and"))
        {
            return this.conjunctsOverColumns(Integer.parseInt(query.substring(3)), all);
        }
        if (query.startsWith("conj"))
        {
            return this.conjunctsOverSubQuery(Integer.parseInt(query.substring(4)), all);
        }
        if (query.startsWith("join"))
        {
            return this.joinChain(Integer.parseInt(query.substring(4)), all);
        }
        if (query.equals("hop") || query.equals("uhop"))
        {
            if (this.config.hops <= 0)
            {
                throw new IllegalArgumentException("The hop and uhop query shapes require --hops <count>");
            }
            String projection = all + "->project([x|$x.p0, x|$x.hop.p0], ['c0', 'h'])";
            return query.equals("hop") ? projection : projection + "->restrict(['c0'])";
        }
        if (query.startsWith("ujoin"))
        {
            return this.joinChain(Integer.parseInt(query.substring(5)), all) + "->restrict(['c0'])";
        }
        if (query.startsWith("ustar"))
        {
            return this.starJoins(Integer.parseInt(query.substring(5)), all, false) + "->restrict(['c0'])";
        }
        if (query.startsWith("star"))
        {
            return this.starJoins(Integer.parseInt(query.substring(4)), all, false);
        }
        if (query.startsWith("dup"))
        {
            return this.starJoins(Integer.parseInt(query.substring(3)), all, true);
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

    private String restrictedWideSubSelect(int width, String all)
    {
        int saved = this.config.width;
        this.config.width = width;
        try
        {
            return this.widthProjection(all) + "->limit(1000000)->restrict(['c0'])";
        }
        finally
        {
            this.config.width = saved;
        }
    }

    private String widthProjection(String all)
    {
        StringBuilder columns = new StringBuilder();
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < this.config.width; i++)
        {
            columns.append(i > 0 ? ", " : "").append("x|$x.p").append(i % 4);
            names.append(i > 0 ? ", " : "").append("'c").append(i).append("'");
        }
        return all + "->project([" + columns + "], [" + names + "])";
    }

    private String orOfConjunctPairs(int count, String all)
    {
        StringBuilder predicate = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            predicate.append(i > 0 ? " || " : "").append("($x.id == ").append(i).append(" && $x.p0 == 'a").append(i).append("')");
        }
        return this.widthProjection(all + "->filter(x|" + predicate + ")");
    }

    private String conditionalColumns(int count, String all)
    {
        StringBuilder columns = new StringBuilder("x|$x.p0");
        StringBuilder names = new StringBuilder("'c0'");
        for (int i = 1; i <= count; i++)
        {
            columns.append(", x|if($x.p0 == 'a").append(i).append("' || $x.p1 == 'b").append(i).append("' || $x.p2 == 'c").append(i).append("', |1, |0)");
            names.append(", 'k").append(i).append("'");
        }
        return all + "->project([" + columns + "], [" + names + "])";
    }

    private String membership(int count, String all)
    {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            values.append(i > 0 ? ", " : "").append(i);
        }
        return this.widthProjection(all + "->filter(x|$x.id->in([" + values + "]))");
    }

    private String windowLayers(int layers, String all)
    {
        StringBuilder query = new StringBuilder(all + "->project([x|$x.p0, x|$x.p3], ['c0', 'c3'])");
        for (int i = 0; i < layers; i++)
        {
            query.append("->olapGroupBy(['c0'], func('c3', y | $y->sum()), 'w").append(i).append("')");
        }
        return query.toString();
    }

    private String tower(int layers, String base)
    {
        StringBuilder query = new StringBuilder(base);
        for (int i = 0; i < layers; i++)
        {
            query.append("->limit(").append(1000000 + i).append(")->filter(r|$r.getString('c0') != 'v").append(i).append("')");
        }
        return query.toString();
    }

    private String conjunctsOverColumns(int count, String all)
    {
        StringBuilder conjuncts = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            conjuncts.append(i > 0 ? " && " : "").append("$x.id != ").append(i);
        }
        return this.widthProjection(all + "->filter(x|" + conjuncts + ")");
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

    private String starJoins(int count, String all, boolean duplicates)
    {
        if (this.config.starJoins < count)
        {
            throw new IllegalArgumentException("The starK, ustarK and dupK query shapes require --star of at least K");
        }
        StringBuilder columns = new StringBuilder("x|$x.p0");
        StringBuilder names = new StringBuilder("'c0'");
        for (int s = 1; s <= count; s++)
        {
            columns.append(", x|$x.s").append(s).append(".p0");
            names.append(", 's").append(s).append("'");
            if (duplicates)
            {
                columns.append(", x|$x.d").append(s).append(".p1");
                names.append(", 'd").append(s).append("'");
            }
        }
        return all + "->project([" + columns + "], [" + names + "])";
    }

    private String conjunctsOverSubQuery(int count, String all)
    {
        if (this.config.aggregateViews <= 0)
        {
            throw new IllegalArgumentException("The conjK query shape requires --aggviews <count>");
        }
        StringBuilder conjuncts = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            conjuncts.append(i > 0 ? " && " : "").append("$x.id != ").append(i);
        }
        StringBuilder columns = new StringBuilder("x|$x.p0");
        StringBuilder names = new StringBuilder("'c0'");
        for (int v = 0; v < this.config.aggregateViews; v++)
        {
            columns.append(", x|$x.g").append(v).append(".total");
            names.append(", 't").append(v).append("'");
        }
        return all + "->filter(x|" + conjuncts + ")->project([" + columns + "], [" + names + "])";
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
