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

package org.finos.legend.engine.test.emit.junit;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.test.emit.EMITModel;
import org.finos.legend.engine.test.emit.EMITModelLoader;
import org.finos.legend.engine.test.emit.EMITSourceSet;
import org.finos.legend.engine.test.emit.EMITTasks;
import org.finos.legend.engine.test.emit.EMITTasks.ParseResult;
import org.finos.legend.engine.test.emit.EMITTasks.TestCandidate;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JUnit 5 integration for EMIT. Discovers {@code *.emit.yaml} files on the
 * classpath under a given root and yields one {@link DynamicTest} per granular
 * operation on each model: a Load Model Descriptor task, then an Initialization
 * group (Parsing, Compilation, Model Generation), followed by one task per
 * file-generation spec, artifact-generation candidate, individual test, and
 * service, with each test suite's tasks framed by an Open Test Suite and a Close
 * Session task. Discovery operates on the model-generation-enriched PMCD, so
 * any element produced by a {@code GenerationSpecification} is eligible for
 * the downstream tasks just like a hand-authored element.
 *
 * <p>Only the descriptor load is eager (the model's container is named after
 * the descriptor). Parsing, compilation, and model generation run inside their
 * own tasks: JUnit consumes dynamic-node streams lazily, executing each node
 * before pulling the next, so each Initialization task's reported duration is
 * the real cost of that phase, and downstream tasks — which can only be
 * discovered from the compiled, enriched model — are produced by deferred
 * stream segments evaluated after the Initialization tasks have run. If a
 * phase fails, its task fails and no subsequent tasks are emitted for that
 * model (model-generation failure still emits downstream tasks against the
 * un-enriched model). The per-phase results are memoized, so a consumer that
 * materializes the stream without executing the tasks (or executes a single
 * task out of context, e.g. an IDE re-running one atomic test) triggers the
 * same work on demand and sees the same tasks; the tasks then simply replay
 * the memoized outcome.
 *
 * <p>Each dynamic test calls into {@link EMITTasks} for its body, so that
 * the standalone {@code EMITRunner} and this JUnit integration share a single
 * per-item engine.
 *
 * <p>Typical usage in a test module:
 *
 * <pre>{@code
 * public class MyModuleEMITTestSuite
 * {
 *     @TestFactory
 *     Stream<DynamicContainer> emit()
 *     {
 *         return EMITTestSuiteBuilder.testContainers("emit-models/");
 *     }
 * }
 * }</pre>
 */
public final class EMITTestSuiteBuilder
{
    private EMITTestSuiteBuilder()
    {
    }

    /**
     * Discovers EMIT models under the given classpath root and returns a stream of dynamic tests covering every
     * granular operation across every model. This is equivalent to calling {@link #testContainers} and flattening down
     * to stream of tests (thus eliminating the containers).
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @return stream of DynamicTest
     */
    public static Stream<DynamicTest> tests(String classpathRoot)
    {
        return testContainers(classpathRoot, true).flatMap(EMITTestSuiteBuilder::toTests);
    }

