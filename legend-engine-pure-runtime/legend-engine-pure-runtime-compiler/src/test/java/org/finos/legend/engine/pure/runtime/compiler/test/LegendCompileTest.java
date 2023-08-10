// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.compiler.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.runtime.compiler.Tools;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

public abstract class LegendCompileTest
{
    protected static PureRuntime runtime;
    protected static FunctionExecution functionExecution;

    @Test
    public void testClass()
    {
        test("let x = meta::legend::compile('Class a::A{}')->cast(@Class<Any>);" +
                   "println($x);" +
                   "assertEquals(1, $x->size());" +
                   "assertEquals('A', $x.name);");
    }

    @Test
    public void testClassPropertiesNavigation()
    {
        test("let x = meta::legend::compile('Class a::A{name:String[1];other:Integer[1];}')->cast(@Class<Any>);" +
                   "println($x);" +
                   "assertEquals(1, $x->size());" +
                   "assertEquals(['name', 'other'], $x.properties.name->sort());");
    }

    @Test
    public void testCoreInstanceCopy()
    {
        test("let x = meta::legend::compile('function a::f():Integer[1]{1+1}')->toOne()->cast(@ConcreteFunctionDefinition<Any>);" +
                "assert($x.name != 'copied name');" +
                "let copy = ^$x(name = 'copied name');" +
                "assert($copy.name == 'copied name');");
    }


    @Test
    public void testProfile()
    {
        test("let x =  meta::legend::compile('Profile a::prof {stereotypes:[a,b];tags:[c,d,e];}')->cast(@Profile);" +
                  "println($x);" +
                  "assertEquals(1, $x->size());" +
                  "assertEquals(2, $x.p_stereotypes->size());" +
                  "assertEquals(3, $x.p_tags->size());");
    }

    @Test
    public void testEnum()
    {
        test("let x =  meta::legend::compile('Enum a::E{a,b,c}')->cast(@Enumeration<Any>);\n" +
                   "println($x);" +
                   "assertEquals(['a','b','c'], $x->toOne()->enumValues()->map(z|$z->id()));");
    }

    @Test
    public void testAssociation()
    {
        test("let x =  meta::legend::compile('Class a::A{} Class a::B{} Association a::C{a : a::A[1]; b: a::B[1];}')->filter(x | $x->instanceOf(Association))->cast(@Association);\n" +
                   "println($x);" +
                   "assertEquals(['a','b'], $x.properties.name);");
    }

    @Test
    public void testMapping()
    {
        test("let mappingStr = '" +
                       "###Pure\\n" +
                       "Class a::A{name:String[1];}\\n" +
                       "\\n" +
                       "###Mapping\\n" +
                       "Mapping a::M\\n" +
                        "(\\n" +
                        "  a::A : Pure\\n" +
                        "  {\\n" +
                        "    ~src a::A\\n" +
                        "    name : $src.name\\n" +
                        "  }\\n" +
                        ")\\n" +
                        "';\n" +
                "let x =  meta::legend::compile($mappingStr)->filter(x | $x->instanceOf(meta::pure::mapping::Mapping))->cast(@meta::pure::mapping::Mapping);\n" +
                "assertEquals('name', $x.classMappings->cast(@meta::pure::mapping::modelToModel::PureInstanceSetImplementation).propertyMappings.property.name);");
    }

    @Test
    public void testMappingReferenceInMapping()
    {
        testWith("let mappingStr = '" +
                "###Pure\\n" +
                "Class a::A{name:String[1];}\\n" +
                "\\n" +
                "###Mapping\\n" +
                "Mapping a::M\\n" +
                "(\\n" +
                "  include a::baseM" +
                "  a::A : Pure\\n" +
                "  {\\n" +
                "    ~src a::A\\n" +
                "    name : $src.name\\n" +
                "  }\\n" +
                ")\\n" +
                "';\n" +
                "let x =  meta::legend::compile($mappingStr)->filter(x | $x->instanceOf(meta::pure::mapping::Mapping))->cast(@meta::pure::mapping::Mapping);\n" +
                "assertEquals('name', $x.classMappings->cast(@meta::pure::mapping::modelToModel::PureInstanceSetImplementation).propertyMappings.property.name);",
                basicModel()
            );
    }

    @Test
    public void testText()
    {
        test("let x =  meta::legend::compile('###Text\\nText a::ok\\n{\\ntype:STRING; content:\\'oeoeoeo\\';}')->cast(@meta::pure::metamodel::text::Text);\n" +
                   "assertEquals('oeoeoeo', $x.content);");
    }

    @Test
    public void testValueSpecification()
    {
        test("let x =  meta::legend::compileVS('1');\n" +
                   "assertEquals(1, $x);");
    }

    @Test
    public void testValueSpecificationEmbeddedPure()
    {
        test("let x =  meta::legend::compileVS('#Test{X X Test}#');\n" +
                "assertEquals('X X Test', $x);");
    }

    @Test
    public void testFunction()
    {
        test("let funcStr ='function a::f():Integer[1]\\n" +
                "                 {\\n" +
                "                    1+1\\n" +
                "                  }\\n';" +
                "let x=  meta::legend::compile($funcStr)->cast(@ConcreteFunctionDefinition<Any>);" +
                "println($x);" +
                "assertEquals(1, $x->size());" +
                "assertEquals(1, $x.expressionSequence->size());");
    }

    @Test
    public void testTypeReferenceInFunction()
    {
        testWith(
                "let funcStr ='function a::f():a::baseA[1]\\n" +
                "                 {\\n" +
                "                    a::baseA.all()->toOne();\\n" +
                "                  }\\n';" +
                "let x =  meta::legend::compile($funcStr)->cast(@ConcreteFunctionDefinition<Any>);" +
                "println($x);" +
                "assertEquals(1, $x->size());" +
                "assertEquals(1, $x.expressionSequence->size());",
                basicModel()
        );
    }

    @Test
    public void testMappingReferenceInFunction()
    {
        testWith(
                "let funcStr ='function a::f():a::A[1]\\n" +
                        "                 {\\n" +
                        "                    a::A.all()->from(a::M, ^Runtime())->toOne();\\n" +
                        "                  }\\n';" +
                        "let x =  meta::legend::compile($funcStr)->cast(@ConcreteFunctionDefinition<Any>);" +
                        "println($x);" +
                        "assertEquals(1, $x->size());" +
                        "assertEquals(1, $x.expressionSequence->size());",
                basicModel()
        );
    }

    public String basicModel()
    {
        return
            "###Pure\n" +
            "Class a::baseA\n" +
            "{\n" +
            "   name:String[1];\n" +
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping a::baseM\n" +
            "(\n" +
            "  a::baseA : Pure\n" +
            "  {\n" +
            "    ~src a::baseA\n" +
            "    name : $src.name\n" +
            "  }\n" +
            ")\n";
    }


    private void test(String code)
    {
        Tools.test(code, "", functionExecution, runtime);
    }

    private void testWith(String code, String otherParserCode)
    {
        Tools.test(code, otherParserCode, functionExecution, runtime);
    }

}
