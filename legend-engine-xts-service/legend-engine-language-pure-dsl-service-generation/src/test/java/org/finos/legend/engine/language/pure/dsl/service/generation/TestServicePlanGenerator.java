// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension;
import org.finos.legend.pure.generated.core_pure_extensions_functions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestServicePlanGenerator
{

    private class DummyExecutionOption extends ExecutionOption
    {
    }

    @Test
    public void testSingleExecutionServiceGenerationJSON()
    {
        PureModelContextData data = loadModelDataFromResource("simpleJsonService.json");
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension.Root_meta_pure_mapping_modelToModel_executionPlan_platformBinding_legendJava_inMemoryExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers);
        Assert.assertTrue(plan instanceof SingleExecutionPlan);
    }

    @Test
    public void testSingleExecutionServiceGenerationWithExecutionOptions()
    {
        PureModelContextData data = loadModelDataFromResource("simpleJsonService.json");
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        ((PureSingleExecution) service.execution).executionOptions = Lists.mutable.of(new DummyExecutionOption());
        try
        {
            ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, core_pure_extensions_functions.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers);
            Assert.fail("Expected Exception, since we did not include the dummy execution option in the compiler extensions");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Unsupported execution option type 'class org.finos.legend.engine.language.pure.dsl.service.generation.TestServicePlanGenerator$DummyExecutionOption'", e.getMessage());
        }
    }

    @Test
    public void testGenerationWithFunctionUsingFromEnvironment() throws Exception
    {
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(new Scanner(getClass().getClassLoader().getResource("ServiceSpecificationUsingFromEnvironment.pure").openStream(), "UTF-8").useDelimiter("\\A").next());
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        MutableList<PlanGeneratorExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, routerExtensions, LegendPlanTransformers.transformers);
        Assert.assertTrue(plan instanceof SingleExecutionPlan);
    }

    // [A][B] two services over the same model: bound separately each emits its own copy of the shared,
    // model-derived type layer; bound through the merge path each shared type is emitted exactly once
    // while both services keep their own per-plan node classes.
    @Test
    public void testCrossServiceTypeDeduplicationOnMerge()
    {
        PureModelContextData data = loadModelDataFromResource("simpleJsonService.json");
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        PureSingleExecution se = (PureSingleExecution) service.execution;

        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension.Root_meta_pure_mapping_modelToModel_executionPlan_platformBinding_legendJava_inMemoryExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport());

        Mapping mapping = se.mapping != null ? pureModel.getMapping(se.mapping) : null;
        Root_meta_core_runtime_Runtime runtime = se.runtime != null ? HelperRuntimeBuilder.buildPureRuntime(se.runtime, pureModel.getContext()) : null;
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(se.func.body, se.func.parameters, pureModel.getContext());

        Root_meta_pure_executionPlan_ExecutionPlan routed = PlanGenerator.generateExecutionPlanAsPure(lambda, mapping, runtime, null, pureModel, null, null, extensions);

        // baseline: each service bound on its own, so each emits a full copy
        List<String> baseline = new ArrayList<>();
        baseline.addAll(classFqns(PlanPlatform.JAVA.bindPlan(routed, "svcA", pureModel, extensions), pureModel, extensions));
        baseline.addAll(classFqns(PlanPlatform.JAVA.bindPlan(routed, "svcB", pureModel, extensions), pureModel, extensions));

        // merge path: node code per leaf, shared layer built once at the root, dependencies resolved once
        Root_meta_pure_executionPlan_platformBinding_legendJava_NodesAndTypeInfos nt =
                PlanPlatform.JAVA.bindPlansToNodeAst(Lists.mutable.with(routed, routed), Lists.mutable.with("svcA", "svcB"), pureModel, extensions);
        Root_meta_external_language_java_metamodel_project_Project full =
                PlanPlatform.JAVA.generateSharedTypes(nt._typeInfos(), pureModel, extensions);
        if (nt._project() != null)
        {
            full = PlanPlatform.JAVA.mergeAsts(full, nt._project(), pureModel);
        }
        Root_meta_external_language_java_metamodel_project_Project deps = PlanPlatform.JAVA.resolveProjectDependencies(full, pureModel);
        if (deps != null)
        {
            full = PlanPlatform.JAVA.mergeAsts(full, deps, pureModel);
        }
        List<String> merged = classFqns(PlanPlatform.JAVA.attachMergedImplementationParallel(nt._plans(), full, pureModel, null).getFirst(), pureModel, extensions);

        // shared (model-derived) classes are those NOT in a per-plan node package (planNodeClass -> ".plan.")
        List<String> baselineShared = baseline.stream().filter(fqn -> !fqn.contains(".plan.")).collect(Collectors.toList());
        List<String> mergedShared = merged.stream().filter(fqn -> !fqn.contains(".plan.")).collect(Collectors.toList());
        Set<String> distinctShared = new java.util.HashSet<>(baselineShared);

        Assert.assertFalse("expected some model-derived type classes", distinctShared.isEmpty());
        Assert.assertEquals("baseline emits each shared type once per service", distinctShared.size() * 2, baselineShared.size());
        Assert.assertEquals("merge emits each shared type exactly once", distinctShared.size(), mergedShared.size());
        Assert.assertEquals("merged shared types are distinct", distinctShared.size(), new java.util.HashSet<>(mergedShared).size());
        Assert.assertEquals("merged shared-type set matches the baseline's", distinctShared, new java.util.HashSet<>(mergedShared));
        Assert.assertTrue("svcA keeps its own node classes", merged.stream().anyMatch(fqn -> fqn.toLowerCase().contains("plan_svca")));
        Assert.assertTrue("svcB keeps its own node classes", merged.stream().anyMatch(fqn -> fqn.toLowerCase().contains("plan_svcb")));
        Assert.assertTrue("merge reduces total generated class count (" + merged.size() + " < " + baseline.size() + ")", merged.size() < baseline.size());
    }

    // [B] a plan bound on its own must be self-contained. Node generators leave each CodeDependency
    // attached to the code it was generated into, so a binding path that skips dependency resolution
    // still emits compilable-looking Java that references helper and model classes nobody generated.
    // Asserted as completeness rather than a class count: counts stay consistent when both sides of a
    // comparison are missing the same classes.
    @Test
    public void testSinglePlanBindingEmitsEveryClassItReferences() throws Exception
    {
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResource("ServiceWithFunctionDependency.pure"), "Can't find fixture").openStream(), "UTF-8").useDelimiter("\\A").next());
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        PureSingleExecution se = (PureSingleExecution) service.execution;

        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension.Root_meta_pure_mapping_modelToModel_executionPlan_platformBinding_legendJava_inMemoryExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport());

        Mapping mapping = pureModel.getMapping(se.mapping);
        Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(se.runtime, pureModel.getContext());
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(se.func.body, se.func.parameters, pureModel.getContext());

        Root_meta_pure_executionPlan_ExecutionPlan routed = PlanGenerator.generateExecutionPlanAsPure(lambda, mapping, runtime, null, pureModel, null, null, extensions);
        Root_meta_pure_executionPlan_ExecutionPlan bound = PlanPlatform.JAVA.bindPlan(routed, "svc", pureModel, extensions);

        Set<String> defined = new HashSet<>(classFqns(bound, pureModel, extensions));

        Set<String> unresolved = new TreeSet<>();
        for (String source : classSources(bound, pureModel, extensions))
        {
            for (String line : source.split("\n"))
            {
                String trimmed = line.trim();
                if (trimmed.startsWith("package ") || trimmed.startsWith("import "))
                {
                    continue;
                }
                Matcher matcher = GENERATED_CLASS_REFERENCE.matcher(line);
                while (matcher.find())
                {
                    if (!isGenerated(matcher.group(), defined))
                    {
                        unresolved.add(matcher.group());
                    }
                }
            }
        }
        Assert.assertEquals("emitted Java references classes the plan never generated", new TreeSet<String>(), unresolved);

        // guards the assertion above: a fixture whose transforms call no functions never reaches
        // dependency resolution, so it would satisfy completeness without proving anything
        Assert.assertTrue("fixture must exercise dependency resolution (no _pure.functions.* emitted)", defined.stream().anyMatch(fqn -> fqn.startsWith("_pure.functions.")));
    }

    private static final Pattern GENERATED_CLASS_REFERENCE = Pattern.compile("_pure\\.[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");

    // a reference may name an inner class, an enum constant or a static member, so a defined prefix counts
    private boolean isGenerated(String reference, Set<String> defined)
    {
        for (String candidate = reference; candidate.indexOf('.') > 0; candidate = candidate.substring(0, candidate.lastIndexOf('.')))
        {
            if (defined.contains(candidate))
            {
                return true;
            }
        }
        return false;
    }

    private List<String> classSources(Root_meta_pure_executionPlan_ExecutionPlan bound, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        SingleExecutionPlan plan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(bound, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers));
        if (!(plan.globalImplementationSupport instanceof JavaPlatformImplementation))
        {
            return Lists.mutable.empty();
        }
        JavaPlatformImplementation impl = (JavaPlatformImplementation) plan.globalImplementationSupport;
        if (impl.classes == null)
        {
            return Lists.mutable.empty();
        }
        return impl.classes.stream().map((JavaClass jc) -> jc.source == null ? "" : jc.source).collect(Collectors.toList());
    }

    private List<String> classFqns(Root_meta_pure_executionPlan_ExecutionPlan bound, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        SingleExecutionPlan plan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(bound, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers));
        if (!(plan.globalImplementationSupport instanceof JavaPlatformImplementation))
        {
            return Lists.mutable.empty();
        }
        JavaPlatformImplementation impl = (JavaPlatformImplementation) plan.globalImplementationSupport;
        if (impl.classes == null)
        {
            return Lists.mutable.empty();
        }
        return impl.classes.stream()
                .map((JavaClass jc) -> ((jc._package == null || jc._package.isEmpty()) ? "" : jc._package + ".") + jc.name)
                .collect(Collectors.toList());
    }

    private PureModelContextData loadModelDataFromResource(String resourceName)
    {
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(Objects.requireNonNull(getClass().getClassLoader().getResource(resourceName), "Can't find resource '" + resourceName + "'"), PureModelContextData.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
