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
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class Execute implements Command
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Client client;

    private final PlanExecutor planExecutor;

    public Execute(Client client, PlanExecutor planExecutor)
    {
        this.client = client;
        this.planExecutor = planExecutor;
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
                list.addAll(result.getCompletion().collect(this::buildCandidate));
                return list;
            }
            else
            {
                this.client.printError(result.getEngineException(), parsedLine.line());
                AttributedStringBuilder ab = new AttributedStringBuilder();
                ab.append("> ");
                ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
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

    private Candidate buildCandidate(CompletionItem s)
    {
        return new Candidate(s.getCompletion(), s.getDisplay(), (String) null, (String) null, (String) null, (String) null, false, 0);
    }


    public String execute(String txt)
    {
        String code = "###Pure\n" +
                "function a::b::c::d():Any[*]\n{\n" + txt + ";\n}";

        PureModelContextData d = this.client.getModelState().parseWithTransient(code);

        if (this.client.isDebug())
        {
            try
            {
                this.client.getTerminal().writer().println((objectMapper.writeValueAsString(d)));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // Compile
        PureModel pureModel = this.client.getLegendInterface().compile(d);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        if (this.client.isDebug())
        {
            this.client.getTerminal().writer().println(">> " + extensions.collect(Root_meta_pure_extension_Extension::_type).makeString(", "));
        }

        // Plan
        Root_meta_pure_executionPlan_ExecutionPlan plan = this.client.getLegendInterface().generatePlan(pureModel, this.client.isDebug());
        String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);
        if (this.client.isDebug())
        {
            this.client.getTerminal().writer().println(planStr);
        }

        // Execute
        Result res = this.planExecutor.execute(planStr);
        if (res instanceof ConstantResult)
        {
            return ((ConstantResult) res).getValue().toString();
        }
        else
        {
            ReplExtension extension = this.client.getReplExtensions().detect(x -> x.supports(res));
            if (extension != null)
            {
                return extension.print(res);
            }
            else
            {
                throw new RuntimeException(res.getClass() + " not supported!");
            }
        }
    }

    public PlanExecutor getPlanExecutor()
    {
        return this.planExecutor;
    }
}
