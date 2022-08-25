//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.shared.format.model.transformation.toModel;

import org.finos.legend.engine.external.shared.format.model.exampleSchema.ExampleSchemaToModelConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.junit.Assert;
import org.junit.Test;

public class TestExampleSchemaToModelGeneration extends SchemaToModelGenerationTest
{
    @Test
    public void testModelGenerationWithoutBinding()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "Example").withSchemaText("dummy").build();

        PureModelContextData model = generateModel(schemaCode, config());

        String expected = ">>>meta::external::shared::format::transformation::tests::ExampleSchema\n" +
                "Class meta::external::shared::format::transformation::tests::ExampleSchema extends meta::external::shared::format::metamodel::SchemaDetail\n" +
                "{\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));

        Assert.assertEquals(1, model.getElements().size());
    }

    @Test
    public void testModelGenerationWithBinding()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "Example").withSchemaText("dummy").build();

        PureModelContextData model = generateModel(schemaCode, config(), true, "test::gen::TargetBinding");

        String expected = ">>>meta::external::shared::format::transformation::tests::ExampleSchema\n" +
                "Class meta::external::shared::format::transformation::tests::ExampleSchema extends meta::external::shared::format::metamodel::SchemaDetail\n" +
                "{\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));

        Assert.assertEquals(2, model.getElements().size());

        Binding genBinding = model.getElementsOfType(Binding.class).get(0);
        Assert.assertEquals("test::gen::TargetBinding", genBinding.getPath());
        Assert.assertEquals("text/example", genBinding.contentType);
        Assert.assertArrayEquals(new String[] {"meta::external::shared::format::transformation::tests::ExampleSchema"}, genBinding.modelUnit.packageableElementIncludes.toArray());
    }

    private ExampleSchemaToModelConfiguration config()
    {
        ExampleSchemaToModelConfiguration config = new ExampleSchemaToModelConfiguration();

        config.format = "Example";
        config.targetPackage = "test";

        return config;
    }
}
