// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.shared.structures;

import org.finos.legend.shared.stuctures.TreeNode;
import org.junit.Assert;
import org.junit.Test;

public class TestTreeNode
{
    @Test
    public void testPrint()
    {
        TreeNode root = new TreeNode("Core");
        TreeNode child1 = new TreeNode("Core");
        TreeNode child2 = new TreeNode("Pure");
        TreeNode child21 = new TreeNode("Pure2");
        TreeNode child3 = new TreeNode("Lang");
        TreeNode child4 = new TreeNode("Compiler");
        TreeNode child5 = new TreeNode("Composer");
        TreeNode child6 = new TreeNode("Parser");
        TreeNode child7 = new TreeNode("Protocol");
        TreeNode child8 = new TreeNode("Plan");
        TreeNode child9 = new TreeNode("Binding_Java");
        TreeNode child10 = new TreeNode("Pure");

        root.addChild(child1);

        child1.addChild(child2);

        child2.addChild(child21);

        root.addChild(child3);

        child3.addChild(child4);
        child3.addChild(child5);
        child3.addChild(child6);
        child3.addChild(child7);

        root.addChild(child8);

        child8.addChild(child9);
        child9.addChild(child10);


        Assert.assertEquals(
                "Core\n" +
                        "  ├Core\n" +
                        "  │  └Pure\n" +
                        "  │     └Pure2\n" +
                        "  ├Lang\n" +
                        "  │  ├Compiler\n" +
                        "  │  ├Composer\n" +
                        "  │  ├Parser\n" +
                        "  │  └Protocol\n" +
                        "  └Plan\n" +
                        "     └Binding_Java\n" +
                        "        └Pure", root.print());

    }
}
