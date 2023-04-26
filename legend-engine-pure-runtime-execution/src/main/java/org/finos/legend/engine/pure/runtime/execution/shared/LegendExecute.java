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

package org.finos.legend.engine.pure.runtime.execution.shared;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class LegendExecute
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendExecute.class);

    public static String doExecute(String jsonPlan, Map<String, Object> variables)
    {
        // create plan executor
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(true);

        // run using current user credentials
        IdentityFactory instance = IdentityFactoryProvider.getInstance();
        Identity identity = instance.makeIdentity(SubjectTools.getCurrentSubject());
        MutableList<CommonProfile> profiles = ListAdapter.adapt(instance.adapt(identity));

        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withParams(variables)
                .withPlanAsString(jsonPlan)
                .withProfiles(profiles)
                .withUser(identity.getName())
                .build();

        // execute plan
        Result result = planExecutor.executeWithArgs(executeArgs);
        try (AutoCloseable ignore = result::close)
        {
            if (result instanceof StreamingResult)
            {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ((StreamingResult) result).stream(outputStream, SerializationFormat.defaultFormat);
                return outputStream.toString();
            }
            else if (result instanceof ConstantResult)
            {
                return ((ConstantResult) result).stream().toString();
            }
            else
            {
                throw new UnsupportedOperationException("Result type not supported: " + result.getClass().getName());
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to execute plan", e);
            throw new PureExecutionException("Failed to execute plan - " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }
}
