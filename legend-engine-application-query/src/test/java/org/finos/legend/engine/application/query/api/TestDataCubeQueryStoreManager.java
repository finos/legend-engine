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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.application.query.model.*;
import org.finos.legend.engine.application.query.utils.TestMongoClientProvider;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestDataCubeQueryStoreManager
{
    static class TestQuerySearchSpecificationBuilder
    {
        public QuerySearchTermSpecification searchTermSpecification;
        public Integer limit;
        public Boolean showCurrentUserQueriesOnly;
        public QuerySearchSortBy sortByOption;

        TestQuerySearchSpecificationBuilder withSearchTerm(String searchTerm)
        {
            if (this.searchTermSpecification == null)
            {
                this.searchTermSpecification = new QuerySearchTermSpecification();
            }
            this.searchTermSpecification.searchTerm = searchTerm;
            return this;
        }

        TestQuerySearchSpecificationBuilder withLimit(Integer limit)
        {
            this.limit = limit;
            return this;
        }

        TestQuerySearchSpecificationBuilder withShowCurrentUserQueriesOnly(Boolean showCurrentUserQueriesOnly)
        {
            this.showCurrentUserQueriesOnly = showCurrentUserQueriesOnly;
            return this;
        }

        TestQuerySearchSpecificationBuilder withExactNameSearch(Boolean exactMatchName)
        {
            if (this.searchTermSpecification == null)
            {
                this.searchTermSpecification = new QuerySearchTermSpecification();
            }
            this.searchTermSpecification.exactMatchName = exactMatchName;
            return this;
        }

        TestQuerySearchSpecificationBuilder withIncludeOwner(Boolean includeOwner)
        {
            if (this.searchTermSpecification == null)
            {
                this.searchTermSpecification = new QuerySearchTermSpecification();
            }
            this.searchTermSpecification.includeOwner = includeOwner;
            return this;
        }

        TestQuerySearchSpecificationBuilder withSortByOption(QuerySearchSortBy sortByOption)
        {
            this.sortByOption = sortByOption;
            return this;
        }

        QuerySearchSpecification build()
        {
            QuerySearchSpecification searchSpecification = new QuerySearchSpecification();
            searchSpecification.searchTermSpecification = this.searchTermSpecification;
            searchSpecification.limit = this.limit;
            searchSpecification.showCurrentUserQueriesOnly = this.showCurrentUserQueriesOnly;
            searchSpecification.sortByOption = this.sortByOption;
            return searchSpecification;
        }
    }

    static class TestQueryBuilder
    {
        public String id;
        public String name;
        public String owner;
        public String description = "description";
        public Map<String, Object> content = Maps.mutable.empty();

        static TestQueryBuilder create(String id, String name, String owner)
        {
            TestQueryBuilder queryBuilder = new TestQueryBuilder();
            queryBuilder.id = id;
            queryBuilder.name = name;
            queryBuilder.owner = owner;
            return queryBuilder;
        }

        DataCubeQuery build()
        {
            DataCubeQuery query = new DataCubeQuery();
            query.id = this.id;
            query.name = this.name;
            query.description = this.description;
            query.content = this.content;
            query.owner = this.owner;
            return query;
        }
    }

    private TestMongoClientProvider testMongoClientProvider = new TestMongoClientProvider();
    private final DataCubeQueryStoreManager store = new DataCubeQueryStoreManager(testMongoClientProvider.mongoClient);
    private static final TestVaultImplementation testVaultImplementation = new TestVaultImplementation();

    @BeforeClass
    public static void setupClass()
    {
        testVaultImplementation.setValue("query.mongo.database", "test");
        testVaultImplementation.setValue("query.mongo.collection.dataCube", "dataCube");
        testVaultImplementation.setValue("query.mongo.collection.dataCubeEvent", "dataCube-event");
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

    @Test
    public void testValidateQuery()
    {
        String currentUser = "testUser";
        Function0<DataCubeQuery> _createTestQuery = () -> TestQueryBuilder.create("1", "query1", currentUser).build();
        DataCubeQuery goodQuery = _createTestQuery.get();
        DataCubeQueryStoreManager.validateQuery(goodQuery);

        // ID
        DataCubeQuery queryWithInvalidId = _createTestQuery.get();
        queryWithInvalidId.id = null;
        Assert.assertEquals("Query ID is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> DataCubeQueryStoreManager.validateQuery(queryWithInvalidId)).getMessage());
        queryWithInvalidId.id = "";
        Assert.assertEquals("Query ID is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> DataCubeQueryStoreManager.validateQuery(queryWithInvalidId)).getMessage());

        // Name
        DataCubeQuery queryWithInvalidName = _createTestQuery.get();
        queryWithInvalidName.name = null;
        Assert.assertEquals("Query name is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> DataCubeQueryStoreManager.validateQuery(queryWithInvalidName)).getMessage());
        queryWithInvalidId.name = "";
        Assert.assertEquals("Query name is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> DataCubeQueryStoreManager.validateQuery(queryWithInvalidName)).getMessage());

        // Content
        DataCubeQuery queryWithInvalidContent = _createTestQuery.get();
        queryWithInvalidContent.content = null;
        Assert.assertEquals("Query content is missing", Assert.assertThrows(ApplicationQueryException.class, () -> DataCubeQueryStoreManager.validateQuery(queryWithInvalidContent)).getMessage());
    }

    @Test
    public void testSearchQueries() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery newQuery = TestQueryBuilder.create("1", "query1", currentUser).build();
        store.createQuery(newQuery, currentUser);
        List<DataCubeQuery> queries = store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser);
        Assert.assertEquals(1, queries.size());
        DataCubeQuery lightQuery = queries.get(0);
        Assert.assertEquals("1", lightQuery.id);
        Assert.assertEquals("query1", lightQuery.name);
        Assert.assertNotNull(lightQuery.createdAt);
        Assert.assertNotNull(lightQuery.lastUpdatedAt);
        Assert.assertNull(lightQuery.content);
        Assert.assertNull(lightQuery.description);
    }

    @Test
    public void testMatchExactNameQuery() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery newQuery = TestQueryBuilder.create("1", "Test Query 1", currentUser).build();
        DataCubeQuery newQueryTwo = TestQueryBuilder.create("2", "Test Query 12", currentUser).build();
        DataCubeQuery newQueryThree = TestQueryBuilder.create("3", "Test Query 13", currentUser).build();
        store.createQuery(newQuery, currentUser);
        store.createQuery(newQueryTwo, currentUser);
        store.createQuery(newQueryThree, currentUser);
        List<DataCubeQuery> queriesGeneralSearch = store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("Test Query 1").build(), currentUser);
        Assert.assertEquals(3, queriesGeneralSearch.size());
        List<DataCubeQuery> queriesExactSearch = store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("Test Query 1").withExactNameSearch(true).build(), currentUser);
        Assert.assertEquals(1, queriesExactSearch.size());
    }

    @Test
    public void testGetQueriesWithLimit() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(1, store.searchQueries(new TestQuerySearchSpecificationBuilder().withLimit(1).build(), currentUser).size());
        Assert.assertEquals(2, store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(0, store.searchQueries(new TestQuerySearchSpecificationBuilder().withLimit(0).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithSortBy() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery testQuery1 = TestQueryBuilder.create("1", "query1", currentUser).build();
        DataCubeQuery testQuery2 = TestQueryBuilder.create("2", "query2", currentUser).build();
        DataCubeQuery testQuery3 = TestQueryBuilder.create("3", "query3", currentUser).build();
        DataCubeQuery testQuery4 = TestQueryBuilder.create("4", "query4", currentUser).build();

        // create in order 1 -> 4 -> 2 -> 3
        store.createQuery(testQuery1, currentUser);
        Thread.sleep(100);
        store.createQuery(testQuery4, currentUser);
        Thread.sleep(100);
        store.createQuery(testQuery2, currentUser);
        Thread.sleep(100);
        store.createQuery(testQuery3, currentUser);

        Assert.assertEquals(4, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSortByOption(QuerySearchSortBy.SORT_BY_CREATE).build(), currentUser).size());
        Assert.assertEquals(Arrays.asList("3", "2", "4", "1"), store.searchQueries(new TestQuerySearchSpecificationBuilder().withSortByOption(QuerySearchSortBy.SORT_BY_CREATE).build(), currentUser).stream().map(q -> q.id).collect(Collectors.toList()));

        store.updateQuery("2", TestQueryBuilder.create("2", "query2NewlyUpdated", currentUser).build(), currentUser);
        Assert.assertEquals(Arrays.asList("2", "3", "4", "1"), store.searchQueries(new TestQuerySearchSpecificationBuilder().withSortByOption(QuerySearchSortBy.SORT_BY_UPDATE).build(), currentUser).stream().map(q -> q.id).collect(Collectors.toList()));

        store.getQuery("1");
        Assert.assertEquals(Arrays.asList("1", "2", "3", "4"), store.searchQueries(new TestQuerySearchSpecificationBuilder().withSortByOption(QuerySearchSortBy.SORT_BY_VIEW).build(), currentUser).stream().map(q -> q.id).collect(Collectors.toList()));
    }

    @Test
    public void testGetQueriesWithSearchText() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("3", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(3, store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(1, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query1").build(), currentUser).size());
        Assert.assertEquals(2, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query2").build(), currentUser).size());
        Assert.assertEquals(3, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query").build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithSearchTextSpec() throws Exception
    {
        String currentUser = "user1";
        String user2 = "user2";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("3", "query3", user2).build(), user2);
        Assert.assertEquals(1, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withSearchTerm("user2").withIncludeOwner(true).build(), currentUser).size());
        Assert.assertEquals(2, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withSearchTerm("user1").withIncludeOwner(true).build(), currentUser).size());
        Assert.assertEquals(3, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withSearchTerm("user").withIncludeOwner(true).build(), currentUser).size());
        Assert.assertEquals(0, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withSearchTerm("user").withExactNameSearch(true).withIncludeOwner(true).build(), currentUser).size());
        Assert.assertEquals(0, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withSearchTerm("user").withIncludeOwner(false).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesForCurrentUser() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), "testUser1");
        store.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(2, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(2, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withShowCurrentUserQueriesOnly(false).build(), currentUser).size());
        Assert.assertEquals(1, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().withShowCurrentUserQueriesOnly(true).build(), currentUser).size());
    }

    @Test
    public void testGetNotFoundQuery()
    {
        Assert.assertEquals("Can't find query with ID '1'", Assert.assertThrows(ApplicationQueryException.class, () -> store.getQuery("1")).getMessage());
    }

    @Test
    public void testCreateSimpleQuery() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery newQuery = TestQueryBuilder.create("1", "query1", currentUser).build();
        DataCubeQuery createdQuery = store.createQuery(newQuery, currentUser);
        Assert.assertEquals("1", createdQuery.id);
        Assert.assertEquals("query1", createdQuery.name);
        Assert.assertEquals("description", createdQuery.description);
        Assert.assertNotNull(createdQuery.createdAt);
        Assert.assertEquals(createdQuery.createdAt, createdQuery.lastUpdatedAt);
        Assert.assertEquals(createdQuery.lastOpenAt, createdQuery.lastUpdatedAt);
    }

    @Test
    public void testCreateInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertEquals("Query name is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> store.createQuery(TestQueryBuilder.create("1", null, currentUser).build(), currentUser)).getMessage());
    }

    @Test
    public void testCreateQueryWithSameId() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertEquals("Query with ID '1' already existed", Assert.assertThrows(ApplicationQueryException.class, () -> store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser)).getMessage());
    }

    @Test
    public void testForceCurrentUserToBeOwnerWhenCreatingQuery() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", null).build(), currentUser);
        Assert.assertEquals(currentUser, store.getQuery("1").owner);
        store.createQuery(TestQueryBuilder.create("2", "query2", "testUser2").build(), currentUser);
        Assert.assertEquals(currentUser, store.getQuery("2").owner);
        store.createQuery(TestQueryBuilder.create("3", "query1", "testUser2").build(), null);
        Assert.assertNull(store.getQuery("3").owner);
    }

    @Test
    public void testUpdateQuery() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        store.updateQuery("1", TestQueryBuilder.create("1", "query2", currentUser).build(), currentUser);
        Assert.assertEquals("query2", store.getQuery("1").name);
    }

    @Test
    public void testUpdateWithInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertEquals("Query name is missing or empty", Assert.assertThrows(ApplicationQueryException.class, () -> store.updateQuery("1", TestQueryBuilder.create("1", null, currentUser).build(), currentUser)).getMessage());
    }

    @Test
    public void testPreventUpdateQueryId() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        Assert.assertEquals("Updating query ID is not supported", Assert.assertThrows(ApplicationQueryException.class, () -> store.updateQuery("1", TestQueryBuilder.create("2", "query1", "testUser2").build(), currentUser)).getMessage());
    }

    @Test
    public void testUpdateNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> store.updateQuery("1", TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser));
    }

    @Test
    public void testAllowUpdateQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        store.updateQuery("1", TestQueryBuilder.create("1", "query2", null).build(), currentUser);
        Assert.assertEquals(currentUser, store.getQuery("1").owner);
    }

    @Test
    public void testForbidUpdateQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertEquals("Only owner can update the query", Assert.assertThrows(ApplicationQueryException.class, () -> store.updateQuery("1", TestQueryBuilder.create("1", "query1", "testUser2").build(), "testUser2")).getMessage());
    }

    @Test
    public void testDeleteQuery() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        store.deleteQuery("1", currentUser);
        Assert.assertEquals(0, store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
    }

    @Test
    public void testDeleteNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertEquals("Can't find query with ID '1'", Assert.assertThrows(ApplicationQueryException.class, () -> store.deleteQuery("1", currentUser)).getMessage());
    }

    @Test
    public void testAllowDeleteQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        store.deleteQuery("1", currentUser);
        Assert.assertEquals(0, store.searchQueries(new TestQueryStoreManager.TestQuerySearchSpecificationBuilder().build(), currentUser).size());
    }

    @Test
    public void testForbidDeleteQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertEquals("Only owner can delete the query", Assert.assertThrows(ApplicationQueryException.class, () -> store.deleteQuery("1", "testUser2")).getMessage());
    }

    @Test
    public void testCreateSimpleQueryContainsTimestamps() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery newQuery = TestQueryBuilder.create("1", "query1", currentUser).build();
        DataCubeQuery createdQuery = store.createQuery(newQuery, currentUser);
        Assert.assertNotNull(createdQuery.lastUpdatedAt);
        Assert.assertNotNull(createdQuery.createdAt);
    }

    @Test
    public void testSearchQueriesContainTimestamps() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery newQuery = TestQueryBuilder.create("1", "query1", currentUser).build();
        store.createQuery(newQuery, currentUser);
        List<DataCubeQuery> queries = store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser);
        Assert.assertEquals(1, queries.size());
        DataCubeQuery lightQuery = queries.get(0);
        Assert.assertNotNull(lightQuery.lastUpdatedAt);
        Assert.assertNotNull(lightQuery.createdAt);
    }

    @Test
    public void testSearchQueriesWithSearchByQueryId() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("26929514-237c-11ed-861d-0242ac120002", "query_a", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("26929515-237c-11bd-851d-0243ac120002", "query_b", currentUser).build(), currentUser);
        store.createQuery(TestQueryBuilder.create("23929515-235c-11ad-851d-0143ac120002", "query_c", currentUser).build(), currentUser);
        Assert.assertEquals(3, store.searchQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(1, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("23929515-235c-11ad-851d-0143ac120002").build(), currentUser).size());
        Assert.assertEquals(0, store.searchQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("23929515-235c-11ad").build(), currentUser).size());
    }

    @Test
    public void testGetQueries() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery testQuery1 = TestQueryBuilder.create("1", "query1", currentUser).build();
        DataCubeQuery testQuery2 = TestQueryBuilder.create("2", "query2", currentUser).build();
        DataCubeQuery testQuery3 = TestQueryBuilder.create("3", "query3", currentUser).build();
        store.createQuery(testQuery1, currentUser);
        store.createQuery(testQuery2, currentUser);
        store.createQuery(testQuery3, currentUser);

        Assert.assertEquals(1, store.getQueries(Lists.fixedSize.of("2")).size());
        Assert.assertEquals(1, store.getQueries(Lists.fixedSize.of("3")).size());
        Assert.assertEquals(2, store.getQueries(Lists.fixedSize.of("2", "3")).size());

        Assert.assertEquals("Can't find queries for the following ID(s):\\n4", Assert.assertThrows(ApplicationQueryException.class, () -> store.getQueries(Lists.fixedSize.of("4"))).getMessage());
        Assert.assertEquals("Can't find queries for the following ID(s):\\n4\\n6", Assert.assertThrows(ApplicationQueryException.class, () -> store.getQueries(Lists.fixedSize.of("4", "3", "6"))).getMessage());

        Assert.assertEquals("Can't fetch more than 50 queries", Assert.assertThrows(ApplicationQueryException.class, () -> store.getQueries(Lists.fixedSize.ofAll(Collections.nCopies(51, "5")))).getMessage());
    }


    @Test
    public void testCreateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        store.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        List<QueryEvent> events = store.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(1, events.size());
        QueryEvent event = events.get(0);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.CREATED, event.eventType);
    }

    @Test
    public void testUpdateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery query = TestQueryBuilder.create("1", "query1", currentUser).build();
        store.createQuery(query, currentUser);
        store.updateQuery(query.id, query, currentUser);
        List<QueryEvent> events = store.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.UPDATED, event.eventType);
    }

    @Test
    public void testDeleteQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery query = TestQueryBuilder.create("1", "query1", currentUser).build();
        store.createQuery(query, currentUser);
        store.deleteQuery(query.id, currentUser);
        List<QueryEvent> events = store.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.DELETED, event.eventType);
    }

    @Test
    public void testGetQueryEvents() throws Exception
    {
        String currentUser = "testUser";
        DataCubeQuery query1 = TestQueryBuilder.create("1", "query1", currentUser).build();

        // NOTE: for this test to work well, we need leave a tiny window of time (10 ms) between each operation
        // so the test for filter using timestamp can be correct
        store.createQuery(query1, currentUser);
        Thread.sleep(10);
        store.updateQuery(query1.id, query1, currentUser);
        Thread.sleep(10);
        store.deleteQuery(query1.id, currentUser);
        Thread.sleep(10);
        DataCubeQuery query2 = TestQueryBuilder.create("2", "query2", currentUser).build();
        store.createQuery(query2, currentUser);
        Thread.sleep(10);
        store.updateQuery(query2.id, query2, currentUser);
        Thread.sleep(10);
        store.deleteQuery(query2.id, currentUser);
        Thread.sleep(10);

        Assert.assertEquals(6, store.getQueryEvents(null, null, null, null, null).size());

        // Query ID
        Assert.assertEquals(3, store.getQueryEvents("1", null, null, null, null).size());
        Assert.assertEquals(3, store.getQueryEvents("2", null, null, null, null).size());

        // Event Type
        Assert.assertEquals(2, store.getQueryEvents(null, QueryEvent.QueryEventType.CREATED, null, null, null).size());
        Assert.assertEquals(2, store.getQueryEvents(null, QueryEvent.QueryEventType.UPDATED, null, null, null).size());
        Assert.assertEquals(2, store.getQueryEvents(null, QueryEvent.QueryEventType.DELETED, null, null, null).size());

        // Limit
        Assert.assertEquals(1, store.getQueryEvents(null, null, null, null, 1).size());
        Assert.assertEquals(5, store.getQueryEvents(null, null, null, null, 5).size());

        Long now = Instant.now().toEpochMilli();
        Assert.assertEquals(0, store.getQueryEvents(null, null, now, null, null).size());
        Assert.assertEquals(6, store.getQueryEvents(null, null, null, now, null).size());

        QueryEvent event1 = store.getQueryEvents("1", QueryEvent.QueryEventType.DELETED, null, null, null).get(0);
        Assert.assertEquals(4, store.getQueryEvents(null, null, event1.timestamp, null, null).size());
        Assert.assertEquals(3, store.getQueryEvents(null, null, null, event1.timestamp, null).size());

        QueryEvent event2 = store.getQueryEvents("2", QueryEvent.QueryEventType.CREATED, null, null, null).get(0);
        Assert.assertEquals(3, store.getQueryEvents(null, null, event2.timestamp, null, null).size());
        Assert.assertEquals(4, store.getQueryEvents(null, null, null, event2.timestamp, null).size());
    }
}
