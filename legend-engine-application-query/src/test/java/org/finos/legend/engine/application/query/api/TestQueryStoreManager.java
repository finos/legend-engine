// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.application.query.api;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.utils.TestMongoClientProvider;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class TestQueryStoreManager
{
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    private TestMongoClientProvider testMongoClientProvider = new TestMongoClientProvider();
    private final QueryStoreManager queryStoreManager = new QueryStoreManager(testMongoClientProvider.mongoClient);
    // NOTE: Vault is static, so perhaps we'd better mock it instead of creating a test vault like this
    private static final TestVaultImplementation testVaultImplementation = new TestVaultImplementation();

    @BeforeClass
    public static void setupClass()
    {
        testVaultImplementation.setValue("query.mongo.database", "test");
        testVaultImplementation.setValue("query.mongo.collection", "query");
        Vault.INSTANCE.registerImplementation(testVaultImplementation);
    }

    @AfterClass
    public static void cleanUpClass()
    {
        Vault.INSTANCE.unregisterImplementation(testVaultImplementation);
    }

    @Before
    public void setup()
    {
        this.testMongoClientProvider = new TestMongoClientProvider();
    }

    @After
    public void cleanUp()
    {
        this.testMongoClientProvider.cleanUp();
    }

    private static Query createTestQuery(String id, String name, String owner)
    {
        Query query = new Query();
        query.id = id;
        query.name = name;
        query.owner = owner;
        return query;
    }

    @Test
    public void testGetLightQueries() throws Exception
    {
        String currentUser = "testUser";
        Query fullQuery = createTestQuery("1", "query1", currentUser);
        fullQuery.content = "some content";
        queryStoreManager.createQuery(fullQuery, currentUser);
        List<Query> queries = queryStoreManager.getQueries(null, null, false, currentUser);
        Assert.assertEquals(1, queries.size());
        Assert.assertEquals("{" +
            "\"artifactId\":null," +
            "\"content\":null," +
            "\"groupId\":null," +
            "\"id\":\"1\"," +
            "\"mapping\":null," +
            "\"name\":\"query1\"," +
            "\"owner\":null," +
            "\"projectId\":null," +
            "\"runtime\":null," +
            "\"versionId\":null" +
            "}", objectMapper.writeValueAsString(queries.get(0)));
    }

    @Test
    public void testGetQueriesWithLimit() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        Assert.assertEquals(1, queryStoreManager.getQueries(null, 1, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(null, null, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(null, 0, false, currentUser).size());
    }

    @Test
    public void testGetQueriesWithSearchText() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("3", "query2", currentUser), currentUser);
        Assert.assertEquals(3, queryStoreManager.getQueries(null, null, false, currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries("query1", null, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries("query2", null, false, currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries("query", null, false, currentUser).size());
    }

    @Test
    public void testGetQueriesForCurrentUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), "testUser1");
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        Assert.assertEquals(2, queryStoreManager.getQueries(null, null, false, currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries(null, null, true, currentUser).size());
    }

    @Test
    public void testGetNotFoundQuery()
    {
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.getQuery("1"));
    }

    @Test
    public void testCreateSimpleQuery() throws Exception
    {
        String currentUser = "testUser";
        Query newQuery = createTestQuery("1", "query1", currentUser);
        newQuery.projectId = "projectId";
        newQuery.groupId = "groupId";
        newQuery.artifactId = "artifactId";
        newQuery.versionId = "versionId";
        newQuery.mapping = "mapping";
        newQuery.runtime = "runtime";
        newQuery.content = "content";
        queryStoreManager.createQuery(newQuery, currentUser);
        List<Query> queries = queryStoreManager.getQueries(null, null, false, currentUser);
        Assert.assertEquals(1, queries.size());
        Assert.assertEquals("{" +
            "\"artifactId\":\"artifactId\"," +
            "\"content\":\"content\"," +
            "\"groupId\":\"groupId\"," +
            "\"id\":\"1\"," +
            "\"mapping\":\"mapping\"," +
            "\"name\":\"query1\"," +
            "\"owner\":\"" + currentUser + "\"," +
            "\"projectId\":\"projectId\"," +
            "\"runtime\":\"runtime\"," +
            "\"versionId\":\"versionId\"" +
            "}", objectMapper.writeValueAsString(queryStoreManager.getQuery("1")));
    }

    @Test
    public void testCreateQueryWithSameId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser));
    }

    @Test
    public void testForceCurrentUserToBeOwnerWhenCreatingQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
        queryStoreManager.createQuery(createTestQuery("2", "query2", "testUser2"), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("2").owner);
        queryStoreManager.createQuery(createTestQuery("3", "query1", "testUser2"), null);
        Assert.assertNull(queryStoreManager.getQuery("3").owner);
    }

    @Test
    public void testUpdateQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.updateQuery("1", createTestQuery("1", "query2", currentUser), currentUser);
        Assert.assertEquals("query2", queryStoreManager.getQuery("1").name);
    }

    @Test
    public void testPreventUpdateQueryId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("2", "query1", "testUser2"), currentUser));
    }

    @Test
    public void testUpdateNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("1", "query1", currentUser), currentUser));
    }

    @Test
    public void testAllowUpdateQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        queryStoreManager.updateQuery("1", createTestQuery("1", "query2", null), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
    }

    @Test
    public void testForbidUpdateQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("1", "query1", "testUser2"), "testUser2"));
    }

    @Test
    public void testDeleteQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(null, null, false, currentUser).size());
    }

    @Test
    public void testDeleteNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.deleteQuery("1", currentUser));
    }

    @Test
    public void testAllowDeleteQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(null, null, false, currentUser).size());
    }

    @Test
    public void testForbidDeleteQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.deleteQuery("1", "testUser2"));
    }
}
