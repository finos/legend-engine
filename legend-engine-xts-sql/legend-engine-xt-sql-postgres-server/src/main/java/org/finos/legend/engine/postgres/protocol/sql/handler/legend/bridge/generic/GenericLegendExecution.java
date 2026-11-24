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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.api.C3LinearizationInput;
import org.finos.legend.engine.language.pure.compiler.api.C3LinearizationReturn;
import org.finos.legend.engine.language.pure.compiler.api.LambdaReturnTypeInput;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendColumn;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecutionResult;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendExecutionResultFromTds;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendTdsResultParser;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.relation.RelationType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCombination;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.query.sql.api.schema.SchemaResult;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public class GenericLegendExecution implements LegendExecution
{
    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Pattern ELEMENT_PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(::[A-Za-z_][A-Za-z0-9_]*)*");

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
            // Relation --------------------------------
            LambdaReturnTypeInput input = new LambdaReturnTypeInput();
            input.model = buildPointer(database);
            input.lambda = parseLambda("#SQL{" + query + "}#");
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            RelationType type = callServer(bodyEntity, "pure/v1/compilation/lambdaRelationType", RelationType.class);
            // Relation --------------------------------

            C3LinearizationReturn c3Linearizations = getC3LinearizationReturn(ListIterate.collect(type.columns, c -> ((PackageableType) c.genericType.rawType).fullPath));

            return LazyIterate
                    .zip(type.columns, c3Linearizations.typeInheritances)
                    .collect(c -> new LegendColumn(c.getOne().name, c.getTwo().type, c.getTwo().linearizedInheritance))
                    .toList();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SchemaResult getProjectSchema(String database)
    {
        try
        {
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(buildPointer(database)));
            bodyEntity.setContentType("application/json");
            return callServer(bodyEntity, "sql/v1/schema/getSchema", SchemaResult.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("The project schema can't be fetched for the database '" + database + "'", e);
        }
    }

    @Override
    public LegendExecutionResult executeQuery(String query, String database, String options)
    {
        try
        {
            // Execute Input --------------------------
            ComputeState computeState = parseOptions(options);
            ExecuteInput input = new ExecuteInput();
            input.context = new BaseExecutionContext();
            if (computeState.runtime != null)
            {
                input.model = buildPointer(database);
                String fromArguments = computeState.mapping == null ? computeState.runtime : computeState.mapping + ", " + computeState.runtime;
                input.function = parseLambda("#SQL{" + query + "}#->from(" + fromArguments + ")");
            }
            else
            {
                input.model = buildComposite(Lists.mutable.with(buildPointer(database), buildRuntimeModel(computeState)));
                input.function = parseLambda("#SQL{" + query + "}#->from(sqlServer::dynamically::added::runtime::Runtime)");
            }
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            // Execute Input --------------------------

            return callServer(bodyEntity, "pure/v1/execution/execute", x ->
            {
                try
                {
                    LegendExecutionResultFromTds res = new LegendExecutionResultFromTds(new LegendTdsResultParser(x));
                    // Fill linearization for columns returned in the resultset
                    C3LinearizationReturn c3Linearizations = getC3LinearizationReturn(ListIterate.collect(res.getLegendColumns(), LegendColumn::getType));
                    LazyIterate
                            .zip(res.getLegendColumns(), c3Linearizations.typeInheritances)
                            .forEach(c -> c.getOne().setLinearizedInheritances(c.getTwo().linearizedInheritance));
                    return res;
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });
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

    private C3LinearizationReturn getC3LinearizationReturn(MutableList<String> types)
    {
        try
        {
            // C3Linearization--------------------------
            C3LinearizationInput c3Input = new C3LinearizationInput();
            c3Input.fullPathTypes = types;
            c3Input.model = PureModelContextData.newPureModelContextData();
            StringEntity c3BodyEntity = new StringEntity(MAPPER.writeValueAsString(c3Input));
            c3BodyEntity.setContentType("application/json");
            return callServer(c3BodyEntity, "pure/v1/compilation/c3Linearization", C3LinearizationReturn.class);
            // C3Linearization--------------------------
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextCombination buildComposite(MutableList<PureModelContext> contexts)
    {
        PureModelContextCombination combination = new PureModelContextCombination();
        combination.contexts = contexts;
        return combination;
    }

    private static ComputeState parseOptions(String options)
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

        if (computeState.runtime != null && computeState.compute != null)
        {
            throw new RuntimeException("The options --runtime and --compute can't be combined: --runtime executes on a runtime defined in the project, --compute on a runtime built for the connection");
        }
        if (computeState.mapping != null && computeState.runtime == null)
        {
            throw new RuntimeException("The option --mapping requires --runtime");
        }
        return computeState;
    }

    private PureModelContextData buildRuntimeModel(ComputeState computeState) throws Exception
    {
        if (computeState.compute == Compute.TEST)
        {
            return parseModel(
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
            return parseModel(
                    "###LakehouseRuntime\n" +
                            "LakehouseRuntime sqlServer::dynamically::added::runtime::Runtime\n" +
                            "{\n" +
                            "    environment : '" + computeState.environment + "';\n" +
                            "    warehouse: '" + computeState.warehouse + "';\n" +
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

        if ("projects|test".equals(database))
        {
            return buildTestPayload();
        }
        else
        {
            String projectInfo = database.substring("projects|".length()).trim();
            String[] projects = projectInfo.split("\\|");
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
    }

    private PureModelContextText buildTestPayload()
    {
        PureModelContextText pureModelContextText = new PureModelContextText();
        pureModelContextText.code = "###Relational\n" +
                "Database pack::myDatabase (Table myTable(myColumn VARCHAR(200)))";
        return pureModelContextText;
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
        else if (parameter.startsWith("--runtime="))
        {
            state.runtime = elementPath("runtime", parameter.substring("--runtime=".length()));
        }
        else if (parameter.startsWith("--mapping="))
        {
            state.mapping = elementPath("mapping", parameter.substring("--mapping=".length()));
        }
        else
        {
            throw new RuntimeException(String.format("Unknown parameter: %s", parameter));
        }
    }

    private static String elementPath(String option, String path)
    {
        // The path is spliced into the executed lambda, so nothing but a qualified element path may pass
        if (!ELEMENT_PATH.matcher(path).matches())
        {
            throw new RuntimeException(String.format("Invalid %s path: '%s'. Expected a qualified element path, e.g. my::package::MyElement", option, path));
        }
        return path;
    }

    private LambdaFunction parseLambda(String lambda) throws Exception
    {
        return callServer(new StringEntity(lambda), "pure/v1/grammar/grammarToJson/lambda", LambdaFunction.class);
    }

    private PureModelContextData parseModel(String model) throws Exception
    {
        return callServer(new StringEntity(model), "pure/v1/grammar/grammarToJson/model", PureModelContextData.class);
    }

    private <T> T callServer(StringEntity bodyEntity, String url, Class<T> c) throws Exception
    {
        return callServer(bodyEntity, url, x ->
        {
            String body = null;
            try
            {
                body = new String(x.readAllBytes(), StandardCharsets.UTF_8);
                return MAPPER.readValue(body, c);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unexpected response body: " + body, e);
            }
        });
    }

    private <T> T callServer(StringEntity bodyEntity, String url, Function<InputStream, T> transformer) throws Exception
    {
        HttpPost request = new HttpPost(protocol + "://" + host + ":" + port + "/api/" + url);
        request.setEntity(bodyEntity);
        HttpClient httpClient = HttpClientBuilder.getHttpClient(new BasicCookieStore());
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode >= 300)
        {
            throw new RuntimeException("Error calling the engine (url=" + url + ", status=" + statusCode + "): " + engineErrorMessage(EntityUtils.toString(response.getEntity())));
        }
        try
        {
            return transformer.apply(response.getEntity().getContent());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing (url=" + url + ")", e);
        }
    }

    private static String engineErrorMessage(String responseBody)
    {
        try
        {
            JsonNode message = MAPPER.readTree(responseBody).get("message");
            return message == null ? responseBody : message.asText();
        }
        catch (Exception e)
        {
            return responseBody;
        }
    }

}
