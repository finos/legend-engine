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

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.h2.tools.Server;

/**
 * Times each stage of the query pipeline separately - grammar parse, compile, lambda build,
 * extension resolution, plan generation in Pure, Java platform binding, plan serialization,
 * plan deserialization and (optionally) execution against in-memory H2.
 *
 * The first iteration of a run is the cold one: it pays class loading and JIT for the generated
 * Pure code base. Steady-state cost is the median of the iterations after warmup.
 *
 * See README.md for the workload matrix and the canary ratios worth watching in CI.
 */
public class PipelineBench
{
    private static final String[] PHASES = {"parse", "compile", "lambda", "extensions", "planPure", "javaBind", "serialize", "deserialize", "execute"};
    private static final String CLIENT_VERSION = "vX_X_X";

    private final BenchConfig config;
    private final String grammar;
    private PlanExecutor planExecutor;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private boolean verbose = true;

    public PipelineBench(BenchConfig config) throws Exception
    {
        this.config = config;
        this.grammar = config.fileList == null ? this.generateGrammar() : this.readGrammar();
    }

    public static void main(String[] args) throws Exception
    {
        for (String arg : args)
        {
            if (arg.equals("--suite"))
            {
                System.exit(PerfSuite.run(args));
            }
        }
        BenchConfig config = BenchConfig.parse(args);
        PipelineBench bench = new PipelineBench(config);
        bench.run();
        System.exit(0);
    }

    public PipelineBench quiet()
    {
        this.verbose = false;
        return this;
    }

    private String generateGrammar()
    {
        return new ModelGenerator(this.config).generate() + new QueryGenerator(this.config).generate();
    }

