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

package org.finos.legend.engine.repl.core.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.Helpers;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.HashMap;
import java.util.UUID;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_SIGNATURE;
import static org.jline.jansi.Ansi.ansi;

public class Execute implements Command
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final Client client;
    private final PlanExecutor planExecutor;
    private ExecuteResultSummary lastExecuteResultSummary;
    private String currentExecutionId;

    public Execute(Client client, PlanExecutor planExecutor)
    {
        this.client = client;
        this.planExecutor = planExecutor;
    }

    public ExecuteResultSummary getLastExecuteResultSummary()
    {
        return this.lastExecuteResultSummary;
    }

    public String getCurrentExecutionId()
    {
        return this.currentExecutionId;
    }

    @Override
    public String documentation()
    {
        return "<pure expression>";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        this.client.getTerminal().writer().println(execute(line));
        return true;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        try
        {
            MutableList<Candidate> list = Lists.mutable.empty();
            CompletionResult result = new org.finos.legend.engine.repl.autocomplete.Completer(this.client.getModelState().getText(), this.client.getCompleterExtensions()).complete(inScope);
            if (result.getEngineException() == null)
            {
                list.addAll(result.getCompletion().collect(s -> new Candidate(s.getCompletion(), s.getDisplay(), null, null, null, null, false, 0)));
                return list;
            }
            else
            {
                this.client.printError(result.getEngineException(), parsedLine.line());
                AttributedStringBuilder ab = new AttributedStringBuilder();
                ab.append("> ");
                ab.style(new AttributedStyle().foreground(AttributedStyle.GREEN));
                ab.append(parsedLine.line());
                this.client.getTerminal().writer().print(ab.toAnsi());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public String execute(String txt)
    {
        this.currentExecutionId = UUID.randomUUID().toString();
        this.lastExecuteResultSummary = executeCode(txt, this.client, this.planExecutor, this.currentExecutionId);
        return this.lastExecuteResultSummary.resultPreview;
    }

    public static ExecuteResultSummary executeCode(String txt, Client client, PlanExecutor planExecutor, String executionId)
    {
        String code = "###Pure\n" +
                "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + txt + ";\n}";

        if (client.isDebug())
        {
            client.getTerminal().writer().println(ansi().fgBrightBlack().a("---------------------------------------- INPUT ----------------------------------------").reset());
            client.getTerminal().writer().println(ansi().fgBrightBlack().a("Function: " + code).reset());
        }

        PureModelContextData d = client.getModelState().parseWithTransient(code);

        if (client.isDebug())
        {
            try
            {
                client.getTerminal().writer().println(ansi().fgBrightBlack().a("PMCD: " + objectMapper.writeValueAsString(d)).reset());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // Compile
        PureModel pureModel = client.getLegendInterface().compile(d);

        // Plan
        Root_meta_pure_executionPlan_ExecutionPlan plan = client.getLegendInterface().generatePlan(pureModel, client.isDebug());
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);

        if (client.isDebug())
        {
            client.getTerminal().writer().println(ansi().fgBrightBlack().a("---------------------------------------- PLAN ----------------------------------------").reset());
            client.getTerminal().writer().println(ansi().fgBrightBlack().a("Extensions: " + extensions.collect(Root_meta_pure_extension_Extension::_type).makeString(", ")).reset());
            client.getTerminal().writer().println(ansi().fgBrightBlack().a("Generated Plan: " + planStr).reset());
        }

        // Execute
        Identity identity = Helpers.resolveIdentityFromLocalSubject(client);
        SingleExecutionPlan execPlan = (SingleExecutionPlan) PlanExecutor.readExecutionPlan(planStr);
        Result res = planExecutor.execute(execPlan, new HashMap<>(), identity.getName(), identity, null);

        // Show result
        if (res instanceof ConstantResult)
        {
            return new ExecuteResultSummary(d, pureModel, res, ((ConstantResult) res).getValue().toString(), null);
        }
        else
        {
            ReplExtension extension = client.getReplExtensions().detect(x -> x.supports(res));
            if (extension != null)
            {
                return new ExecuteResultSummary(d, pureModel, res, extension.print(res), executionId);
            }
            else
            {
                throw new RuntimeException(res.getClass() + " not supported!");
            }
        }
    }

    public static class ExecuteResultSummary
    {
        public final String executionId;
        public final PureModelContextData pureModelContextData;
        public final PureModel pureModel;
        public final Result result;
        public final String resultPreview;

        public ExecuteResultSummary(PureModelContextData pureModelContextData, PureModel pureModel, Result result, String resultPreview, String executionId)
        {
            this.pureModelContextData = pureModelContextData;
            this.pureModel = pureModel;
            this.result = result;
            this.resultPreview = resultPreview;
            this.executionId = executionId;
        }
    }
}
