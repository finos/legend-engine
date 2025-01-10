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

package org.finos.legend.engine.repl.dataCube.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.dataCube.server.handler.DataCubeInfrastructure;
import org.finos.legend.engine.repl.dataCube.server.handler.DataCubeQueryBuilder;
import org.finos.legend.engine.repl.dataCube.server.handler.DataCubeQueryExecutor;
import org.finos.legend.engine.repl.shared.ExecutionHelper;

import java.net.InetSocketAddress;
import java.util.List;

import static org.finos.legend.engine.repl.dataCube.server.REPLServerHelpers.DEV__CORSFilter;

public class REPLServer
{
    private final Client client;
    private final REPLServerHelpers.REPLServerState state;

    private int port;
    private String webAppDevBaseUrl;
    private String urlTemplate; // e.g. https://my.template:{{port}}/subApp

    public REPLServer(Client client)
    {
        this.client = client;
        this.state = new REPLServerHelpers.REPLServerState(client, client.getObjectMapper(), client.getPlanExecutor(), client.getLegendInterface());
    }

    public void initializeStateWithREPLExecutedQuery(ExecutionHelper.ExecuteResultSummary executeResultSummary)
    {
        this.state.initializeWithREPLExecutedQuery(executeResultSummary);
    }

    public void initializeStateFromTable(PureModelContextData pureModelContextData)
    {
        this.state.initializeFromTable(pureModelContextData);
    }

    public String getUrl()
    {
        String baseUrl = "http://localhost:" + this.port;
        if (this.webAppDevBaseUrl != null)
        {
            baseUrl = this.webAppDevBaseUrl;
        }
        else if (this.urlTemplate != null)
        {
            baseUrl = this.urlTemplate.replace("{{port}}", this.port + "");
        }
        return baseUrl + "/repl/dataCube";
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
                .withKeyValue("/api/dataCube/parseValueSpecification", new DataCubeQueryBuilder.ParseValueSpecification())
                .withKeyValue("/api/dataCube/getValueSpecificationCode", new DataCubeQueryBuilder.GetValueSpecificationCode())
                .withKeyValue("/api/dataCube/getValueSpecificationCode/batch", new DataCubeQueryBuilder.GetValueSpecificationCodeBatch())
                .withKeyValue("/api/dataCube/getBaseQuery", new DataCubeQueryBuilder.GetBaseQuery())
                .withKeyValue("/api/dataCube/getRelationReturnType", new DataCubeQueryBuilder.GetRelationReturnType())
                .withKeyValue("/api/dataCube/getRelationReturnType/code", new DataCubeQueryBuilder.GetQueryCodeRelationReturnType())
                .withKeyValue("/api/dataCube/executeQuery", new DataCubeQueryExecutor.ExecuteQuery())
                .withKeyValue("/api/dataCube/getExecutionPlan", new DataCubeQueryExecutor.GetExecutionPlan())
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

        if (System.getProperty("legend.repl.dataCube.urlTemplate") != null)
        {
            this.urlTemplate = System.getProperty("legend.repl.dataCube.urlTemplate");
        }

        server.setExecutor(null);
        server.start();
        this.port = server.getAddress().getPort();
        if (this.webAppDevBaseUrl != null)
        {
            this.client.printDebug("[DEV] DataCube expects webapp at: " + webAppDevBaseUrl);
        }
        this.client.printDebug("[DEV] DataCube has started at port: " + this.port);
    }
}
