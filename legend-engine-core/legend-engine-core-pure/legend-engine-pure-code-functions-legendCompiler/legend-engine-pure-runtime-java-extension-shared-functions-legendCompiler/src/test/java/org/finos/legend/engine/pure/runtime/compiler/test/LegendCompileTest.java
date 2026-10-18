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

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.junit.Assert;
import org.junit.Test;

public abstract class LegendCompileTest
{
    protected static PureRuntime runtime;
    protected static FunctionExecution functionExecution;

    @Test
    public void testRedefiningAnElementOfTheCallingGraphFails()
    {
        Exception e = Assert.assertThrows(Exception.class, () -> Tools.test("Class test::Existing {}\n", "meta::legend::compile('Class test::Existing {}');", functionExecution, runtime));

        Throwable cause = e;
        while (cause.getCause() != null)
        {
            cause = cause.getCause();
        }
        Assert.assertEquals("An element named 'Existing' already exists in the package 'test'", cause.getMessage());
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

    private void test(String code)
    {
        Tools.test(code, functionExecution, runtime);
    }


}
