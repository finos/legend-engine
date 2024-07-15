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

package org.finos.legend.engine.repl.relational.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.relational.server.handler.DataCubeInfrastructure;
import org.finos.legend.engine.repl.relational.server.handler.DataCubeQueryBuilder;
import org.finos.legend.engine.repl.relational.server.handler.DataCubeQueryExecutor;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.net.InetSocketAddress;
import java.util.List;

import static org.finos.legend.engine.repl.relational.server.REPLServerHelpers.DEV__CORSFilter;
import static org.jline.jansi.Ansi.ansi;

public class REPLServer
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
    private final Client client;
    private final REPLServerHelpers.REPLServerState state;

    private int port;
    private String webAppDevBaseUrl;

    public REPLServer(Client client)
    {
        this.client = client;
        this.state = new REPLServerHelpers.REPLServerState(client, objectMapper, planExecutor, client.getLegendInterface());
    }

    public void setExecuteResult(Execute.ExecuteResult executeResult)
    {
        this.state.initializeWithREPLExecutedQuery(executeResult);
    }

    public String getUrl()
    {
        return (this.webAppDevBaseUrl != null ? this.webAppDevBaseUrl : "http://localhost:" + this.port) + "/repl/dataCube";
    }

    public void initialize() throws Exception
    {
        InetSocketAddress serverPortAddress = new InetSocketAddress(System.getProperty("legend.repl.dataCube.devPort") != null ? Integer.parseInt(System.getProperty("legend.repl.dataCube.devPort")) : 0);
        HttpServer server = HttpServer.create(serverPortAddress, 0);

        // register handlers
        MutableList<HttpContext> contexts = Maps.mutable.<String, REPLServerHelpers.DataCubeServerHandler>empty()
                .withKeyValue("/repl/", new DataCubeInfrastructure.StaticContent())
                .withKeyValue("/api/dataCube/infrastructureInfo", new DataCubeInfrastructure.GridLicenseKey())
                .withKeyValue("/api/dataCube/typeahead", new DataCubeQueryBuilder.QueryTypeahead())
                .withKeyValue("/api/dataCube/parseQuery", new DataCubeQueryBuilder.ParseQuery())
                .withKeyValue("/api/dataCube/getQueryCode", new DataCubeQueryBuilder.GetQueryCode())
                .withKeyValue("/api/dataCube/getQueryCode/batch", new DataCubeQueryBuilder.GetQueryCodeBatch())
                .withKeyValue("/api/dataCube/getBaseQuery", new DataCubeQueryBuilder.GetBaseQuery())
                .withKeyValue("/api/dataCube/getRelationReturnType", new DataCubeQueryBuilder.GetRelationReturnType())
                .withKeyValue("/api/dataCube/executeQuery", new DataCubeQueryExecutor.ExecuteQuery())
                .keyValuesView().collect(config -> server.createContext(config.getOne(), config.getTwo().getHandler(this.state))).toList();

        // CORS filter
        // only needed if we're not serving the webapp from the same server, e.g. in development
        if (System.getProperty("legend.repl.dataCube.devWebAppBaseUrl") != null)
        {
            this.webAppDevBaseUrl = System.getProperty("legend.repl.dataCube.devWebAppBaseUrl");
            List<Filter> filters = Lists.mutable.empty();
            filters.add(new DEV__CORSFilter(Lists.mutable.with(this.webAppDevBaseUrl)));
            contexts.forEach(context -> context.getFilters().addAll(filters));
        }

        server.setExecutor(null);
        server.start();
        this.port = server.getAddress().getPort();
        if (this.webAppDevBaseUrl != null)
        {
            this.client.getTerminal().writer().println(ansi().fgBrightBlack().a("[DEV] DataCube expects webapp at: " + webAppDevBaseUrl).reset());
        }
        this.client.getTerminal().writer().println(ansi().fgBrightBlack().a("[DEV] DataCube has started at port: " + this.port).reset());
    }
}
