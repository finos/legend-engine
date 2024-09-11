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

package org.finos.legend.pure.runtime.java.interpreted.function.base.lang;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestNewAtRuntime;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNew extends AbstractTestNewAtRuntime
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage());
    }

    @Test
    public void testNewWithMultiplicityParameter()
    {
        compileTestSource("fromString.pure",
                "Class MyClass<|m>\n" +
                        "{\n" +
                        "  value:String[m];\n" +
                        "}\n" +
                        "\n" +
                        "function testFn():Any[*]\n" +
                        "{\n" +
                        "  ^MyClass<|1>(value='hello');\n" +
                        "}");
        execute("testFn():Any[*]");
        // TODO add asserts
    }

    @Test
    public void testNewWithMissingOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                "function test(): Any[*]\n" +
                        "{\n" +
                        "   ^test::Owner(firstName='John', lastName='Roe')\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Car\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Owner\n" +
                        "{\n" +
                        "   firstName: String[1];\n" +
                        "   lastName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::Car_Owner\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "   car  : test::Car[1];\n" +
                        "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'car' requires 1 value, got 0", "fromString.pure", 3, 4, e);
    }

    @Test
    public void testNewWithMissingOneToManyProperty()
    {
        compileTestSource("fromString.pure",
                "function test(): Any[*]\n" +
                        "{\n" +
                        "   ^test::Owner(firstName='John', lastName='Roe')\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Car\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Owner\n" +
                        "{\n" +
                        "   firstName: String[1];\n" +
                        "   lastName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::Car_Owner\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "   cars  : test::Car[1..*];\n" +
                        "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'cars' requires 1..* values, got 0", "fromString.pure", 3, 4, e);
    }

    @Test
    public void testNewWithChildWithMismatchedReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                "function test(): Any[*]\n" +
                        "{\n" +
                        "   ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', car=^test::Car(name='Audi')))\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Car\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Owner\n" +
                        "{\n" +
                        "   firstName: String[1];\n" +
                        "   lastName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::Car_Owner\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "   car  : test::Car[1];\n" +
                        "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating the type 'Owner'. The property 'car' has a multiplicity range of [1] when the given list has a cardinality equal to 2", "fromString.pure", 3, 4, e);
    }

    @Override
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                "function test(): Any[*]\n" +
                        "{\n" +
                        "   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                        "   print($car.owner.car->size()->toString(), 1);\n" +
                        "   $car;" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Car\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::FastCar extends test::Car\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "}" +
                        "\n" +
                        "Class\n" +
                        "test::Owner\n" +
                        "{\n" +
                        "   firstName: String[1];\n" +
                        "   lastName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::Car_Owner\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "   car  : test::Car[1];\n" +
                        "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'car' requires 1 value, got 0", "fromString.pure", 3, 14, e);
    }

    @Override
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure",
                "function test(): Any[*]\n" +
                        "{\n" +
                        "   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                        "   print($car.owner.cars->size()->toString(), 1);\n" +
                        "   $car;" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::Car\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class\n" +
                        "test::FastCar extends test::Car\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "}" +
                        "\n" +
                        "Class\n" +
                        "test::Owner\n" +
                        "{\n" +
                        "   firstName: String[1];\n" +
                        "   lastName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::Car_Owner\n" +
                        "{\n" +
                        "   owner : test::Owner[1];\n" +
                        "   cars  : test::Car[1..*];\n" +
                        "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'cars' requires 1..* values, got 0", "fromString.pure", 3, 14, e);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
