// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification;

import java.util.Collections;
import java.util.List;

public class GenerationTreeNodeHelper
{
    private GenerationTreeNodeHelper()
    {
    }

    /**
     * The generation elements of a node, whichever of the two forms it was written in.
     */
    public static List<String> generationElements(GenerationTreeNode node)
    {
        return node.generationElement == null ? node.generationElements : Collections.singletonList(node.generationElement);
    }

    /**
     * The id a node carries when its author did not give it one. For a single element that is the
     * element path, which is the rule the singular form has always followed. The parser defaults to
     * it and the composer omits an id matching it, so the two have to agree.
     */
    public static String defaultId(GenerationTreeNode node)
    {
        return String.join(",", generationElements(node));
    }
}
