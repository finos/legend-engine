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
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a fixed set of workloads in one JVM and writes a results file describing both the numbers
 * and the machine they were measured on.
 *
 * With --rebase the results become the stored baseline. Without it the run is compared against the
 * baseline and the process exits non-zero when a deviation exceeds the allowed margin.
 *
 * Deviation is judged in both directions. A slower result is a regression; a faster one fails too,
 * because a baseline that still records the old cost silently permits a later change to give the
 * gain back. Failing forces the improvement into the baseline, so it becomes the level future runs
 * are held to. Pass --allow-improvement to only enforce the slower side.
 *
 * Two kinds of check run, because they tolerate machine differences differently:
 *   - canary ratios between two workloads measured in the same JVM, which cancel machine speed
 *     and catch a phase turning quadratic or exponential.
 *   - absolute phase medians, which a different machine moves several-fold on its own.
 * Both are compared only against an entry recorded on this same environment. A run on a machine the
 * baseline does not know reports its numbers and checks nothing, so record one there with --rebase,
 * or feed that run's results file back with --record.
 */
public class PerfSuite
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASELINE = "perf-baseline.json";
    private static final int SCHEMA_VERSION = 1;
    private static final double SQL_SIZE_MARGIN = 0.02;

    public static int run(String[] args) throws Exception
    {
        Map<String, String> options = options(args);
        String suiteName = options.getOrDefault("suite", "default");
        int iterations = Integer.parseInt(options.getOrDefault("iters", "5"));
        int warmup = Integer.parseInt(options.getOrDefault("warmup", "2"));
        String out = options.get("out");
        String baselinePath = options.getOrDefault("baseline", DEFAULT_BASELINE);
        boolean rebase = options.containsKey("rebase");
        double margin = Double.parseDouble(options.getOrDefault("margin", "0.5"));
        double canaryMargin = Double.parseDouble(options.getOrDefault("canary-margin", "0.3"));
        long minDelta = Long.parseLong(options.getOrDefault("min-delta-ms", "25"));
        boolean allowImprovement = options.containsKey("allow-improvement");
        int rounds = Integer.parseInt(options.getOrDefault("rounds", "1"));

        String record = options.get("record");
        if (record != null)
        {
            return record(new File(record), new File(baselinePath), baselinePath);
        }

        List<BenchConfig> workloads = workloads(suiteName, iterations, warmup);
        String only = options.get("only");
        if (only != null)
        {
            workloads.removeIf(config -> !config.workloadId().contains(only));
        }
        System.out.println("suite '" + suiteName + "': " + workloads.size() + " workloads, "
                + warmup + " warmup + " + iterations + " measured iterations each");

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("schemaVersion", 1);
        results.put("suite", suiteName);
        results.put("generatedAt", Instant.now().toString());
        results.put("environment", Environment.capture());

        List<List<Map<String, Object>>> roundResults = new ArrayList<>();
        for (int round = 1; round <= rounds; round++)
        {
            List<Map<String, Object>> workloadRound = new ArrayList<>();
            for (BenchConfig config : workloads)
            {
                System.out.println("  running " + config.workloadId() + (rounds > 1 ? " (round " + round + " of " + rounds + ")" : ""));
                workloadRound.add(new PipelineBench(config).quiet().measure());
            }
            roundResults.add(workloadRound);
        }
        List<Map<String, Object>> workloadResults = mergeRounds(roundResults);
        results.put("rounds", rounds);
        results.put("workloads", workloadResults);
        results.put("canaries", canaries(workloadResults));

        if (out != null)
        {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(out), results);
            System.out.println("results written to " + out);
        }

        File baselineFile = new File(baselinePath);
        Map<String, Object> baseline = baselineFile.exists() ? readBaseline(baselineFile) : null;

        if (rebase)
        {
            Map<String, Object> updated = recordInBaseline(baseline, results);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(baselineFile, updated);
            System.out.println("baseline recorded for " + Environment.fingerprint(PipelineBench.asMap(results.get("environment"))));
            System.out.println("environments now in " + baselinePath + ": " + environments(updated).keySet());
            return 0;
        }

        if (baseline == null)
        {
            System.out.println("no baseline at " + baselinePath + " - run once with --rebase to create one");
            printCanaries(results);
            return 0;
        }

        String fingerprint = Environment.fingerprint(PipelineBench.asMap(results.get("environment")));
        Map<String, Object> recorded = PipelineBench.asMap(environments(baseline).get(fingerprint));
        if (recorded.isEmpty())
        {
            System.out.println();
            System.out.println("no baseline recorded for this machine");
            System.out.println("  this machine: " + fingerprint);
            for (String known : environments(baseline).keySet())
            {
                System.out.println("  baseline has: " + known);
            }
            System.out.println("  timings from another machine are not comparable, so they are not being checked.");
            System.out.println("  run this suite with --rebase here to record an entry for it.");
            printCanaries(results);
            return compareSqlSizesWithAnyEntry(baseline, results, allowImprovement) ? 0 : 1;
        }
        return compare(recorded, results, margin, canaryMargin, minDelta, allowImprovement) ? 0 : 1;
    }

    private static boolean compareSqlSizesWithAnyEntry(Map<String, Object> baseline, Map<String, Object> results, boolean allowImprovement)
    {
        for (Map.Entry<String, Object> environment : environments(baseline).entrySet())
        {
            Map<String, Object> entry = PipelineBench.asMap(environment.getValue());
            if (entry.isEmpty() || workloadList(entry).stream().noneMatch(w -> w.get("sql") != null))
            {
                continue;
            }
            List<String> regressions = new ArrayList<>();
            List<String> improvements = new ArrayList<>();
            System.out.println();
            System.out.println("checking sql sizes against the entry for " + environment.getKey());
            checkSqlSizes(entry, results, allowImprovement, regressions, improvements);
            if (regressions.isEmpty() && improvements.isEmpty())
            {
                System.out.println("PASS - sql sizes match the baseline");
                return true;
            }
            System.out.println("FAIL - sql sizes differ from the baseline: " + (regressions.size() + improvements.size()) + " metric(s)");
            return false;
        }
        System.out.println("no baseline entry holds sql sizes yet");
        return true;
    }

    /**
     * Records a results file measured elsewhere - a CI run's uploaded artifact, most often - into
     * the baseline without re-running the suite here. A hosted runner cannot commit its own
     * baseline back, and hand-editing the file drifts from what the writer produces.
     */
    private static int record(File results, File baselineFile, String baselinePath) throws Exception
    {
        Map<String, Object> measured = MAPPER.readValue(results, Map.class);
        Map<String, Object> updated = recordInBaseline(baselineFile.exists() ? readBaseline(baselineFile) : null, measured);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(baselineFile, updated);
        System.out.println("baseline recorded for " + Environment.fingerprint(PipelineBench.asMap(measured.get("environment"))));
        System.out.println("environments now in " + baselinePath + ": " + environments(updated).keySet());
        return 0;
    }

    /**
     * A baseline holds one entry per machine, keyed by environment fingerprint, because neither
     * absolute timings nor ratios carry reliably across different hardware. Recording on one machine
     * leaves every other machine's entry untouched.
     */
    static Map<String, Object> readBaseline(File file) throws Exception
    {
        return MAPPER.readValue(file, Map.class);
    }

    static Map<String, Object> recordInBaseline(Map<String, Object> baseline, Map<String, Object> results)
    {
        Map<String, Object> updated = new LinkedHashMap<>();
        updated.put("schemaVersion", SCHEMA_VERSION);
        updated.put("suite", results.get("suite"));
        Map<String, Object> byEnvironment = new LinkedHashMap<>();
        if (baseline != null)
        {
            byEnvironment.putAll(environments(baseline));
        }
        byEnvironment.put(Environment.fingerprint(PipelineBench.asMap(results.get("environment"))), entry(results));
        updated.put("environments", byEnvironment);
        return updated;
    }

    private static Map<String, Object> entry(Map<String, Object> results)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("generatedAt", results.get("generatedAt"));
        entry.put("environment", results.get("environment"));
        entry.put("workloads", results.get("workloads"));
        entry.put("canaries", results.get("canaries"));
        return entry;
    }

    static Map<String, Object> environments(Map<String, Object> baseline)
    {
        return PipelineBench.asMap(baseline.get("environments"));
    }

    private static void printCanaries(Map<String, Object> results)
    {
        System.out.println();
        System.out.println("canary ratios measured on this run");
        for (Map.Entry<String, Double> entry : canaryValues(results).entrySet())
        {
            System.out.println(String.format("  %-28s %8.2f", entry.getKey(), entry.getValue()));
        }
    }

    static List<BenchConfig> workloads(String suiteName, int iterations, int warmup)
    {
        List<BenchConfig> configs = new ArrayList<>();
        if (suiteName.equals("default") || suiteName.equals("canary"))
        {
            configs.add(workload("simple", 100));
            configs.add(workload("simple", 1000));
            configs.add(workload("join8", 30));
            configs.add(workload("join16", 30));
            configs.add(workload("join2", 30));
            configs.add(workload("join10", 30));
            configs.add(unionWorkload("join2", 10, 10));
            configs.add(unionWorkload("join2", 10, 40));
            configs.add(relationWorkload("simple", 50));
            configs.add(relationWorkload("simple", 200));
            configs.add(workload("graph1", 30));
            configs.add(workload("graph4", 30));
            configs.add(aggregateViewWorkload("conj4", 30, 10));
            configs.add(aggregateViewWorkload("conj10", 30, 10));
            configs.add(starWorkload("dup10", 41, 40));
            configs.add(starWorkload("dup40", 41, 40));
            configs.add(starWorkload("star10", 41, 40));
            configs.add(starWorkload("star40", 41, 40));
            configs.add(hopWorkload("uhop", 41, 10));
            configs.add(hopWorkload("uhop", 41, 40));
            for (String shape : new String[] {"tower20", "tower80", "win5", "win20", "case50", "case200", "and100", "and300", "orand50", "orand200", "trim20", "trim200"})
            {
                configs.add(workload(shape, 30));
            }
            configs.add(starWorkload("towerstar8", 41, 40));
            configs.add(starWorkload("towerstar25", 41, 40));
            for (String shape : new String[] {"tower80", "win20", "case200", "and300", "orand200", "trim200"})
            {
                configs.add(withoutSqlOptimizer(workload(shape, 30)));
            }
            configs.add(withoutSqlOptimizer(starWorkload("towerstar25", 41, 40)));
        }
        else
        {
            throw new IllegalArgumentException("Unknown suite: " + suiteName);
        }
        for (BenchConfig config : configs)
        {
            config.iterations = iterations;
            config.warmup = warmup;
        }
        return configs;
    }

    /**
     * One result per workload: the first round's cold timings and SQL sizes, and each warm phase as the median over the rounds, which
     * damps the noise of a single run and leaves the first round to absorb the JIT warm-up.
     */
    static List<Map<String, Object>> mergeRounds(List<List<Map<String, Object>>> rounds)
    {
        List<Map<String, Object>> merged = new ArrayList<>();
        List<Map<String, Object>> first = rounds.get(0);
        for (int i = 0; i < first.size(); i++)
        {
            Map<String, Object> result = new LinkedHashMap<>(first.get(i));
            Map<String, Object> medians = new LinkedHashMap<>();
            for (String phase : PipelineBench.asMap(first.get(i).get("warmMedianMs")).keySet())
            {
                List<Long> values = new ArrayList<>();
                for (List<Map<String, Object>> round : rounds)
                {
                    Object value = PipelineBench.asMap(round.get(i).get("warmMedianMs")).get(phase);
                    if (value != null)
                    {
                        values.add(((Number) value).longValue());
                    }
                }
                values.sort(Long::compare);
                medians.put(phase, values.get(values.size() / 2));
            }
            result.put("warmMedianMs", medians);
            merged.add(result);
        }
        return merged;
    }

    private static BenchConfig workload(String query, int scale)
    {
        BenchConfig config = new BenchConfig();
        config.query = query;
        config.scale = scale;
        return config;
    }

    private static BenchConfig unionWorkload(String query, int scale, int unionSets)
    {
        BenchConfig config = workload(query, scale);
        config.unionSets = unionSets;
        return config;
    }

    private static BenchConfig aggregateViewWorkload(String query, int scale, int views)
    {
        BenchConfig config = workload(query, scale);
        config.aggregateViews = views;
        return config;
    }

    private static BenchConfig starWorkload(String query, int scale, int starJoins)
    {
        BenchConfig config = workload(query, scale);
        config.starJoins = starJoins;
        return config;
    }

    private static BenchConfig hopWorkload(String query, int scale, int hops)
    {
        BenchConfig config = workload(query, scale);
        config.hops = hops;
        return config;
    }

    private static BenchConfig withoutSqlOptimizer(BenchConfig config)
    {
        config.sqlOptimizer = "none";
        return config;
    }

    private static BenchConfig relationWorkload(String query, int scale)
    {
        BenchConfig config = workload(query, scale);
        config.relationFunction = true;
        return config;
    }

    /**
     * Ratios between two workloads in the same run. Each one guards a dimension where a phase could
     * silently change complexity class; the pair is chosen so the ratio grows if that happens.
     */
    static List<Map<String, Object>> canaries(List<Map<String, Object>> workloads)
    {
        List<Map<String, Object>> canaries = new ArrayList<>();
        canaries.add(ratio(workloads, "compileCliffJoinDepth", "compile", "join16@scale30/H2", "join8@scale30/H2",
                "exponential lambda compilation on optional navigation chains"));
        canaries.add(ratio(workloads, "compileVsModelSize", "compile", "simple@scale1000/H2", "simple@scale100/H2",
                "quadratic terms in mapping validation and table resolution"));
        canaries.add(ratio(workloads, "planPureUnionSets", "planPure", "join2@scale10+union40/H2", "join2@scale10+union10/H2",
                "quadratic pure-to-SQL in union set implementations"));
        canaries.add(ratio(workloads, "planPureJoinDepth", "planPure", "join10@scale30/H2", "join2@scale30/H2",
                "pure-to-SQL superlinearity in navigation depth"));
        canaries.add(ratio(workloads, "compileRelationMappings", "compile", "simple@scale200+relfunc/H2", "simple@scale50+relfunc/H2",
                "per-column lambda compilation in Relation mappings"));
        canaries.add(ratio(workloads, "javaBindGraphDepth", "javaBind", "graph4@scale30/H2", "graph1@scale30/H2",
                "platform binder growth in graph-fetch tree size"));
        canaries.add(ratio(workloads, "planPureFilterConjuncts", "planPure", "conj10@scale30+aggviews10/H2", "conj4@scale30+aggviews10/H2",
                "SQL optimization superlinearity in the number of filter conjuncts pushed into subqueries"));
        canaries.add(ratio(workloads, "planPureRepeatedLeftJoins", "planPure", "dup40@scale41+star40/H2", "dup10@scale41+star40/H2",
                "SQL optimization superlinearity in the number of repeated left joins merged"));
        canaries.add(ratio(workloads, "planPureLeftJoinFanOut", "planPure", "star40@scale41+star40/H2", "star10@scale41+star40/H2",
                "SQL optimization superlinearity in the number of read left joins checked for elimination"));
        canaries.add(ratio(workloads, "planPureUnreadJoinChain", "planPure", "uhop@scale41+hops40/H2", "uhop@scale41+hops10/H2",
                "SQL optimization superlinearity in the depth of an unread left-join chain"));
        canaries.add(ratio(workloads, "planPureWrapperTower", "planPure", "tower80@scale30/H2", "tower20@scale30/H2",
                "SQL optimization superlinearity in the number of nested wrapper selects"));
        canaries.add(ratio(workloads, "planPureWindowLayers", "planPure", "win20@scale30/H2", "win5@scale30/H2",
                "SQL optimization superlinearity in the number of nested window layers"));
        canaries.add(ratio(workloads, "planPureConditionalColumns", "planPure", "case200@scale30/H2", "case50@scale30/H2",
                "SQL optimization superlinearity in the number of conditional select-list columns"));
        canaries.add(ratio(workloads, "planPureConjunctList", "planPure", "and300@scale30/H2", "and100@scale30/H2",
                "SQL optimization superlinearity in the number of conjuncts of one filter"));
        canaries.add(ratio(workloads, "planPureFlatPredicate", "planPure", "orand200@scale30/H2", "orand50@scale30/H2",
                "SQL optimization superlinearity in the size of one flat OR-of-AND predicate"));
        canaries.add(ratio(workloads, "optimizerCostWrapperTower", "planPure", "tower80@scale30/H2", "tower80@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for a tower of wrapper selects it cannot shrink"));
        canaries.add(ratio(workloads, "optimizerCostWindowLayers", "planPure", "win20@scale30/H2", "win20@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for nested window layers"));
        canaries.add(ratio(workloads, "optimizerCostConditionalColumns", "planPure", "case200@scale30/H2", "case200@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for a select list of conditional columns"));
        canaries.add(ratio(workloads, "optimizerCostConjunctList", "planPure", "and300@scale30/H2", "and300@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for a filter of many conjuncts"));
        canaries.add(ratio(workloads, "optimizerCostFlatPredicate", "planPure", "orand200@scale30/H2", "orand200@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for one flat predicate of thousands of nodes"));
        canaries.add(ratio(workloads, "planPureWrapperTowerOverStar", "planPure", "towerstar25@scale41+star40/H2", "towerstar8@scale41+star40/H2",
                "SQL optimization superlinearity in the depth of a wrapper tower over a star of left joins"));
        canaries.add(ratio(workloads, "optimizerCostWrapperTowerOverStar", "planPure", "towerstar25@scale41+star40/H2", "towerstar25@scale41+star40+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for a deep wrapper tower over a star of left joins"));
        canaries.add(ratio(workloads, "planPureColumnTrimming", "planPure", "trim200@scale30/H2", "trim20@scale30/H2",
                "SQL optimization superlinearity in the number of columns trimmed from a subselect"));
        canaries.add(ratio(workloads, "optimizerCostColumnTrimming", "planPure", "trim200@scale30/H2", "trim200@scale30+sqlopt-none/H2",
                "what the SQL optimizer adds to plan generation for a wide subselect it trims"));
        canaries.removeIf(canary -> canary.get("value") == null);
        return canaries;
    }

    private static Map<String, Object> ratio(List<Map<String, Object>> workloads, String name, String phase,
                                             String numeratorId, String denominatorId, String guards)
    {
        Double numerator = phaseValue(workloads, numeratorId, phase);
        Double denominator = phaseValue(workloads, denominatorId, phase);
        Map<String, Object> canary = new LinkedHashMap<>();
        canary.put("name", name);
        canary.put("phase", phase);
        canary.put("numerator", numeratorId);
        canary.put("denominator", denominatorId);
        canary.put("guards", guards);
        canary.put("value", numerator == null || denominator == null || denominator == 0.0 ? null : round(numerator / denominator));
        return canary;
    }

    private static Double phaseValue(List<Map<String, Object>> workloads, String workloadId, String phase)
    {
        for (Map<String, Object> workload : workloads)
        {
            if (workloadId.equals(workload.get("workload")))
            {
                Object value = PipelineBench.asMap(workload.get("warmMedianMs")).get(phase);
                return value == null ? null : ((Number) value).doubleValue();
            }
        }
        return null;
    }

    /**
     * Holds each workload's SQL to the size recorded in the baseline. Sizes do not depend on the machine, so this also runs against
     * an entry recorded elsewhere.
     */
    static void checkSqlSizes(Map<String, Object> baseline, Map<String, Object> current, boolean allowImprovement,
                              List<String> regressions, List<String> improvements)
    {
        Map<String, Map<String, Object>> baselineWorkloads = byWorkload(baseline);
        System.out.println();
        System.out.println("sql sizes (enforced, margin " + percent(SQL_SIZE_MARGIN) + ", the same on every machine)");
        for (Map<String, Object> workload : workloadList(current))
        {
            String id = String.valueOf(workload.get("workload"));
            Map<String, Object> baselineWorkload = baselineWorkloads.get(id);
            if (baselineWorkload == null)
            {
                continue;
            }
            Map<String, Object> before = PipelineBench.asMap(baselineWorkload.get("sql"));
            Map<String, Object> now = PipelineBench.asMap(workload.get("sql"));
            for (Map.Entry<String, Object> entry : now.entrySet())
            {
                Object baselineValue = before.get(entry.getKey());
                if (baselineValue == null)
                {
                    continue;
                }
                long baselineSize = ((Number) baselineValue).longValue();
                long currentSize = ((Number) entry.getValue()).longValue();
                String message = id + " sql " + entry.getKey() + ": " + baselineSize + " -> " + currentSize
                        + " (expected " + Math.round(baselineSize * (1.0 - SQL_SIZE_MARGIN)) + " to " + Math.round(baselineSize * (1.0 + SQL_SIZE_MARGIN)) + ")";
                if (currentSize > baselineSize * (1.0 + SQL_SIZE_MARGIN))
                {
                    System.out.println("  LARGER " + message);
                    regressions.add(message);
                }
                else if (currentSize < baselineSize * (1.0 - SQL_SIZE_MARGIN) && !allowImprovement)
                {
                    System.out.println("  SMALLER " + message);
                    improvements.add(message);
                }
            }
        }
    }

    static boolean compare(Map<String, Object> baseline, Map<String, Object> current, double margin, double canaryMargin,
                           long minDelta, boolean allowImprovement)
    {
        List<String> regressions = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // The caller looked the entry up by fingerprint, so both sides describe the same machine.
        boolean sameMachine = true;

        System.out.println();
        System.out.println("machine   " + Environment.fingerprint(PipelineBench.asMap(current.get("environment"))));
        System.out.println("baseline  recorded " + baseline.get("generatedAt"));
        System.out.println("current   measured " + current.get("generatedAt"));
        System.out.println();

        Map<String, Double> baselineCanaries = canaryValues(baseline);
        Map<String, Double> currentCanaries = canaryValues(current);
        System.out.println("canary ratios (enforced, margin " + percent(canaryMargin) + ")");
        for (Map.Entry<String, Double> entry : currentCanaries.entrySet())
        {
            Double before = baselineCanaries.get(entry.getKey());
            Double now = entry.getValue();
            if (before == null || now == null)
            {
                notes.add("canary " + entry.getKey() + " missing from baseline");
                continue;
            }
            double upper = before * (1.0 + canaryMargin);
            double lower = before * (1.0 - canaryMargin);
            String verdict = "ok";
            if (now > upper)
            {
                verdict = "SLOWER";
                regressions.add("canary " + entry.getKey() + ": " + round(before) + " -> " + round(now) + ", above " + round(upper));
            }
            else if (now < lower && !allowImprovement)
            {
                verdict = "FASTER";
                improvements.add("canary " + entry.getKey() + ": " + round(before) + " -> " + round(now) + ", below " + round(lower));
            }
            System.out.println(String.format("  %-28s %8.2f -> %8.2f  (expected %.2f to %.2f) %s",
                    entry.getKey(), before, now, lower, upper, verdict));
        }

        System.out.println();
        System.out.println("phase medians (" + (sameMachine ? "enforced" : "advisory") + ", margin " + percent(margin)
                + ", ignoring deltas under " + minDelta + "ms)");
        Map<String, Map<String, Object>> baselineWorkloads = byWorkload(baseline);
        for (Map<String, Object> workload : workloadList(current))
        {
            String id = String.valueOf(workload.get("workload"));
            Map<String, Object> baselineWorkload = baselineWorkloads.get(id);
            if (baselineWorkload == null)
            {
                notes.add("workload " + id + " missing from baseline");
                continue;
            }
            Map<String, Object> before = PipelineBench.asMap(baselineWorkload.get("warmMedianMs"));
            Map<String, Object> now = PipelineBench.asMap(workload.get("warmMedianMs"));
            for (Map.Entry<String, Object> entry : now.entrySet())
            {
                Object baselineValue = before.get(entry.getKey());
                if (baselineValue == null)
                {
                    continue;
                }
                double baselineMs = ((Number) baselineValue).doubleValue();
                double currentMs = ((Number) entry.getValue()).doubleValue();
                double upper = baselineMs * (1.0 + margin);
                double lower = baselineMs * (1.0 - margin);
                boolean beyondNoise = Math.abs(currentMs - baselineMs) >= minDelta;
                if (!beyondNoise)
                {
                    continue;
                }
                String message = id + " " + entry.getKey() + ": " + (long) baselineMs + "ms -> " + (long) currentMs
                        + "ms (expected " + (long) lower + "ms to " + (long) upper + "ms)";
                if (currentMs > upper)
                {
                    System.out.println("  SLOWER " + message);
                    (sameMachine ? regressions : notes).add(message);
                }
                else if (currentMs < lower && !allowImprovement)
                {
                    System.out.println("  FASTER " + message);
                    (sameMachine ? improvements : notes).add(message);
                }
            }
        }

        checkSqlSizes(baseline, current, allowImprovement, regressions, improvements);

        System.out.println();
        for (String note : notes)
        {
            System.out.println("note: " + note);
        }
        if (regressions.isEmpty() && improvements.isEmpty())
        {
            System.out.println("PASS - within the allowed margin of the baseline");
            return true;
        }
        if (!regressions.isEmpty())
        {
            System.out.println("FAIL - " + regressions.size() + " regression(s):");
            for (String regression : regressions)
            {
                System.out.println("  " + regression);
            }
        }
        if (!improvements.isEmpty())
        {
            System.out.println("FAIL - " + improvements.size() + " improvement(s) not yet in the baseline:");
            for (String improvement : improvements)
            {
                System.out.println("  " + improvement);
            }
            System.out.println("  a faster result is only protected once it is recorded - rerun with --rebase and commit the baseline");
        }
        return false;
    }

    private static Map<String, Double> canaryValues(Map<String, Object> results)
    {
        Map<String, Double> values = new LinkedHashMap<>();
        Object canaries = results.get("canaries");
        if (canaries instanceof List)
        {
            for (Object entry : (List<?>) canaries)
            {
                Map<String, Object> canary = PipelineBench.asMap(entry);
                Object value = canary.get("value");
                if (value != null)
                {
                    values.put(String.valueOf(canary.get("name")), ((Number) value).doubleValue());
                }
            }
        }
        return values;
    }

    private static List<Map<String, Object>> workloadList(Map<String, Object> results)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        Object workloads = results.get("workloads");
        if (workloads instanceof List)
        {
            for (Object entry : (List<?>) workloads)
            {
                list.add(PipelineBench.asMap(entry));
            }
        }
        return list;
    }

    private static Map<String, Map<String, Object>> byWorkload(Map<String, Object> results)
    {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> workload : workloadList(results))
        {
            map.put(String.valueOf(workload.get("workload")), workload);
        }
        return map;
    }

    private static String percent(double margin)
    {
        return Math.round(margin * 100) + "%";
    }

    private static double round(double value)
    {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Map<String, String> options(String[] args)
    {
        Map<String, String> options = new LinkedHashMap<>();
        List<String> list = Arrays.asList(args);
        for (int i = 0; i < list.size(); i++)
        {
            String arg = list.get(i);
            if (arg.startsWith("--"))
            {
                String key = arg.substring(2);
                if (i + 1 < list.size() && !list.get(i + 1).startsWith("--"))
                {
                    options.put(key, list.get(++i));
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
