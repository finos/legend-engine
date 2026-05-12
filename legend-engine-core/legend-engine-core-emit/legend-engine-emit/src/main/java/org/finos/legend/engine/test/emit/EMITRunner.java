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

package org.finos.legend.engine.test.emit;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.test.emit.EMITTasks.ArtifactCandidate;
import org.finos.legend.engine.test.emit.EMITTasks.LegacyMappingTestCandidate;
import org.finos.legend.engine.test.emit.EMITTasks.LegacyServiceTestCandidate;
import org.finos.legend.engine.test.emit.EMITTasks.ModelGenResult;
import org.finos.legend.engine.test.emit.EMITTasks.ParseResult;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.finos.legend.engine.test.runner.mapping.RichMappingTestResult;
import org.finos.legend.engine.test.runner.service.RichServiceTestResult;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.finos.legend.engine.testable.model.RunTestsResult;
import org.finos.legend.engine.testable.model.RunTestsTestableInput;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full EMIT pipeline (init → parse → compile → model gen →
 * file gen → test execution → plan generation) and aggregates per-phase
 * results into an {@link EMITResult}. The actual per-item work is delegated
 * to {@link EMITTasks} so that the JUnit integration can drive the same
 * primitives at a finer granularity without duplicating logic.
 */
public class EMITRunner
{
    private final EMITModelLoader loader;

    public EMITRunner()
    {
        this(new EMITModelLoader());
    }

    public EMITRunner(EMITModelLoader loader)
    {
        this.loader = loader;
    }

    public EMITResult runFromYaml(Path emitYaml)
    {
        EMITResult result = new EMITResult();
        EMITSourceSet sourceSet = init(result, () -> this.loader.load(emitYaml));
        return (sourceSet == null) ? result : run(sourceSet, result);
    }

    public EMITResult run(EMITModelDescriptor descriptor)
    {
        EMITResult result = new EMITResult();
        EMITSourceSet sourceSet = init(result, () -> this.loader.load(descriptor));
        return (sourceSet == null) ? result : run(sourceSet, result);
    }

