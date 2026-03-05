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
import org.finos.legend.engine.application.query.model.DataProductModelAccessExecutionContext;
import org.finos.legend.engine.application.query.model.DataProductNativeExecutionContext;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryDataSpaceExecutionContext;
import org.finos.legend.engine.application.query.model.QueryExplicitExecutionContext;
import org.junit.Assert;
import org.junit.Test;

public class TestQuerySerialization
{

    final ObjectMapper objectMapper = new ObjectMapper();

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

    String DATA_PRODUCT_NATIVE_QUERY = "{\n" +
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
            "      \"_type\": \"dataProductNativeExecutionContext\",\n" +
            "      \"dataProductPath\": \"my::dataProduct\",\n" +
            "      \"executionKey\": \"nativeKey\"\n" +
            "    }\n" +
            "  ,\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";

    String DATA_PRODUCT_MODEL_ACCESS_QUERY = "{\n" +
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
            "      \"_type\": \"dataProductModelAccessExecutionContext\",\n" +
            "      \"dataProductPath\": \"my::dataProduct\",\n" +
            "      \"accessPointGroupId\": \"group1\"\n" +
            "    }\n" +
            "  ,\n" +
            "  \"versionId\": \"0.0.0\"\n" +
            "}";

    @Test
    public void testDeserialization() throws Exception
    {
        Query explicitExecutionQuery = objectMapper.readValue(EXPLICIT_EX_QUERY, Query.class);
        Assert.assertTrue(explicitExecutionQuery.executionContext instanceof QueryExplicitExecutionContext);
        Query dataSpaceQuery = objectMapper.readValue(DATA_SPACE_QUERY, Query.class);
        Assert.assertTrue(dataSpaceQuery.executionContext instanceof QueryDataSpaceExecutionContext);
        QueryDataSpaceExecutionContext dataSpaceExecutionContext = (QueryDataSpaceExecutionContext) dataSpaceQuery.executionContext;
        Assert.assertEquals(dataSpaceExecutionContext.dataSpacePath, "my::dataSpace");
        Assert.assertEquals(dataSpaceExecutionContext.executionKey, "myKey");
        Query dataSpaceQuery2 = objectMapper.readValue(DATA_SPACE_QUERY_NO_KEY, Query.class);
        Assert.assertNull(((QueryDataSpaceExecutionContext)dataSpaceQuery2.executionContext).executionKey);
    }

    @Test
    public void testDataProductNativeDeserialization() throws Exception
    {
        Query query = objectMapper.readValue(DATA_PRODUCT_NATIVE_QUERY, Query.class);
        Assert.assertTrue(query.executionContext instanceof DataProductNativeExecutionContext);
        DataProductNativeExecutionContext ctx = (DataProductNativeExecutionContext) query.executionContext;
        Assert.assertEquals("my::dataProduct", ctx.dataProductPath);
        Assert.assertEquals("nativeKey", ctx.executionKey);
    }

    @Test
    public void testDataProductModelAccessDeserialization() throws Exception
    {
        Query query = objectMapper.readValue(DATA_PRODUCT_MODEL_ACCESS_QUERY, Query.class);
        Assert.assertTrue(query.executionContext instanceof DataProductModelAccessExecutionContext);
        DataProductModelAccessExecutionContext ctx = (DataProductModelAccessExecutionContext) query.executionContext;
        Assert.assertEquals("my::dataProduct", ctx.dataProductPath);
        Assert.assertEquals("group1", ctx.accessPointGroupId);
    }

    @Test
    public void testDataProductNativeSerialization() throws Exception
    {
        DataProductNativeExecutionContext ctx = new DataProductNativeExecutionContext();
        ctx.dataProductPath = "my::dataProduct";
        ctx.executionKey = "nativeKey";
        String json = objectMapper.writeValueAsString(ctx);
        Assert.assertTrue(json.contains("\"dataProductPath\":\"my::dataProduct\""));
        Assert.assertTrue(json.contains("\"executionKey\":\"nativeKey\""));
        Assert.assertTrue(json.contains("\"_type\":\"dataProductNativeExecutionContext\""));
    }

    @Test
    public void testDataProductModelAccessSerialization() throws Exception
    {
        DataProductModelAccessExecutionContext ctx = new DataProductModelAccessExecutionContext();
        ctx.dataProductPath = "my::dataProduct";
        ctx.accessPointGroupId = "group1";
        String json = objectMapper.writeValueAsString(ctx);
        Assert.assertTrue(json.contains("\"dataProductPath\":\"my::dataProduct\""));
        Assert.assertTrue(json.contains("\"accessPointGroupId\":\"group1\""));
        Assert.assertTrue(json.contains("\"_type\":\"dataProductModelAccessExecutionContext\""));
    }
}