    /**
     * Discovers the single named EMIT model under the given classpath root and returns a stream of dynamic tests
     * covering its every granular operation. This is equivalent to {@link #testContainer(String, String)} flattened
     * down to a stream of tests (thus eliminating the containers).
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param name          the location of the model's {@code *.emit.yaml}, relative to {@code classpathRoot} and
     *                      without the {@code .emit.yaml} suffix, e.g. {@code "basic/class-simple"}
     * @return stream of DynamicTest for the named model
     * @throws IllegalArgumentException if no model with the given name is found under the root
     */
    public static Stream<DynamicTest> tests(String classpathRoot, String name)
    {
        return toTests(testContainer(classpathRoot, name, true, true));
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of dynamic tests covering
     * every granular operation across each named model, in the order the names are given.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicTest across the named models
     * @throws IllegalArgumentException if any named model is not found under the root
     */
    public static Stream<DynamicTest> tests(String classpathRoot, String... names)
    {
        return tests(classpathRoot, Arrays.stream(names));
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of dynamic tests covering
     * every granular operation across each named model, in iteration order.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicTest across the named models
     * @throws IllegalArgumentException if any named model is not found under the root
     */
    public static Stream<DynamicTest> tests(String classpathRoot, Iterable<? extends String> names)
    {
        return tests(classpathRoot, (names instanceof Collection) ? ((Collection<? extends String>) names).stream() : StreamSupport.stream(names.spliterator(), false));
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of dynamic tests covering
     * every granular operation across each named model, in stream order.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicTest across the named models
     * @throws IllegalArgumentException if any named model is not found under the root (thrown when the returned stream is consumed)
     */
    public static Stream<DynamicTest> tests(String classpathRoot, Stream<? extends String> names)
    {
        return testContainers(classpathRoot, names, true).flatMap(EMITTestSuiteBuilder::toTests);
    }

    /**
     * Discovers EMIT models under the given classpath root and returns a stream of dynamic tests covering every
     * granular operation across every model. The tests are grouped by model, then by phase.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @return stream of DynamicContainer
     */
    public static Stream<DynamicContainer> testContainers(String classpathRoot)
    {
        return testContainers(classpathRoot, false);
    }

    /**
     * Discovers the single named EMIT model under the given classpath root and returns its {@link DynamicContainer},
     * with the granular tasks grouped by model, then by phase.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param name          the location of the model's {@code *.emit.yaml}, relative to {@code classpathRoot} and
     *                      without the {@code .emit.yaml} suffix, e.g. {@code "basic/class-simple"}
     * @return the model's DynamicContainer, or {@code null} if no model with the given name is found under the root
     */
    public static DynamicContainer testContainer(String classpathRoot, String name)
    {
        return testContainer(classpathRoot, name, false, false);
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of their
     * {@link DynamicContainer}s (tasks grouped by model, then by phase), in the order the names are given.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicContainer for the named models
     * @throws IllegalArgumentException if any named model is not found under the root
     */
    public static Stream<DynamicContainer> testContainers(String classpathRoot, String... names)
    {
        return testContainers(classpathRoot, Arrays.stream(names));
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of their
     * {@link DynamicContainer}s (tasks grouped by model, then by phase), in iteration order.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicContainer for the named models
     * @throws IllegalArgumentException if any named model is not found under the root
     */
    public static Stream<DynamicContainer> testContainers(String classpathRoot, Iterable<? extends String> names)
    {
        return testContainers(classpathRoot, (names instanceof Collection) ? ((Collection<? extends String>) names).stream() : StreamSupport.stream(names.spliterator(), false));
    }

    /**
     * Discovers the named EMIT models under the given classpath root and returns a stream of their
     * {@link DynamicContainer}s (tasks grouped by model, then by phase), in stream order.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @param names         the locations of the models' {@code *.emit.yaml} files, each relative to
     *                      {@code classpathRoot} and without the {@code .emit.yaml} suffix
     * @return stream of DynamicContainer for the named models
     * @throws IllegalArgumentException if any named model is not found under the root (thrown when the returned stream is consumed)
     */
    public static Stream<DynamicContainer> testContainers(String classpathRoot, Stream<? extends String> names)
    {
        return testContainers(classpathRoot, names, false);
    }

    /**
     * Deprecated: use {@link #tests(String)} instead, or {@link #testContainers(String)} for better reporting structure.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @return list of DynamicTests
     */
    @Deprecated
    public static Stream<DynamicTest> taskStream(String classpathRoot)
    {
        return tests(classpathRoot);
    }

    /**
     * Deprecated: use {@link #tests(String)} instead, or {@link #testContainers(String)} for better reporting structure.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     * @return list of DynamicTests
     */
    @Deprecated
    public static List<DynamicTest> taskList(String classpathRoot)
    {
        return taskStream(classpathRoot).collect(Collectors.toList());
    }

    private static Stream<DynamicContainer> testContainers(String classpathRoot, boolean verboseNames)
    {
        List<Path> yamls = EMITModelDiscovery.findEmitYamls(classpathRoot);
        EMITModelLoader loader = new EMITModelLoader();
        return yamls.stream().map(yaml -> containerFor(yaml, loader, verboseNames));
    }

    private static Stream<DynamicContainer> testContainers(String classpathRoot, Stream<? extends String> names, boolean verboseNames)
    {
        EMITModelLoader loader = new EMITModelLoader();
        return names.map(name -> testContainer(classpathRoot, name, loader, verboseNames, true));
    }

    private static DynamicContainer testContainer(String classpathRoot, String name, boolean verboseNames, boolean errorIfNotFound)
    {
        return testContainer(classpathRoot, name, new EMITModelLoader(), verboseNames, errorIfNotFound);
    }

    private static DynamicContainer testContainer(String classpathRoot, String name, EMITModelLoader loader, boolean verboseNames, boolean errorIfNotFound)
    {
        Path yaml = EMITModelDiscovery.findEmitYaml(classpathRoot, name);
        if (yaml != null)
        {
            return containerFor(yaml, loader, verboseNames);
        }
        if (errorIfNotFound)
        {
            throw new IllegalArgumentException("Failed to find EMIT model '" + name + "' under classpath root '" + classpathRoot + "'");
        }
        return null;
    }

    private static Stream<DynamicTest> toTests(DynamicNode node)
    {
        return (node instanceof DynamicContainer) ? toTests((DynamicContainer) node) : Stream.of((DynamicTest) node);
    }

    private static Stream<DynamicTest> toTests(DynamicContainer container)
    {
        return container.getChildren().flatMap(EMITTestSuiteBuilder::toTests);
    }

    private static DynamicContainer containerFor(Path yaml, EMITModelLoader loader, boolean verboseNames)
    {
        String fallbackLabel = stripExtension(yaml.getFileName().toString());
        EMITSourceSet sourceSet;
        try
        {
            sourceSet = loader.load(yaml);
        }
        catch (Exception e)
        {
            return DynamicContainer.dynamicContainer(fallbackLabel, Stream.of(failingTask(e, verboseNames, fallbackLabel, "Load Model Descriptor")));
        }
        String label = (sourceSet.getDescriptor() != null && sourceSet.getDescriptor().getName() != null) ? sourceSet.getDescriptor().getName() : fallbackLabel;

        ModelState state = new ModelState(sourceSet);
        Stream<DynamicNode> children = Stream.concat(
                Stream.of(passingTask(verboseNames, label, "Load Model Descriptor"), initializationContainer(verboseNames, label, state)),
                deferred(() -> downstreamNodes(verboseNames, label, state)));
        return DynamicContainer.dynamicContainer(label, children);
    }

    private static DynamicContainer initializationContainer(boolean verboseNames, String label, ModelState state)
    {
        String containerName = "Initialization";
        Stream<DynamicNode> tasks = Stream.concat(
                Stream.of(test(state::runParse, verboseNames, label, containerName, "Parsing")),
                deferred(() -> !state.ensureParsed() ? Stream.<DynamicNode>empty() : Stream.concat(
                        Stream.of(test(state::runCompile, verboseNames, label, containerName, "Compilation")),
                        deferred(() -> !state.ensureCompiled()
                                       ? Stream.<DynamicNode>empty()
                                       : Stream.<DynamicNode>of(test(state::runModelGeneration, verboseNames, label, containerName, "Model Generation"))))));
        return DynamicContainer.dynamicContainer(containerName, tasks);
    }

    private static Stream<DynamicNode> downstreamNodes(boolean verboseNames, String label, ModelState state)
    {
        EMITModel model = state.modelForDownstreamDiscovery();
        if (model == null)
        {
            return Stream.empty();
        }
        Stream.Builder<DynamicNode> builder = Stream.builder();
        fileArtifactGenTasks(verboseNames, label, model, builder);
        testTasks(verboseNames, label, model, builder);
        servicePlanTasks(verboseNames, label, model, builder);
        return builder.build();
    }

    /**
     * A lazily built stream segment: the supplier is invoked only when the
     * returned stream is consumed past the elements preceding it. JUnit
     * executes each dynamic node as it pulls it from the stream, so a deferred
     * segment is built only after the tasks before it have executed — which is
     * what lets downstream task discovery read state the Initialization tasks
     * produced while running.
     */
    private static Stream<DynamicNode> deferred(Supplier<? extends Stream<? extends DynamicNode>> supplier)
    {
        return Stream.of(supplier).flatMap(Supplier::get);
    }

    /**
     * Memoized parse → compile → model-generation pipeline state for one
     * model. Each {@code run*} method is the body of the corresponding
     * Initialization task: the first call performs the work — so the task's
     * reported duration is the phase's real cost and its failure is the
     * phase's real failure — and caches the outcome; later calls replay it.
     * The {@code ensure*} methods drive the same memoized steps without
     * throwing, so deferred stream segments can decide which downstream tasks
     * to emit, and so a task executed out of context (e.g. one atomic test
     * re-run from an IDE without its predecessors) still computes whatever it
     * needs on demand.
     *
     * <p>All state transitions are synchronized, so concurrent access (e.g.
     * JUnit's parallel execution mode) cannot run a phase twice or observe a
     * torn outcome: whichever thread arrives first performs the work, and
     * everyone else blocks and replays the memoized result. Note that under
     * parallel execution the per-task timing attribution is still degraded —
     * a deferred segment may end up paying for a phase before that phase's
     * task runs — so ordered same-thread execution (the JUnit default)
     * remains the intended mode.
     */
    private static final class ModelState
    {
        private final EMITSourceSet sourceSet;
        private boolean parseAttempted;
        private ParseResult parseResult;
        private Throwable parseFailure;
        private boolean compileAttempted;
        private Throwable compileFailure;
        private boolean modelGenAttempted;
        private Throwable modelGenFailure;
        private EMITModel model;

        private ModelState(EMITSourceSet sourceSet)
        {
            this.sourceSet = sourceSet;
        }

        synchronized void runParse() throws Throwable
        {
            if (!ensureParsed())
            {
                throw this.parseFailure;
            }
        }

        synchronized boolean ensureParsed()
        {
            if (!this.parseAttempted)
            {
                this.parseAttempted = true;
                try
                {
                    this.parseResult = EMITTasks.parse(this.sourceSet);
                }
                catch (EMITAssertionError | Exception e)
                {
                    this.parseFailure = e;
                }
            }
            return this.parseFailure == null;
        }

        synchronized void runCompile() throws Throwable
        {
            if (!ensureCompiled())
            {
                throw (this.compileFailure != null) ? this.compileFailure : this.parseFailure;
            }
        }

        synchronized boolean ensureCompiled()
        {
            if (!this.compileAttempted)
            {
                this.compileAttempted = true;
                if (ensureParsed())
                {
                    try
                    {
                        PureModel pureModel = EMITTasks.compile(this.parseResult.getPmcd());
                        this.model = new EMITModel(this.sourceSet, this.parseResult.getPmcd(), pureModel, this.parseResult.getPrimarySourceIds());
                    }
                    catch (EMITAssertionError | Exception e)
                    {
                        this.compileFailure = e;
                    }
                }
            }
            return this.model != null;
        }

        synchronized void runModelGeneration() throws Throwable
        {
            if (!ensureCompiled())
            {
                throw (this.compileFailure != null) ? this.compileFailure : this.parseFailure;
            }
            ensureModelGenerated();
            if (this.modelGenFailure != null)
            {
                throw this.modelGenFailure;
            }
        }

        private synchronized void ensureModelGenerated()
        {
            if (!this.modelGenAttempted)
            {
                this.modelGenAttempted = true;
                if (ensureCompiled())
                {
                    try
                    {
                        EMITTasks.ModelGenResult modelGenResult = EMITTasks.runModelGeneration(this.model);
                        if (modelGenResult.getNewModel() != null)
                        {
                            this.model = modelGenResult.getNewModel();
                        }
                    }
                    catch (EMITAssertionError | Exception e)
                    {
                        this.modelGenFailure = e;
                    }
                }
            }
        }

        /**
         * The model downstream task discovery operates on: the
         * model-generation-enriched model when generation succeeded, the base
         * compiled model when generation failed or was not applicable, or
         * {@code null} when the model never compiled (in which case no
         * downstream tasks are emitted). Triggers any step not yet run.
         */
        synchronized EMITModel modelForDownstreamDiscovery()
        {
            ensureModelGenerated();
            return this.model;
        }
    }

    private static void fileArtifactGenTasks(boolean verboseNames, String label, EMITModel model, Consumer<? super DynamicContainer> containerConsumer)
    {
        List<FileGenerationSpecification> fileGenSpecs = EMITTasks.findFileGenerationSpecs(model);
        List<EMITTasks.ArtifactCandidate> artifactCandidates = EMITTasks.findArtifactGenerationCandidates(model);
        if (!(fileGenSpecs.isEmpty() && artifactCandidates.isEmpty()))
        {
            Stream<DynamicNode> fileGenStream = fileGenSpecs.isEmpty() ? null : fileGenSpecs.stream().map(spec -> test(() -> EMITTasks.runFileGeneration(spec, model.getPureModel()), verboseNames, label, "File Generation", spec.getPath()));
            Stream<DynamicNode> artGenStream = artifactCandidates.isEmpty() ? null : artifactCandidates.stream().map(candidate -> test(() -> EMITTasks.runArtifactGeneration(candidate.extension, candidate.pureElement, model.getPmcd(), model.getPureModel()), verboseNames, label, "Artifact Generation", candidate.elementPath + " (" + candidate.extension.getKey() + ")"));
            containerConsumer.accept(DynamicContainer.dynamicContainer("File/Artifact Generation", (fileGenStream == null) ? artGenStream : ((artGenStream == null) ? fileGenStream : Stream.concat(fileGenStream, artGenStream))));
        }
    }

    private static void testTasks(boolean verboseNames, String label, EMITModel model, Consumer<? super DynamicContainer> containerConsumer)
    {
        List<TestCandidate> testCandidates = EMITTasks.findTestCandidates(model);
        List<EMITTasks.LegacyMappingTestCandidate> legacyMappingTestCandidates = EMITTasks.findLegacyMappingTestCandidates(model);
        List<EMITTasks.LegacyServiceTestCandidate> legacyServiceTestCandidates = EMITTasks.findLegacyServiceTestCandidates(model);
        if (!(testCandidates.isEmpty() && legacyMappingTestCandidates.isEmpty() && legacyServiceTestCandidates.isEmpty()))
        {
            String containerName = "Test";
            Stream<DynamicNode> testableStream = testCandidates.isEmpty() ? null : sessionGroupedTestStream(verboseNames, label, containerName, model, testCandidates);
            Stream<DynamicNode> legacyMappingTestStream = legacyMappingTestCandidates.isEmpty() ? null : legacyMappingTestCandidates.stream().map(candidate ->
                    test(() -> EMITTasks.assertLegacyMappingTestPassed(EMITTasks.runLegacyMappingTest(candidate.mappingPath, candidate.test, model.getPureModel())), verboseNames, label, containerName, "Legacy Mapping Test", candidate.mappingPath + " / " + candidate.test.name));
            Stream<DynamicNode> legacyServiceTestSteam = legacyServiceTestCandidates.isEmpty() ? null : legacyServiceTestCandidates.stream().map(candidate ->
                    test(() -> EMITTasks.assertLegacyServiceTestPassed(EMITTasks.runLegacyServiceTest(candidate.service, model.getPmcd(), model.getPureModel())), verboseNames, label, containerName, "Legacy Service Test", candidate.servicePath));
            Stream<DynamicNode> legacyTestStream = (legacyMappingTestStream == null)
                                                   ? legacyServiceTestSteam
                                                   : ((legacyServiceTestSteam == null) ? legacyMappingTestStream : Stream.concat(legacyMappingTestStream, legacyServiceTestSteam));
            Stream<DynamicNode> testStream = (testableStream == null)
                                             ? legacyTestStream
                                             : (legacyTestStream == null) ? testableStream : Stream.concat(testableStream, legacyTestStream);
            containerConsumer.accept(DynamicContainer.dynamicContainer(containerName, testStream));
        }
    }

    /**
     * Yield one {@link DynamicTest} per atomic test, mirroring the engine's own
     * {@code TestRunner} split between standalone atomic tests and suite members:
     * <ul>
     *   <li><b>Suite-less atomic tests</b> ({@code suiteId == null}) are run
     *       standalone via {@link EMITTasks#runTest}. There is no suite, hence no
     *       per-suite setup to amortize and no {@link TestSuiteSession} to
     *       open.</li>
     *   <li><b>Suite members</b> are grouped by {@code (testablePath, suiteId)};
     *       each group shares a single {@link TestSuiteSession}, framed by an
     *       explicit Open Test Suite task — whose reported duration is the suite's
     *       real setup cost (plan generation, runtime build) and whose failure
     *       is the suite's setup failure — and a Close Test Suite task that reports
     *       the cost and outcome of releasing the session's runtime / connection
     *       resources. Suite-level setup is therefore paid once per suite instead
     *       of once per atomic test. The open is memoized and on-demand, so an
     *       atomic test executed without its Open Test Suite predecessor (e.g. a
     *       single test re-run from an IDE) still opens the session itself; a
     *       close handler on the group's sub-stream remains as a safety net that
     *       releases the session if the Close Test Suite task never executes.</li>
     * </ul>
     * Standalone atomic tests are emitted before suite tests.
     */
    private static Stream<DynamicNode> sessionGroupedTestStream(boolean verboseNames, String label, String containerName, EMITModel model, List<TestCandidate> testCandidates)
    {
        List<TestCandidate> standalone = new ArrayList<>();
        Map<String, List<TestCandidate>> suiteGroups = new LinkedHashMap<>();
        testCandidates.forEach(candidate ->
        {
            if (candidate.suiteId == null)
            {
                standalone.add(candidate);
            }
            else
            {
                suiteGroups.computeIfAbsent(candidate.testablePath + "\0" + candidate.suiteId, k -> new ArrayList<>()).add(candidate);
            }
        });
        Stream<DynamicNode> standaloneStream = standalone.stream().map(candidate -> test(
                () -> EMITTasks.assertTestPassed(EMITTasks.runTest(candidate.testablePath, null, candidate.atomicTestId, model.getPmcd(), model.getPureModel())),
                verboseNames, label, containerName, candidate.testablePath + " / " + candidate.atomicTestId));
        Stream<DynamicNode> suiteStream = suiteGroups.values().stream().flatMap(group -> sessionGroupStream(verboseNames, label, containerName, model, group));
        return Stream.concat(standaloneStream, suiteStream);
    }

    private static Stream<DynamicNode> sessionGroupStream(boolean verboseNames, String label, String containerName, EMITModel model, List<TestCandidate> group)
    {
        TestCandidate head = group.get(0);
        LazyTestSession lazy = new LazyTestSession(head.testablePath, head.suiteId, model);
        String suitePrefix = head.testablePath + " / " + head.suiteId + " / ";
        Stream<DynamicNode> openTask = Stream.of(test(lazy::open, verboseNames, label, containerName, suitePrefix + "Open Test Suite"));
        Stream<DynamicNode> atomicTestTasks = group.stream().map(candidate -> test(
                () -> EMITTasks.assertTestPassed(lazy.get().runAtomicTest(candidate.atomicTestId)),
                verboseNames, label, containerName,
                suitePrefix + candidate.atomicTestId));
        Stream<DynamicNode> closeTask = Stream.of(test(lazy::closeReporting, verboseNames, label, containerName, suitePrefix + "Close Test Suite"));
        return Stream.concat(openTask, Stream.concat(atomicTestTasks, closeTask)).onClose(lazy::close);
    }

    /**
     * Memoized opener for a {@link TestSuiteSession}. The first
     * {@link #get} attempts the open and caches either the live session or
     * the failure that prevented it; subsequent calls return the cached
     * outcome rather than retrying. {@link #close} releases the live
     * session if there is one, and is a no-op otherwise so it is safe to
     * register on stream close handlers even when no test ever runs.
     *
     * <p>Open and close are synchronized, so concurrent access (e.g. JUnit's
     * parallel execution mode) cannot double-open (and thus leak) a session
     * or tear an open against a close. Ordering is still the stream's
     * responsibility, though: under parallel execution the Close Test Suite
     * task could run while atomic tests are in flight, so ordered same-thread
     * execution (the JUnit default) remains the intended mode.
     */
    private static class LazyTestSession
    {
        private final String testablePath;
        private final String suiteId;
        private final EMITModel model;
        private boolean opened;
        private TestSuiteSession<TestResult> session;
        private RuntimeException openError;

        LazyTestSession(String testablePath, String suiteId, EMITModel model)
        {
            this.testablePath = testablePath;
            this.suiteId = suiteId;
            this.model = model;
        }

        synchronized TestSuiteSession<TestResult> get()
        {
            if (!this.opened)
            {
                this.opened = true;
                try
                {
                    this.session = EMITTasks.openTestSession(this.testablePath, this.suiteId, this.model.getPmcd(), this.model.getPureModel());
                }
                catch (RuntimeException e)
                {
                    this.openError = e;
                }
                catch (Exception e)
                {
                    this.openError = new RuntimeException(e);
                }
            }
            if (this.openError != null)
            {
                throw this.openError;
            }
            return this.session;
        }

        /**
         * Task body for the suite's Open Test Suite task: performs (or, if an
         * atomic test already triggered the on-demand open, replays) the
         * memoized open, so the task reports the session setup cost and
         * surfaces any setup failure.
         */
        void open()
        {
            get();
        }

        /**
         * Task body for the suite's Close Test Suite task: closes the session,
         * reporting the close cost and letting any close failure fail the
         * task. Aborted (reported as skipped) when there is no live session
         * to close — because the open failed, or never ran.
         */
        synchronized void closeReporting()
        {
            TestSuiteSession<TestResult> current = this.session;
            if (current == null)
            {
                Assumptions.abort((this.openError != null) ? "No test session to close (open failed)" : "No test session to close (session was never opened)");
                return;
            }
            this.session = null;
            current.close();
        }

        synchronized void close()
        {
            if (this.session != null)
            {
                try
                {
                    this.session.close();
                }
                catch (Exception ignored)
                {
                    // Best-effort safety net when the Close Test Suite task never executed; failures of a real close are reported by that task.
                }
                this.session = null;
            }
        }
    }

    private static void servicePlanTasks(boolean verboseNames, String label, EMITModel model, Consumer<? super DynamicContainer> containerConsumer)
    {
        List<Service> services = EMITTasks.findServices(model);
        if (!services.isEmpty())
        {
            containerConsumer.accept(DynamicContainer.dynamicContainer("Service Plan Generation", services.stream()
                    .map(service -> test(() -> EMITTasks.runPlan(service, model.getPureModel()), verboseNames, label, "Plan", service.getPath()))));
        }
    }

    private static DynamicTest test(Executable executable, boolean verboseName, String name, String... moreNames)
    {
        return DynamicTest.dynamicTest(format(verboseName, name, moreNames), executable);
    }

    private static DynamicTest passingTask(boolean verboseName, String name, String... moreNames)
    {
        return DynamicTest.dynamicTest(format(verboseName, name, moreNames), () ->
        {
            // do nothing for pass
        });
    }

    private static DynamicTest failingTask(Throwable failure, boolean verboseName, String name, String... moreNames)
    {
        return DynamicTest.dynamicTest(format(verboseName, name, moreNames), () ->
        {
            throw failure;
        });
    }

    private static String format(boolean verbose, String name, String... moreNames)
    {
        int lastNameIndex = (moreNames == null) ? -1 : moreNames.length - 1;
        if (lastNameIndex == -1)
        {
            return name;
        }
        String lastName = moreNames[lastNameIndex];
        if (!verbose)
        {
            return lastName;
        }
        switch (lastNameIndex)
        {
            case 0:
            {
                return "[" + name + "] " + lastName;
            }
            case 1:
            {
                return "[" + name + "] " + moreNames[0] + ": " + lastName;
            }
            default:
            {
                StringBuilder sb = new StringBuilder().append("[").append(name).append("] ");
                for (int i = 0; i < lastNameIndex; i++)
                {
                    sb.append(moreNames[i]).append(" / ");
                }
                sb.setLength(sb.length() - 3);
                return sb.append(": ").append(lastName).toString();
            }
        }
    }

    private static String stripExtension(String fileName)
    {
        if (fileName.endsWith(".emit.yaml"))
        {
            return fileName.substring(0, fileName.length() - ".emit.yaml".length());
        }
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot < 0) ? fileName : fileName.substring(0, lastDot);
    }
}
