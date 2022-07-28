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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.plan.platform.java.JavaSourceHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.core_pure_extensions_functions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestServicePlanJavaSourceHelper
{
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testSingleExecutionJsonServiceJavaExtractionAndRemoval() throws IOException
    {
        testJavaExtractionAndRemoval("simpleJsonService.json");
    }

    private void testJavaExtractionAndRemoval(String... planResourceNames) throws IOException
    {
        List<ExecutionPlan> plans = Arrays.stream(planResourceNames)
                .map(this::loadModelDataFromResource)
                .flatMap(pureModelContextData ->
                {
                    PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
                    return pureModelContextData.getElementsOfType(Service.class).stream().map(s -> ServicePlanGenerator.generateServiceExecutionPlan(s, null, pureModel, "vX_X_X", PlanPlatform.JAVA, core_pure_extensions_functions.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers));
                })
                .collect(Collectors.toList());
        Map<String, String> sources = JavaSourceHelper.getJavaSourceCodeByRelativeFilePath(plans.stream());
        if (plans.stream().anyMatch(this::hasJavaPlatformImplementation))
        {
            Assert.assertFalse("Expected sources", sources.isEmpty());
        }
        else
        {
            Assert.assertTrue(sources.keySet().stream().sorted().collect(Collectors.joining(", ", "Unexpected sources: ", "")), sources.isEmpty());
        }

        Path srcDir = writeSourcesToFiles(plans);
        assertSourcesInDirectory(sources, srcDir);

        JavaSourceHelper.removeJavaImplementationClasses(plans.stream());
        Assert.assertEquals(Collections.emptyMap(), JavaSourceHelper.getJavaSourceCodeByRelativeFilePath(plans.stream()));
    }

    private Path writeSourcesToFiles(Collection<? extends ExecutionPlan> plans) throws IOException
    {
        Path root = this.tmpFolder.newFolder("src", "main", "java").toPath();
        JavaSourceHelper.writeJavaSourceFiles(root, plans.stream());
        return root;
    }

    private Map<Path, String> gatherSources(Path srcDir) throws IOException
    {
        Map<Path, String> sourcesByRelativePath = new HashMap<>();
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Path relativePath = srcDir.relativize(file);
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                sourcesByRelativePath.put(relativePath, source);
                return FileVisitResult.CONTINUE;
            }
        });
        return sourcesByRelativePath;
    }

    private void assertSourcesInDirectory(Map<String, String> expectedSources, Path srcDir) throws IOException
    {
        Map<Path, String> expectedSourcesWithPaths = expectedSources.entrySet().stream().collect(Collectors.toMap(p -> srcDir.getFileSystem().getPath(p.getKey()), Map.Entry::getValue));
        Map<Path, String> actualSources = gatherSources(srcDir);
        if (!expectedSourcesWithPaths.equals(actualSources))
        {
            List<String> missing = new ArrayList<>();
            List<String> different = new ArrayList<>();
            expectedSourcesWithPaths.forEach((expectedPath, expectedSource) ->
            {
                String actualSource = actualSources.remove(expectedPath);
                if (actualSource == null)
                {
                    missing.add(expectedPath.toString());
                }
                else if (!actualSource.equals(expectedSource))
                {
                    different.add(expectedPath.toString());
                }
            });

            StringBuilder builder = new StringBuilder();
            if (!missing.isEmpty())
            {
                missing.sort(Comparator.naturalOrder());
                builder.append("Missing sources: ").append(String.join(", ", missing));
            }
            if (!actualSources.isEmpty())
            {
                if (!missing.isEmpty())
                {
                    builder.append('\n');
                }
                builder.append(actualSources.keySet().stream().map(Path::toString).sorted().collect(Collectors.joining(", ", "Extra sources: ", "")));
            }
            if (!different.isEmpty())
            {
                if (!missing.isEmpty() || !actualSources.isEmpty())
                {
                    builder.append('\n');
                }
                different.sort(Comparator.naturalOrder());
                builder.append("Different sources: ").append(String.join(", ", different));
            }
            Assert.fail(builder.toString());
        }
    }

    private boolean hasJavaPlatformImplementation(ExecutionPlan plan)
    {
        if (plan instanceof SingleExecutionPlan)
        {
            return hasJavaPlatformImplementation((SingleExecutionPlan) plan);
        }
        if (plan instanceof CompositeExecutionPlan)
        {
            return ((CompositeExecutionPlan) plan).executionPlans.values().stream().anyMatch(this::hasJavaPlatformImplementation);
        }
        return false;
    }

    private boolean hasJavaPlatformImplementation(SingleExecutionPlan plan)
    {
        return (plan.globalImplementationSupport instanceof JavaPlatformImplementation) || hasJavaPlatformImplementation(plan.rootExecutionNode);
    }

    private boolean hasJavaPlatformImplementation(ExecutionNode node)
    {
        return (node.implementation instanceof JavaPlatformImplementation) || node.executionNodes.stream().anyMatch(this::hasJavaPlatformImplementation);
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
