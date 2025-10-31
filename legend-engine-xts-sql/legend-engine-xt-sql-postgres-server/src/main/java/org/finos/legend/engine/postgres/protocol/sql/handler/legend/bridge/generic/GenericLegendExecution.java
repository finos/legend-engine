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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;

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
            HttpPost request = new HttpPost(protocol + "://" + host + ":" + port + "/api/pure/v1/compilation/lambdaRelationType");
            LambdaReturnTypeInput input = new LambdaReturnTypeInput();
            input.model = buildPointer(database);
            input.lambda = PureGrammarParser.newInstance().parseLambda("#SQL{" + query + "}#->from(test::test)");
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            request.setEntity(bodyEntity);

            try (CloseableHttpClient httpClient = HttpClients.createDefault())
            {
                try (CloseableHttpResponse response = httpClient.execute(request))
                {
                    RelationType type = MAPPER.readValue(response.getEntity().getContent(), RelationType.class);
                    return ListIterate.collect(type.columns, c -> new LegendColumn(c.name, ((PackageableType) c.genericType.rawType).fullPath));
                }
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LegendExecutionResult executeQuery(String query, String database)
    {
        try
        {
            HttpPost request = new HttpPost(protocol + "://" + host + ":" + port + "/api/pure/v1/execution/execute");

            // Input --------------------------------
            ExecuteInput input = new ExecuteInput();
            input.model = buildPointer(database);
            input.context = new BaseExecutionContext();
            input.function = PureGrammarParser.newInstance().parseLambda("#SQL{" + query + "}#->from(test::test)");
            StringEntity bodyEntity = new StringEntity(MAPPER.writeValueAsString(input));
            bodyEntity.setContentType("application/json");
            request.setEntity(bodyEntity);
            // Input --------------------------------

            try (CloseableHttpClient httpClient = HttpClients.createDefault())
            {
                try (CloseableHttpResponse response = httpClient.execute(request))
                {
                    return new LegendExecutionResultFromTds(new LegendTdsResultParser(response.getEntity().getContent()));
                }
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(String database)
    {
        return database.startsWith("projects|");
    }

    private PureModelContext buildPointer(String database)
    {
        if (!database.startsWith("projects|"))
        {
            throw new RuntimeException("database must start with 'projects|'");
        }
        String projectInfo = database.substring("projects|".length());
        String[] projects = projectInfo.split(",");
        MutableList<PureModelContextPointer> pointers = ArrayIterate.collect(projects, project ->
        {
            String[] proj = project.split(":");
            if (proj.length != 3)
            {
                throw new RuntimeException("projects must be of the form group:artifact:version. Provided info:\"" + project + "\"");
            }
            PureModelContextPointer pointer = new PureModelContextPointer();
            AlloySDLC alloySDLC = new AlloySDLC();
            pointer.sdlcInfo = alloySDLC;
            alloySDLC.baseVersion = "latest";
            alloySDLC.groupId = proj[0];
            alloySDLC.artifactId = proj[1];
            alloySDLC.version = proj[2];
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