    private String readGrammar() throws Exception
    {
        StringBuilder b = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(this.config.fileList)))
        {
            if (!line.trim().isEmpty())
            {
                b.append(new String(Files.readAllBytes(Paths.get(line.trim())), StandardCharsets.UTF_8)).append("\n");
            }
        }
        b.append("###Runtime\nRuntime test::Runtime\n{\n  mappings:\n  [\n    ").append(this.config.mappingPath)
                .append("\n  ];\n  connections:\n  [\n    ").append(this.config.storePath)
                .append(":\n    [\n      c1: #{\n        RelationalDatabaseConnection\n        {\n")
                .append(ModelGenerator.connectionBody(this.config.dbType))
                .append("        }\n      }#\n    ]\n  ];\n}\n");
        String body = new String(Files.readAllBytes(Paths.get(this.config.queryFile)), StandardCharsets.UTF_8);
        b.append("###Pure\nfunction test::fetch(): Any[1]\n{\n  {|").append(body.trim()).append("}\n}\n");
        return b.toString();
    }

    public Map<String, Object> measure() throws Exception
    {
        Server server = null;
        Map<String, Object> cold = null;
        List<Map<String, Object>> measured = new ArrayList<>();
        try
        {
            if (this.config.execute)
            {
                server = AlloyH2Server.startServer();
                loadTestData(server.getPort());
                this.planExecutor = PlanExecutor.newPlanExecutor(Relational.build(server.getPort()));
            }

            if (this.config.pause)
            {
                System.out.println("JVM=" + ManagementFactory.getRuntimeMXBean().getName() + " - attach a profiler to that pid, then press ENTER");
                System.in.read();
            }

            for (int i = 0; i < this.config.warmup + this.config.iterations; i++)
            {
                boolean warming = i < this.config.warmup;
                Map<String, Object> row = this.runOnce();
                row.put("iteration", warming ? -1 : i - this.config.warmup);
                if (i == 0)
                {
                    cold = row;
                }
                if (!warming)
                {
                    measured.add(row);
                }
                if (this.verbose)
                {
                    System.out.println(format(row, warming ? "warmup" : "iter " + (i - this.config.warmup)));
                }
            }
        }
        finally
        {
            if (server != null)
            {
                server.shutdown();
                server.stop();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", this.config.workloadId());
        result.put("config", this.config.toMap());
        result.put("iterations", this.config.iterations);
        result.put("warmup", this.config.warmup);
        result.put("coldMs", phaseValues(cold));
        result.put("warmMedianMs", medians(measured));
        result.put("planJsonChars", cold == null ? null : cold.get("planJsonChars"));
        this.rows = measured;
        return result;
    }

    public void run() throws Exception
    {
        System.out.println(this.config.describe() + " grammarChars=" + this.grammar.length());
        Map<String, Object> result = this.measure();
        System.out.println("--- medians in ms over " + this.rows.size() + " measured iterations ---");
        Map<String, Object> warm = asMap(result.get("warmMedianMs"));
        for (String phase : PHASES)
        {
            if (warm.containsKey(phase))
            {
                System.out.println(String.format("%-12s %7s", phase, warm.get(phase)));
            }
        }
        if (this.config.csv != null)
        {
            this.writeCsv(this.rows);
        }
    }

    static Map<String, Object> phaseValues(Map<String, Object> row)
    {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null)
        {
            return values;
        }
        for (String phase : PHASES)
        {
            if (row.containsKey(phase))
            {
                values.put(phase, row.get(phase));
            }
        }
        return values;
    }

    static Map<String, Object> medians(List<Map<String, Object>> rows)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String phase : PHASES)
        {
            List<Long> values = new ArrayList<>();
            for (Map<String, Object> row : rows)
            {
                if (row.containsKey(phase))
                {
                    values.add((Long) row.get(phase));
                }
            }
            if (!values.isEmpty())
            {
                values.sort(Long::compare);
                result.put(phase, values.get(values.size() / 2));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value)
    {
        return value == null ? new LinkedHashMap<>() : (Map<String, Object>) value;
    }

    private Map<String, Object> runOnce()
    {
        Map<String, Object> row = new LinkedHashMap<>();

        long t0 = System.nanoTime();
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(this.grammar);
        long t1 = System.nanoTime();
        row.put("parse", millis(t0, t1));

        PureModel pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());
        long t2 = System.nanoTime();
        row.put("compile", millis(t1, t2));

        Function fetch = contextData.getElementsOfType(Function.class).stream()
                .filter(f -> f.name != null && f.name.startsWith("fetch"))
                .findFirst()
                .orElseGet(() -> contextData.getElementsOfType(Function.class).get(0));
        LambdaFunction lambda = (LambdaFunction) fetch.body.get(0);
        FunctionDefinition<?> definition = HelperValueSpecificationBuilder.buildLambda(lambda.body, lambda.parameters, pureModel.getContext());
        Mapping mapping = pureModel.getMapping(this.config.mappingPath);
        Root_meta_core_runtime_Runtime runtime = pureModel.getRuntime("test::Runtime");
        long t3 = System.nanoTime();
        row.put("lambda", millis(t2, t3));

        RichIterable<? extends Root_meta_pure_extension_Extension> extensions =
                Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport());
        long t4 = System.nanoTime();
        row.put("extensions", millis(t3, t4));

        Root_meta_pure_executionPlan_ExecutionPlan purePlan =
                PlanGenerator.generateExecutionPlanAsPure(definition, mapping, runtime, null, pureModel, null, null, extensions);
        long t5 = System.nanoTime();
        row.put("planPure", millis(t4, t5));

        Root_meta_pure_executionPlan_ExecutionPlan boundPlan = PlanPlatform.JAVA.bindPlan(purePlan, null, pureModel, extensions);
        long t6 = System.nanoTime();
        row.put("javaBind", millis(t5, t6));

        String json = PlanGenerator.serializeToJSON(boundPlan, CLIENT_VERSION, pureModel, extensions, LegendPlanTransformers.transformers);
        long t7 = System.nanoTime();
        row.put("serialize", millis(t6, t7));
        row.put("planJsonChars", json.length());

        SingleExecutionPlan plan = PlanGenerator.stringToPlan(json);
        long t8 = System.nanoTime();
        row.put("deserialize", millis(t7, t8));

        if (this.config.dumpPlan != null)
        {
            writePlan(this.config.dumpPlan, json);
        }

        if (this.config.execute && this.planExecutor != null)
        {
            RelationalResult result = (RelationalResult) this.planExecutor.execute(plan, Maps.mutable.empty(), (String) null, Identity.getAnonymousIdentity());
            String output = result.flush(new RelationalResultToJsonDefaultSerializer(result));
            row.put("execute", millis(t8, System.nanoTime()));
            row.put("resultChars", output.length());
        }
        return row;
    }

    private void writeCsv(List<Map<String, Object>> rows) throws Exception
    {
        boolean fresh = !new File(this.config.csv).exists();
        try (PrintWriter writer = new PrintWriter(new FileWriter(this.config.csv, true)))
        {
            if (fresh)
            {
                writer.println("dbType,scale,query,union,semi,includes,milestoned,relfunc,nextMult,iteration," + String.join(",", PHASES) + ",planJsonChars");
            }
            for (Map<String, Object> row : rows)
            {
                StringBuilder line = new StringBuilder();
                line.append(this.config.dbType).append(",").append(this.config.scale).append(",").append(this.config.query).append(",")
                        .append(this.config.unionSets).append(",").append(this.config.semiDepth).append(",").append(this.config.includes).append(",")
                        .append(this.config.milestoned).append(",").append(this.config.relationFunction).append(",")
                        .append(this.config.nextMultiplicity).append(",").append(row.get("iteration"));
                for (String phase : PHASES)
                {
                    line.append(",").append(row.getOrDefault(phase, ""));
                }
                line.append(",").append(row.getOrDefault("planJsonChars", ""));
                writer.println(line);
            }
        }
        System.out.println("CSV appended to " + this.config.csv);
    }

    private static String format(Map<String, Object> row, String label)
    {
        StringBuilder line = new StringBuilder(label + ":");
        for (String phase : PHASES)
        {
            if (row.containsKey(phase))
            {
                line.append(" ").append(phase).append("=").append(row.get(phase)).append("ms");
            }
        }
        return line.toString();
    }

    private static void writePlan(String path, String json)
    {
        try
        {
            Files.write(Paths.get(path), json.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to write plan to " + path, e);
        }
    }

    private static long millis(long from, long to)
    {
        return (to - from) / 1_000_000L;
    }

    private static void loadTestData(int port) throws Exception
    {
        RelationalExecutor executor = TestExecutionScope.buildTestExecutor(port);
        Connection connection = executor.getConnectionManager().getTestDatabaseConnection();
        try (Statement statement = connection.createStatement())
        {
            for (int i = 0; i < 6; i++)
            {
                statement.execute("Drop table if exists T" + i + ";");
                statement.execute("Create Table T" + i + " (id INT NOT NULL, p0 VARCHAR(100), p1 VARCHAR(100), p2 VARCHAR(100), p3 INT, fk INT, PRIMARY KEY(id));");
                for (int row = 1; row <= 5; row++)
                {
                    statement.execute("insert into T" + i + " (id,p0,p1,p2,p3,fk) values (" + row + ",'abc','v1','v2'," + (row * 10) + "," + row + ");");
                }
            }
        }
    }
}
