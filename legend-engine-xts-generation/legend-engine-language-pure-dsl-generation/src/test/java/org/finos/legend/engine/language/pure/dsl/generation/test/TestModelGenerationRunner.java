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

package org.finos.legend.engine.language.pure.dsl.generation.test;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.model.ModelGenerationRunner;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.ForkJoinPool;

public class TestModelGenerationRunner
{
    private static final String SOURCE_A = "###Pure\n"
            + "Class test::modelgen::SourceA\n"
            + "{\n"
            + "  name: String[1];\n"
            + "}\n";

    @Test
    void parallelGroupGeneratesEveryElement()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class test::modelgen::SourceB\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElements: [\n"
                + "        test::modelgen::SourceA,\n"
                + "        test::modelgen::SourceB\n"
                + "      ];\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ModelGenerationRunner runner = newRunner(model).withForkJoinPool(new ForkJoinPool(2));
        ModelGenerationRunner.Result result = runner.run();

        Assertions.assertEquals(2, result.getGenerationCount());
        Assertions.assertEquals(
                Sets.mutable.with(generatedPath("SourceA"), generatedPath("SourceB")),
                Iterate.collect(result.getGeneratedModel().getElements(), PackageableElement::getPath, Sets.mutable.empty()));
        Assertions.assertNotNull(runner.getPureModel().getClass(generatedPath("SourceA")), "the run must leave a graph holding the generated elements");
    }

    @Test
    void generationsInDifferentNodesProducingTheSameElementStayDeduped()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class test::modelgen::other::SourceA\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
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

        ModelGenerationRunner.Result result = newRunner(model).run();

        Assertions.assertEquals(2, result.getGenerationCount());
        Assertions.assertEquals(
                Collections.singletonList(generatedPath("SourceA")),
                Iterate.collect(result.getGeneratedModel().getElements(), PackageableElement::getPath),
                "two nodes generating the same element was deduped before, and must stay so");
    }

    @Test
    void oneGenerationProducingAPathTwiceStaysDeduped()
    {
        PureModelContextData model = parse(SOURCE_A
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: test::modelgen::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ModelGenerationRunner runner = newRunner(model);
        runner.addGeneratedModel("test::modelgen::SourceA", PureModelContextData.newBuilder()
                .withElement(generatedClass())
                .withElement(generatedClass())
                .build());

        Assertions.assertEquals(1, runner.getGeneratedModel().getElements().size(), "a generation repeating a path within its own output was deduped before, and must stay so");
    }

    @Test
    void generationElementsOfANodeCannotGenerateTheSameElement()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class test::modelgen::other::SourceA\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElements: [\n"
                + "        test::modelgen::SourceA,\n"
                + "        test::modelgen::other::SourceA\n"
                + "      ];\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        EngineException e = Assertions.assertThrows(EngineException.class, () -> newRunner(model).run());

        Assertions.assertTrue(e.getMessage().contains("both generated element '" + generatedPath("SourceA") + "'"),
                () -> "Expected the colliding generated element to be reported, got: " + e.getMessage());
    }

    @Test
    void generatedElementsCannotOverrideCoreElements()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class " + generatedPath("SourceA") + "\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification test::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: test::modelgen::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        EngineException e = Assertions.assertThrows(EngineException.class, () -> newRunner(model).run());

        Assertions.assertEquals("Generated element '" + generatedPath("SourceA") + "' of type Class can't override existing element of type Class", e.getMessage());
    }

    private static org.finos.legend.engine.protocol.pure.m3.type.Class generatedClass()
    {
        org.finos.legend.engine.protocol.pure.m3.type.Class generated = new org.finos.legend.engine.protocol.pure.m3.type.Class();
        generated.name = "SourceA" + TestModelGenerationExtension.GENERATED_SUFFIX;
        generated._package = TestModelGenerationExtension.GENERATED_PACKAGE;
        return generated;
    }

    /**
     * Compiles the domain without the generation specification, and hands the runner the
     * specification itself. A generation specification is compiled after its generation elements
     * because of its prerequisite on AbstractGenerationSpecification, which covers anything
     * extending ModelGenerationSpecification. The generation elements here are plain classes,
     * which that does not cover, so compiling the two together resolves them or not depending on
     * the order the compiler processes its dependency groups in, and that order varies from one
     * JVM to the next. Compiling a specification is covered by
     * TestGenerationSpecificationCompilationFromGrammar; these tests are about running one.
     */
    private static ModelGenerationRunner newRunner(PureModelContextData model)
    {
        GenerationSpecification spec = model.getElementsOfType(GenerationSpecification.class).get(0);
        PureModelContextData domain = PureModelContextData.newBuilder()
                .withElements(ListIterate.reject(model.getElements(), element -> element instanceof GenerationSpecification))
                .build();
        return ModelGenerationRunner.newRunner(spec, domain, new PureModel(domain, null, null, DeploymentMode.PROD));
    }

    private static String generatedPath(String sourceName)
    {
        return TestModelGenerationExtension.GENERATED_PACKAGE + "::" + sourceName + TestModelGenerationExtension.GENERATED_SUFFIX;
    }

    private static PureModelContextData parse(String grammar)
    {
        return PureGrammarParser.newInstance().parseModel(grammar);
    }
}
