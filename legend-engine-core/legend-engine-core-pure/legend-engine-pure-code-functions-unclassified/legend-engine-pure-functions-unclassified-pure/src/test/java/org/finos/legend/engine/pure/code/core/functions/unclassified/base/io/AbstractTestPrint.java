// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.io;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestPrint extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrint()
    {
        compileTestSource("fromString.pure","function testPrint():Nil[0]\n" +
                "{\n" +
                "    print('Hello World', 1);\n" +
                "}\n");
        this.execute("testPrint():Nil[0]");
        Assert.assertEquals("'Hello World'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testArrowWithAFunctionWithNoParameters()
    {
        compileTestSource("fromString.pure","function testArrowWithFunctionNoParameters():Nil[0]\n" +
                "{\n" +
                "    'a'->print(1);\n" +
                "}\n");
        this.execute("testArrowWithFunctionNoParameters():Nil[0]");
        Assert.assertEquals("'a'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintInteger()
    {
        compileTestSource("fromString.pure","function testPrintInteger():Nil[0]\n" +
                "{\n" +
                "    print(123, 1);\n" +
                "}\n");
        this.execute("testPrintInteger():Nil[0]");
        Assert.assertEquals("123", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintFloat()
    {
        compileTestSource("fromString.pure","function testPrintFloat():Nil[0]\n" +
                "{\n" +
                "    print(123.456, 1);\n" +
                "}\n");
        this.execute("testPrintFloat():Nil[0]");
        Assert.assertEquals("123.456", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintDate()
    {
        compileTestSource("fromString.pure","function testPrintDate():Nil[0]\n" +
                "{\n" +
                "    print(%2016-07-08, 1);\n" +
                "}\n");
        this.execute("testPrintDate():Nil[0]");
        Assert.assertEquals("2016-07-08", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintBoolean()
    {
        compileTestSource("fromString.pure","function testPrintBoolean():Nil[0]\n" +
                "{\n" +
                "    print(true, 1);\n" +
                "}\n");
        this.execute("testPrintBoolean():Nil[0]");
        Assert.assertEquals("true", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintIntegerCollection()
    {
        compileTestSource("fromString.pure","function testPrintIntegerCollection():Nil[0]\n" +
                "{\n" +
                "    print([1, 2, 3], 1);\n" +
                "}\n");
        this.execute("testPrintIntegerCollection():Nil[0]");
        Assert.assertEquals("[\n" +
                "   1\n" +
                "   2\n" +
                "   3\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintFloatCollection()
    {
        compileTestSource("fromString.pure","function testPrintFloatCollection():Nil[0]\n" +
                "{\n" +
                "    print([1.0, 2.5, 3.0], 1);\n" +
                "}\n");
        this.execute("testPrintFloatCollection():Nil[0]");
        Assert.assertEquals("[\n" +
                "   1.0\n" +
                "   2.5\n" +
                "   3.0\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintDateCollection()
    {
        compileTestSource("fromString.pure","function testPrintDateCollection():Nil[0]\n" +
                "{\n" +
                "    print([%1973-11-13T23:09:11, %2016-07-08], 1);\n" +
                "}\n");
        this.execute("testPrintDateCollection():Nil[0]");
        Assert.assertEquals("[\n" +
                "   1973-11-13T23:09:11+0000\n" +
                "   2016-07-08\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintStringCollection()
    {
        compileTestSource("fromString.pure","function testPrintStringCollection():Nil[0]\n" +
                "{\n" +
                "    print(['testString', '2.5', '%2016-07-08'], 1);\n" +
                "}\n");
        this.execute("testPrintStringCollection():Nil[0]");
        Assert.assertEquals("[\n" +
                "   'testString'\n" +
                "   '2.5'\n" +
                "   '%2016-07-08'\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintMixedCollection()
    {
        compileTestSource("fromString.pure","function testPrintMixedCollection():Nil[0]\n" +
                "{\n" +
                "    print([1, 2.5, 'testString', %2016-07-08], 1);\n" +
                "}\n");
        this.execute("testPrintMixedCollection():Nil[0]");
        Assert.assertEquals("[\n" +
                "   1\n" +
                "   2.5\n" +
                "   'testString'\n" +
                "   2016-07-08\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testFunction()
    {
        compileTestSource("fromString.pure",
                "function tst():String[1]{let c = 'a'+'b'; $c+'c';}\n" +
                        "\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "   print(tst__String_1_,2);\n" +
                        "}\n");
        this.compileAndExecute("test():Nil[0]");
        Assert.assertEquals("tst__String_1_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance FunctionType\n" +
                "    expressionSequence(Property):\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                [X] letFunction_String_1__T_m__T_m_ instance NativeFunction\n" +
                "            functionName(Property):\n" +
                "                letFunction instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] String instance PrimitiveType\n" +
                "            importGroup(Property):\n" +
                "                [X] import_fromString_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                [X] PureOne instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GenericType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                    values(Property):\n" +
                "                        [>2] c instance String\n" +
                "                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                    func(Property):\n" +
                "                        [X] plus_String_MANY__String_1_ instance ConcreteFunctionDefinition\n" +
                "                    functionName(Property):\n" +
                "                        [>2] plus instance String\n" +
                "                    genericType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance InferredGenericType\n" +
                "                    importGroup(Property):\n" +
                "                        [X] import_fromString_pure_1 instance ImportGroup\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    parametersValues(Property):\n" +
                "                        [>2] Anonymous_StripedId instance InstanceValue\n" +
                "                    usageContext(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        [X] tst__String_1_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        [>2] 0 instance Integer\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                [X] plus_String_MANY__String_1_ instance ConcreteFunctionDefinition\n" +
                "            functionName(Property):\n" +
                "                plus instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] String instance PrimitiveType\n" +
                "            importGroup(Property):\n" +
                "                [X] import_fromString_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                [X] PureOne instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GenericType\n" +
                "                    multiplicity(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Multiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                    values(Property):\n" +
                "                        [>2] Anonymous_StripedId instance VariableExpression\n" +
                "                        [>2] Anonymous_StripedId instance InstanceValue\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        [X] tst__String_1_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        [>2] 1 instance Integer\n" +
                "    functionName(Property):\n" +
                "        tst instance String\n" +
                "    name(Property):\n" +
                "        tst__String_1_ instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GenericType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                    values(Property):\n" +
                "                        [~>] tst__String_1_ instance ConcreteFunctionDefinition\n" +
                "            propertyName(Property):\n" +
                "                values instance String", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintObj()
    {
        compileTestSource("fromString.pure","Class A\n" +
                "{\n" +
                "    test : String[1];\n" +
                "    test2 : String[1];\n" +
                "}\n" +
                "\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   print(A,0);\n" +
                "}\n");
        this.compileAndExecute("test():Nil[0]");
        Assert.assertEquals("A instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        [>0] Anonymous_StripedId instance GenericType\n" +
                "    generalizations(Property):\n" +
                "        [>0] Anonymous_StripedId instance Generalization\n" +
                "    name(Property):\n" +
                "        [>0] A instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package\n" +
                "    properties(Property):\n" +
                "        [>0] test instance Property\n" +
                "        [>0] test2 instance Property\n" +
                "    referenceUsages(Property):\n" +
                "        [>0] Anonymous_StripedId instance ReferenceUsage\n" +
                "        [>0] Anonymous_StripedId instance ReferenceUsage\n" +
                "        [>0] Anonymous_StripedId instance ReferenceUsage\n" +
                "    typeVariables(Property):", functionExecution.getConsole().getLine(0));
    }
}

