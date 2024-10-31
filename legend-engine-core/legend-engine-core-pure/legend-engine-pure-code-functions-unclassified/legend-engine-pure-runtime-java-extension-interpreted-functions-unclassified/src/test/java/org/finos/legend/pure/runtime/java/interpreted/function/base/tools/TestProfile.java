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

package org.finos.legend.pure.runtime.java.interpreted.function.base.tools;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProfile extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testPathToElementProfile()
    {
        compileTestSource("Class A::B::C::D::E\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class A::B::C::K::D\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "function test::findElement(path:String[1], separator:String[1]):PackageableElement[1]\n" +
                "{\n" +
                "    $path->split($separator)->fold({str:String[1], pkg:PackageableElement[1] | let p = $pkg->cast(@Package).children->filter(c | $c.name == $str);\n" +
                "                                                                               if ($p->isEmpty(), | fail(| $path + ' is not a valid PackageableElement. Package \\'' + $str + '\\' not found'); $p->toOne();, | $p->toOne());\n" +
                "                                   }, ::)\n" +
                "}\n" +
                "\n" +
                "function test::testProfile():Nil[0]\n" +
                "{\n" +
                "    print('A::B::C::K::D'->test::findElement('::')->profile(false).result, 1);\n" +
                "}\n");
        this.execute("test::testProfile():Nil[0]");
        Assert.assertEquals("'\n" +
                "################################################## Profiler report ##################################################\n" +
                "       ##---------------------------- TreeStart\n" +
                "       + 1 profile_T_m__Boolean_1__ProfileResult_1_\n" +
                "       +     1 findElement_String_1__String_1__PackageableElement_1_\n" +
                "       +         1 fold_T_MANY__Function_1__V_m__V_m_\n" +
                "       +             1 split_String_1__String_1__String_MANY_\n" +
                "       +             5 letFunction_String_1__T_m__T_m_\n" +
                "       +                 5 filter_T_MANY__Function_1__T_MANY_\n" +
                "       +                     5 children(P)\n" +
                "       +                         5 cast_Any_m__T_1__T_m_\n" +
                "       +                     9 equal_Any_MANY__Any_MANY__Boolean_1_\n" +
                "       +                         9 name(P)\n" +
                "       +             5 if_Boolean_1__Function_1__Function_1__T_m_\n" +
                "       +                 5 isEmpty_Any_MANY__Boolean_1_\n" +
                "       +                 5 toOne_T_MANY__T_1_\n" +
                "       ##---------------------------- TreeEnd\n" +
                "################################################## Finished Report ##################################################\n'", functionExecution.getConsole().getLine(0));

        Assert.assertEquals("D instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] Class instance Class\n" +
                "            typeArguments(Property):\n" +
                "                [>1] Anonymous_StripedId instance GenericType\n" +
                "    generalizations(Property):\n" +
                "        Anonymous_StripedId instance Generalization\n" +
                "            general(Property):\n" +
                "                [>1] Anonymous_StripedId instance GenericType\n" +
                "            specific(Property):\n" +
                "                [>1] D instance Class\n" +
                "    name(Property):\n" +
                "        D instance String\n" +
                "    package(Property):\n" +
                "        K instance Package\n" +
                "            children(Property):\n" +
                "                [>1] D instance Class\n" +
                "            name(Property):\n" +
                "                [>1] K instance String\n" +
                "            package(Property):\n" +
                "                [>1] C instance Package\n" +
                "    typeVariables(Property):", functionExecution.getConsole().getLine(1));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
