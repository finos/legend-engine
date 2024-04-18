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

package org.finos.legend.engine.plan.execution;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TestPlanExecutor
{
    @Test
    public void testStateWithReferralHeaderPlaceHolder()
    {
        final String session = "testSession";
        Map<String, Result> res = Maps.mutable.empty();
        res.put("execID", new ConstantResult("b26973f8-8857-4ece-bfdc-107176c9da8b"));
        res.put("userId", new ConstantResult("anumam"));
        Identity identity = IdentityFactoryProvider.getInstance().getAnonymousIdentity();

        ExecutionState state1 = new ExecutionState(res, Lists.mutable.empty(), Lists.mutable.empty(), false, 52_428_800L, new RequestContext(session, "https://allo'y'.site.gs.com/"));
        PlanExecutor.setUpState(new SingleExecutionPlan(), state1, identity, "anumam");
        String sqlQuery1 = FreeMarkerExecutor.process("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';", state1, "snowflake", null);
        Assert.assertEquals("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"b26973f8-8857-4ece-bfdc-107176c9da8b\", \"engineUser\" : \"anumam\", \"referer\" : \"https://allo''y''.site.gs.com/\"}';", sqlQuery1);

        ExecutionState state2 = new ExecutionState(res, Lists.mutable.empty(), Lists.mutable.empty(), false, 52_428_800L, new RequestContext(session, null));
        PlanExecutor.setUpState(new SingleExecutionPlan(), state2, identity, "anumam");
        String sqlQuery2 = FreeMarkerExecutor.process("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';", state2, "snowflake", null);
        Assert.assertEquals("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"b26973f8-8857-4ece-bfdc-107176c9da8b\", \"engineUser\" : \"anumam\", \"referer\" : \"null\"}';", sqlQuery2);

        ExecutionState state3 = new ExecutionState(res, Lists.mutable.empty(), Lists.mutable.empty(), false, 52_428_800L, null);
        PlanExecutor.setUpState(new SingleExecutionPlan(), state3, identity, "anumam");
        String sqlQuery3 = FreeMarkerExecutor.process("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';", state3, "snowflake", null);
        Assert.assertEquals("ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"b26973f8-8857-4ece-bfdc-107176c9da8b\", \"engineUser\" : \"anumam\", \"referer\" : \"null\"}';", sqlQuery3);
    }
}
