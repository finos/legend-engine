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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.util.GlobalTracer;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.engine.pure.preeval.PreevalStatistics;

public final class PreevalComparison
{
    private static final String SPAN = "preval";
    private static final List<String> IMPLEMENTATIONS = Arrays.asList("PURE", "JAVA");

    private PreevalComparison()
    {
    }

    public static int run(String[] args) throws Exception
    {
        Map<String, String> options = options(args);
        int iterations = Integer.parseInt(options.getOrDefault("iters", "10"));
        int warmup = Integer.parseInt(options.getOrDefault("warmup", "3"));
        double margin = Double.parseDouble(options.getOrDefault("margin", "0.10"));
        long minDelta = Long.parseLong(options.getOrDefault("min-delta-ms", "10"));
        String out = options.get("out");

        SpanTimer timer = new SpanTimer();
        if (!GlobalTracer.registerIfAbsent(timer))
        {
            throw new IllegalStateException("A tracer is already registered, so preval spans cannot be timed");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try
        {
            for (BenchConfig config : workloads(iterations, warmup))
            {
                System.out.println("  comparing " + config.workloadId());
                results.add(compare(new PipelineBench(config).quiet(), config, timer));
            }
        }
        finally
        {
            System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("suite", "preeval");
        report.put("generatedAt", Instant.now().toString());
        report.put("environment", Environment.capture());
        report.put("workloads", results);
        report.put("totals", totals(results));
        List<String> regressions = regressions(results, margin, minDelta);
        report.put("regressions", regressions);
        print(results, PipelineBench.asMap(report.get("totals")), regressions);
        if (out != null)
        {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(out), report);
            System.out.println("results written to " + out);
        }
        return regressions.isEmpty() ? 0 : 1;
    }

    static List<BenchConfig> workloads(int iterations, int warmup)
    {
        List<BenchConfig> configs = new ArrayList<>();
        configs.add(directWorkload("simple", 100));
        configs.add(directWorkload("simple", 1000));
        configs.add(directWorkload("join8", 30));
        configs.add(directWorkload("agg4", 30));
        configs.add(directWorkload("groupBy", 30));
        BenchConfig relation = directWorkload("simple", 50);
        relation.relationFunction = true;
        configs.add(relation);
        BenchConfig milestoned = directWorkload("simple", 30);
        milestoned.milestoned = true;
        configs.add(milestoned);
        configs.add(directWorkload("graph4", 30));
        BenchConfig view = directWorkload("m2mview", 30);
        view.modelToModel = true;
        configs.add(view);
        configs.add(workload("relsort", 30));
        for (BenchConfig config : configs)
        {
            config.iterations = iterations;
            config.warmup = warmup;
        }
        return configs;
    }

    static List<String> regressions(List<Map<String, Object>> workloads, double margin, long minDeltaMs)
    {
        List<String> found = new ArrayList<>();
        for (Map<String, Object> workload : workloads)
        {
            Map<String, Object> pure = PipelineBench.asMap(workload.get("PURE"));
            Map<String, Object> java = PipelineBench.asMap(workload.get("JAVA"));
            for (String measure : Arrays.asList("planPureMs", "prevalMs"))
            {
                double before = ((Number) pure.get(measure)).doubleValue();
                double after = ((Number) java.get(measure)).doubleValue();
                if (after - before > minDeltaMs && after > before * (1 + margin))
                {
                    found.add(workload.get("workload") + ": " + measure.replace("Ms", "") + " " + before + " ms under PURE, " + after + " ms under JAVA");
                }
            }
        }
        return found;
    }

    static void checkSpans(String workload, long pureSpans, long javaSpans)
    {
        if (pureSpans == 0 && javaSpans == 0)
        {
            throw new IllegalStateException(workload + ": no preval spans were recorded, so there is nothing to compare");
        }
        if (pureSpans != javaSpans)
        {
            throw new IllegalStateException(workload + ": preval ran a different number of times (PURE " + pureSpans + ", JAVA " + javaSpans + ")");
        }
    }

    private static Map<String, Object> compare(PipelineBench bench, BenchConfig config, SpanTimer timer)
    {
        Map<String, List<Long>> planPure = new LinkedHashMap<>();
        Map<String, List<Long>> prevalNanos = new LinkedHashMap<>();
        Map<String, Long> spans = new LinkedHashMap<>();
        for (String implementation : IMPLEMENTATIONS)
        {
            planPure.put(implementation, new ArrayList<>());
            prevalNanos.put(implementation, new ArrayList<>());
        }
        for (int i = 0; i < config.warmup + config.iterations; i++)
        {
            List<String> order = i % 2 == 0 ? IMPLEMENTATIONS : Arrays.asList("JAVA", "PURE");
            for (String implementation : order)
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, implementation);
                timer.reset();
                Map<String, Object> row = bench.runOnce();
                if (i >= config.warmup)
                {
                    planPure.get(implementation).add((Long) row.get("planPure"));
                    prevalNanos.get(implementation).add(timer.totalNanos(SPAN));
                    spans.put(implementation, timer.count(SPAN));
                }
            }
        }
        checkSpans(config.workloadId(), spans.get("PURE"), spans.get("JAVA"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", config.workloadId());
        for (String implementation : IMPLEMENTATIONS)
        {
            Map<String, Object> side = new LinkedHashMap<>();
            side.put("planPureMs", median(planPure.get(implementation)));
            side.put("prevalMs", round(median(prevalNanos.get(implementation)) / 1_000_000.0));
            side.put("prevalSpans", spans.get(implementation));
            result.put(implementation, side);
        }
        double pureMs = ((Number) PipelineBench.asMap(result.get("PURE")).get("prevalMs")).doubleValue();
        double javaMs = ((Number) PipelineBench.asMap(result.get("JAVA")).get("prevalMs")).doubleValue();
        result.put("prevalSpeedup", javaMs == 0 ? null : round(pureMs / javaMs));
        result.put("statistics", statistics(bench));
        return result;
    }

    private static Map<String, Object> statistics(PipelineBench bench)
    {
        System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, "JAVA");
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.reset();
        try
        {
            bench.runOnce();
            Map<String, Object> statistics = new LinkedHashMap<>();
            statistics.put("rules", PreevalStatistics.ruleCounts());
            statistics.put("hookCalls", PreevalStatistics.hookCallCounts());
            Map<String, Object> hookMs = new LinkedHashMap<>();
            PreevalStatistics.hookNanos().forEach((hook, nanos) -> hookMs.put(hook, round(nanos / 1_000_000.0)));
            statistics.put("hookMs", hookMs);
            return statistics;
        }
        finally
        {
            System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
            PreevalStatistics.reset();
        }
    }

