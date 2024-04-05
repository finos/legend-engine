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

package org.finos.legend.engine.language.bigqueryFunction.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionArtifact;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionContent;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionDeploymentResult;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.bigqueryFunction.deployment.*;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.pure.generated.*;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;

public class BigQueryFunctionService implements FunctionActivatorService<Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction, BigQueryFunctionDeploymentConfiguration, BigQueryFunctionDeploymentResult>
{
    private final BigQueryFunctionDeploymentManager bigQueryFunctionDeploymentManager;

    public BigQueryFunctionService()
    {
        this.bigQueryFunctionDeploymentManager = new BigQueryFunctionDeploymentManager();
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "BigQuery");
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "BigQuery Function",
                "Create a BigQuery Function that can activate in BigQuery.",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::bigQueryFunction::BigQueryFunction",
                BigQueryFunctionProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        BigQueryFunctionArtifact artifact = BigQueryFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
        return this.validateArtifact(artifact);
    }

    @Override
    public BigQueryFunctionDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator, PureModelContext inputModel, List<BigQueryFunctionDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        BigQueryFunctionArtifact artifact = BigQueryFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
        MutableList<? extends FunctionActivatorError> validationErrors = this.validateArtifact(artifact);

        Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration deploymentConfiguration = ((Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration) activator._activationConfiguration());
        return validationErrors.notEmpty() ?
                new BigQueryFunctionDeploymentResult(validationErrors.collect(e -> e.message)) :
                this.bigQueryFunctionDeploymentManager.deployImpl(artifact, deploymentConfiguration);
    }

    @Override
    public BigQueryFunctionArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return BigQueryFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public List<BigQueryFunctionDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof BigQueryFunctionDeploymentConfiguration).collect(e -> (BigQueryFunctionDeploymentConfiguration) e);
    }

    private MutableList<? extends FunctionActivatorError> validateArtifact(BigQueryFunctionArtifact artifact)
    {
        int size = ((BigQueryFunctionContent)artifact.content).sqlExpressions.size();
        return size == 1 ?
                Lists.fixedSize.empty() :
                Lists.fixedSize.with(new BigQueryFunctionError("BigQuery Function can't be used with a plan containing '" + size + "' SQL expressions", ((BigQueryFunctionContent)artifact.content).sqlExpressions));
    }
}
