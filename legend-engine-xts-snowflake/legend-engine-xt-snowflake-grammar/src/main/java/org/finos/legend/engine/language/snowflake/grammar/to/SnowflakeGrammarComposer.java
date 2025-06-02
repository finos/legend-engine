//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.snowflake.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.snowflake.grammar.from.SnowflakeGrammarParserExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdf;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfDeploymentConfiguration;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;

public class SnowflakeGrammarComposer implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Snowflake");
    }

    private static String renderElement(PackageableElement element)
    {
        if (element instanceof SnowflakeApp)
        {
            return renderSnowflakeApp((SnowflakeApp) element);
        }
        if (element instanceof SnowflakeM2MUdf)
        {
            return renderSnowflakeM2MUdf((SnowflakeM2MUdf) element);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderSnowflakeApp(SnowflakeApp app)
    {
        String packageName = app._package == null || app._package.isEmpty() ? app.name : app._package + "::" + app.name;

        return "SnowflakeApp " + renderAnnotations(app.stereotypes, app.taggedValues) + packageName + "\n" +
                "{\n" +
                "   applicationName : '" + app.applicationName + "';\n" +
                "   function : " + app.function.path + ";\n" +
                "   ownership : Deployment { identifier: '" + ((DeploymentOwner)app.ownership).id + "'};\n" +
                (app.description == null ? "" : "   description : '" + app.description + "';\n") +
                (app.usageRole == null ? "" : "   usageRole : '" + app.usageRole + "';\n") +
                (app.permissionScheme == null ? "" : "   permissionScheme : " + app.permissionScheme + ";\n") +
                (app.deploymentSchema == null ? "" : "   deploymentSchema : '" + app.deploymentSchema + "';\n") +
                (app.activationConfiguration == null ? "" : "   activationConfiguration : " + ((SnowflakeAppDeploymentConfiguration)app.activationConfiguration).activationConnection.connection + ";\n") +
                "}";
    }

    private static String renderSnowflakeM2MUdf(SnowflakeM2MUdf udf)
    {
        String packageName = udf._package == null || udf._package.isEmpty() ? udf.name : udf._package + "::" + udf.name;

        return "SnowflakeM2MUdf " + renderAnnotations(udf.stereotypes, udf.taggedValues) + packageName + "\n" +
                "{\n" +
                "   udfName : '" + udf.udfName + "';\n" +
                "   function : " + udf.function.path + ";\n" +
                "   ownership : Deployment { identifier: '" + ((DeploymentOwner)udf.ownership).id + "'};\n" +
                "   deploymentSchema : '" + udf.deploymentSchema + "';\n" +
                "   deploymentStage : '" + udf.deploymentStage + "';\n" +
                (udf.description == null ? "" : "   description : '" + udf.description + "';\n") +
                (udf.activationConfiguration == null ? "" : "   activationConfiguration : " + ((SnowflakeM2MUdfDeploymentConfiguration)udf.activationConfiguration).activationConnection.connection + ";\n") +
                "}";
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, sectionName) ->
        {
            if (!SnowflakeGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof SnowflakeApp)
                {
                    return renderSnowflakeApp((SnowflakeApp) element);
                }
                if (element instanceof SnowflakeM2MUdf)
                {
                    return renderSnowflakeM2MUdf((SnowflakeM2MUdf) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof SnowflakeApp || e instanceof SnowflakeM2MUdf), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(SnowflakeGrammarComposer::renderElement).makeString("###" + SnowflakeGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }
}
