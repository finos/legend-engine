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

package org.finos.legend.engine.language.memsqlFunction.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.memsqlFunction.grammar.from.MemSqlFunctionGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunction;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;

public class MemSqlFunctionGrammarComposer implements PureGrammarComposerExtension
{
    private static String renderElement(PackageableElement element)
    {
        if (element instanceof MemSqlFunction)
        {
            return renderMemSqlFunction((MemSqlFunction) element);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderMemSqlFunction(MemSqlFunction app)
    {
        String packageName = app._package == null || app._package.isEmpty() ? app.name : app._package + "::" + app.name;

        return "MemSqlFunction " + renderAnnotations(app.stereotypes, app.taggedValues) + packageName + "\n" +
                "{\n" +
                "   functionName : '" + app.functionName + "';\n" +
                "   function : " + app.function.path + ";\n" +
                (app.owner == null ? "" : "   owner : '" + app.owner + "';\n") +
                (app.description == null ? "" : "   description : '" + app.description + "';\n") +
                (app.activationConfiguration == null ? "" : "   activationConfiguration : " + ((MemSqlFunctionDeploymentConfiguration) app.activationConfiguration).activationConnection.connection + ";\n") +
                "}";
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, sectionName) ->
        {
            if (!MemSqlFunctionGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof MemSqlFunction)
                {
                    return renderMemSqlFunction((MemSqlFunction) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof MemSqlFunction), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(MemSqlFunctionGrammarComposer::renderElement).makeString("###" + MemSqlFunctionGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }
}
