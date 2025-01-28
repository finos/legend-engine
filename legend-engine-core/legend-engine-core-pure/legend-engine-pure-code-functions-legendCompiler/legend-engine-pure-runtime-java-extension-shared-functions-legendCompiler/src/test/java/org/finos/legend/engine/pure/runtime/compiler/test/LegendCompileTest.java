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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
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
    public void testFunction()
    {
        test("let x = meta::legend::compile('function a::f():Integer[1]{1+1}')->cast(@ConcreteFunctionDefinition<Any>);" +
                   "println($x);" +
                   "assertEquals(1, $x->size());" +
                   "assertEquals(1, $x.expressionSequence->size());");
    }

    @Test
    public void testClassPropertyCopy()
    {
        runtime.createInMemoryAndCompile(Tuples.pair("prop.pure",  " Class  l::classWithProperty{prop:AbstractProperty<Any>[1];} "));

        test("let x = meta::legend::compile('Class  <<meta::pure::profiles::temporal.processingtemporal>>  l::Firm{employee: l::Person[1];  name: String[1];   loc: String[1];    }  Class <<meta::pure::profiles::temporal.processingtemporal>>  l::Person{name:String[1]; }  ');\n" +
                        "let p = $x->at(0)->cast(@Class<Any>).properties->at(0)->toOne(); " +
                        "let y = ^$p();" +
                        "$x->at(0)->cast(@Class<Any>).properties ->map(p|^l::classWithProperty(  prop=$p));  print($y);\n"
                        );
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
        test("let x =  meta::legend::compile('Class a::A{name:String[1];}\\n###Mapping\\nMapping a::M(a::A : Pure{~src a::A name : $src.name})')->filter(x | $x->instanceOf(meta::pure::mapping::Mapping))->cast(@meta::pure::mapping::Mapping);\n" +
                  "assertEquals('name', $x.classMappings->cast(@meta::external::store::model::PureInstanceSetImplementation).propertyMappings.property.name);");
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
    public void testPMCD() throws JsonProcessingException
    {
        Class test = new Class();
        test.name = "a";
        test._package = "test::class";

        PureModelContextData  pmcd = PureModelContextData.newBuilder().withElement(test).build();
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
        String pmcdJson = objectMapper.writeValueAsString(pmcd);

        test("let x = meta::legend::compilePMCD('" + pmcdJson + "');\n" +
                "let p = $x->at(0)->cast(@Class<Any>).name;\n " +
                "assertEquals('a', $p);"
        );
    }



    private void test(String code)
    {
        Tools.test(code, functionExecution, runtime);
    }


}
