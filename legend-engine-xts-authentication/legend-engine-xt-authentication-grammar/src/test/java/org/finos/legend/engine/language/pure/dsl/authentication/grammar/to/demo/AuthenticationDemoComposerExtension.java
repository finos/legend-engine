// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.demo;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.demo.AuthenticationDemoParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.demo.AuthenticationDemo;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class AuthenticationDemoComposerExtension implements IAuthenticationDemoComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("__Test__");

    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof AuthenticationDemo)
        {
            return renderAuthenticationDemo((AuthenticationDemo) element, context);
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
        return Lists.mutable.with(buildSectionComposer(AuthenticationDemoParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.of((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Lists.mutable.empty();
            composableElements.addAll(ListIterate.selectInstancesOf(elements, AuthenticationDemo.class));

            return composableElements.isEmpty()
                    ? null
                    : new PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult(composableElements
                    .collect(element ->
                    {
                        if (element instanceof AuthenticationDemo)
                        {
                            return AuthenticationDemoComposerExtension.renderAuthenticationDemo((AuthenticationDemo) element, context);
                        }
                        throw new UnsupportedOperationException("Unsupported type " + element.getClass().getName());
                    })
                    .makeString("###" + AuthenticationDemoParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderAuthenticationDemo(AuthenticationDemo authenticationDemo, PureGrammarComposerContext context)
    {
        return HelperAuthenticationDemoComposer.renderAuthenticationDemo(authenticationDemo, 1, context);
    }
}
