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

package org.finos.legend.engine.language.jarService.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.jarService.grammar.from.JarServiceGrammarParserExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.functionActivator.metamodel.Ownership;
import org.finos.legend.engine.protocol.jarService.metamodel.control.UserList;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;

import org.finos.legend.engine.protocol.jarService.metamodel.JarService;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class JarServiceGrammarComposer implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Jar_Service");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof JarService)
        {
            return renderJarService((JarService) element);
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
        return Lists.mutable.with(buildSectionComposer(JarServiceGrammarParserExtension.NAME, renderers));
    }

    private static String renderElement(PackageableElement element)
    {
        if (element instanceof JarService)
        {
            return renderJarService((JarService) element);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderJarService(JarService app)
    {
        String packageName = app._package == null || app._package.isEmpty() ? app.name : app._package + "::" + app.name;

        return "JarService " + renderAnnotations(app.stereotypes, app.taggedValues) + packageName + "\n" +
                "{\n" +
                "   ownership : " + renderServiceOwner(app.ownership) +
                "   function : " + app.function.path + ";\n" +
                (app.documentation == null ? "" : "   documentation : '" + app.documentation + "';\n") +
                "}";
    }

    private static String renderServiceOwner(Ownership owner)
    {
        if (owner instanceof UserList)
        {
            return "UserList { users: [\n" + LazyIterate.collect(((UserList) owner).users, o -> getTabString(2) + convertString(o, true)).makeString(",\n") + "\n" + getTabString(2) + "] };\n";
        }
        else if (owner instanceof DeploymentOwner)
        {
            return "Deployment { identifier: '" + ((DeploymentOwner)owner).id + "' };\n";
        }
        throw new RuntimeException("Owner type invalid");
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof JarService), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(JarServiceGrammarComposer::renderElement).makeString("###" + JarServiceGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }
}
