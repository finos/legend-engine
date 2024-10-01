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

package org.finos.legend.engine.repl.shared;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.util.HashMap;

public class ExecutionHelper
{
    public static final String REPL_RUN_FUNCTION_QUALIFIED_PATH = "repl::__internal__::run__Any_MANY_";
    public static final String REPL_RUN_FUNCTION_SIGNATURE = "repl::__internal__::run():Any[*]";

    public static Identity resolveIdentityFromLocalSubject(Client client)
    {
        try
        {
            return Identity.makeIdentity(SubjectTools.getLocalSubject());
        }
        catch (Exception e)
        {
            if (client.isDebug())
            {
                client.println("Error: Couldn't resolve identity from local subject");
            }
            return Identity.getAnonymousIdentity();
        }
    }

    public static ExecutionHelper.ExecuteResultSummary executeCode(String txt, Client client, Function4<Result, PureModelContextData, PureModel, SingleExecutionPlan, ExecuteResultSummary> resultHandler)
    {
        String code = "###Pure\n" +
                "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + txt + ";\n}";

        if (client.isDebug())
        {
            client.printDebug("---------------------------------------- INPUT ----------------------------------------");
            client.println("Function: " + code);
        }

        PureModelContextData pmcd = client.getModelState().parseWithTransient(code);

        if (client.isDebug())
        {
            try
            {
                client.println("PMCD: " + client.getObjectMapper().writeValueAsString(pmcd));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // Compile
        PureModel pureModel = client.getLegendInterface().compile(pmcd);

        // Plan
        Root_meta_pure_executionPlan_ExecutionPlan plan = client.getLegendInterface().generatePlan(pureModel, client.isDebug());
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);

        if (client.isDebug())
        {
            client.printDebug("---------------------------------------- PLAN ----------------------------------------");
            client.println("Extensions: " + extensions.collect(Root_meta_pure_extension_Extension::_type).makeString(", "));
            client.println("Generated Plan: " + planStr);
        }

        // Execute
        Identity identity = ExecutionHelper.resolveIdentityFromLocalSubject(client);
        SingleExecutionPlan execPlan = (SingleExecutionPlan) PlanExecutor.readExecutionPlan(planStr);
        Result res = client.getPlanExecutor().execute(execPlan, new HashMap<>(), identity.getName(), identity, null);
        return resultHandler.value(res, pmcd, pureModel, execPlan);
    }

    public static class ExecuteResultSummary
    {
        public final PureModelContextData pureModelContextData;
        public final PureModel pureModel;
        public final Result result;
        public final SingleExecutionPlan plan;
        public final String resultPreview;

        public ExecuteResultSummary(PureModelContextData pureModelContextData, PureModel pureModel, SingleExecutionPlan plan, Result result, String resultPreview)
        {
            this.pureModelContextData = pureModelContextData;
            this.pureModel = pureModel;
            this.plan = plan;
            this.result = result;
            this.resultPreview = resultPreview;
        }
    }
}
