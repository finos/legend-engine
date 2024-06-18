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

package org.finos.legend.pure.runtime.java.interpreted.function.base.collection;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAppendTreeToNode extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testSimple()
    {
        compileTestSource("Class MyNode extends TreeNode\n" +
                "{\n" +
                "    value :String[1];\n" +
                "    children(){$this.childrenData->cast(@MyNode)}:MyNode[*];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::collection::appendTreeToNode(root:TreeNode[1], position: TreeNode[1], joinTree:TreeNode[1]): TreeNode[1]\n" +
                "{\n" +
                "   $root->meta::pure::functions::collection::replaceTreeNode($position, ^$position(childrenData += $joinTree));\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    let tree = ^MyNode(value='1', childrenData = [^MyNode(value ='2', childrenData=[^MyNode(value ='4')]), ^MyNode(value='3')]);\n" +
                "    let subTree = ^MyNode(value='10', childrenData = [^MyNode(value ='11'), ^MyNode(value='12', childrenData=[^MyNode(value ='13')])]);\n" +
                "    let point = $tree.children()->filter(n|$n.value == '3')->at(0);\n" +
                "    print($tree->appendTreeToNode($point, $subTree),10);\n" +
                "    print($tree,10);\n" +
                "}\n");
        this.execute("test():Nil[0]");
        Assert.assertEquals("Anonymous_StripedId instance MyNode\n" +
                "    childrenData(Property):\n" +
                "        Anonymous_StripedId instance MyNode\n" +
                "            childrenData(Property):\n" +
                "                Anonymous_StripedId instance MyNode\n" +
                "                    classifierGenericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] MyNode instance Class\n" +
                "                    value(Property):\n" +
                "                        4 instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] MyNode instance Class\n" +
                "            value(Property):\n" +
                "                2 instance String\n" +
                "        Anonymous_StripedId instance MyNode\n" +
                "            childrenData(Property):\n" +
                "                Anonymous_StripedId instance MyNode\n" +
                "                    childrenData(Property):\n" +
                "                        Anonymous_StripedId instance MyNode\n" +
                "                            classifierGenericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        [X] MyNode instance Class\n" +
                "                            value(Property):\n" +
                "                                11 instance String\n" +
                "                        Anonymous_StripedId instance MyNode\n" +
                "                            childrenData(Property):\n" +
                "                                Anonymous_StripedId instance MyNode\n" +
                "                                    classifierGenericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                [X] MyNode instance Class\n" +
                "                                    value(Property):\n" +
                "                                        13 instance String\n" +
                "                            classifierGenericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        [X] MyNode instance Class\n" +
                "                            value(Property):\n" +
                "                                12 instance String\n" +
                "                    classifierGenericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] MyNode instance Class\n" +
                "                    value(Property):\n" +
                "                        10 instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] MyNode instance Class\n" +
                "            value(Property):\n" +
                "                3 instance String\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] MyNode instance Class\n" +
                "    value(Property):\n" +
                "        1 instance String", functionExecution.getConsole().getLine(0));
        Assert.assertEquals("Anonymous_StripedId instance MyNode\n" +
                "    childrenData(Property):\n" +
                "        Anonymous_StripedId instance MyNode\n" +
                "            childrenData(Property):\n" +
                "                Anonymous_StripedId instance MyNode\n" +
                "                    classifierGenericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] MyNode instance Class\n" +
                "                    value(Property):\n" +
                "                        4 instance String\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] MyNode instance Class\n" +
                "            value(Property):\n" +
                "                2 instance String\n" +
                "        Anonymous_StripedId instance MyNode\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] MyNode instance Class\n" +
                "            value(Property):\n" +
                "                3 instance String\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] MyNode instance Class\n" +
                "    value(Property):\n" +
                "        1 instance String", functionExecution.getConsole().getLine(1));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
