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

package org.finos.legend.engine.language.pure.dsl.generation.project;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.test.TestModelGenerationExtension;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestProjectGenerationRunner
{
    private static final String ARTIFACT_MODEL = "###Pure\n"
            + "Enum model::MyEnum\n"
            + "{\n"
            + "  A,\n"
            + "  B,\n"
            + "  C\n"
            + "}\n"
            + "Class model::A\n"
            + "{\n"
            + "}\n"
            + "function model::myFunction(s: String[1]): String[1]\n"
            + "{\n"
            + "  ''\n"
            + "}\n";

    private static final String MODEL_GENERATION_MODEL = "###Pure\n"
            + "Class test::modelgen::SourceA\n"
            + "{\n"
            + "  name: String[1];\n"
            + "}\n"
            + "Class test::modelgen::other::SourceA\n"
            + "{\n"
            + "  name: String[1];\n"
            + "}\n";

    private static final String GENERATED_SOURCE_A = TestModelGenerationExtension.GENERATED_PACKAGE + "::SourceA" + TestModelGenerationExtension.GENERATED_SUFFIX;

    // artifact generation, ported from legend-sdlc's TestArtifactGenerationFactory

    @Test
    void artifactsAreGeneratedForEveryElementAnExtensionCanGenerateFor()
    {
        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(ARTIFACT_MODEL)).run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED);
        ProjectGenerationResult.ArtifactResult enumeration = artifact(result, "model::MyEnum", TestEnumArtifactGenerationExtension.KEY);
        Assertions.assertEquals("model/MyEnum/test-enumeration-generation", enumeration.folder);
        assertFile(enumeration, "SomeTestOutput.txt", "txt", "Some output for enumeration 'MyEnum'");
        ProjectGenerationResult.ArtifactResult function = artifact(result, "model::myFunction_String_1__String_1_", TestFunctionArtifactGenerationExtension.KEY);
        Assertions.assertEquals("model/myFunction_String_1__String_1_/test-function-generation", function.folder);
        assertFile(function, "MyFunction.txt", "txt", "Some output for function 'myFunction_String_1__String_1_'");
        // the file is written as the build writes it, so JSON is indented
        assertFile(artifact(result, "model::A", TestClassArtifactGenerationExtension.KEY), "Class.json", "json", String.format("{%n  \"name\" : \"A\"%n}"));
        // an extension returning nothing still has its result, as in the build
        Assertions.assertTrue(artifact(result, "model::MyEnum", TestSdlcInfoArtifactGenerationExtension.KEY).files.isEmpty());
    }

    @Test
    void artifactExtensionsAreHandedTheModelsSdlcInformation()
    {
        PureModelContextPointer origin = new PureModelContextPointer();
        AlloySDLC sdlc = new AlloySDLC();
        sdlc.groupId = "org.finos.test";
        sdlc.artifactId = "test-project";
        sdlc.version = "1.0.0";
        origin.sdlcInfo = sdlc;
        PureModelContextData model = PureModelContextData.newBuilder().withOrigin(origin).withPureModelContextData(parse(ARTIFACT_MODEL)).build();

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(model).run();

        assertFile(artifact(result, "model::MyEnum", TestSdlcInfoArtifactGenerationExtension.KEY), "SDLCInfo.txt", "txt", "Some output for enumeration 'MyEnum' - SDLC Info 'org.finos.test:test-project:1.0.0'");
    }

    @Test
    void dependencyElementsAreCompiledAgainstButGetNoArtifacts()
    {
        PureModelContextData model = parse(ARTIFACT_MODEL + "Class dependency::B\n{\n}\n");

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(model)
                .withProjectElementPaths(Sets.mutable.with("model::MyEnum", "model::A", "model::myFunction_String_1__String_1_"))
                .run();

        Assertions.assertEquals(Sets.mutable.with("model::MyEnum", "model::A", "model::myFunction_String_1__String_1_"), ListIterate.collect(result.artifactGeneration.results, r -> r.element).toSet());
    }

    @Test
    void includedExtensionKeysNarrowAndExcludedOnesRemove()
    {
        ArtifactGenerationConfig config = new ArtifactGenerationConfig();
        config.includedExtensionKeys = Sets.mutable.with(TestEnumArtifactGenerationExtension.KEY, TestClassArtifactGenerationExtension.KEY);
        config.excludedExtensionKeys = Sets.mutable.with(TestClassArtifactGenerationExtension.KEY);

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(ARTIFACT_MODEL)).withArtifactGenerationConfig(config).run();

        Assertions.assertEquals(Collections.singletonList(TestEnumArtifactGenerationExtension.KEY), ListIterate.collect(result.artifactGeneration.results, r -> r.extension));
    }

    @Test
    void anUnknownIncludedExtensionKeyIsRejectedBeforeAnythingRuns()
    {
        ArtifactGenerationConfig config = new ArtifactGenerationConfig();
        config.includedExtensionKeys = Sets.mutable.with(TestEnumArtifactGenerationExtension.KEY, "noSuchExtension");
        // the model doesn't compile, so a run getting that far would fail the compilation step instead
        ProjectGenerationRunner runner = ProjectGenerationRunner.newRunner(parse("###Pure\nClass model::A\n{\n  b: model::Missing[1];\n}\n")).withArtifactGenerationConfig(config);

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, runner::run);

        Assertions.assertEquals("Unknown artifact extension keys: noSuchExtension. Available: test-class-generation, test-enumeration-generation, test-function-generation, test-sdlcinfo-generation", e.getMessage());
    }

    @Test
    void anUnknownExcludedExtensionKeyIsIgnored()
    {
        ArtifactGenerationConfig config = new ArtifactGenerationConfig();
        config.excludedExtensionKeys = Sets.mutable.with("noSuchExtension");

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(ARTIFACT_MODEL)).withArtifactGenerationConfig(config).run();

        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, result.status);
    }

    // file generation, ported from legend-sdlc's TestFileGenerationFactory

    @Test
    void fileGenerationsRunInTheSpecificationsOrderUnderTheBuildsFolders()
    {
        PureModelContextData model = parse("###FileGeneration\n"
                + "TestFile model::fileA\n"
                + "{\n"
                + "  generationOutputPath: 'aRoot';\n"
                + "}\n"
                + "TestFile model::fileB\n"
                + "{\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification model::Spec\n"
                + "{\n"
                + "  fileGenerations: [model::fileB, model::fileA];\n"
                + "}\n");

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(model).run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED);
        Assertions.assertEquals(Arrays.asList("model::fileB", "model::fileA"), ListIterate.collect(result.fileGeneration.results, r -> r.fileGeneration));
        ProjectGenerationResult.FileGenerationResult fileB = result.fileGeneration.results.get(0);
        Assertions.assertEquals(TestFileGenerationExtension.KEY, fileB.type);
        Assertions.assertEquals("model_fileB", fileB.rootFolder, "without a generationOutputPath the folder is the path with '::' replaced");
        Assertions.assertEquals("out.txt", fileB.files.get(0).fileName);
        Assertions.assertEquals("from model::fileB", fileB.files.get(0).content);
        Assertions.assertEquals("aRoot", result.fileGeneration.results.get(1).rootFolder);
    }

    @Test
    void aFileWrittenTwiceWithTheSameContentWarns()
    {
        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(twoFileGenerationsWritingOneFile("same", "same"))).run();

        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, result.fileGeneration.status);
        Assertions.assertEquals(Collections.singletonList("Duplicate file paths found with the same content: shared/out.txt"), ListIterate.collect(result.fileGeneration.warnings, w -> w.message));
        Assertions.assertEquals(2, result.fileGeneration.results.size(), "each file generation still lists the file it wrote");
    }

    @Test
    void aFileWrittenTwiceWithDifferentContentFailsAndSkipsArtifacts()
    {
        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(twoFileGenerationsWritingOneFile("one", "two"))).run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.FAILED, ProjectGenerationResult.Status.SKIPPED);
        Assertions.assertEquals("Duplicate file paths found when serializing file generations outputs : 'shared/out.txt'", result.fileGeneration.failure.message);
        Assertions.assertEquals("model::fileB", result.fileGeneration.failure.fileGeneration);
        Assertions.assertEquals(Collections.singletonList("model::fileA"), ListIterate.collect(result.fileGeneration.results, r -> r.fileGeneration), "what was done before the failure is returned");
    }

    @Test
    void anArtifactWritingAFileAFileGenerationWroteFails()
    {
        // the build writes file generations and artifacts into one folder
        PureModelContextData model = parse(ARTIFACT_MODEL
                + "###FileGeneration\n"
                + "TestFile model::fileA\n"
                + "{\n"
                + "  generationOutputPath: 'model/MyEnum/test-enumeration-generation';\n"
                + "  fileName: 'SomeTestOutput.txt';\n"
                + "  content: 'something else';\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification model::Spec\n"
                + "{\n"
                + "  fileGenerations: [model::fileA];\n"
                + "}\n");

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(model).run();

        Assertions.assertEquals(ProjectGenerationResult.Status.FAILED, result.artifactGeneration.status);
        Assertions.assertEquals("model::MyEnum", result.artifactGeneration.failure.element);
        Assertions.assertEquals(TestEnumArtifactGenerationExtension.KEY, result.artifactGeneration.failure.extension);
        Assertions.assertTrue(result.artifactGeneration.failure.message.endsWith("output: 'model/MyEnum/test-enumeration-generation/SomeTestOutput.txt'"), result.artifactGeneration.failure.message);
    }

    // the generation specification, as both of the build's goals pick it

    @Test
    void onlyTheProjectsGenerationSpecificationCounts()
    {
        PureModelContextData model = parse(ARTIFACT_MODEL
                + "###GenerationSpecification\n"
                + "GenerationSpecification model::Spec\n"
                + "{\n"
                + "}\n"
                + "GenerationSpecification dependency::Spec\n"
                + "{\n"
                + "}\n");

        ProjectGenerationResult both = ProjectGenerationRunner.newRunner(model).run();
        Assertions.assertEquals(ProjectGenerationResult.Status.FAILED, both.modelGeneration.status);
        Assertions.assertEquals("Only one generation specification allowed, found: 2", both.modelGeneration.failure.message);

        ModelGenerationConfig noModelGeneration = new ModelGenerationConfig();
        noModelGeneration.disabled = true;
        ProjectGenerationResult bothWithoutModelGeneration = ProjectGenerationRunner.newRunner(model).withModelGenerationConfig(noModelGeneration).run();
        Assertions.assertEquals("Only 1 generation specification allowed, found 2: dependency::Spec, model::Spec", bothWithoutModelGeneration.fileGeneration.failure.message);

        Set<String> projectElements = ListIterate.collect(model.getElements(), PackageableElement::getPath).reject(path -> path.startsWith("dependency::")).toSet();
        ProjectGenerationResult projectOnly = ProjectGenerationRunner.newRunner(model).withProjectElementPaths(projectElements).run();
        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, projectOnly.status);
    }

    // model generation

    @Test
    void generatedElementsAreCompiledWithTheProjectAndGetArtifacts()
    {
        PureModelContextData model = parse(MODEL_GENERATION_MODEL
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: test::modelgen::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ProjectGenerationResult result = runnerWithoutCompilingSpecifications(model)
                .withProjectElementPaths(Sets.mutable.with("test::modelgen::SourceA", "test::modelgen::Spec"))
                .run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.SUCCEEDED);
        Assertions.assertEquals(Collections.singletonList(GENERATED_SOURCE_A), paths(result.modelGeneration.generatedModel));
        ProjectGenerationResult.Stage stage = result.modelGeneration.stages.get(0);
        Assertions.assertEquals("test::modelgen::SourceA", stage.id);
        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, stage.status);
        Assertions.assertEquals("test::modelgen::SourceA", stage.generations.get(0).element);
        Assertions.assertEquals(Collections.singletonList(GENERATED_SOURCE_A), stage.generations.get(0).generated);
        // the generated class is the project's, so it gets artifacts; the class outside the project does not
        Assertions.assertEquals(Sets.mutable.with("test::modelgen::SourceA", GENERATED_SOURCE_A), ListIterate.collect(result.artifactGeneration.results, r -> r.element).toSet());
    }

    @Test
    void aPathAnEarlierStageGeneratedIsAWarningAndKeepsItsFirstSource()
    {
        PureModelContextData model = parse(MODEL_GENERATION_MODEL
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: test::modelgen::SourceA;\n"
                + "    },\n"
                + "    {\n"
                + "      generationElement: test::modelgen::other::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ProjectGenerationResult result = runnerWithoutCompilingSpecifications(model).run();

        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, result.modelGeneration.status);
        Assertions.assertEquals(Collections.singletonList(GENERATED_SOURCE_A), result.modelGeneration.stages.get(0).generations.get(0).generated);
        Assertions.assertEquals(Collections.emptyList(), result.modelGeneration.stages.get(1).generations.get(0).generated);
        ProjectGenerationResult.Warning warning = result.modelGeneration.warnings.get(0);
        Assertions.assertEquals("test::modelgen::other::SourceA", warning.stage);
        Assertions.assertEquals("test::modelgen::other::SourceA", warning.element);
        Assertions.assertEquals(GENERATED_SOURCE_A, warning.path);
        Assertions.assertEquals("Generation element 'test::modelgen::other::SourceA' generated element '" + GENERATED_SOURCE_A + "', which generation element 'test::modelgen::SourceA' already generated in an earlier node, keeping the first", warning.message);
    }

    @Test
    void aModelGenerationFailureNamesItsStageAndElementAndSkipsTheRest()
    {
        PureModelContextData model = parse(MODEL_GENERATION_MODEL
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: test::modelgen::SourceA;\n"
                + "    },\n"
                + "    {\n"
                + "      id: 'broken';\n"
                + "      generationElement: test::modelgen::Missing;\n"
                + "    },\n"
                + "    {\n"
                + "      generationElement: test::modelgen::other::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ProjectGenerationResult result = runnerWithoutCompilingSpecifications(model).run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.FAILED, ProjectGenerationResult.Status.SKIPPED, ProjectGenerationResult.Status.SKIPPED);
        Assertions.assertEquals("broken", result.modelGeneration.failure.stage);
        Assertions.assertEquals("test::modelgen::Missing", result.modelGeneration.failure.element);
        Assertions.assertNotNull(result.modelGeneration.failure.message);
        Assertions.assertEquals(Arrays.asList(ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.FAILED, ProjectGenerationResult.Status.SKIPPED), ListIterate.collect(result.modelGeneration.stages, s -> s.status));
        Assertions.assertEquals(Collections.singletonList(GENERATED_SOURCE_A), paths(result.modelGeneration.generatedModel), "what the stages before the failure generated is returned");
    }

    // steps

    @Test
    void stepsTurnedOffAreDisabled()
    {
        ModelGenerationConfig noModelGeneration = new ModelGenerationConfig();
        noModelGeneration.disabled = true;
        FileGenerationConfig noFileGeneration = new FileGenerationConfig();
        noFileGeneration.disabled = true;

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse(ARTIFACT_MODEL))
                .withModelGenerationConfig(noModelGeneration)
                .withFileGenerationConfig(noFileGeneration)
                .run();

        assertStatuses(result, ProjectGenerationResult.Status.SUCCEEDED, ProjectGenerationResult.Status.DISABLED, ProjectGenerationResult.Status.DISABLED, ProjectGenerationResult.Status.SUCCEEDED);
        Assertions.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, result.status);
        Assertions.assertNull(result.modelGeneration.durationMs);
        Assertions.assertFalse(result.artifactGeneration.results.isEmpty(), "artifacts run over the project's elements as given");
    }

    @Test
    void aCompilationFailureSkipsEveryStep()
    {
        ArtifactGenerationConfig noArtifacts = new ArtifactGenerationConfig();
        noArtifacts.disabled = true;

        ProjectGenerationResult result = ProjectGenerationRunner.newRunner(parse("###Pure\nClass model::A\n{\n  b: model::Missing[1];\n}\n"))
                .withArtifactGenerationConfig(noArtifacts)
                .run();

        assertStatuses(result, ProjectGenerationResult.Status.FAILED, ProjectGenerationResult.Status.SKIPPED, ProjectGenerationResult.Status.SKIPPED, ProjectGenerationResult.Status.DISABLED);
        Assertions.assertEquals(ProjectGenerationResult.Status.FAILED, result.status);
        Assertions.assertTrue(result.compilation.failure.message.contains("model::Missing"), result.compilation.failure.message);
        Assertions.assertNotNull(result.compilation.failure.sourceInformation);
    }

    private static String twoFileGenerationsWritingOneFile(String contentA, String contentB)
    {
        return "###FileGeneration\n"
                + "TestFile model::fileA\n"
                + "{\n"
                + "  generationOutputPath: 'shared';\n"
                + "  content: '" + contentA + "';\n"
                + "}\n"
                + "TestFile model::fileB\n"
                + "{\n"
                + "  generationOutputPath: 'shared';\n"
                + "  content: '" + contentB + "';\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification model::Spec\n"
                + "{\n"
                + "  fileGenerations: [model::fileA, model::fileB];\n"
                + "}\n";
    }

    /**
     * Compiles the model without its generation specification, as TestModelGenerationRunner does:
     * the generation elements here are plain classes, and a specification compiled together with
     * them resolves them or not depending on the order the compiler processes its groups in.
     */
    private static ProjectGenerationRunner runnerWithoutCompilingSpecifications(PureModelContextData model)
    {
        return ProjectGenerationRunner.newRunner(model).withCompiler(m -> new PureModel(PureModelContextData.newBuilder()
                .withElements(ListIterate.reject(m.getElements(), element -> element instanceof GenerationSpecification))
                .build(), null, null, DeploymentMode.PROD));
    }

    private static void assertStatuses(ProjectGenerationResult result, ProjectGenerationResult.Status compilation, ProjectGenerationResult.Status modelGeneration, ProjectGenerationResult.Status fileGeneration, ProjectGenerationResult.Status artifactGeneration)
    {
        Assertions.assertEquals(
                Arrays.asList(compilation, modelGeneration, fileGeneration, artifactGeneration),
                Arrays.asList(result.compilation.status, result.modelGeneration.status, result.fileGeneration.status, result.artifactGeneration.status),
                () -> "compilation, model, file and artifact generation; failures: " + Arrays.asList(result.compilation.failure, result.modelGeneration.failure, result.fileGeneration.failure, result.artifactGeneration.failure).stream().map(f -> (f == null) ? null : f.message).collect(Collectors.toList()));
    }

    private static ProjectGenerationResult.ArtifactResult artifact(ProjectGenerationResult result, String element, String extension)
    {
        return ListIterate.detectOptional(result.artifactGeneration.results, r -> element.equals(r.element) && extension.equals(r.extension))
                .orElseThrow(() -> new AssertionError("No artifact of '" + extension + "' for '" + element + "' in " + ListIterate.collect(result.artifactGeneration.results, r -> r.element + "/" + r.extension)));
    }

    private static void assertFile(ProjectGenerationResult.ArtifactResult artifact, String fileName, String format, String content)
    {
        Assertions.assertEquals(1, artifact.files.size());
        ProjectGenerationResult.GeneratedFile file = artifact.files.get(0);
        Assertions.assertEquals(fileName, file.fileName);
        Assertions.assertEquals(format, file.format);
        Assertions.assertEquals(content, file.content);
    }

    private static List<String> paths(PureModelContextData model)
    {
        return ListIterate.collect(model.getElements(), PackageableElement::getPath);
    }

    private static PureModelContextData parse(String grammar)
    {
        return PureGrammarParser.newInstance().parseModel(grammar);
    }
}
