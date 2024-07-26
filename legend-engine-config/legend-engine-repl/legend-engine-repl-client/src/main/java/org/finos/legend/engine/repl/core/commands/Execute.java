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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.executeCode;

public class Execute implements Command
{
    private final Client client;

    public Execute(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "<pure expression>";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        this.client.printInfo(execute(line));
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
                this.client.printEngineError(result.getEngineException(), parsedLine.line());
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
        return executeCode(txt, this.client, (Result res, PureModelContextData pmcd, PureModel pureModel) ->
        {
            // Show result
            if (res instanceof ConstantResult)
            {
                return new ExecutionHelper.ExecuteResultSummary(pmcd, pureModel, res, ((ConstantResult) res).getValue().toString());
            }
            else
            {
                ReplExtension extension = this.client.getReplExtensions().detect(x -> x.supports(res));
                if (extension != null)
                {
                    return new ExecutionHelper.ExecuteResultSummary(pmcd, pureModel, res, extension.print(res));
                }
                else
                {
                    throw new RuntimeException(res.getClass() + " not supported!");
                }
            }
        }).resultPreview;
    }
}
