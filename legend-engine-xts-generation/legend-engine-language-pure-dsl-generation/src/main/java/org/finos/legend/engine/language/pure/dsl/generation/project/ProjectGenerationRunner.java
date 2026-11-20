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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.model.ModelGenerationException;
import org.finos.legend.engine.language.pure.dsl.generation.model.ModelGenerationRunner;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNodeHelper;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates what a project's build generates, from its model, and returns it rather than
 * writing it: the generation specification's model generations, then its file generations, then
 * every artifact generation, each step over what the steps before it produced.
 * <p>
 * The build runs these steps in two Maven goals, which this follows step for step:
 * <ul>
 *     <li>{@code ModelGenerationMojo} compiles the model, picks the project's one generation
 *     specification and runs it with {@link ModelGenerationRunner};</li>
 *     <li>{@code FileGenerationMojo} compiles the project's elements together with the generated
 *     ones, runs the specification's file generations, then the artifact generations of every
 *     element of the project, generated ones included.</li>
 * </ul>
 * The build stops at its first failure, so does this: the step that failed returns what it did
 * before failing, and the steps after it are {@link ProjectGenerationResult.Status#SKIPPED}.
 * <p>
 * The build tells the project's elements from its dependencies' by the folder they are in. Here
 * that is {@link #withProjectElementPaths}: only the project's elements can hold the
 * generation specification and get artifacts, while the rest of the model is only compiled
 * against.
 */
public class ProjectGenerationRunner
{
    private final PureModelContextData model;
    private Set<String> projectElementPaths;
    private String clientVersion = PureClientVersions.production;
    private ModelGenerationConfig modelGenerationConfig = new ModelGenerationConfig();
    private FileGenerationConfig fileGenerationConfig = new FileGenerationConfig();
    private ArtifactGenerationConfig artifactGenerationConfig = new ArtifactGenerationConfig();
    private Function<PureModelContextData, PureModel> compiler = model -> new PureModel(model, null, null, DeploymentMode.PROD);

    private ProjectGenerationRunner(PureModelContextData model)
    {
        this.model = model;
    }

    /**
     * @param model the project's elements and its dependencies'
     */
    public static ProjectGenerationRunner newRunner(PureModelContextData model)
    {
        return new ProjectGenerationRunner(model);
    }

    /**
     * The paths of the project's own elements. Unset or empty means every element of the model.
     */
    public ProjectGenerationRunner withProjectElementPaths(Set<String> projectElementPaths)
    {
        this.projectElementPaths = projectElementPaths;
        return this;
    }

    /**
     * The client version handed to model generators and artifact extensions; unset means
     * {@link PureClientVersions#production}, as the build uses.
     */
    public ProjectGenerationRunner withClientVersion(String clientVersion)
    {
        this.clientVersion = (clientVersion == null) ? PureClientVersions.production : clientVersion;
        return this;
    }

    public ProjectGenerationRunner withModelGenerationConfig(ModelGenerationConfig modelGenerationConfig)
    {
        this.modelGenerationConfig = (modelGenerationConfig == null) ? new ModelGenerationConfig() : modelGenerationConfig;
        return this;
    }

    public ProjectGenerationRunner withFileGenerationConfig(FileGenerationConfig fileGenerationConfig)
    {
        this.fileGenerationConfig = (fileGenerationConfig == null) ? new FileGenerationConfig() : fileGenerationConfig;
        return this;
    }

    public ProjectGenerationRunner withArtifactGenerationConfig(ArtifactGenerationConfig artifactGenerationConfig)
    {
        this.artifactGenerationConfig = (artifactGenerationConfig == null) ? new ArtifactGenerationConfig() : artifactGenerationConfig;
        return this;
    }

    /**
     * Overrides how the model is compiled. The build compiles in {@link DeploymentMode#PROD}, and
     * fails on any compilation error, which is checked after this compiler either way.
     */
    public ProjectGenerationRunner withCompiler(Function<PureModelContextData, PureModel> compiler)
    {
        this.compiler = compiler;
        return this;
    }

    /**
     * Checks what can be checked before anything runs.
     *
     * @throws IllegalArgumentException when an included artifact extension key is not one this
     *                                  server has
     */
    public void validate()
    {
        validate(this.artifactGenerationConfig);
    }

    /**
     * Checks a request's artifact generation config before anything is loaded or run.
     *
     * @throws IllegalArgumentException when an included artifact extension key is not one this
     *                                  server has
     */
    public static void validate(ArtifactGenerationConfig artifactGenerationConfig)
    {
        Set<String> unknown = ProjectArtifactGenerator.unknownIncludedExtensionKeys(artifactGenerationConfig);
        if (!unknown.isEmpty())
        {
            throw new IllegalArgumentException("Unknown artifact extension keys: " + String.join(", ", unknown) + ". Available: " + String.join(", ", ProjectArtifactGenerator.availableExtensionKeys()));
        }
    }

    public ProjectGenerationResult run()
    {
        validate();
        ProjectGenerationResult result = new ProjectGenerationResult();
        result.compilation = new ProjectGenerationResult.StepResult();
        result.modelGeneration = new ProjectGenerationResult.ModelGenerationStepResult();
        result.fileGeneration = new ProjectGenerationResult.FileGenerationStepResult();
        result.artifactGeneration = new ProjectGenerationResult.ArtifactGenerationStepResult();

        // step 1, as ModelGenerationMojo: compile, then run the project's generation specification
        long start = System.nanoTime();
        PureModel pureModel;
        try
        {
            pureModel = compile(this.model);
            succeed(result.compilation, start);
        }
        catch (RuntimeException e)
        {
            fail(result.compilation, start, failureOf(e));
            return skipAfter(result, result.compilation);
        }

        Predicate<String> isProjectElement = isEmpty(this.projectElementPaths) ? path -> true : this.projectElementPaths::contains;
        PureModelContextData modelWithGenerated = this.model;
        PureModel pureModelWithGenerated = pureModel;
        Set<String> generatedPaths = Collections.emptySet();
        if (this.modelGenerationConfig.disabled)
        {
            result.modelGeneration.status = ProjectGenerationResult.Status.DISABLED;
        }
        else
        {
            ModelGenerationRunner.Result modelGeneration = generateModels(pureModel, isProjectElement, result.modelGeneration);
            if (result.modelGeneration.status == ProjectGenerationResult.Status.FAILED)
            {
                return skipAfter(result, result.modelGeneration);
            }
            if ((modelGeneration != null) && !modelGeneration.getGeneratedModel().getElements().isEmpty())
            {
                modelWithGenerated = modelGeneration.getFullModel();
                generatedPaths = ListIterate.collect(modelGeneration.getGeneratedModel().getElements(), PackageableElement::getPath).toSet();
            }
        }

        if (this.fileGenerationConfig.disabled && this.artifactGenerationConfig.disabled)
        {
            result.fileGeneration.status = ProjectGenerationResult.Status.DISABLED;
            result.artifactGeneration.status = ProjectGenerationResult.Status.DISABLED;
            return finish(result);
        }

        // step 2, as FileGenerationMojo: compile the project with what it generated, then its file generations, then its artifacts.
        // With nothing generated that is the model already compiled, and compiling it again would change nothing.
        Set<String> generated = generatedPaths;
        Predicate<String> isProjectOrGeneratedElement = path -> isProjectElement.test(path) || generated.contains(path);
        GeneratedFiles.OutputFolder outputFolder = new GeneratedFiles.OutputFolder();
        if (!generatedPaths.isEmpty())
        {
            ProjectGenerationResult.StepResult firstStep = this.fileGenerationConfig.disabled ? result.artifactGeneration : result.fileGeneration;
            start = System.nanoTime();
            try
            {
                pureModelWithGenerated = compile(modelWithGenerated);
            }
            catch (RuntimeException e)
            {
                fail(firstStep, start, failureOf(e));
                return skipAfter(result, firstStep);
            }
        }

        if (this.fileGenerationConfig.disabled)
        {
            result.fileGeneration.status = ProjectGenerationResult.Status.DISABLED;
        }
        else
        {
            generateFiles(modelWithGenerated, pureModelWithGenerated, isProjectOrGeneratedElement, outputFolder, result.fileGeneration);
            if (result.fileGeneration.status == ProjectGenerationResult.Status.FAILED)
            {
                return skipAfter(result, result.fileGeneration);
            }
        }

        if (this.artifactGenerationConfig.disabled)
        {
            result.artifactGeneration.status = ProjectGenerationResult.Status.DISABLED;
        }
        else
        {
            generateArtifacts(modelWithGenerated, pureModelWithGenerated, isProjectOrGeneratedElement, outputFolder, result.artifactGeneration);
        }
        return finish(result);
    }

    private ModelGenerationRunner.Result generateModels(PureModel pureModel, Predicate<String> isProjectElement, ProjectGenerationResult.ModelGenerationStepResult step)
    {
        long start = System.nanoTime();
        step.generatedModel = PureModelContextData.newBuilder().build();
        List<GenerationSpecification> generationSpecifications = generationSpecifications(this.model, isProjectElement);
        if (generationSpecifications.size() > 1)
        {
            fail(step, start, failureOf("Only one generation specification allowed, found: " + generationSpecifications.size()));
            return null;
        }
        if (generationSpecifications.isEmpty())
        {
            succeed(step, start);
            return null;
        }
        GenerationSpecification generationSpecification = generationSpecifications.get(0);
        ModelGenerationRunner runner = ModelGenerationRunner.newRunner(generationSpecification, this.model, pureModel)
                .withCompiler(this.compiler)
                .withClientVersion(this.clientVersion);
        try
        {
            ModelGenerationRunner.Result runResult = runner.run();
            step.generatedModel = runResult.getGeneratedModel();
            step.stages = stages(generationSpecification, runResult.getGeneratedPathsByNode(), null);
            step.warnings.addAll(warnings(runResult.getWarnings()));
            succeed(step, start);
            return runResult;
        }
        catch (RuntimeException e)
        {
            ProjectGenerationResult.Failure failure = failureOf(e);
            if (e instanceof ModelGenerationException)
            {
                failure.stage = ((ModelGenerationException) e).getGenerationNodeId();
                failure.element = ((ModelGenerationException) e).getGenerationElement();
            }
            step.generatedModel = runner.getGeneratedModel();
            step.stages = stages(generationSpecification, runner.getGeneratedPathsByNode(), (failure.stage == null) ? "" : failure.stage);
            step.warnings.addAll(warnings(runner.getWarnings()));
            fail(step, start, failure);
            return null;
        }
    }

    /**
     * Every node of the specification as a stage. When the run failed, the stages before the one
     * that failed succeeded and those after it were skipped; a failure no stage can be named for
     * marks every stage skipped.
     */
    private static List<ProjectGenerationResult.Stage> stages(GenerationSpecification generationSpecification, Map<String, Map<String, List<String>>> generatedPathsByNode, String failedStage)
    {
        boolean failed = failedStage != null;
        boolean beforeFailure = failed && generationSpecification.generationNodes.stream().anyMatch(node -> failedStage.equals(nodeId(node)));
        List<ProjectGenerationResult.Stage> stages = ListIterate.collect(generationSpecification.generationNodes, node -> new ProjectGenerationResult.Stage());
        for (int i = 0; i < stages.size(); i++)
        {
            GenerationTreeNode node = generationSpecification.generationNodes.get(i);
            ProjectGenerationResult.Stage stage = stages.get(i);
            stage.id = nodeId(node);
            if (!failed)
            {
                stage.status = ProjectGenerationResult.Status.SUCCEEDED;
            }
            else if (beforeFailure)
            {
                if (failedStage.equals(stage.id))
                {
                    stage.status = ProjectGenerationResult.Status.FAILED;
                    beforeFailure = false;
                }
                else
                {
                    stage.status = ProjectGenerationResult.Status.SUCCEEDED;
                }
            }
            else
            {
                stage.status = ProjectGenerationResult.Status.SKIPPED;
            }
            if (stage.status != ProjectGenerationResult.Status.SKIPPED)
            {
                Map<String, List<String>> generatedPaths = generatedPathsByNode.getOrDefault(stage.id, Collections.emptyMap());
                for (String generationElement : GenerationTreeNodeHelper.generationElements(node))
                {
                    ProjectGenerationResult.Generation generation = new ProjectGenerationResult.Generation();
                    generation.element = generationElement;
                    generation.generated.addAll(generatedPaths.getOrDefault(generationElement, Collections.emptyList()));
                    stage.generations.add(generation);
                }
            }
        }
        return stages;
    }

    private static List<ProjectGenerationResult.Warning> warnings(List<ModelGenerationRunner.Warning> warnings)
    {
        return ListIterate.collect(warnings, runnerWarning ->
        {
            ProjectGenerationResult.Warning warning = new ProjectGenerationResult.Warning();
            warning.message = runnerWarning.getMessage();
            warning.stage = runnerWarning.getGenerationNodeId();
            warning.element = runnerWarning.getGenerationElement();
            warning.path = runnerWarning.getGeneratedElement();
            return warning;
        });
    }

    private void generateFiles(PureModelContextData model, PureModel pureModel, Predicate<String> isProjectElement, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.FileGenerationStepResult step)
    {
        long start = System.nanoTime();
        List<GenerationSpecification> generationSpecifications = generationSpecifications(model, isProjectElement);
        if (generationSpecifications.size() > 1)
        {
            fail(step, start, failureOf("Only 1 generation specification allowed, found " + generationSpecifications.size() + ": " + generationSpecifications.stream().map(PackageableElement::getPath).collect(Collectors.joining(", "))));
            return;
        }
        if (generationSpecifications.isEmpty())
        {
            succeed(step, start);
            return;
        }
        try
        {
            new ProjectFileGenerator(model, pureModel).generate(generationSpecifications.get(0), outputFolder, step);
            succeed(step, start);
        }
        catch (ProjectGenerationStepException e)
        {
            ProjectGenerationResult.Failure failure = failureOf(e.failure);
            failure.fileGeneration = e.fileGeneration;
            fail(step, start, failure);
        }
        catch (RuntimeException e)
        {
            fail(step, start, failureOf(e));
        }
    }

    private void generateArtifacts(PureModelContextData model, PureModel pureModel, Predicate<String> isProjectElement, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.ArtifactGenerationStepResult step)
    {
        long start = System.nanoTime();
        List<PackageableElement> elements = model.getElements().stream()
                .filter(element -> isProjectElement.test(element.getPath()))
                .sorted(Comparator.comparing(PackageableElement::getPath))
                .collect(Collectors.toList());
        try
        {
            new ProjectArtifactGenerator(pureModel, model, this.artifactGenerationConfig, this.clientVersion).generate(elements, outputFolder, step);
            succeed(step, start);
        }
        catch (ProjectGenerationStepException e)
        {
            ProjectGenerationResult.Failure failure = failureOf(e.failure);
            failure.element = e.element;
            failure.extension = e.extension;
            fail(step, start, failure);
        }
        catch (RuntimeException e)
        {
            fail(step, start, failureOf(e));
        }
    }

    private static List<GenerationSpecification> generationSpecifications(PureModelContextData model, Predicate<String> isProjectElement)
    {
        return model.getElementsOfType(GenerationSpecification.class).stream()
                .filter(generationSpecification -> isProjectElement.test(generationSpecification.getPath()))
                .sorted(Comparator.comparing(PackageableElement::getPath))
                .collect(Collectors.toList());
    }

    private PureModel compile(PureModelContextData model)
    {
        PureModel pureModel = this.compiler.apply(model);
        pureModel.checkNoErrorDefect();
        return pureModel;
    }

    private static String nodeId(GenerationTreeNode node)
    {
        return (node.id == null) ? GenerationTreeNodeHelper.defaultId(node) : node.id;
    }

    private static boolean isEmpty(Set<String> set)
    {
        return (set == null) || set.isEmpty();
    }

    private static ProjectGenerationResult.Failure failureOf(RuntimeException e)
    {
        ProjectGenerationResult.Failure failure = failureOf(e.getMessage());
        if (e instanceof EngineException)
        {
            failure.sourceInformation = ((EngineException) e).getSourceInformation();
        }
        return failure;
    }

    private static ProjectGenerationResult.Failure failureOf(String message)
    {
        ProjectGenerationResult.Failure failure = new ProjectGenerationResult.Failure();
        failure.message = message;
        return failure;
    }

    private static void succeed(ProjectGenerationResult.StepResult step, long start)
    {
        step.status = ProjectGenerationResult.Status.SUCCEEDED;
        step.durationMs = (System.nanoTime() - start) / 1_000_000;
    }

    private static void fail(ProjectGenerationResult.StepResult step, long start, ProjectGenerationResult.Failure failure)
    {
        step.status = ProjectGenerationResult.Status.FAILED;
        step.durationMs = (System.nanoTime() - start) / 1_000_000;
        step.failure = failure;
    }

    /**
     * Marks every step after the one that failed as skipped, or disabled when the request turned
     * it off.
     */
    private ProjectGenerationResult skipAfter(ProjectGenerationResult result, ProjectGenerationResult.StepResult failedStep)
    {
        List<ProjectGenerationResult.StepResult> steps = Arrays.asList(result.compilation, result.modelGeneration, result.fileGeneration, result.artifactGeneration);
        List<Boolean> disabled = Arrays.asList(false, this.modelGenerationConfig.disabled, this.fileGenerationConfig.disabled, this.artifactGenerationConfig.disabled);
        for (int i = steps.indexOf(failedStep) + 1; i < steps.size(); i++)
        {
            if (steps.get(i).status == null)
            {
                steps.get(i).status = disabled.get(i) ? ProjectGenerationResult.Status.DISABLED : ProjectGenerationResult.Status.SKIPPED;
            }
        }
        return finish(result);
    }

    private static ProjectGenerationResult finish(ProjectGenerationResult result)
    {
        boolean failed = Stream.of(result.compilation, result.modelGeneration, result.fileGeneration, result.artifactGeneration)
                .anyMatch(step -> step.status == ProjectGenerationResult.Status.FAILED);
        result.status = failed ? ProjectGenerationResult.Status.FAILED : ProjectGenerationResult.Status.SUCCEEDED;
        return result;
    }
}
