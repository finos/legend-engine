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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.core_external_query_sql_server_schema;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;

/**
 * Times the SQL flows that compile generated Pure through meta::legend::compile, over a generated
 * database of `scale` tables. Every tb() source compiles an accessor on the database it names, so
 * a scale sweep shows whether that cost follows the size of the database.
 *
 * See README.md for the phases.
 */
public class SqlBench
{
    static final String QUERY = "sql";
    private static final String[] PHASES = {"compile", "sqlQuery", "sqlJoin", "schema"};
    private static final int COLUMNS = 20;
    private static final int STATEMENTS = 10;

    private final BenchConfig config;
    private final PureModelContextData database;
    private final PureModelContextData databaseWithSqlFunctions;

    public SqlBench(BenchConfig config)
    {
        this.config = config;
        String database = database(config.scale);
        this.database = PureGrammarParser.newInstance().parseModel(database);
        this.databaseWithSqlFunctions = PureGrammarParser.newInstance().parseModel(database + sqlFunctions(config.scale));
    }

    public Map<String, Object> measure()
    {
        PureModel model = compile(this.database);
        Map<String, Object> cold = null;
        List<Map<String, Object>> measured = new ArrayList<>();
        for (int i = 0; i < this.config.warmup + this.config.iterations; i++)
        {
            Map<String, Object> row = this.runOnce(model);
            if (i == 0)
            {
                cold = row;
            }
            if (i >= this.config.warmup)
            {
                measured.add(row);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", this.config.workloadId());
        result.put("config", this.config.toMap());
        result.put("iterations", this.config.iterations);
        result.put("warmup", this.config.warmup);
        result.put("coldMs", PipelineBench.phaseValues(cold, PHASES));
        result.put("warmMedianMs", PipelineBench.medians(measured, PHASES));
        return result;
    }

    private Map<String, Object> runOnce(PureModel model)
    {
        Map<String, Object> row = new LinkedHashMap<>();

        long t0 = System.nanoTime();
        compile(this.databaseWithSqlFunctions);
        long t1 = System.nanoTime();
        row.put("compile", PipelineBench.millis(t0, t1));

        for (int i = 0; i < STATEMENTS; i++)
        {
            compileSql(query(i, this.config.scale), model);
        }
        long t2 = System.nanoTime();
        row.put("sqlQuery", PipelineBench.millis(t1, t2));

        for (int i = 0; i < STATEMENTS; i++)
        {
            compileSql(join(i, this.config.scale), model);
        }
        long t3 = System.nanoTime();
        row.put("sqlJoin", PipelineBench.millis(t2, t3));

        core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(
                core_external_query_sql_server_schema.Root_meta_external_query_sql_server_schema_transform_PackageableElement_MANY__AddressableRelationCollection_1_(model.getPackageableElements(), model.getExecutionSupport()),
                model.getExecutionSupport());
        row.put("schema", PipelineBench.millis(t3, System.nanoTime()));
        return row;
    }

    private static PureModel compile(PureModelContextData data)
    {
        return Compiler.compile(data, null, Identity.getAnonymousIdentity().getName());
    }

    private static void compileSql(String sql, PureModel model)
    {
        HelperValueSpecificationBuilder.buildLambda(PureGrammarParser.newInstance().parseLambda("#SQL{" + sql + "}#"), model.getContext());
    }

    private static String database(int tables)
    {
        StringBuilder builder = new StringBuilder("###Relational\nDatabase bench::DB\n(\n");
        for (int t = 0; t < tables; t++)
        {
            builder.append("  Table T").append(t).append(" (ID INTEGER PRIMARY KEY");
            for (int c = 1; c < COLUMNS; c++)
            {
                builder.append(", C").append(c).append((c % 2 == 0) ? " INTEGER" : " VARCHAR(200)");
            }
            builder.append(")\n");
        }
        return builder.append(")\n").toString();
    }

    private static String sqlFunctions(int tables)
    {
        StringBuilder builder = new StringBuilder("###Pure\n");
        for (int i = 0; i < STATEMENTS; i++)
        {
            builder.append("function bench::query").append(i).append("():Boolean[1]\n{\n  #SQL{").append(query(i, tables)).append("}#;\n  true;\n}\n");
        }
        return builder.toString();
    }

    private static String query(int i, int tables)
    {
        return "select ID, C1 from " + table(i, tables) + " where C1 = 'abc'";
    }

    private static String join(int i, int tables)
    {
        return "select a.ID, b.C2 from (select ID, C1 from " + table(i, tables) + ") as a join (select ID as BID, C2 from " + table(i + 1, tables) + ") as b on a.ID = b.BID";
    }

    private static String table(int i, int tables)
    {
        return "tb('bench::DB.T" + (i % tables) + "')";
    }
}
