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
import org.finos.legend.shared.stuctures.TreeNode;

import java.util.ServiceLoader;

public class Extensions
{
    public static MutableList<LegendExtension> get()
    {
        return Lists.mutable
                // Pure code extension for Store, External Format, etc.
                .withAll(loadExtensions("org.finos.legend.engine.pure.code.core.LegendPureCoreExtension"))

                // All Function Activators
                .withAll(loadExtensions("org.finos.legend.engine.functionActivator.service.FunctionActivatorService"))

                // Language Extensions
                .withAll(loadExtensions("org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension"))
                
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.from.IRelationalGrammarParserExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.to.IRelationalGrammarComposerExtension"))

                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.from.IRelationalGrammarParserExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.grammar.to.IRelationalGrammarComposerExtension"))

                .withAll(loadExtensions("org.finos.legend.engine.language.pure.code.completer.CompleterExtension"))

                // ExternalFormat Extension
                .withAll(loadExtensions("org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension"))

                // Plan extensions
                .withAll(loadExtensions("org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.extension.ExecutionExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder"))

                // Code generation extensions
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.external.shared.format.extension.GenerationExtension"))

                // Connections
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension"))
                .withAll(loadExtensions("org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection"))

                // All Testable Packageable Elements
                .withAll(loadExtensions("org.finos.legend.engine.testable.extension.TestableRunnerExtension"));
    }

    public static TreeNode buildTree(MutableList<LegendExtension> extensionList)
    {
        TreeNode extensions = new TreeNode("Extensions");
        extensionList
                .groupBy(LegendExtension::group)
                .keyMultiValuePairsView()
                .toSortedListBy(x -> x.getOne().toString())
                .forEach(c ->
                {
                    TreeNode module = extensions;
                    for (String x : c.getOne())
                    {
                        module = module.createOrReturnChild("#" + x + "#");
                    }
                    for (LegendExtension z : c.getTwo())
                    {
                        TreeNode cursor = module;
                        for (String x : z.typeGroup())
                        {
                            cursor = cursor.createOrReturnChild(x);
                        }
                        cursor.createOrReturnChild(z.type() + "   [" + z.getClass().getSimpleName() + ".class]");
                    }
                });
        return extensions;
    }

    private static MutableList<LegendExtension> loadExtensions(String _class)
    {
        MutableList<LegendExtension> res = Lists.mutable.empty();
        try
        {
            Class<?> _cl = Class.forName(_class);
            for (LegendExtension ext : (ServiceLoader<LegendExtension>) ServiceLoader.load(_cl))
            {
                res.add(ext);
            }
            return res;
        }
        catch (Exception e)
        {
            System.out.println("Not Found: " + _class);
            return res;
        }
    }
}
