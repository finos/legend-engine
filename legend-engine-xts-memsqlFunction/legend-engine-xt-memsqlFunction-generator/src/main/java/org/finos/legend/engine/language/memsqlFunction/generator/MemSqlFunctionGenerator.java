// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.language.memsqlFunction.generator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;

import org.finos.legend.pure.generated.*;

public class MemSqlFunctionGenerator
{

    public static MemSqlFunctionArtifact generateArtifact(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        String sqlExpressions = core_memsqlfunction_generation_generation.Root_meta_external_function_activator_memSqlFunction_generation_generateArtifact_MemSqlFunction_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());

        RelationalDatabaseConnection connection;

        if (activator._activationConfiguration() != null)
        {
            //identify connection
            MemSqlFunction protocolActivator = org.eclipse.collections.impl.factory.Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(MemSqlFunction.class))
                    .select(c -> c.getPath().equals(platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(activator, pureModel.getExecutionSupport())))
                    .getFirst();
            connection = (RelationalDatabaseConnection) org.eclipse.collections.impl.factory.Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(PackageableConnection.class))
                    .select(c -> c.getPath().equals(((org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionDeploymentConfiguration) protocolActivator.activationConfiguration).activationConnection.connection)).getFirst().connectionValue;
            return new MemSqlFunctionArtifact(activator._functionName(), org.eclipse.collections.api.factory.Lists.mutable.of(sqlExpressions), new MemSqlFunctionDeploymentConfiguration(connection));
        }
        return new MemSqlFunctionArtifact(activator._functionName(), org.eclipse.collections.api.factory.Lists.mutable.of(sqlExpressions));
    }

    public static String generateLineage(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return core_memsqlfunction_generation_generation.Root_meta_external_function_activator_memSqlFunction_generation_computeLineage_MemSqlFunction_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
    }
}
