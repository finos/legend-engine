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

import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.test.emit.EMITPhaseResult.Status;
import org.finos.legend.engine.test.emit.EMITRunner.FileGenerationOutput;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.finos.legend.engine.test.emit.extensions.EmitDemoArtifactGenerationExtension;
import org.finos.legend.engine.test.emit.extensions.EmitDemoModelGenerationExtension;
import org.finos.legend.engine.testable.model.RunTestsResult;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TestEMITRunner
{
    @Test
    void classSimpleRunsThroughAllPhases()
    {
        Path emitYaml = resource("emit-models/basic/class-simple.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed but got:\n" + result.getSummary());

        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.INITIALIZATION).getStatus());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.PARSE).getStatus());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.MODEL_GENERATION).getStatus());
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.TEST_EXECUTION).getStatus());
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.PLAN_GENERATION).getStatus());
    }

    @Test
    void initializationFailureSkipsRemainingPhases()
    {
        Path missing = Paths.get("does-not-exist.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(missing);

        Assertions.assertFalse(result.isSuccess(), "Expected failure on missing descriptor");
        Assertions.assertEquals(Status.FAILURE, result.getPhase(EMITPhase.INITIALIZATION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.PARSE).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.COMPILE).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.MODEL_GENERATION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.TEST_EXECUTION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.PLAN_GENERATION).getStatus());
    }

    @Test
    void compileFailureSkipsDownstreamPhases()
    {
        Path emitYaml = resource("emit-models/basic/compile-failure.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        // result as a whole is not a success
        Assertions.assertFalse(result.isSuccess(), "Expected EMIT run to fail at COMPILE");

        // init and parse phases should succeed
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.INITIALIZATION).getStatus());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.PARSE).getStatus());

        // compile phase should fail
        String expectedMessage = "FAILURE [Compilation]: COMPILATION error at model.pure[20:12-29]: Can't find type 'demo::DoesNotExist'";
        EMITPhaseResult compileResult = result.getPhase(EMITPhase.COMPILE);
        Assertions.assertEquals(Status.FAILURE, compileResult.getStatus());
        Assertions.assertEquals(expectedMessage, compileResult.getMessage());
        Assertions.assertInstanceOf(EMITAssertionError.class, compileResult.getThrowable(), "Expected an assertion error from the failing test");
        Assertions.assertEquals(expectedMessage, compileResult.getThrowable().getMessage());
        Assertions.assertInstanceOf(EngineException.class, compileResult.getThrowable().getCause());
        Assertions.assertSame(EngineErrorType.COMPILATION, ((EngineException) compileResult.getThrowable().getCause()).getErrorType());

        // remaining phases are skipped
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.MODEL_GENERATION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.FILE_GENERATION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.TEST_EXECUTION).getStatus());
        Assertions.assertEquals(Status.NOT_RUN, result.getPhase(EMITPhase.PLAN_GENERATION).getStatus());

    }

    @Test
    void m2mMappingTestsExecuteAndPass()
    {
        Path emitYaml = resource("emit-models/basic/m2m-passing.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.PARSE).getStatus());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult testPhase = result.getPhase(EMITPhase.TEST_EXECUTION);
        List<?> outputs = testPhase.getOutputs();
        Assertions.assertEquals(1, outputs.size());
        RunTestsResult runTests = (RunTestsResult) outputs.get(0);
        Assertions.assertEquals(Status.SUCCESS, testPhase.getStatus(),
                () -> "Expected TEST_EXECUTION SUCCESS but got:\n" + result.getSummary() + "\n" + describeTestResults(runTests));

        Assertions.assertEquals(2, runTests.results.size(), "Expected exactly 2 mapping tests to be executed");
        runTests.results.forEach(r ->
        {
            Assertions.assertInstanceOf(TestExecuted.class, r, () -> "Expected TestExecuted, got " + r.getClass().getSimpleName());
            Assertions.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) r).testExecutionStatus,
                    () -> "Expected test " + r.atomicTestId + " to PASS but got " + ((TestExecuted) r).testExecutionStatus + "; assertions=" + ((TestExecuted) r).assertStatuses);
        });

        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed but got:\n" + result.getSummary());
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.PLAN_GENERATION).getStatus());
    }

    @Test
    void m2mMappingTestFailureIsReported()
    {
        Path emitYaml = resource("emit-models/basic/m2m-mixed.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertFalse(result.isSuccess(), "Expected EMIT run to fail because one mapping test fails");

        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult testPhase = result.getPhase(EMITPhase.TEST_EXECUTION);
        List<?> outputs = testPhase.getOutputs();
        Assertions.assertEquals(1, outputs.size());
        RunTestsResult runTests = (RunTestsResult) outputs.get(0);
        Assertions.assertEquals(Status.FAILURE, testPhase.getStatus(),
                () -> "Expected TEST_EXECUTION FAILURE but got:\n" + result.getSummary() + "\n" + describeTestResults(runTests));
        Assertions.assertTrue(testPhase.getMessage() != null && testPhase.getMessage().contains("1 failed"),
                () -> "Expected message to mention '1 failed', got: " + testPhase.getMessage()
                        + "\n" + result.getSummary() + "\n" + describeTestResults(runTests));

        // PLAN_GENERATION still runs (failure of TEST_EXECUTION does not short-circuit it).
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.PLAN_GENERATION).getStatus());
    }

    @Test
    void modelGenerationProducesAdditionalElements()
    {
        Path emitYaml = resource("emit-models/basic/model-generation.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed but got:\n" + result.getSummary());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult modelGenPhase = result.getPhase(EMITPhase.MODEL_GENERATION);
        Assertions.assertEquals(Status.SUCCESS, modelGenPhase.getStatus(), () -> "Expected MODEL_GENERATION SUCCESS but got:\n" + result.getSummary());

        PureModelContextData enriched = (PureModelContextData) modelGenPhase.getOutputs().get(0);
        String generatedPath = EmitDemoModelGenerationExtension.GENERATED_PACKAGE + "::Source" + EmitDemoModelGenerationExtension.GENERATED_SUFFIX;
        PackageableElement generated = enriched.getElements().stream().filter(e -> generatedPath.equals(e.getPath())).findFirst()
                .orElseGet(() -> Assertions.fail(() -> "Generated element '" + generatedPath + "' missing from enriched PMCD; elements: "
                        + enriched.getElements().stream().map(PackageableElement::getPath).reduce("", (l, r) -> l + " " + r)));
        Assertions.assertInstanceOf(Class.class, generated);
        Class generatedClass = (Class) generated;
        Assertions.assertEquals(1, generatedClass.properties.size(), "Expected exactly one property on the generated class");
        Assertions.assertEquals("generatedAt", generatedClass.properties.get(0).name);

        // Original element must survive the merge.
        Assertions.assertTrue(enriched.getElements().stream().anyMatch(e -> "demo::modelgen::Source".equals(e.getPath())), "Source element must remain in enriched PMCD");

        // FILE_GENERATION runs because the artifact extension does not match this fixture's namespace.
        Assertions.assertEquals(Status.SKIPPED, result.getPhase(EMITPhase.FILE_GENERATION).getStatus());
    }

    @Test
    void fileGenerationProducesExpectedOutput()
    {
        Path emitYaml = resource("emit-models/basic/file-generation.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed but got:\n" + result.getSummary());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult fileGenPhase = result.getPhase(EMITPhase.FILE_GENERATION);
        Assertions.assertEquals(Status.SUCCESS, fileGenPhase.getStatus(), () -> "Expected FILE_GENERATION SUCCESS but got:\n" + result.getSummary());

        FileGenerationOutput output = (FileGenerationOutput) fileGenPhase.getOutputs().get(0);
        Map<String, List<Root_meta_pure_generation_metamodel_GenerationOutput>> bySpec = output.getBySpecification();
        Assertions.assertEquals(1, bySpec.size(), "Expected exactly one FileGenerationSpecification to fire");
        Assertions.assertTrue(bySpec.containsKey("demo::filegen::PersonFileGen"), () -> "Expected output keyed by demo::filegen::PersonFileGen but got: " + bySpec.keySet());

        List<Root_meta_pure_generation_metamodel_GenerationOutput> outputs = bySpec.get("demo::filegen::PersonFileGen");
        Assertions.assertEquals(1, outputs.size(), "Expected exactly one GenerationOutput from the fake extension");
        Root_meta_pure_generation_metamodel_GenerationOutput genOutput = outputs.get(0);
        Assertions.assertEquals("demo::filegen::PersonFileGen.txt", genOutput._fileName());
        Assertions.assertEquals("hello from demo::filegen::PersonFileGen", genOutput._content());
        Assertions.assertEquals("text/plain", genOutput._format());

        // No ArtifactGenerationExtension should match this fixture's package.
        Assertions.assertTrue(output.getByArtifactExtension().isEmpty(), () -> "Did not expect any artifact extensions to fire; got: " + output.getByArtifactExtension().keySet());
    }

    @Test
    void artifactGenerationProducesExpectedArtifacts()
    {
        Path emitYaml = resource("emit-models/basic/artifact-generation.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed but got:\n" + result.getSummary());
        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult fileGenPhase = result.getPhase(EMITPhase.FILE_GENERATION);
        Assertions.assertEquals(Status.SUCCESS, fileGenPhase.getStatus(), () -> "Expected FILE_GENERATION SUCCESS but got:\n" + result.getSummary());

        FileGenerationOutput output = (FileGenerationOutput) fileGenPhase.getOutputs().get(0);
        Assertions.assertTrue(output.getBySpecification().isEmpty(), "No FileGenerationSpecification should be present in this fixture");

        Map<String, List<Artifact>> byExtension = output.getByArtifactExtension();
        Assertions.assertEquals(1, byExtension.size(), () -> "Expected exactly one artifact extension to fire; got: " + byExtension.keySet());
        List<Artifact> artifacts = byExtension.get(EmitDemoArtifactGenerationExtension.KEY);
        Assertions.assertNotNull(artifacts, () -> "Expected artifacts under key '" + EmitDemoArtifactGenerationExtension.KEY + "' but found keys: " + byExtension.keySet());
        Assertions.assertEquals(2, artifacts.size(), "Expected one artifact per primary class");

        Artifact person = artifacts.stream().filter(a -> "demo/artifactgen/Person.artifact.txt".equals(a.path)).findFirst()
                .orElseGet(() -> Assertions.fail("Did not find Person artifact; got: " + artifacts.stream().map(a -> a.path).reduce("", (l, r) -> l + " " + r)));
        Assertions.assertEquals("artifact-for:demo::artifactgen::Person", person.content);
        Assertions.assertEquals("text/plain", person.format);

        Artifact firm = artifacts.stream().filter(a -> "demo/artifactgen/Firm.artifact.txt".equals(a.path)).findFirst()
                .orElseGet(() -> Assertions.fail("Did not find Firm artifact; got: " + artifacts.stream().map(a -> a.path).reduce("", (l, r) -> l + " " + r)));
        Assertions.assertEquals("artifact-for:demo::artifactgen::Firm", firm.content);
    }

    @Test
    void dependencyTestsAreNotExecuted()
    {
        Path emitYaml = resource("emit-models/basic/m2m-with-dep.emit.yaml");

        EMITResult result = new EMITRunner().runFromYaml(emitYaml);

        Assertions.assertEquals(Status.SUCCESS, result.getPhase(EMITPhase.COMPILE).getStatus());

        EMITPhaseResult testPhase = result.getPhase(EMITPhase.TEST_EXECUTION);
        List<?> outputs = testPhase.getOutputs();
        Assertions.assertEquals(1, outputs.size());
        RunTestsResult runTests = (RunTestsResult) outputs.get(0);
        Assertions.assertEquals(1, runTests.results.size(),
                () -> "Expected exactly 1 test (only the primary mapping's test should run); got "
                        + runTests.results.size() + " results: " + describe(runTests));
        TestResult only = runTests.results.get(0);
        Assertions.assertEquals("demo::primary::PrimaryMapping", only.testable);

        Assertions.assertEquals(Status.SUCCESS, testPhase.getStatus(),
                () -> "TEST_EXECUTION did not succeed:\n" + result.getSummary() + "\n" + describeTestResults(runTests));
        Assertions.assertTrue(result.isSuccess(), () -> "Expected EMIT run to succeed (dep tests must be ignored):\n" + result.getSummary());
    }

    private static String describe(RunTestsResult runTests)
    {
        StringBuilder sb = new StringBuilder();
        runTests.results.forEach(r -> sb.append(r.testable).append('/').append(r.testSuiteId).append('/').append(r.atomicTestId).append(' '));
        return sb.toString();
    }

    private static String describeTestResults(RunTestsResult runTests)
    {
        if (runTests == null)
        {
            return "(no test results)";
        }
        StringBuilder sb = new StringBuilder("Test results:\n");
        runTests.results.forEach(r ->
        {
            sb.append("  ").append(r.testable).append('/').append(r.testSuiteId).append('/').append(r.atomicTestId).append(" -> ");
            if (r instanceof TestExecuted)
            {
                TestExecuted te = (TestExecuted) r;
                sb.append(te.testExecutionStatus).append("; assertions=").append(te.assertStatuses);
            }
            else if (r instanceof TestError)
            {
                sb.append("ERROR: ").append(((TestError) r).error);
            }
            else
            {
                sb.append(r.getClass().getSimpleName()).append(": ").append(r);
            }
            sb.append('\n');
        });
        return sb.toString();
    }

    private static Path resource(String name)
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        Assertions.assertNotNull(url, "test resource not found: " + name);
        try
        {
            return Paths.get(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}
