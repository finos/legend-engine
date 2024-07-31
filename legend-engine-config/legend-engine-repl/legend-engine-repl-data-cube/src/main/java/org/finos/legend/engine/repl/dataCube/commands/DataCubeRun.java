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

package org.finos.legend.engine.repl.dataCube.commands;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.executeCode;

public class DataCubeRun implements Command
{
    private final DataCube parentCommand;
    private final Client client;
    private final REPLServer replServer;

    public DataCubeRun(DataCube parentCommand, Client client, REPLServer replServer)
    {
        this.parentCommand = parentCommand;
        this.client = client;
        this.replServer = replServer;
    }

    @Override
    public String documentation()
    {
        return "datacube run -- <pure expression>";
    }

    @Override
    public String description()
    {
        return "run the query and launch DataCube";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube run --"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length <= 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            int commandLength = "datacube run --".length() + 1;
            String expression = line.substring(commandLength).trim();
            ExecutionHelper.ExecuteResultSummary executeResultSummary;
            try
            {
                executeResultSummary = executeCode(expression, this.client, (Result res, PureModelContextData pmcd, PureModel pureModel) ->
                {
                    ReplExtension extension = this.client.getReplExtensions().detect(x -> x.supports(res));
                    if (extension != null)
                    {
                        return new ExecutionHelper.ExecuteResultSummary(pmcd, pureModel, res, null);
                    }
                    else
                    {
                        throw new RuntimeException(res.getClass() + " not supported!");
                    }
                });
            }
            catch (EngineException e)
            {
                SourceInformation originalSourceInformation = e.getSourceInformation();
                SourceInformation sourceInformation = new SourceInformation();
                sourceInformation.sourceId = originalSourceInformation.sourceId;
                sourceInformation.startLine = originalSourceInformation.startLine;
                sourceInformation.startColumn = originalSourceInformation.startColumn + commandLength;
                sourceInformation.endLine = originalSourceInformation.endLine;
                sourceInformation.endColumn = originalSourceInformation.endColumn + commandLength;
                throw new EngineException(e.getMessage(), sourceInformation, e.getErrorType(), e.getCause(), e.getErrorCategory());
            }

            this.replServer.initializeStateWithREPLExecutedQuery(executeResultSummary);
            Show.launchDataCube(this.client, this.replServer);
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("datacube run --"))
        {
            if (parsedLine.words().size() >= 5)
            {
                // In expression block
                try
                {
                    String argsString = inScope.substring("datacube run --".length() + 1);
                    String expression = argsString.substring(argsString.indexOf(" ") + 1);

                    MutableList<Candidate> list = Lists.mutable.empty();
                    CompletionResult result = new org.finos.legend.engine.repl.autocomplete.Completer(this.client.getModelState().getText(), this.client.getCompleterExtensions()).complete(expression);
                    if (result.getEngineException() == null)
                    {
                        list.addAll(result.getCompletion().collect(c -> new Candidate(c.getCompletion(), c.getDisplay(), null, null, null, null, false, 0)));
                        return list;
                    }
                    else
                    {
                        this.client.printEngineError(result.getEngineException(), expression);
                        AttributedStringBuilder ab = new AttributedStringBuilder();
                        ab.append("> ");
                        ab.style(new AttributedStyle().foreground(AttributedStyle.GREEN));
                        ab.append(parsedLine.line());
                        this.client.getTerminal().writer().print(ab.toAnsi());
                        return Lists.mutable.empty();
                    }
                }
                catch (Exception ignored)
                {
                }
                return Lists.mutable.empty();
            }
            return Lists.mutable.empty();
        }
        return null;
    }
}
