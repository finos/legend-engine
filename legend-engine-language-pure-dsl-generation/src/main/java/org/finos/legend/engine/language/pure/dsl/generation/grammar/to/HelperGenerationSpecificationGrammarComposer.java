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

package org.finos.legend.engine.language.pure.dsl.generation.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNode;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;


public class HelperGenerationSpecificationGrammarComposer
{
    public static String renderGenerationNode(GenerationTreeNode generationTreeNode)
    {
        return getTabString(2) + "{\n" +
                (generationTreeNode.id != null && !generationTreeNode.id.equals(generationTreeNode.generationElement) ? getTabString(3) + "id: " + convertString(generationTreeNode.id, true) + ";\n" : "") +
                getTabString(3) + "generationElement: " + generationTreeNode.generationElement + ";\n" +
                getTabString(2) + "}";
    }

    public static String renderFileGenerationNode(List<PackageableElementPointer> fileGenerations)
    {
        return fileGenerations.isEmpty() ? "" :
                getTabString() + "fileGenerations: " +
                        "[\n" +
                        ListIterate.collect(fileGenerations, fileGenPointer -> getTabString(2) + fileGenPointer.path).makeString(",\n") + '\n'
                        + getTabString() + "];";

    }
}