    private EMITSourceSet init(EMITResult result, ThrowingSupplier<EMITSourceSet> loader)
    {
        long start = System.currentTimeMillis();
        try
        {
            EMITSourceSet sourceSet = loader.get();
            long elapsed = System.currentTimeMillis() - start;
            String message = sourceSet.getModelFiles().size() + " model files, " + sourceSet.getDependencyFiles().size() + " dependency files";
            result.add(EMITPhaseResult.success(EMITPhase.INITIALIZATION, elapsed, message, sourceSet));
            return sourceSet;
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.INITIALIZATION, elapsed, e.getMessage(), e));
            markRemainingNotRun(result, EMITPhase.INITIALIZATION);
            return null;
        }
    }

    private EMITResult run(EMITSourceSet sourceSet, EMITResult result)
    {
        ParseResult parsed = parse(result, sourceSet);
        if (parsed == null)
        {
            markRemainingNotRun(result, EMITPhase.PARSE);
            return result;
        }

        PureModel pureModel = compile(result, parsed.getPmcd());
        if (pureModel == null)
        {
            markRemainingNotRun(result, EMITPhase.COMPILE);
            return result;
        }

        EMITModel model = new EMITModel(sourceSet, parsed.getPmcd(), pureModel, parsed.getPrimarySourceIds());
        EMITModel effective = runModelGeneration(result, model);
        if (effective == null)
        {
            markRemainingNotRun(result, EMITPhase.MODEL_GENERATION);
            return result;
        }

        runFileGeneration(result, effective);
        runTestExecution(result, effective);
        runPlanGeneration(result, effective);

        return result;
    }

    // ----- Phase 1 -----

    private ParseResult parse(EMITResult result, EMITSourceSet sourceSet)
    {
        long start = System.currentTimeMillis();
        try
        {
            ParseResult parsed = EMITTasks.parse(sourceSet);
            long elapsed = System.currentTimeMillis() - start;
            String message = sourceSet.getTotalFileCount() + " files, " + parsed.getTotalElementCount() + " elements";
            result.add(EMITPhaseResult.success(EMITPhase.PARSE, elapsed, message, parsed.getPmcd()));
            return parsed;
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.PARSE, elapsed, e.getMessage(), e));
            return null;
        }
    }

    // ----- Phase 2 -----

    private PureModel compile(EMITResult result, PureModelContextData pmcd)
    {
        long start = System.currentTimeMillis();
        try
        {
            PureModel pureModel = EMITTasks.compile(pmcd);
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.success(EMITPhase.COMPILE, elapsed, "PureModel built successfully", pureModel));
            return pureModel;
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.COMPILE, elapsed, e.getMessage(), e));
            return null;
        }
    }

    // ----- Phase 3 -----

    private EMITModel runModelGeneration(EMITResult result, EMITModel model)
    {
        long start = System.currentTimeMillis();
        try
        {
            ModelGenResult genResult = EMITTasks.runModelGeneration(model);
            long elapsed = System.currentTimeMillis() - start;
            if (!genResult.isApplicable())
            {
                result.add(EMITPhaseResult.skipped(EMITPhase.MODEL_GENERATION, "no GenerationSpecification"));
                return model;
            }
            if (genResult.getNewModel() == null)
            {
                result.add(EMITPhaseResult.success(EMITPhase.MODEL_GENERATION, elapsed, "no generators produced output", model.getPmcd()));
                return model;
            }
            String message = "generated " + genResult.getGeneratedElementCount() + " elements from " + genResult.getGenerationCount() + " extension calls";
            result.add(EMITPhaseResult.success(EMITPhase.MODEL_GENERATION, elapsed, message, genResult.getNewModel().getPmcd()));
            return genResult.getNewModel();
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.MODEL_GENERATION, elapsed, e.getMessage(), e));
            return null;
        }
    }

    // ----- Phase 4: file gen specs + artifact gen extensions -----

    public static class FileGenerationOutput
    {
        private final Map<String, List<Root_meta_pure_generation_metamodel_GenerationOutput>> bySpecification = new LinkedHashMap<>();
        private final Map<String, List<Artifact>> byArtifactExtension = new LinkedHashMap<>();

        public Map<String, List<Root_meta_pure_generation_metamodel_GenerationOutput>> getBySpecification()
        {
            return Collections.unmodifiableMap(this.bySpecification);
        }

        public Map<String, List<Artifact>> getByArtifactExtension()
        {
            return Collections.unmodifiableMap(this.byArtifactExtension);
        }
    }

    private void runFileGeneration(EMITResult result, EMITModel model)
    {
        long start = System.currentTimeMillis();
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification> specs = EMITTasks.findFileGenerationSpecs(model);
        List<ArtifactCandidate> artifactCandidates = EMITTasks.findArtifactGenerationCandidates(model);

        if (specs.isEmpty() && artifactCandidates.isEmpty())
        {
            result.add(EMITPhaseResult.skipped(EMITPhase.FILE_GENERATION, "no FileGenerationSpecification or ArtifactGenerationExtension applies"));
            return;
        }

        try
        {
            FileGenerationOutput output = new FileGenerationOutput();

            specs.forEach(spec -> output.bySpecification.put(spec.getPath(), EMITTasks.runFileGeneration(spec, model.getPureModel())));

            artifactCandidates.forEach(candidate ->
            {
                List<Artifact> artifacts = EMITTasks.runArtifactGeneration(candidate.extension, candidate.pureElement, model.getPmcd(), model.getPureModel());
                if (!artifacts.isEmpty())
                {
                    output.byArtifactExtension.computeIfAbsent(candidate.extension.getKey(), k -> Lists.mutable.empty()).addAll(artifacts);
                }
            });

            long elapsed = System.currentTimeMillis() - start;
            String message = output.bySpecification.size() + " file generations, " + output.byArtifactExtension.size() + " artifact extensions";
            result.add(EMITPhaseResult.success(EMITPhase.FILE_GENERATION, elapsed, message, output));
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.FILE_GENERATION, elapsed, e.getMessage(), e));
        }
    }

    // ----- Phase 5 -----

    private void runTestExecution(EMITResult result, EMITModel model)
    {
        long start = System.currentTimeMillis();
        MutableList<Object> outputs = Lists.mutable.empty();
        MutableList<RunTestsTestableInput> testableInputs = EMITTasks.findTestableInputs(model);
        List<LegacyMappingTestCandidate> legacyMappingCandidates = EMITTasks.findLegacyMappingTestCandidates(model);
        List<LegacyServiceTestCandidate> legacyServiceCandidates = EMITTasks.findLegacyServiceTestCandidates(model);

        if (testableInputs.isEmpty() && legacyMappingCandidates.isEmpty() && legacyServiceCandidates.isEmpty())
        {
            result.add(EMITPhaseResult.skipped(EMITPhase.TEST_EXECUTION, "no Testable or legacy test elements in primary scope"));
            return;
        }
        EMITPhaseResult phaseResult;
        try
        {
            int total = 0;
            int failed = 0;

            // Modern Testable tests
            if (testableInputs.notEmpty())
            {
                RunTestsResult runTestsResult = EMITTasks.runTests(testableInputs, model.getPmcd(), model.getPureModel());
                outputs.add(runTestsResult);
                total += runTestsResult.results.size();
                failed += ListIterate.count(runTestsResult.results, r -> !(r instanceof TestExecuted) || (((TestExecuted) r).testExecutionStatus == TestExecutionStatus.FAIL));
            }

            // Legacy Mapping tests
            for (LegacyMappingTestCandidate candidate : legacyMappingCandidates)
            {
                total++;
                try
                {
                    RichMappingTestResult mappingResult = EMITTasks.runLegacyMappingTest(candidate.mappingPath, candidate.test, model.getPureModel());
                    outputs.add(mappingResult);
                    if (mappingResult.getResult() != TestResult.SUCCESS)
                    {
                        failed++;
                    }
                }
                catch (Exception e)
                {
                    failed++;
                }
            }

            // Legacy Service tests
            for (LegacyServiceTestCandidate candidate : legacyServiceCandidates)
            {
                total++;
                try
                {
                    List<RichServiceTestResult> serviceResults = EMITTasks.runLegacyServiceTest(candidate.service, model.getPmcd(), model.getPureModel());
                    outputs.addAll(serviceResults);
                    boolean anyFail = ListIterate.anySatisfy(serviceResults, r -> r.getResults() != null && MapIterate.anySatisfy(r.getResults(), v -> v != TestResult.SUCCESS));
                    if (anyFail)
                    {
                        failed++;
                    }
                }
                catch (Exception e)
                {
                    failed++;
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            String message = total + " tests (testable: " + testableInputs.size() + ", legacy mapping: " + legacyMappingCandidates.size() + ", legacy service: " + legacyServiceCandidates.size() + "); " + failed + " failed";
            phaseResult = (failed == 0)
                          ? EMITPhaseResult.success(EMITPhase.TEST_EXECUTION, elapsed, message, outputs)
                          : EMITPhaseResult.failure(EMITPhase.TEST_EXECUTION, elapsed, message, null, outputs);
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            phaseResult = EMITPhaseResult.failure(EMITPhase.TEST_EXECUTION, elapsed, e.getMessage(), e);
        }
        result.add(phaseResult);
    }

    // ----- Phase 6 -----

    private void runPlanGeneration(EMITResult result, EMITModel model)
    {
        long start = System.currentTimeMillis();
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service> services = EMITTasks.findServices(model);
        if (services.isEmpty())
        {
            result.add(EMITPhaseResult.skipped(EMITPhase.PLAN_GENERATION, "no Service elements in primary scope"));
            return;
        }
        try
        {
            Map<String, ExecutionPlan> plans = new LinkedHashMap<>();
            services.forEach(service -> plans.put(service.getPath(), EMITTasks.runPlan(service, model.getPureModel())));
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.success(EMITPhase.PLAN_GENERATION, elapsed, "plans for " + plans.size() + " service(s)", Collections.unmodifiableMap(plans)));
        }
        catch (EMITAssertionError | Exception e)
        {
            long elapsed = System.currentTimeMillis() - start;
            result.add(EMITPhaseResult.failure(EMITPhase.PLAN_GENERATION, elapsed, e.getMessage(), e));
        }
    }

    // ----- helpers -----

    private static void markRemainingNotRun(EMITResult result, EMITPhase failed)
    {
        EMITPhase[] values = EMITPhase.values();
        for (int i = failed.ordinal() + 1; i < values.length; i++)
        {
            EMITPhase phase = values[i];
            if (result.getPhase(phase) == null)
            {
                result.add(EMITPhaseResult.notRun(phase, "skipped due to failure in " + failed.name()));
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T>
    {
        T get() throws Exception;
    }
}
