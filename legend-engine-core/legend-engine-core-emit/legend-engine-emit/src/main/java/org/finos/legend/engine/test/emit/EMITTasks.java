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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtension;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.HelperGenerationSpecificationBuilder;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.service.generation.ServicePlanGenerator;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToRelationAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.finos.legend.engine.test.emit.error.EMITException;
import org.finos.legend.engine.test.runner.mapping.MappingTestRunner;
import org.finos.legend.engine.test.runner.mapping.RichMappingTestResult;
import org.finos.legend.engine.test.runner.service.RichServiceTestResult;
import org.finos.legend.engine.test.runner.service.ServiceTestRunner;
import org.finos.legend.engine.testable.TestableRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;
import org.finos.legend.engine.testable.model.RunTestsResult;
import org.finos.legend.engine.testable.model.RunTestsTestableInput;
import org.finos.legend.engine.testable.model.UniqueTestId;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Testable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Stateless per-item engine for the EMIT pipeline. {@link EMITRunner} drives
 * these methods to produce an aggregated {@link EMITResult}; the JUnit
 * integration drives them per-item to produce one {@code DynamicTest} per
 * granular operation. Both paths share this single implementation.
 */
public final class EMITTasks
{
    public static final String CLIENT_VERSION = PureClientVersions.production;

    private EMITTasks()
    {
    }

    // ----- Phase 1: Parse -----

