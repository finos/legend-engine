// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.api.LambdaReturnTypeInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendColumn;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecutionResult;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendExecutionResultFromTds;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendTdsResultParser;
import org.finos.legend.engine.protocol.pure.m3.relation.RelationType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCombination;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;

import java.io.IOException;
import java.util.List;

public class GenericLegendExecution implements LegendExecution
{
    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private final String protocol;
    private final String host;
    private final String port;

    public GenericLegendExecution(String protocol, String host, String port)
    {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    @Override
    public List<LegendColumn> getSchema(String query, String database)
    {
        try
        {
            // Input --------------------------------
            HttpPost request = new HttpPost(protocol + "://" + host + ":" + port + "/api/pure/v1/compilation/lambdaRelationType");
            LambdaReturnTypeInput input = new LambdaReturnTypeInput();
            input.model = buildPointer(database);
            input.lambda = PureGrammarParser.newInstance().parseLambda("#SQL{" + query + "}#");
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            request.setEntity(bodyEntity);
            // Input --------------------------------

            try
            {
                HttpClient httpClient = HttpClientBuilder.getHttpClient(new BasicCookieStore());
                HttpResponse response = httpClient.execute(request);
                RelationType type = MAPPER.readValue(response.getEntity().getContent(), RelationType.class);
                return ListIterate.collect(type.columns, c -> new LegendColumn(c.name, ((PackageableType) c.genericType.rawType).fullPath));
            }
            catch (IOException e)
            {
                HttpClient httpClient = HttpClientBuilder.getHttpClient(new BasicCookieStore());
                HttpResponse response = httpClient.execute(request);
                String content = EntityUtils.toString(response.getEntity());
                throw new RuntimeException("Error parsing: " + content, e);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LegendExecutionResult executeQuery(String query, String database, String options)
    {
        try
        {
            HttpPost request = new HttpPost(protocol + "://" + host + ":" + port + "/api/pure/v1/execution/execute");

            // Input --------------------------------
            ExecuteInput input = new ExecuteInput();
            input.model = buildComposite(Lists.mutable.with(buildPointer(database), buildRuntimeModel(options)));
            input.context = new BaseExecutionContext();
            input.function = PureGrammarParser.newInstance().parseLambda("#SQL{" + query + "}#->from(sqlServer::dynamically::added::runtime::Runtime)");
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            request.setEntity(bodyEntity);
            // Input --------------------------------

            try
            {
                HttpClient httpClient = HttpClientBuilder.getHttpClient(new BasicCookieStore());
                HttpResponse response = httpClient.execute(request);
                return new LegendExecutionResultFromTds(new LegendTdsResultParser(response.getEntity().getContent()));
            }
            catch (Exception ex)
            {
                HttpClient httpClient = HttpClientBuilder.getHttpClient(new BasicCookieStore());
                HttpResponse response = httpClient.execute(request);
                String content = EntityUtils.toString(response.getEntity());
                throw new RuntimeException("Error parsing: " + content, ex);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean supports(String database)
    {
        return database.startsWith("projects|");
    }

    private PureModelContextCombination buildComposite(MutableList<PureModelContext> contexts)
    {
        PureModelContextCombination combination = new PureModelContextCombination();
        combination.contexts = contexts;
        return combination;
    }

    private PureModelContextData buildRuntimeModel(String options)
    {
        options = options.trim();
        if (options.startsWith("'") && options.endsWith("'"))
        {
            options = options.substring(1, options.length() - 1);
        }
        // Split by space, knowing space can be escaped by \ and \ by \\
        String[] parameters = options.split("(?<!\\\\)(?:\\\\\\\\)* ");

        ComputeState computeState = new ComputeState();
        for (int i = 0; i < parameters.length; i++)
        {
            process(parameters[i], computeState);
        }

        if (computeState.compute == Compute.TEST)
        {
            return PureGrammarParser.newInstance().parseModel(
                    "###Runtime\n" +
                            "Runtime sqlServer::dynamically::added::runtime::Runtime\n" +
                            "{\n" +
                            "    mappings : [];\n" +
                            "    connections:\n" +
                            "    [\n" +
                            "        sqlServer::dynamically::added::database::DummyDB : [connection: sqlServer::dynamically::added::connection::Connection]\n" +
                            "    ];\n" +
                            "}\n" +
                            "\n" +
                            "###Connection\n" +
                            "RelationalDatabaseConnection sqlServer::dynamically::added::connection::Connection\n" +
                            "{\n" +
                            "    store: sqlServer::dynamically::added::database::DummyDB;\n" +
                            "    specification: LocalH2{};\n" +
                            "    type: H2;\n" +
                            "    auth: DefaultH2;\n" +
                            "}\n" +
                            "###Relational\n" +
                            "Database sqlServer::dynamically::added::database::DummyDB" +
                            "(" +
                            ")");
        }
        else if (computeState.compute == Compute.SNOWFLAKE)
        {
            return PureGrammarParser.newInstance().parseModel(
                    "###LakehouseRuntime\n" +
                            "LakehouseRuntime sqlServer::dynamically::added::runtime::Runtime\n" +
                            "{\n" +
                            "    environment : '" + computeState.environment + "';\n" +
                            "    warehouse: '" + computeState.warehouse + " ';\n" +
                            "}\n");
        }
        else
        {
            throw new RuntimeException(String.format("Unknown platform: %s", computeState.compute));
        }
    }

    private PureModelContext buildPointer(String _database)
    {
        String database = _database.trim();
        if (!database.startsWith("projects|"))
        {
            throw new RuntimeException("database must start with 'projects|'");
        }
        String projectInfo = database.substring("projects|".length()).trim();
        String[] projects = projectInfo.split(",");
        MutableList<PureModelContextPointer> pointers = ArrayIterate.collect(projects, project ->
        {
            String[] proj = project.trim().split(":");
            if (proj.length != 3)
            {
                throw new RuntimeException("projects must be of the form group:artifact:version,group:artifact:version,... Provided info:\"" + project + "\"");
            }
            PureModelContextPointer pointer = new PureModelContextPointer();
            AlloySDLC alloySDLC = new AlloySDLC();
            pointer.sdlcInfo = alloySDLC;
            alloySDLC.baseVersion = "latest";
            alloySDLC.groupId = proj[0].trim();
            alloySDLC.artifactId = proj[1].trim();
            alloySDLC.version = proj[2].trim();
            return pointer;
        });

        if (pointers.size() == 1)
        {
            return pointers.get(0);
        }
        else
        {
            PureModelContextCombination combination = new PureModelContextCombination();
            combination.contexts = pointers;
            return combination;
        }
    }

    private static void process(String parameter, ComputeState state)
    {
        if (parameter.startsWith("--compute="))
        {
            state.compute = Compute.valueOf(parameter.substring("--compute=".length()).toUpperCase());
        }
        else if (parameter.startsWith("--environment="))
        {
            state.environment = parameter.substring("--environment=".length());
        }
        else if (parameter.startsWith("--warehouse="))
        {
            state.warehouse = parameter.substring("--warehouse=".length());
        }
        else if (parameter.startsWith("--workspace="))
        {
            state.workspace = parameter.substring("--workspace=".length());
        }
        else
        {
            throw new RuntimeException(String.format("Unknown parameter: %s", parameter));
        }
    }
}
