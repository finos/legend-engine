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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.test.emit.EMITModel;
import org.finos.legend.engine.test.emit.EMITModelLoader;
import org.finos.legend.engine.test.emit.EMITSourceSet;
import org.finos.legend.engine.test.emit.EMITTasks;
import org.finos.legend.engine.test.emit.EMITTasks.ParseResult;
import org.finos.legend.engine.test.emit.EMITTasks.TestCandidate;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

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
 *     Stream<DynamicTest> emit()
 *     {
 *         return EMITTestSuiteBuilder.taskStream("emit-models/");
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
     * Discovers EMIT models under the given classpath root and returns a stream
     * of dynamic tests covering every granular operation across every model.
     *
     * @param classpathRoot a directory on the classpath under which {@code *.emit.yaml}
     *                      files will be located, e.g. {@code "emit-models/"}
     */
    public static Stream<DynamicTest> taskStream(String classpathRoot)
    {
        MutableList<Path> yamls = EMITModelDiscovery.findEmitYamls(classpathRoot);
        EMITModelLoader loader = new EMITModelLoader();
        return yamls.stream().flatMap(yaml -> tasksFor(yaml, loader).stream());
    }

    public static List<DynamicTest> taskList(String classpathRoot)
    {
        MutableList<Path> yamls = EMITModelDiscovery.findEmitYamls(classpathRoot);
        EMITModelLoader loader = new EMITModelLoader();
        return yamls.flatCollect(yaml -> tasksFor(yaml, loader));
    }

    private static List<DynamicTest> tasksFor(Path yaml, EMITModelLoader loader)
    {
        String fallbackLabel = stripExtension(yaml.getFileName().toString());

        MutableList<DynamicTest> tasks = Lists.mutable.empty();

        EMITSourceSet sourceSet;
        try
        {
            sourceSet = loader.load(yaml);
        }
        catch (Exception e)
        {
            return tasks.with(failingTask(fallbackLabel, "Initialization", e));
        }
        String label = (sourceSet.getDescriptor() != null && sourceSet.getDescriptor().getName() != null) ? sourceSet.getDescriptor().getName() : fallbackLabel;
        tasks.add(passingTask(label, "Initialization"));

        ParseResult parseResult;
        try
        {
            parseResult = EMITTasks.parse(sourceSet);
            tasks.add(passingTask(label, "Parsing"));
        }
        catch (EMITAssertionError | Exception e)
        {
            return tasks.with(failingTask(label, "Parsing", e));
        }

        PureModel pureModel;
        try
        {
            pureModel = EMITTasks.compile(parseResult.getPmcd());
            tasks.add(passingTask(label, "Compilation"));
        }
        catch (EMITAssertionError | Exception e)
        {
            return tasks.with(failingTask(label, "Compilation", e));
        }

        EMITModel coreModel = new EMITModel(sourceSet, parseResult.getPmcd(), pureModel, parseResult.getPrimarySourceIds());
        EMITTasks.ModelGenResult modelGenResult;
        try
        {
            modelGenResult = EMITTasks.runModelGeneration(coreModel);
            tasks.add(passingTask(label, "Model Generation"));
        }
        catch (EMITAssertionError | Exception e)
        {
            modelGenResult = null;
            tasks.add(failingTask(label, "Model Generation", e));
        }
        EMITModel model = ((modelGenResult == null) || (modelGenResult.getNewModel() == null)) ? coreModel : modelGenResult.getNewModel();

        ListIterate.collect(EMITTasks.findFileGenerationSpecs(model), spec ->
        {
            String name = "File Generation: " + spec.getPath();
            return test(label, name, () -> EMITTasks.runFileGeneration(spec, model.getPureModel()));
        }, tasks);

        ListIterate.collect(EMITTasks.findArtifactGenerationCandidates(model), candidate ->
        {
            String name = "Artifact Generation: " + candidate.elementPath + " (" + candidate.extension.getKey() + ")";
            return test(label, name, () -> EMITTasks.runArtifactGeneration(candidate.extension, candidate.pureElement, model.getPmcd(), model.getPureModel()));
        }, tasks);

        ListIterate.collect(EMITTasks.findTestCandidates(model), candidate ->
        {
            String suitePart = (candidate.suiteId == null) ? "" : (" / " + candidate.suiteId);
            String name = "Test: " + candidate.testablePath + suitePart + " / " + candidate.atomicTestId;
            return test(label, name, () -> assertTestPassed(model, candidate));
        }, tasks);

        ListIterate.collect(EMITTasks.findServices(model), service ->
        {
            String name = "Plan: " + service.getPath();
            return test(label, name, () -> EMITTasks.runPlan(service, model.getPureModel()));
        }, tasks);

        ListIterate.collect(EMITTasks.findLegacyMappingTestCandidates(model), candidate ->
        {
            String name = "Legacy Mapping Test: " + candidate.mappingPath + " / " + candidate.test.name;
            return test(label, name, () -> EMITTasks.assertLegacyMappingTestPassed(
                    EMITTasks.runLegacyMappingTest(candidate.mappingPath, candidate.test, model.getPureModel())));
        }, tasks);

        ListIterate.collect(EMITTasks.findLegacyServiceTestCandidates(model), candidate ->
        {
            String name = "Legacy Service Test: " + candidate.servicePath;
            return test(label, name, () -> EMITTasks.assertLegacyServiceTestPassed(
                    EMITTasks.runLegacyServiceTest(candidate.service, model.getPmcd(), model.getPureModel())));
        }, tasks);

        return tasks;
    }

    private static void assertTestPassed(EMITModel model, TestCandidate candidate)
    {
        EMITTasks.assertTestPassed(EMITTasks.runTest(candidate.testablePath, candidate.suiteId, candidate.atomicTestId, model.getPmcd(), model.getPureModel()));
    }

    private static DynamicTest test(String label, String taskName, Executable executable)
    {
        return DynamicTest.dynamicTest(format(label, taskName), executable);
    }

    private static DynamicTest passingTask(String label, String taskName)
    {
        return DynamicTest.dynamicTest(format(label, taskName), () ->
        {
            // do nothing for pass
        });
    }

    private static DynamicTest failingTask(String label, String taskName, Throwable failure)
    {
        return DynamicTest.dynamicTest(format(label, taskName), () ->
        {
            throw failure;
        });
    }

    private static String format(String label, String taskName)
    {
        return "[" + label + "] " + taskName;
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
