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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestExternalFormatCompilation
{
    @Test
    public void testValidSchemaSet()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###ExternalFormat\n" +
                        "SchemaSet meta::firm::SchemaSet\n" +
                        "{\n" +
                        "  format: Example;\n" +
                        "  schemas: [ \n" +
                        "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                        "    { id: s2; location: 'e2.schema'; content: 'example2'; }\n" +
                        "  ];\n" +
                        "}\n");
        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = HelperExternalFormat.getSchemaSet("meta::firm::SchemaSet", compiledGraph.getTwo().getContext());
        Assert.assertEquals("SchemaSet", schemaSet._name());
        Assert.assertEquals("firm", schemaSet._package().getName());
    }

    @Test
    public void testSchemaSetDuplicateIds()
    {
        test("###ExternalFormat\n" +
                     "SchemaSet meta::firm::SchemaSet\n" +
                     "{\n" +
                     "  format: Example;\n" +
                     "  schemas: [ \n" +
                     "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                     "    { id: s1; location: 'e2.schema'; content: 'example2'; }\n" +
                     "  ];\n" +
                     "}\n",
             "COMPILATION error at [7:5-59]: Schema id 's1' is duplicated");
    }

    @Test
    public void testSchemaSetDuplicateLocations()
    {
        test("###ExternalFormat\n" +
                     "SchemaSet meta::firm::SchemaSet\n" +
                     "{\n" +
                     "  format: Example;\n" +
                     "  schemas: [ \n" +
                     "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                     "    { id: s2; location: 'e1.schema'; content: 'example2'; }\n" +
                     "  ];\n" +
                     "}\n",
             "COMPILATION error at [7:5-59]: Schema location 'e1.schema' is duplicated");
    }

    @Test
    public void testValidBinding()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Pure\n" +
                        "Class meta::firm::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "Class meta::firm::Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###ExternalFormat\n" +
                        "SchemaSet meta::firm::SchemaSet\n" +
                        "{\n" +
                        "  format: Example;\n" +
                        "  schemas: [ \n" +
                        "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                        "    { id: s2; location: 'e2.schema'; content: 'example2'; }\n" +
                        "  ];\n" +
                        "}\n" +
                        "\n" +
                        "Binding meta::firm::Binding\n" +
                        "{\n" +
                        "  schemaSet: meta::firm::SchemaSet;\n" +
                        "  schemaId: s1;\n" +
                        "  contentType: 'text/example';\n" +
                        "  modelIncludes: [ meta::firm ];\n" +
                        "  modelExcludes: [ meta::firm::Person ];\n" +
                        "}\n"
        );
        Root_meta_external_shared_format_binding_Binding binding = HelperExternalFormat.getBinding("meta::firm::Binding", compiledGraph.getTwo().getContext());
        Assert.assertEquals("Binding", binding._name());
        Assert.assertEquals("firm", binding._package().getName());
        Assert.assertEquals("SchemaSet", binding._schemaSet()._name());
        Assert.assertEquals("firm", binding._modelUnit()._packageableElementIncludes().toList().get(0)._name());
        Assert.assertEquals("s1", binding._schemaId());
        Assert.assertEquals("text/example", binding._contentType());
        Assert.assertEquals("Person", binding._modelUnit()._packageableElementExcludes().toList().get(0)._name());
    }

    @Test
    public void testValidBindingWithoutSchemaSet()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Pure\n" +
                        "Class meta::firm::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "Class meta::firm::Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###ExternalFormat\n" +
                        "Binding meta::firm::Binding\n" +
                        "{\n" +
                        "  contentType: 'text/example';\n" +
                        "  modelIncludes: [ meta::firm ];\n" +
                        "  modelExcludes: [ meta::firm::Person ];\n" +
                        "}\n"
        );
        Root_meta_external_shared_format_binding_Binding binding = HelperExternalFormat.getBinding("meta::firm::Binding", compiledGraph.getTwo().getContext());
        Assert.assertEquals("Binding", binding._name());
        Assert.assertEquals("firm", binding._package().getName());
        Assert.assertEquals("text/example", binding._contentType());
        Assert.assertEquals("Person", binding._modelUnit()._packageableElementExcludes().toList().get(0)._name());
    }

    @Test
    public void testBindingInvalidSet()
    {
        test("###Pure\n" +
                     "Class meta::firm::Person\n" +
                     "{\n" +
                     "  fullName: String[1];\n" +
                     "}\n" +
                     "\n" +
                     "###ExternalFormat\n" +
                     "Binding meta::firm::Binding\n" +
                     "{\n" +
                     "  schemaSet: meta::firm::Unknown;\n" +
                     "  contentType: 'text/example';\n" +
                     "  modelIncludes: [\n" +
                     "    meta::firm::Person\n" +
                     "  ];\n" +
                     "}\n",
             "COMPILATION error at [8:1-15:1]: Can't find SchemaSet 'meta::firm::Unknown'"
        );
    }

    @Test
    public void testBindingInvalidContentType()
    {
        test("###Pure\n" +
                     "Class meta::firm::Person\n" +
                     "{\n" +
                     "  fullName: String[1];\n" +
                     "}\n" +
                     "\n" +
                     "###ExternalFormat\n" +
                     "Binding meta::firm::Binding\n" +
                     "{\n" +
                     "  contentType: 'text/unknown';\n" +
                     "  modelIncludes: [\n" +
                     "    meta::firm::Person\n" +
                     "  ];\n" +
                     "}\n",
             "COMPILATION error at [8:1-14:1]: Unknown contentType 'text/unknown'"
        );
    }

    @Test
    public void testBindingInvalidId()
    {
        test("###Pure\n" +
                     "Class meta::firm::Person\n" +
                     "{\n" +
                     "  fullName: String[1];\n" +
                     "}\n" +
                     "\n" +
                     "###ExternalFormat\n" +
                     "SchemaSet meta::firm::SchemaSet\n" +
                     "{\n" +
                     "  format: Example;\n" +
                     "  schemas: [ \n" +
                     "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                     "    { id: s2; location: 'e2.schema'; content: 'example2'; }\n" +
                     "  ];\n" +
                     "}\n" +
                     "\n" +
                     "Binding meta::firm::Binding\n" +
                     "{\n" +
                     "  schemaSet: meta::firm::SchemaSet;\n" +
                     "  schemaId: unknown;\n" +
                     "  contentType: 'text/example';\n" +
                     "  modelIncludes: [ meta::firm::Person ];\n" +
                     "}\n",
             "COMPILATION error at [17:1-23:1]: ID 'unknown' does not exist in SchemaSet 'meta::firm::SchemaSet'"
        );
    }

    @Test
    public void testBindingInvalidIncludeClass()
    {
        test("###Pure\n" +
                     "Class meta::firm::Person\n" +
                     "{\n" +
                     "  fullName: String[1];\n" +
                     "}\n" +
                     "\n" +
                     "###ExternalFormat\n" +
                     "SchemaSet meta::firm::SchemaSet\n" +
                     "{\n" +
                     "  format: Example;\n" +
                     "  schemas: [ \n" +
                     "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                     "    { id: s2; location: 'e2.schema'; content: 'example2'; }\n" +
                     "  ];\n" +
                     "}\n" +
                     "\n" +
                     "Binding meta::firm::Binding\n" +
                     "{\n" +
                     "  schemaSet: meta::firm::SchemaSet;\n" +
                     "  schemaId: s1;\n" +
                     "  contentType: 'text/example';\n" +
                     "  modelIncludes: [ meta::firm::Unknown ];\n" +
                     "}\n",
             "COMPILATION error at [17:1-23:1]: Can't find the packageable element 'meta::firm::Unknown'"
        );
    }

    @Test
    public void testBindingInvalidExcludeClass()
    {
        test("###Pure\n" +
                     "Class meta::firm::Person\n" +
                     "{\n" +
                     "  fullName: String[1];\n" +
                     "}\n" +
                     "\n" +
                     "###ExternalFormat\n" +
                     "SchemaSet meta::firm::SchemaSet\n" +
                     "{\n" +
                     "  format: Example;\n" +
                     "  schemas: [ \n" +
                     "    { id: s1; location: 'e1.schema'; content: 'example1'; },\n" +
                     "    { id: s2; location: 'e2.schema'; content: 'example2'; }\n" +
                     "  ];\n" +
                     "}\n" +
                     "\n" +
                     "Binding meta::firm::Binding\n" +
                     "{\n" +
                     "  schemaSet: meta::firm::SchemaSet;\n" +
                     "  schemaId: s1;\n" +
                     "  contentType: 'text/example';\n" +
                     "  modelIncludes: [ meta::firm ];\n" +
                     "  modelExcludes: [ meta::firm::Unknown ];\n" +
                     "}\n",
             "COMPILATION error at [17:1-24:1]: Can't find the packageable element 'meta::firm::Unknown'"
        );
    }
}