    private static Map<String, Object> totals(List<Map<String, Object>> results)
    {
        double pure = 0;
        double java = 0;
        for (Map<String, Object> result : results)
        {
            pure += ((Number) PipelineBench.asMap(result.get("PURE")).get("prevalMs")).doubleValue();
            java += ((Number) PipelineBench.asMap(result.get("JAVA")).get("prevalMs")).doubleValue();
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("prevalMsPure", round(pure));
        totals.put("prevalMsJava", round(java));
        totals.put("prevalSpeedup", java == 0 ? null : round(pure / java));
        return totals;
    }

    private static void print(List<Map<String, Object>> results, Map<String, Object> totals, List<String> regressions)
    {
        System.out.println(String.format("%-36s %10s %10s %10s %10s %8s", "workload", "plan PURE", "plan JAVA", "prev PURE", "prev JAVA", "speedup"));
        for (Map<String, Object> result : results)
        {
            Map<String, Object> pure = PipelineBench.asMap(result.get("PURE"));
            Map<String, Object> java = PipelineBench.asMap(result.get("JAVA"));
            System.out.println(String.format("%-36s %10s %10s %10s %10s %8s", result.get("workload"), pure.get("planPureMs"), java.get("planPureMs"), pure.get("prevalMs"), java.get("prevalMs"), result.get("prevalSpeedup")));
        }
        System.out.println("total preval ms: PURE " + totals.get("prevalMsPure") + ", JAVA " + totals.get("prevalMsJava") + ", speedup " + totals.get("prevalSpeedup"));
        if (regressions.isEmpty())
        {
            System.out.println("no regressions");
        }
        else
        {
            System.out.println("REGRESSIONS:");
            regressions.forEach(r -> System.out.println("  " + r));
        }
    }

    private static BenchConfig workload(String query, int scale)
    {
        BenchConfig config = new BenchConfig();
        config.query = query;
        config.scale = scale;
        return config;
    }

    private static BenchConfig directWorkload(String query, int scale)
    {
        BenchConfig config = workload(query, scale);
        config.directPreval = true;
        return config;
    }

    private static long median(List<Long> values)
    {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compare);
        return sorted.get(sorted.size() / 2);
    }

    private static double round(double value)
    {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Map<String, String> options(String[] args)
    {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("--"))
            {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--"))
                {
                    options.put(key, args[++i]);
                }
                else
                {
                    options.put(key, "true");
                }
            }
        }
        return options;
    }
}
