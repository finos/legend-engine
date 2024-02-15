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

package org.finos.legend.engine.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtensionLoader;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtensionLoader;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtensionLoader;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilderLoader;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.extension.LegendExtension;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;


public class PrintExtensions
{
    public static void main(String[] args)
    {
        Logger l = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        l.setLevel(Level.INFO);

        MutableList<LegendExtension> extensionList = Lists.mutable
                // Pure code extension for Store, External Format, etc.
                .<LegendExtension>withAll(PureCoreExtensionLoader.extensions())

                // All Function Activators
                .withAll(FunctionActivatorLoader.extensions())

                // Language Extensions
                .withAll(PureProtocolExtensionLoader.extensions())
                .withAll(PureGrammarParserExtensionLoader.extensions())
                .withAll(PureGrammarComposerExtensionLoader.extensions())
                .withAll(CompilerExtensionLoader.extensions())

                // ExternalFormat Extension
                .withAll(ExternalFormatExtensionLoader.extensions().values())
                .withAll(ExternalFormatRuntimeExtensionLoader.extensions().values())

                // Plan extensions
                .withAll(Iterate.addAllTo(ServiceLoader.load(PlanGeneratorExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))
                .withAll(ExecutionExtensionLoader.extensions())
                .withAll(ExecutionPlanJavaCompilerExtensionLoader.extensions())
                .withAll(StoreExecutorBuilderLoader.extensions())

                // Code generation extensions
                .withAll(ArtifactGenerationExtensionLoader.extensions())
                .withAll(Iterate.addAllTo(ServiceLoader.load(ModelGenerationExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))
                .withAll(Iterate.addAllTo(ServiceLoader.load(GenerationExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))

                // Connections
                .withAll(Iterate.addAllTo(ServiceLoader.load(StrategicConnectionExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))
                .withAll(Iterate.addAllTo(ServiceLoader.load(ConnectionFactoryExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))
                .withAll(Iterate.addAllTo(ServiceLoader.load(ConnectionExtension.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))
                .withAll(Iterate.addAllTo(ServiceLoader.load(DynamicTestConnection.class), org.eclipse.collections.api.factory.Lists.mutable.empty()))

                // All Testable Packageable Elements
                .withAll(TestableRunnerExtensionLoader.getClassifierPathToTestableRunnerMap(Thread.currentThread().getContextClassLoader()).values());

        System.out.println(
                extensionList
                        .groupBy(LegendExtension::group)
                        .keyMultiValuePairsView()
                        .toSortedListBy(Pair::getOne)
                        .collect(c -> c.getOne() + "\n    " + c.getTwo().collect(LegendExtension::type).toSortedList().makeString(", "))
                        .makeString("\n")
        );
    }
}
