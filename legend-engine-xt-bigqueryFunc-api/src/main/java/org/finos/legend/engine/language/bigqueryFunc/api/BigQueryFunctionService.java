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

package org.finos.legend.engine.language.bigqueryFunc.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorArtifact;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.bigqueryFunc.metamodel.BigQueryFunctionProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.pure.generated.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class BigQueryFunctionService implements FunctionActivatorService<Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryFunctionService.class);

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "BigQuery Function",
                "Create a BigQuery Function that can activate in BigQuery.",
                "meta::protocols::pure::" + version + "::metamodel::functionActivator::bigQueryFunc::BigQueryFunction",
                BigQueryFunctionProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_functionActivator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(PureModel pureModel, Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        try
        {
            BigQueryFunctionDeployableArtifact.buildArtifact(pureModel, functionActivator, routerExtensions);
            return Lists.mutable.empty();

        }
        catch (BigQueryFunctionArtifactValidationException e)
        {
            return Lists.mutable.with(e.toError());
        }
    }

    @Override
    public MutableList<? extends FunctionActivatorError> publishToSandbox(PureModel pureModel, Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        try
        {
            BigQueryFunctionDeployableArtifact deployableArtifact = BigQueryFunctionDeployableArtifact.buildArtifact(pureModel, functionActivator, routerExtensions);
            LOGGER.info("Starting deployment of {}!", functionActivator._functionName());
            deployableArtifact.deploy();
            LOGGER.info("Completed deployment of {} successfully!", functionActivator._functionName());
            return Lists.mutable.empty();
        }
        catch (BigQueryFunctionArtifactValidationException e)
        {
            return Lists.mutable.with(e.toError());
        }
        catch (SQLException e)
        {
            LOGGER.info("Deployment of {} failed!", functionActivator._functionName());
            return Lists.mutable.with(new FunctionActivatorError(e.getMessage()));
        }
    }

    @Override
    public FunctionActivatorArtifact renderArtifact(PureModel pureModel, Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        try
        {
            return BigQueryFunctionDeployableArtifact.buildArtifact(pureModel, functionActivator, routerExtensions).toArtifact();
        }
        catch (BigQueryFunctionArtifactValidationException e)
        {
            throw new RuntimeException(e);
        }
    }

}
