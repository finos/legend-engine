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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.model.ModelGenerationRunner;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.test.emit.extensions.EmitDemoModelGenerationExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TestModelGenerationRunner
{
    private static final String SOURCE_A = "###Pure\n"
            + "Class demo::modelgen::SourceA\n"
            + "{\n"
            + "  name: String[1];\n"
            + "}\n";

    @Test
    void parallelGroupGeneratesEveryElement()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class demo::modelgen::SourceB\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification demo::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElements: [\n"
                + "        demo::modelgen::SourceA,\n"
                + "        demo::modelgen::SourceB\n"
                + "      ];\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ModelGenerationRunner runner = newRunner(model).withParallelism(2);
        ModelGenerationRunner.Result result = runner.run();

        Assertions.assertEquals(2, result.getGenerationCount());
        Assertions.assertEquals(
                Arrays.asList(generatedPath("SourceA"), generatedPath("SourceB")),
                result.getGeneratedModel().getElements().stream().map(PackageableElement::getPath).collect(Collectors.toList()));
        Assertions.assertNotNull(runner.getPureModel().getClass(generatedPath("SourceA")), "deferred recompile must pick up the generated elements");
    }

    @Test
    void generationElementsOfANodeCannotGenerateTheSameElement()
    {
        PureModelContextData model = parse(SOURCE_A
                + "Class demo::modelgen::other::SourceA\n"
                + "{\n"
                + "  name: String[1];\n"
                + "}\n"
                + "###GenerationSpecification\n"
                + "GenerationSpecification demo::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElements: [\n"
                + "        demo::modelgen::SourceA,\n"
                + "        demo::modelgen::other::SourceA\n"
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
                + "GenerationSpecification demo::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: demo::modelgen::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        EngineException e = Assertions.assertThrows(EngineException.class, () -> newRunner(model).run());

        Assertions.assertEquals("Generated element '" + generatedPath("SourceA") + "' of type Class can't override existing element of type Class", e.getMessage());
    }

    @Test
    void oneGenerationProducingAPathTwiceStaysDeduped()
    {
        PureModelContextData model = parse(SOURCE_A
                + "###GenerationSpecification\n"
                + "GenerationSpecification demo::modelgen::Spec\n"
                + "{\n"
                + "  generationNodes: [\n"
                + "    {\n"
                + "      generationElement: demo::modelgen::SourceA;\n"
                + "    }\n"
                + "  ];\n"
                + "}\n");

        ModelGenerationRunner runner = newRunner(model);
        runner.addGeneratedModel("demo::modelgen::SourceA", PureModelContextData.newBuilder()
                .withElement(generatedClass())
                .withElement(generatedClass())
                .build());

        Assertions.assertEquals(1, runner.getGeneratedModel().getElements().size(), "a generation repeating a path within its own output was deduped before, and must stay so");
    }

    private static org.finos.legend.engine.protocol.pure.m3.type.Class generatedClass()
    {
        org.finos.legend.engine.protocol.pure.m3.type.Class generated = new org.finos.legend.engine.protocol.pure.m3.type.Class();
        generated.name = "SourceA" + EmitDemoModelGenerationExtension.GENERATED_SUFFIX;
        generated._package = EmitDemoModelGenerationExtension.GENERATED_PACKAGE;
        return generated;
    }

    private static ModelGenerationRunner newRunner(PureModelContextData model)
    {
        PureModel pureModel = EMITTasks.compile(model);
        GenerationSpecification spec = model.getElementsOfType(GenerationSpecification.class).get(0);
        return ModelGenerationRunner.newRunner(spec, model, pureModel).withCompiler(EMITTasks::compile);
    }

    private static String generatedPath(String sourceName)
    {
        return EmitDemoModelGenerationExtension.GENERATED_PACKAGE + "::" + sourceName + EmitDemoModelGenerationExtension.GENERATED_SUFFIX;
    }

    private static PureModelContextData parse(String grammar)
    {
        return PureGrammarParser.newInstance().parseModel(grammar);
    }
}