    public static ParseResult parse(EMITSourceSet sourceSet)
    {
        try
        {
            PureGrammarParser parser = PureGrammarParser.newInstance();
            PureModelContextData.Builder builder = PureModelContextData.newBuilder();
            MutableSet<String> primarySourceIds = Sets.mutable.empty();
            int[] totalElements = {0};
            sourceSet.forEachFile(file ->
            {
                PureModelContextData fileData = parser.parseModel(readFile(file), file.getVirtualPath(), 0, 0, true);
                builder.addPureModelContextData(fileData);
                totalElements[0] += fileData.getElements().size();
                if (file.isPrimary())
                {
                    primarySourceIds.add(file.getVirtualPath());
                }
            });
            return new ParseResult(builder.withSectionIndexesMerged().build(), primarySourceIds, totalElements[0]);
        }
        catch (EngineException e)
        {
            if (e.getErrorType() == EngineErrorType.PARSER)
            {
                throw new EMITAssertionError(EMITPhase.PARSE, buildEngineExceptionMessage(e), e);
            }
            throw new EMITException(EMITPhase.PARSE, buildEngineExceptionMessage(e), e);
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.PARSE, "Error parsing source set", e);
        }
    }

    // ----- Phase 2: Compile -----

    public static PureModel compile(PureModelContextData pmcd)
    {
        try
        {
            return new PureModel(pmcd, null, DeploymentMode.PROD);
        }
        catch (EngineException e)
        {
            if (e.getErrorType() == EngineErrorType.COMPILATION)
            {
                throw new EMITAssertionError(EMITPhase.COMPILE, buildEngineExceptionMessage(e), e);
            }
            throw new EMITException(EMITPhase.COMPILE, buildEngineExceptionMessage(e), e);
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.COMPILE, "Error during compilation", e);
        }
    }

    // ----- Phase 3: Model Generation (node-ordered, incremental recompile) -----

    /**
     * Runs model generation aligned with the SDLC {@code ModelGenerationFactory}:
     * <ol>
     *   <li>Finds the (first) {@link GenerationSpecification} in the PMCD.</li>
     *   <li>Iterates over {@code generationNodes} in declared order.</li>
     *   <li>For each node, resolves the generation element via the
     *       {@link GenerationCompilerExtension} resolver chain.</li>
     *   <li>Finds the matching {@link ModelGenerationExtension} SPI generator
     *       and invokes it.</li>
     *   <li>After each node, merges generated elements and <b>recompiles</b>
     *       so that subsequent nodes can reference newly generated elements.</li>
     *   <li>After all nodes, validates that no generated element overwrites a
     *       core (original) element.</li>
     * </ol>
     */
    public static ModelGenResult runModelGeneration(EMITModel model)
    {
        PureModelContextData corePmcd = model.getPmcd();
        List<GenerationSpecification> genSpecs = corePmcd.getElementsOfType(GenerationSpecification.class);
        if (genSpecs.isEmpty())
        {
            return ModelGenResult.notApplicable();
        }
        if (genSpecs.size() > 1)
        {
            throw new EMITException(EMITPhase.MODEL_GENERATION, "Only one generation specification allowed, found: " + genSpecs.size());
        }

        GenerationSpecification genSpec = genSpecs.get(0);
        if (genSpec.generationNodes == null || genSpec.generationNodes.isEmpty())
        {
            return ModelGenResult.applicableButNoOutput();
        }

        // Index core element paths for overwrite validation
        MutableMap<String, PackageableElement> coreElementIndex = Iterate.groupByUniqueKey(corePmcd.getElements(), PackageableElement::getPath);

        // Load all model generation extensions once
        MutableList<ModelGenerationExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(ModelGenerationExtension.class));

        PureModelContextData.Builder fullModelBuilder = PureModelContextData.newBuilder().withPureModelContextData(corePmcd);
        PureModelContextData.Builder generatedModelBuilder = PureModelContextData.newBuilder();
        PureModel pureModel = ListIterate.injectInto(model.getPureModel(), genSpec.generationNodes, (pm, node) ->
        {
            // Resolve the generation element using the standard resolver chain
            CompileContext compileContext = pm.getContext();
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement generationElement = LazyIterate.flatCollect(
                            HelperGenerationSpecificationBuilder.getGenerationCompilerExtensions(compileContext),
                            GenerationCompilerExtension::getExtraModelGenerationSpecificationResolvers)
                    .collect(resolver -> resolver.value(node.generationElement, node.sourceInformation, compileContext))
                    .detect(Objects::nonNull);
            if (generationElement == null)
            {
                throw new EngineException("Can't find generation element '" + node.generationElement + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }

            // Find matching generator from extensions
            PureModelContextData genOutput = extensions.asLazy()
                    .flatCollect(ModelGenerationExtension::getPureModelContextDataGenerators)
                    .collect(g -> g.value(generationElement, compileContext, CLIENT_VERSION))
                    .detect(Objects::nonNull);
            if (genOutput == null)
            {
                throw new EngineException(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(new StringBuilder("No model generator found for element '"), generationElement).append("'").toString(), EngineErrorType.COMPILATION);
            }

            if (genOutput.getElements().isEmpty())
            {
                return pm;
            }

            fullModelBuilder.withPureModelContextData(genOutput).distinct().sorted();
            generatedModelBuilder.withPureModelContextData(genOutput).distinct().sorted();
            // Incremental recompile so next node can reference newly generated elements
            return compile(fullModelBuilder.build());
        });

        // Validate that generated elements don't overwrite core elements
        PureModelContextData generatedPmcd = generatedModelBuilder.build();
        generatedPmcd.getElements().forEach(generated ->
        {
            String path = generated.getPath();
            PackageableElement coreElement = coreElementIndex.get(path);
            if (coreElement != null)
            {
                throw new EMITAssertionError(EMITPhase.MODEL_GENERATION, "Generated element '" + path + "' of type " + generated.getClass().getSimpleName() + " can't override existing element of type " + coreElement.getClass().getSimpleName());
            }
        });


        PureModelContextData enriched = fullModelBuilder.build();
        int newElements = enriched.getElements().size() - corePmcd.getElements().size();
        return ModelGenResult.generated(model.withModel(enriched, pureModel), newElements, genSpec.generationNodes.size());
    }

    // ----- Phase 4a: per-spec file generation -----

    public static List<Root_meta_pure_generation_metamodel_GenerationOutput> runFileGeneration(FileGenerationSpecification spec, PureModel pureModel)
    {
        try
        {
            MutableList<GenerationExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(GenerationExtension.class));
            GenerationExtension match = (spec.type == null) ? null : extensions.detect(c -> spec.type.equals(c.getKey()));
            if (match == null)
            {
                throw new EMITAssertionError(EMITPhase.FILE_GENERATION, "No GenerationExtension registered for FileGenerationSpecification type '" + spec.type + "' (element " + spec.getPath() + ")");
            }
            List<Root_meta_pure_generation_metamodel_GenerationOutput> outputs = match.generateFromElement(spec, pureModel.getContext());
            return (outputs == null) ? Lists.fixedSize.empty() : outputs;
        }
        catch (EMITException | EMITAssertionError e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.FILE_GENERATION, "Error running file generation for specification: " + spec.getPath(), e);
        }
    }

    // ----- Phase 4b: per-(extension, element) artifact generation -----

    public static List<Artifact> runArtifactGeneration(ArtifactGenerationExtension extension, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pureElement, PureModelContextData pmcd, PureModel pureModel)
    {
        List<Artifact> artifacts = extension.generate(pureElement, pureModel, pmcd, CLIENT_VERSION);
        return (artifacts == null) ? Lists.fixedSize.empty() : artifacts;
    }

    // ----- Phase 5: tests (bulk-capable; granularity controlled by the inputs) -----

    public static RunTestsResult runTests(List<RunTestsTestableInput> inputs, PureModelContextData pmcd, PureModel pureModel)
    {
        try
        {
            return new TestableRunner().doTests(inputs, pureModel, pmcd);
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.TEST_EXECUTION, e);
        }
    }

    /**
     * Convenience: run exactly one atomic test, identified by its containing
     * testable element path, optional suite ID (null for top-level atomics),
     * and atomic test ID. Returns the single resulting {@link TestResult}.
     */
    public static TestResult runTest(String testablePath, String suiteId, String atomicTestId, PureModelContextData pmcd, PureModel pureModel)
    {
        RunTestsTestableInput input = new RunTestsTestableInput();
        input.testable = testablePath;
        UniqueTestId id = new UniqueTestId();
        id.testSuiteId = suiteId;
        id.atomicTestId = atomicTestId;
        input.unitTestIds.add(id);

        RunTestsResult result;
        try
        {
            result = runTests(Lists.mutable.with(input), pmcd, pureModel);
        }
        catch (EMITException | EMITAssertionError e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.TEST_EXECUTION, "Error running tests for " + testablePath, e);
        }
        if (result.results.isEmpty())
        {
            throw new EMITException(EMITPhase.TEST_EXECUTION, "Test runner returned no results for " + testablePath + " / " + suiteId + " / " + atomicTestId);
        }
        return result.results.get(0);
    }

    /**
     * Throws an {@link EMITAssertionError} if the given test result is anything other than {@code PASS}.
     *
     * @param result test result
     * @throws EMITAssertionError if the given test result is anything other than {@code PASS}
     */
    public static void assertTestPassed(TestResult result)
    {
        StringBuilder builder = new StringBuilder(result.testable);
        if (result.testSuiteId != null)
        {
            builder.append(" / ").append(result.testSuiteId);
        }
        builder.append(" / ").append(result.atomicTestId);
        if (result instanceof TestError)
        {
            throw new EMITException(EMITPhase.TEST_EXECUTION, builder.append("; ").append(((TestError) result).error).toString());
        }
        if (result instanceof TestExecuted)
        {
            TestExecuted executed = (TestExecuted) result;
            if (executed.testExecutionStatus != TestExecutionStatus.PASS)
            {
                builder.append(" did not pass; status=").append(executed.testExecutionStatus).append("; assertions=[");
                executed.assertStatuses.forEach(s -> printAssertStatus(builder, s).append(", "));
                builder.setLength(builder.length() - 2);
                builder.append(']');
                throw new EMITAssertionError(EMITPhase.TEST_EXECUTION, builder.toString());
            }
            return;
        }
        throw new EMITException(EMITPhase.TEST_EXECUTION, builder.append("Unexpected test result type: ").append(result.getClass().getSimpleName()).toString());
    }

    private static StringBuilder printAssertStatus(StringBuilder builder, AssertionStatus status)
    {
        builder.append("{id=").append(status.id).append(" status=");
        if (status instanceof AssertPass)
        {
            builder.append("PASS");
        }
        else if (status instanceof AssertFail)
        {
            builder.append("FAIL");
            String message = ((AssertFail) status).message;
            if (message != null)
            {
                builder.append(" message='").append(message).append('\'');
            }
            if (status instanceof EqualToJsonAssertFail)
            {
                EqualToJsonAssertFail jsonAssertFail = (EqualToJsonAssertFail) status;
                builder.append(" expected='").append(jsonAssertFail.expected).append("' actual='").append(jsonAssertFail.actual).append('\'');
            }
            else if (status instanceof EqualToRelationAssertFail)
            {
                EqualToRelationAssertFail jsonAssertFail = (EqualToRelationAssertFail) status;
                builder.append(" expected='").append(jsonAssertFail.expected).append("' actual='").append(jsonAssertFail.actual).append('\'');
            }
        }
        else
        {
            builder.append("UNKNOWN (").append(status.getClass().getSimpleName()).append(')');
        }
        return builder.append('}');
    }

    // ----- Phase 6: per-service plan -----

    public static ExecutionPlan runPlan(Service service, PureModel pureModel)
    {
        try
        {
            RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            MutableList<PlanTransformer> planTransformers = Iterate.flatCollect(ServiceLoader.load(PlanGeneratorExtension.class), PlanGeneratorExtension::getExtraPlanTransformers, Lists.mutable.empty());
            if (planTransformers.isEmpty())
            {
                planTransformers.addAllIterable(LegendPlanTransformers.transformers);
            }
            ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, CLIENT_VERSION, PlanPlatform.JAVA, extensions, planTransformers);
            if (plan == null)
            {
                throw new EMITAssertionError(EMITPhase.PLAN_GENERATION, "Failed to generate plan for service: " + service.getPath());
            }
            return plan;
        }
        catch (EMITException | EMITAssertionError e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.PLAN_GENERATION, "Error generating execution plan for service: " + service.getPath(), e);
        }
    }

    // ----- Discovery (primary-scope filtered) -----

    public static List<FileGenerationSpecification> findFileGenerationSpecs(EMITModel model)
    {
        return ListIterate.collectIf(model.getPmcd().getElements(),
                e -> (e instanceof FileGenerationSpecification) && model.isPrimary(e),
                e -> (FileGenerationSpecification) e);
    }

    public static List<Service> findServices(EMITModel model)
    {
        return ListIterate.collectIf(model.getPmcd().getElements(),
                e -> (e instanceof Service) && model.isPrimary(e),
                e -> (Service) e);
    }

    public static List<ArtifactCandidate> findArtifactGenerationCandidates(EMITModel model)
    {
        List<ArtifactGenerationExtension> extensions = ArtifactGenerationExtensionLoader.extensions();
        MutableList<ArtifactCandidate> candidates = Lists.mutable.empty();
        if (!extensions.isEmpty())
        {
            model.getPmcd().getElements().forEach(protocolElement ->
            {
                if (model.isPrimary(protocolElement))
                {
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pureElement = resolvePureElement(model.getPureModel(), protocolElement);
                    if (pureElement != null)
                    {
                        String path = protocolElement.getPath();
                        ListIterate.collectIf(extensions,
                                ext -> ext.canGenerate(pureElement),
                                ext -> new ArtifactCandidate(ext, pureElement, path),
                                candidates);
                    }
                }
            });
        }
        return candidates;
    }

    /**
     * Resolves a protocol element to its compiled Pure element, with a fallback
     * for {@link Function} elements whose Pure path includes the type signature.
     * Mirrors the resolution logic in SDLC's {@code ArtifactGenerationFactory}.
     */
    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement resolvePureElement(PureModel pureModel, PackageableElement protocolElement)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element = pureModel.getPackageableElement_safe(protocolElement.getPath());
        if (element != null)
        {
            return element;
        }
        // Function elements have mangled paths in Pure that include the type signature
        if (protocolElement instanceof Function)
        {
            String fullPath = pureModel.buildPackageString(protocolElement._package, HelperModelBuilder.getSignature((Function) protocolElement));
            element = pureModel.getPackageableElement_safe(fullPath);
        }
        return element;
    }

    public static List<TestCandidate> findTestCandidates(EMITModel model)
    {
        MutableList<TestCandidate> candidates = Lists.mutable.empty();
        model.getPmcd().getElements().forEach(protocolElement ->
        {
            if (model.isPrimary(protocolElement) && TestableRunnerExtensionLoader.isTestable(protocolElement) && !TestableRunnerExtensionLoader.isTestableEmpty(protocolElement))
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pureElement;
                try
                {
                    pureElement = model.getPureModel().getPackageableElement(protocolElement.getPath());
                }
                catch (Exception ignored)
                {
                    return;
                }
                if (pureElement instanceof Testable)
                {
                    String testablePath = protocolElement.getPath();
                    ((Testable) pureElement)._tests().forEach(test ->
                    {
                        if (test instanceof Root_meta_pure_test_AtomicTest)
                        {
                            candidates.add(new TestCandidate(testablePath, null, test._id()));
                        }
                        else if (test instanceof Root_meta_pure_test_TestSuite)
                        {
                            Root_meta_pure_test_TestSuite suite = (Root_meta_pure_test_TestSuite) test;
                            String suiteId = suite._id();
                            suite._tests().collect(t -> new TestCandidate(testablePath, suiteId, t._id()), candidates);
                        }
                    });
                }
            }
        });
        return candidates;
    }

    /**
     * Build {@link RunTestsTestableInput}s for every primary-scope testable
     * element that has at least one test. Each input has empty {@code unitTestIds},
     * meaning all tests on that testable will be executed.
     */
    public static MutableList<RunTestsTestableInput> findTestableInputs(EMITModel model)
    {
        return ListIterate.collectIf(model.getPmcd().getElements(),
                e -> model.isPrimary(e) && TestableRunnerExtensionLoader.isTestable(e) && !TestableRunnerExtensionLoader.isTestableEmpty(e),
                e ->
                {
                    RunTestsTestableInput input = new RunTestsTestableInput();
                    input.testable = e.getPath();
                    return input;
                });
    }

    // ----- helpers -----

    private static String readFile(EMITSourceFile file)
    {
        try
        {
            return new String(Files.readAllBytes(file.getAbsolutePath()), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static String buildEngineExceptionMessage(EngineException e)
    {
        return appendEngineExceptionMessage(new StringBuilder(), e).toString();
    }

    private static StringBuilder appendEngineExceptionMessage(StringBuilder builder, EngineException e)
    {
        EngineErrorType errorType = e.getErrorType();
        if (errorType != null)
        {
            builder.append(errorType).append(" error");
        }

        SourceInformation sourceInfo = e.getSourceInformation();
        if ((sourceInfo != null) && (sourceInfo != SourceInformation.getUnknownSourceInformation()))
        {
            builder.append(" at ").append(sourceInfo.sourceId);
            builder.append('[');
            if (sourceInfo.startLine == sourceInfo.endLine)
            {
                builder.append(sourceInfo.startLine).append(':');
                if (sourceInfo.startColumn == sourceInfo.endColumn)
                {
                    builder.append(sourceInfo.startColumn);
                }
                else
                {
                    builder.append(sourceInfo.startColumn).append('-').append(sourceInfo.endColumn);
                }
                builder.append(']');
            }
            else
            {
                builder.append(sourceInfo.startLine).append(':').append(sourceInfo.startColumn)
                        .append('-')
                        .append(sourceInfo.endLine).append(':').append(sourceInfo.endColumn).append(']');
            }
        }

        String message = e.getMessage();
        if (message != null)
        {
            builder.append(": ").append(message);
        }
        return builder;
    }

    // ----- Result records -----

    /**
     * Output of {@link #parse(EMITSourceSet)}.
     */
    public static final class ParseResult
    {
        private final PureModelContextData pmcd;
        private final MutableSet<String> primarySourceIds;
        private final int totalElementCount;

        ParseResult(PureModelContextData pmcd, MutableSet<String> primarySourceIds, int totalElementCount)
        {
            this.pmcd = pmcd;
            this.primarySourceIds = primarySourceIds;
            this.totalElementCount = totalElementCount;
        }

        public PureModelContextData getPmcd()
        {
            return this.pmcd;
        }

        public MutableSet<String> getPrimarySourceIds()
        {
            return this.primarySourceIds;
        }

        public int getTotalElementCount()
        {
            return this.totalElementCount;
        }
    }

    /**
     * Output of {@link #runModelGeneration(EMITModel)}.
     */
    public static final class ModelGenResult
    {
        private final boolean applicable;
        private final EMITModel newModel;
        private final int generatedElementCount;
        private final int generationCount;

        private ModelGenResult(boolean applicable, EMITModel newModel, int generatedElementCount, int generationCount)
        {
            this.applicable = applicable;
            this.newModel = newModel;
            this.generatedElementCount = generatedElementCount;
            this.generationCount = generationCount;
        }

        public boolean isApplicable()
        {
            return this.applicable;
        }

        /**
         * Non-null only when {@link #isApplicable()} is true and at least one element was generated.
         *
         * @return new model
         */
        public EMITModel getNewModel()
        {
            return this.newModel;
        }

        public int getGeneratedElementCount()
        {
            return this.generatedElementCount;
        }

        public int getGenerationCount()
        {
            return this.generationCount;
        }

        static ModelGenResult notApplicable()
        {
            return new ModelGenResult(false, null, 0, 0);
        }

        static ModelGenResult applicableButNoOutput()
        {
            return new ModelGenResult(true, null, 0, 0);
        }

        static ModelGenResult generated(EMITModel newModel, int generatedElementCount, int generationCount)
        {
            return new ModelGenResult(true, newModel, generatedElementCount, generationCount);
        }
    }

    /**
     * A {@code (extension, element)} pair returned by {@link #findArtifactGenerationCandidates(EMITModel)}.
     */
    public static final class ArtifactCandidate
    {
        public final ArtifactGenerationExtension extension;
        public final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pureElement;
        public final String elementPath;

        ArtifactCandidate(ArtifactGenerationExtension extension, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pureElement, String elementPath)
        {
            this.extension = extension;
            this.pureElement = pureElement;
            this.elementPath = elementPath;
        }
    }

    /**
     * A {@code (testable, suite, atomicTest)} tuple returned by {@link #findTestCandidates(EMITModel)}.
     */
    public static final class TestCandidate
    {
        public final String testablePath;
        /**
         * {@code null} for a top-level atomic test that is not inside any suite.
         */
        public final String suiteId;
        public final String atomicTestId;

        TestCandidate(String testablePath, String suiteId, String atomicTestId)
        {
            this.testablePath = testablePath;
            this.suiteId = suiteId;
            this.atomicTestId = atomicTestId;
        }
    }

    /**
     * A legacy Mapping test candidate: a Mapping with at least one {@link MappingTest_Legacy}.
     */
    public static final class LegacyMappingTestCandidate
    {
        public final String mappingPath;
        public final MappingTest_Legacy test;

        LegacyMappingTestCandidate(String mappingPath, MappingTest_Legacy test)
        {
            this.mappingPath = mappingPath;
            this.test = test;
        }
    }

    /**
     * A legacy Service test candidate: a Service with a non-null deprecated {@code test} field.
     */
    public static final class LegacyServiceTestCandidate
    {
        public final String servicePath;
        public final Service service;

        LegacyServiceTestCandidate(String servicePath, Service service)
        {
            this.servicePath = servicePath;
            this.service = service;
        }
    }

    // ----- Legacy test discovery -----

    /**
     * Finds primary-scope {@link Mapping} elements with non-empty legacy {@code tests}.
     * Returns one candidate per individual {@link MappingTest_Legacy}.
     */
    public static List<LegacyMappingTestCandidate> findLegacyMappingTestCandidates(EMITModel model)
    {
        MutableList<LegacyMappingTestCandidate> candidates = Lists.mutable.empty();
        model.getPmcd().getElements().forEach(e ->
        {
            if (model.isPrimary(e) && (e instanceof Mapping))
            {
                Mapping mapping = (Mapping) e;
                if (mapping.tests != null && !mapping.tests.isEmpty())
                {
                    String path = mapping.getPath();
                    mapping.tests.forEach(test -> candidates.add(new LegacyMappingTestCandidate(path, test)));
                }
            }
        });
        return candidates;
    }

    /**
     * Finds primary-scope {@link Service} elements with a non-null deprecated
     * {@code test} field ({@link ServiceTest_Legacy}).
     */
    public static List<LegacyServiceTestCandidate> findLegacyServiceTestCandidates(EMITModel model)
    {
        MutableList<LegacyServiceTestCandidate> candidates = Lists.mutable.empty();
        model.getPmcd().getElements().forEach(e ->
        {
            if (model.isPrimary(e) && (e instanceof Service))
            {
                Service service = (Service) e;
                if (service.test != null)
                {
                    candidates.add(new LegacyServiceTestCandidate(service.getPath(), service));
                }
            }
        });
        return candidates;
    }

    // ----- Legacy test execution -----

    /**
     * Runs a single legacy {@link MappingTest_Legacy} using the engine's
     * {@link MappingTestRunner}. Mirrors the SDLC's {@code LegacyMappingTestCase}.
     */
    public static RichMappingTestResult runLegacyMappingTest(String mappingPath, MappingTest_Legacy test, PureModel pureModel)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = loadExtensions(pureModel);
        Iterable<? extends PlanTransformer> transformers = loadPlanTransformers();
        PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        MappingTestRunner runner = new MappingTestRunner(pureModel, mappingPath, test, executor, extensions, transformers, null);
        return runner.setupAndRunTest();
    }

    /**
     * Runs legacy service tests using the engine's {@link ServiceTestRunner}.
     * Mirrors the SDLC's {@code LegacyServiceTestCase}.
     */
    public static List<RichServiceTestResult> runLegacyServiceTest(Service service, PureModelContextData pmcd, PureModel pureModel)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = loadExtensions(pureModel);
        Iterable<? extends PlanTransformer> transformers = loadPlanTransformers();
        PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        ServiceTestRunner runner = new ServiceTestRunner(service, null, pmcd, pureModel, null, executor, extensions, transformers, null);
        try
        {
            return runner.executeTests();
        }
        catch (Exception e)
        {
            throw new EMITException(EMITPhase.TEST_EXECUTION, "Error running legacy service test for service " + service.getPath(), e);
        }
    }

    // ----- Legacy test assertions -----

    public static void assertLegacyMappingTestPassed(RichMappingTestResult result)
    {
        switch (result.getResult())
        {
            case ERROR:
            {
                Exception e = result.getException();
                throw new EMITException(EMITPhase.TEST_EXECUTION, "Legacy mapping test errored: " + result.getMappingPath() + " / " + result.getTestName(), e);
            }
            case FAILURE:
            {
                StringBuilder builder = new StringBuilder("Legacy mapping test failed: ").append(result.getMappingPath()).append(" / ").append(result.getTestName());
                if (result.getExpected().isPresent() || result.getActual().isPresent())
                {
                    builder.append("\n  Expected: ").append(result.getExpected().orElse("<none>")).append("\n  Actual: ").append(result.getActual().orElse("<none>"));
                }
                throw new EMITAssertionError(EMITPhase.TEST_EXECUTION, builder.toString());
            }
        }
    }

    /**
     * Throws an {@link AssertionError} if any of the legacy service test
     * results contain failures or errors.
     */
    public static void assertLegacyServiceTestPassed(List<RichServiceTestResult> results)
    {
        MutableList<String> failures = Lists.mutable.empty();
        MutableList<String> errors = Lists.mutable.empty();
        results.forEach(run ->
        {
            if (run.getResults() != null)
            {
                run.getResults().forEach((key, r) ->
                {
                    switch (r)
                    {
                        case FAILURE:
                        {
                            failures.add(key);
                            break;
                        }
                        case ERROR:
                        {
                            errors.add(key);
                            break;
                        }
                    }
                });
            }
        });
        if (failures.notEmpty() || errors.notEmpty())
        {
            StringBuilder builder = new StringBuilder("Legacy service test failures/errors:");
            if (failures.notEmpty())
            {
                builder.append("\n  Failures: ").append(failures.makeString(", "));
            }
            if (errors.notEmpty())
            {
                builder.append("\n  Errors: ").append(errors.makeString(", "));
            }
            throw new EMITAssertionError(EMITPhase.TEST_EXECUTION, builder.toString());
        }
    }

    // ----- Extension loading helpers -----

    private static RichIterable<? extends Root_meta_pure_extension_Extension> loadExtensions(PureModel pureModel)
    {
        return PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
    }

    private static MutableList<PlanTransformer> loadPlanTransformers()
    {
        return Iterate.flatCollect(ServiceLoader.load(PlanGeneratorExtension.class), PlanGeneratorExtension::getExtraPlanTransformers, Lists.mutable.empty());
    }
}
