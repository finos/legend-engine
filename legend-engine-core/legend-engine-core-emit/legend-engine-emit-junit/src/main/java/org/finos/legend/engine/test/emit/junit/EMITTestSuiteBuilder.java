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
import org.finos.legend.engine.test.emit.EMITModel;
import org.finos.legend.engine.test.emit.EMITModelLoader;
import org.finos.legend.engine.test.emit.EMITSourceSet;
import org.finos.legend.engine.test.emit.EMITTasks;
import org.finos.legend.engine.test.emit.EMITTasks.ParseResult;
import org.finos.legend.engine.test.emit.EMITTasks.TestCandidate;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JUnit 5 integration for EMIT. Discovers {@code *.emit.yaml} files on the
 * classpath under a given root, eagerly runs load + parse + compile + model
 * generation on each model, then yields one {@link DynamicTest} per granular
 * operation: an Initialization task, a Parsing task, a Compilation task, and
 * a Model Generation task that report the outcome of the eager phases,
 * followed by one task per file-generation spec, artifact-generation
 * candidate, individual test, and service. If an eager phase fails, its task
 * is failing and no subsequent tasks are emitted for that model. Discovery
 * operates on the model-generation-enriched PMCD, so any element produced by
 * a {@code GenerationSpecification} is eligible for the downstream tasks
 * just like a hand-authored element.
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

        Stream.Builder<DynamicNode> builder = Stream.builder();
        builder.add(passingTask(verboseNames, label, "Load Model Descriptor"));

        EMITModel model = initializationTasks(verboseNames, label, sourceSet, builder);
        if (model != null)
        {
            fileArtifactGenTasks(verboseNames, label, model, builder);
            testTasks(verboseNames, label, model, builder);
            servicePlanTasks(verboseNames, label, model, builder);
        }
        return DynamicContainer.dynamicContainer(label, builder.build());
    }

    private static EMITModel initializationTasks(boolean verboseNames, String label, EMITSourceSet sourceSet, Consumer<? super DynamicContainer> containerConsumer)
    {
        String containerName = "Initialization";
        Stream.Builder<DynamicNode> builder = Stream.builder();

        ParseResult parseResult;
        try
        {
            parseResult = EMITTasks.parse(sourceSet);
            builder.add(passingTask(verboseNames, label, containerName, "Parsing"));
        }
        catch (EMITAssertionError | Exception e)
        {
            builder.add(failingTask(e, verboseNames, label, containerName, "Parsing"));
            containerConsumer.accept(DynamicContainer.dynamicContainer(containerName, builder.build()));
            return null;
        }

        PureModel pureModel;
        try
        {
            pureModel = EMITTasks.compile(parseResult.getPmcd());
            builder.add(passingTask(verboseNames, label, containerName, "Compilation"));
        }
        catch (EMITAssertionError | Exception e)
        {
            builder.add(failingTask(e, verboseNames, label, containerName, "Compilation"));
            containerConsumer.accept(DynamicContainer.dynamicContainer(containerName, builder.build()));
            return null;
        }

        EMITModel model = new EMITModel(sourceSet, parseResult.getPmcd(), pureModel, parseResult.getPrimarySourceIds());
        EMITTasks.ModelGenResult modelGenResult;
        try
        {
            modelGenResult = EMITTasks.runModelGeneration(model);
            builder.add(passingTask(verboseNames, label, containerName, "Model Generation"));
        }
        catch (EMITAssertionError | Exception e)
        {
            modelGenResult = null;
            builder.add(failingTask(e, verboseNames, label, containerName, "Model Generation"));
        }
        containerConsumer.accept(DynamicContainer.dynamicContainer(containerName, builder.build()));
        return ((modelGenResult == null) || (modelGenResult.getNewModel() == null)) ? model : modelGenResult.getNewModel();
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
            Stream<DynamicNode> testableStream = testCandidates.isEmpty() ? null : testCandidates.stream().map(candidate ->
                    test(() -> assertTestPassed(model, candidate), verboseNames, label, containerName, candidate.testablePath + ((candidate.suiteId == null) ? "" : (" / " + candidate.suiteId)) + " / " + candidate.atomicTestId));
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

    private static void servicePlanTasks(boolean verboseNames, String label, EMITModel model, Consumer<? super DynamicContainer> containerConsumer)
    {
        List<Service> services = EMITTasks.findServices(model);
        if (!services.isEmpty())
        {
            containerConsumer.accept(DynamicContainer.dynamicContainer("Service Plan Generation", services.stream()
                    .map(service -> test(() -> EMITTasks.runPlan(service, model.getPureModel()), verboseNames, label, "Plan", service.getPath()))));
        }
    }

    private static void assertTestPassed(EMITModel model, TestCandidate candidate)
    {
        EMITTasks.assertTestPassed(EMITTasks.runTest(candidate.testablePath, candidate.suiteId, candidate.atomicTestId, model.getPmcd(), model.getPureModel()));
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
