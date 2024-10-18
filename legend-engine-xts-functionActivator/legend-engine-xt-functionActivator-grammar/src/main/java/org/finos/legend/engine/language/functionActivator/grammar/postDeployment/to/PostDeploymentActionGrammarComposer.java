//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.functionActivator.grammar.postDeployment.to;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.PostDeploymentAction;
import java.util.List;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class PostDeploymentActionGrammarComposer implements PureGrammarComposerExtension
{
    public static String renderActions(List<PostDeploymentAction> actions)
    {
        List<String> postDeploymentActionStrings = FastList.newList();
        List<IPostDeploymentActionGrammarComposerExtension> extensions = IPostDeploymentActionGrammarComposerExtension.getExtensions();

        postDeploymentActionStrings.addAll(ListIterate.collect(actions, action -> IPostDeploymentActionGrammarComposerExtension.process(
                action,
                ListIterate.flatCollect(extensions, IPostDeploymentActionGrammarComposerExtension::getExtraPostDeploymentActionComposer)
        )));
        return "   actions: [\n" + getTabString() + String.join(",\n", postDeploymentActionStrings) + "\n" + getTabString() + "];";
    }
}
