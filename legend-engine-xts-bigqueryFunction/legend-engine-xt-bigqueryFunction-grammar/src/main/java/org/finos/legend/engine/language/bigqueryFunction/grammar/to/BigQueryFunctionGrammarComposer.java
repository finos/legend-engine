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

package org.finos.legend.engine.language.bigqueryFunction.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.bigqueryFunction.grammar.from.BigQueryFunctionGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunction;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class BigQueryFunctionGrammarComposer implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "BigQuery");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = org.eclipse.collections.impl.factory.Lists.mutable.with((element, context) ->
    {
        if (element instanceof BigQueryFunction)
        {
            return renderBigQueryFunction((BigQueryFunction) element);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(BigQueryFunctionGrammarParserExtension.NAME, renderers));
    }

    private static String renderElement(PackageableElement element)
    {
        if (element instanceof BigQueryFunction)
        {
            return renderBigQueryFunction((BigQueryFunction) element);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderBigQueryFunction(BigQueryFunction app)
    {
        String packageName = app._package == null || app._package.isEmpty() ? app.name : app._package + "::" + app.name;

        return "BigQueryFunction " + renderAnnotations(app.stereotypes, app.taggedValues) + packageName + "\n" +
                "{\n" +
                "   functionName : '" + app.functionName + "';\n" +
                "   function : " + app.function.path + ";\n" +
                "   ownership : Deployment { identifier: '" + ((DeploymentOwner)app.ownership).id + "' };\n" +
                (app.description == null ? "" : "   description : '" + app.description + "';\n") +
                (app.activationConfiguration == null ? "" : "   activationConfiguration : " + ((BigQueryFunctionDeploymentConfiguration) app.activationConfiguration).activationConnection.connection + ";\n") +
                "}";
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof BigQueryFunction), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(BigQueryFunctionGrammarComposer::renderElement).makeString("###" + BigQueryFunctionGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }
}
