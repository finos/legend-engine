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

package org.finos.legend.engine.language.pure.grammar.test;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TestExternalFormatGrammarComposer
{
    @Test
    public void testFreeSectionGrammarCompose()
    {
        ExternalFormatSchema one = new ExternalFormatSchema();
        one.id = "one";
        one.location = "first.place";
        one.content = "Example one";

        ExternalFormatSchema two = new ExternalFormatSchema();
        two.id = "two";
        two.location = "second.place";
        two.content = "Example two";

        ExternalFormatSchemaSet set = new ExternalFormatSchemaSet();
        set._package = "test.grammar";
        set.name = "ExampleSet";
        set.format = "Example";
        set.schemas = Arrays.asList(one, two);

        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test.grammar.AClass");

        Binding binding = new Binding();
        binding._package = "test.grammar";
        binding.name = "ExampleBinding";
        binding.contentType = "test/example";
        binding.schemaSet = "test.grammar.ExampleSet";
        binding.schemaId = "one";
        binding.modelUnit = modelUnit;

        PureModelContextData context = PureModelContextData.newBuilder().withElement(set).withElement(binding).build();
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);

        String expected =
                "###ExternalFormat\n" +
                        "SchemaSet 'test.grammar'::ExampleSet\n" +
                        "{\n" +
                        "  format: Example;\n" +
                        "  schemas: [\n" +
                        "    {\n" +
                        "      id: one;\n" +
                        "      location: 'first.place';\n" +
                        "      content: 'Example one';\n" +
                        "    },\n" +
                        "    {\n" +
                        "      id: two;\n" +
                        "      location: 'second.place';\n" +
                        "      content: 'Example two';\n" +
                        "    }\n" +
                        "  ];\n" +
                        "}\n" +
                        "\n" +
                        "Binding 'test.grammar'::ExampleBinding\n" +
                        "{\n" +
                        "  schemaSet: 'test.grammar.ExampleSet';\n" +
                        "  schemaId: one;\n" +
                        "  contentType: 'test/example';\n" +
                        "  modelIncludes: [\n" +
                        "    'test.grammar.AClass'\n" +
                        "  ];\n" +
                        "}\n";

        Assert.assertEquals(expected, formatted);
    }
}
