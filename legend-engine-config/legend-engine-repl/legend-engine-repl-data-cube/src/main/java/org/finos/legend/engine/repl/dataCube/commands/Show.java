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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.awt.*;
import java.net.URI;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.executeCode;

public class Show implements Command
{
    private final Client client;

    public REPLServer replServer;

    public Show(Client client, REPLServer replServer)
    {
        this.client = client;
        this.replServer = replServer;
    }

    @Override
    public String documentation()
    {
        return "show";
    }

    @Override
    public String description()
    {
        return "show the result for the last executed query in DataCube";
    }

    @Override
    public boolean process(String line)
    {
        if (line.startsWith("show"))
        {
            String expression = this.client.getLastCommand(1);
            if (expression == null)
            {
                this.client.printError("Failed to retrieve the last command");
                return true;
            }
            run(expression, this.client, this.replServer);
            return true;
        }
        return false;
    }

    public static void run(String expression, Client client, REPLServer replServer)
    {
        ExecutionHelper.ExecuteResultSummary lastExecuteResultSummary;
        try
        {
            lastExecuteResultSummary = executeCode(expression, client, (Result res, PureModelContextData pmcd, PureModel pureModel, SingleExecutionPlan plan) ->
            {
                ReplExtension extension = client.getReplExtensions().detect(x -> x.supports(res));
                if (extension != null)
                {
                    return new ExecutionHelper.ExecuteResultSummary(pmcd, pureModel, plan, res, null);
                }
                else
                {
                    throw new RuntimeException(res.getClass() + " not supported!");
                }
            });
        }
        catch (Exception e)
        {
            client.printError("Last command run is not an execution of a Pure expression (command run: '" + expression + "')");
            if (e instanceof EngineException)
            {
                client.printEngineError((EngineException) e, expression);
                return;
            }
            else
            {
                throw e;
            }
        }
        replServer.initializeStateWithREPLExecutedQuery(lastExecuteResultSummary);
        launchDataCube(client, replServer);
    }

    public static void launchDataCube(Client client, REPLServer replServer)
    {
        try
        {
            client.println(replServer.getUrl());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(URI.create(replServer.getUrl()));
            }
        }
        catch (Exception e)
        {
            client.printError(e.getMessage());
        }
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
