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

package org.finos.legend.engine.shared.core.extension;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class TreeNode
{
    private final String name;
    private final MutableList<TreeNode> children = Lists.mutable.empty();

    public TreeNode(String name)
    {
        this.name = name;
    }

    public TreeNode createOrReturnChild(String name)
    {
        TreeNode node = children.detect(c -> name.equals(c.name));
        return node == null ? addChild(new TreeNode(name)) : node;
    }

    public TreeNode addChild(TreeNode child)
    {
        children.add(child);
        return child;
    }

    public String print()
    {
        return printWithDepth(0, Lists.mutable.empty(), false);
    }

    private String space = "  ";

    private String printWithDepth(int depth, MutableList<Boolean> parentLast, boolean last)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < depth - 1; i++)
        {
            builder.append(space);
            builder.append(parentLast.get(i + 1) ? " " : "│");
        }
        if (depth > 0)
        {
            builder.append(space);
            builder.append(last ? "└" : "├");
        }
        String prefix = builder.toString();
        return prefix + name + (children.isEmpty() ? "" : "\n") + children.collectWithIndex((c, i) -> c.printWithDepth(depth + 1, Lists.mutable.withAll(parentLast).with(last), i == children.size() - 1)).makeString("\n");
    }
}
