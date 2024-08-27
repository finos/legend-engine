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

package org.finos.legend.engine.language.memsql.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.memsql.deployment.MemSqlFunctionDeploymentManager;
import org.finos.legend.engine.language.memsql.deployment.MemSqlFunctionGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionContent;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentResult;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.*;

import java.util.List;

public class MemSqlFunctionService implements FunctionActivatorService<Root_meta_external_function_activator_memSqlFunction_MemSqlFunction, MemSqlFunctionDeploymentConfiguration, MemSqlFunctionDeploymentResult>
{
    private MemSqlFunctionDeploymentManager memSqlFunctionDeploymentManager;

    public MemSqlFunctionService()
    {
    }

    public MemSqlFunctionService(PlanExecutor planExecutor)
    {
        this.memSqlFunctionDeploymentManager = new MemSqlFunctionDeploymentManager(planExecutor);
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "MemSql Function",
                "Create a MemSql Function that can activate in MemSql.",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::memSqlFunction::MemSqlFunction",
                MemSqlFunctionProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_memSqlFunction_MemSqlFunction;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MemSqlFunctionArtifact artifact = MemSqlFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
        return this.validateArtifact(artifact);
    }

    @Override
    public MemSqlFunctionDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, List<MemSqlFunctionDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MemSqlFunctionArtifact artifact = MemSqlFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);

        MutableList<? extends FunctionActivatorError> validationErrors = this.validateArtifact(artifact);

        Root_meta_external_function_activator_memSqlFunction_MemSqlFunctionDeploymentConfiguration deploymentConfiguration = ((Root_meta_external_function_activator_memSqlFunction_MemSqlFunctionDeploymentConfiguration) activator._activationConfiguration());
        return validationErrors.notEmpty() ?
                new MemSqlFunctionDeploymentResult(validationErrors.collect(e -> e.message)) :
                this.memSqlFunctionDeploymentManager.deploy(identity, artifact, runtimeConfigurations);
    }

    @Override
    public MemSqlFunctionArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return MemSqlFunctionGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return MemSqlFunctionGenerator.generateLineage(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public List<MemSqlFunctionDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof MemSqlFunctionDeploymentConfiguration).collect(e -> (MemSqlFunctionDeploymentConfiguration) e);
    }

    private MutableList<? extends FunctionActivatorError> validateArtifact(MemSqlFunctionArtifact artifact)
    {
        int size = ((MemSqlFunctionContent)artifact.content).sqlExpressions.size();
        return size == 1 ?
                Lists.fixedSize.empty() :
                Lists.fixedSize.with(new MemSqlFunctionError("MemSql Function can't be used with a plan containing '" + size + "' SQL expressions", ((MemSqlFunctionContent)artifact.content).sqlExpressions));
    }
}
