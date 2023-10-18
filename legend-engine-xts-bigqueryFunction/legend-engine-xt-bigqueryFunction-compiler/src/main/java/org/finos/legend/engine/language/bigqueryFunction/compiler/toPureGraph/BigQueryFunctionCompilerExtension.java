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

package org.finos.legend.engine.language.bigqueryFunction.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunction;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;

import java.util.Collections;

public class BigQueryFunctionCompilerExtension implements CompilerExtension
{
    // Here only for dependency check error ...
    CoreFunctionActivatorCodeRepositoryProvider forDependencies;

    @Override
    public CompilerExtension build()
    {
        return new BigQueryFunctionCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        BigQueryFunction.class,
                        Lists.fixedSize.with(BigQueryFunctionDeploymentConfiguration.class),
                        this::buildBigQueryFunction
                ),
                Processor.newProcessor(
                        BigQueryFunctionDeploymentConfiguration.class,
                        this::buildDeploymentConfig
                )
        );
    }

    public Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction buildBigQueryFunction(BigQueryFunction bigQueryFunction, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(bigQueryFunction.function), bigQueryFunction.sourceInformation);
            return new Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction_Impl(
                    bigQueryFunction.name,
                    null,
                    context.pureModel.getClass("meta::external::function::activator::bigQueryFunction::BigQueryFunction"))
                    ._functionName(bigQueryFunction.functionName)
                    ._function(func)
                    ._description(bigQueryFunction.description)
                    ._owner(bigQueryFunction.owner)
                    ._activationConfiguration(bigQueryFunction.activationConfiguration != null ? buildDeploymentConfig((BigQueryFunctionDeploymentConfiguration) bigQueryFunction.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration buildDeploymentConfig(BigQueryFunctionDeploymentConfiguration configuration, CompileContext context)
    {
        return new Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration_Impl("")
                ._target((Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) context.resolveConnection(configuration.activationConnection.connection, configuration.sourceInformation));
        // ._stage(context.pureModel.getEnumValue("meta::external::function::activator::DeploymentStage", configuration.stage.name()));
    }
}
