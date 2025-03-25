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

package org.finos.legend.engine.application.query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryDataProductExecutionContext;
import org.finos.legend.engine.application.query.model.QueryExplicitExecutionContext;
import org.junit.Assert;
import org.junit.Test;

public class TestQuerySerialization
{

    final ObjectMapper objectMapper = new ObjectMapper();

    String LEGACY_QUERY = "{\n" +
            "  \"_id\": 1242837498375,\n" +
            "  \"artifactId\": \"test-artifact\",\n" +
            "  \"content\": \"content\",\n" +
            "  \"createdAt\": null,\n" +
            "  \"defaultParameterValues\": [],\n" +
            "  \"description\": \"description\",\n" +
            "  \"executionContext\": null,\n" +
            "  \"gridConfig\": null,\n" +
            "  \"groupId\": \"test.group\",\n" +
            "  \"id\": \"1\",\n" +
            "  \"lastUpdatedAt\": null,\n" +
            "  \"mapping\": \"my::mapping\",\n" +
            "  \"name\": \"query1\",\n" +
            "  \"originalVersionId\": \"0.0.0\",\n" +
            "  \"owner\": \"testUser\",\n" +
            "  \"runtime\": \"my::runtime\",\n" +
            "  \"stereotypes\": [],\n" +
            "  \"taggedValues\": [],\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";


    String EXPLICIT_EX_QUERY = "{\n" +
            "  \"artifactId\": \"test-artifact\",\n" +
            "  \"content\": \"content\",\n" +
            "  \"createdAt\": 1713280515492,\n" +
            "  \"defaultParameterValues\": [],\n" +
            "  \"description\": \"description\",\n" +
            "  \"gridConfig\": null,\n" +
            "  \"groupId\": \"test.group\",\n" +
            "  \"id\": \"1\",\n" +
            "  \"lastUpdatedAt\": 1713280515492,\n" +
            "  \"name\": \"query1\",\n" +
            "  \"originalVersionId\": \"0.0.0\",\n" +
            "  \"owner\": \"testUser\",\n" +
            "  \"executionContext\": \n" +
            "    {\n" +
            "      \"_type\": \"explicitExecutionContext\",\n" +
            "      \"mapping\": \"my::mapping\",\n" +
            "      \"runtime\": \"my::runtime\"\n" +
            "    }\n" +
            "  ,\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";


    String DATA_SPACE_QUERY = "{\n" +
            "  \"artifactId\": \"test-artifact\",\n" +
            "  \"content\": \"content\",\n" +
            "  \"createdAt\": 1713280515492,\n" +
            "  \"defaultParameterValues\": [],\n" +
            "  \"description\": \"description\",\n" +
            "  \"gridConfig\": null,\n" +
            "  \"groupId\": \"test.group\",\n" +
            "  \"id\": \"1\",\n" +
            "  \"lastUpdatedAt\": 1713280515492,\n" +
            "  \"name\": \"query1\",\n" +
            "  \"originalVersionId\": \"0.0.0\",\n" +
            "  \"owner\": \"testUser\",\n" +
            "  \"executionContext\": \n" +
            "    {\n" +
            "      \"_type\": \"dataSpaceExecutionContext\",\n" +
            "      \"dataSpacePath\": \"my::dataSpace\",\n" +
            "      \"executionKey\": \"myKey\"\n" +
            "    }\n" +
            "  ,\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";

    String DATA_SPACE_QUERY_NO_KEY = "{\n" +
            "  \"artifactId\": \"test-artifact\",\n" +
            "  \"content\": \"content\",\n" +
            "  \"createdAt\": 1713280515492,\n" +
            "  \"defaultParameterValues\": [],\n" +
            "  \"description\": \"description\",\n" +
            "  \"gridConfig\": null,\n" +
            "  \"groupId\": \"test.group\",\n" +
            "  \"id\": \"1\",\n" +
            "  \"lastUpdatedAt\": 1713280515492,\n" +
            "  \"name\": \"query1\",\n" +
            "  \"originalVersionId\": \"0.0.0\",\n" +
            "  \"owner\": \"testUser\",\n" +
            "  \"executionContext\": \n" +
            "    {\n" +
            "      \"_type\": \"dataSpaceExecutionContext\",\n" +
            "      \"dataSpacePath\": \"my::dataSpace\"\n" +
            "    }\n" +
            "  ,\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";

    @Test
    public void testDeserialization() throws Exception
    {
        Query legacyQuery = objectMapper.readValue(LEGACY_QUERY, Query.class);
        Assert.assertNull(legacyQuery.executionContext);
        Assert.assertEquals(legacyQuery.mapping, "my::mapping");
        Assert.assertEquals(legacyQuery.runtime, "my::runtime");
        Query explicitExecutionQuery = objectMapper.readValue(EXPLICIT_EX_QUERY, Query.class);
        Assert.assertNull(explicitExecutionQuery.runtime);
        Assert.assertNull(explicitExecutionQuery.mapping);
        Assert.assertTrue(explicitExecutionQuery.executionContext instanceof QueryExplicitExecutionContext);
        Query dataSpaceQuery = objectMapper.readValue(DATA_SPACE_QUERY, Query.class);
        Assert.assertTrue(dataSpaceQuery.executionContext instanceof QueryDataProductExecutionContext);
        QueryDataProductExecutionContext dataSpaceExecutionContext = (QueryDataProductExecutionContext) dataSpaceQuery.executionContext;
        Assert.assertEquals(dataSpaceExecutionContext.dataSpacePath, "my::dataSpace");
        Assert.assertEquals(dataSpaceExecutionContext.executionKey, "myKey");
        Query dataSpaceQuery2 = objectMapper.readValue(DATA_SPACE_QUERY_NO_KEY, Query.class);
        Assert.assertNull(((QueryDataProductExecutionContext)dataSpaceQuery2.executionContext).executionKey);
    }
}
