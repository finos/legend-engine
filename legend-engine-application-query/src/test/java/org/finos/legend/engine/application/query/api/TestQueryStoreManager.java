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
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QueryProjectCoordinates;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.application.query.utils.TestMongoClientProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class TestQueryStoreManager
{
    static class TestQuerySearchSpecificationBuilder
    {
        public String searchTerm;
        public List<QueryProjectCoordinates> projectCoordinates;
        public List<TaggedValue> taggedValues;
        public List<StereotypePtr> stereotypes;
        public Integer limit;
        public Boolean showCurrentUserQueriesOnly;

        TestQuerySearchSpecificationBuilder withSearchTerm(String searchTerm)
        {
            this.searchTerm = searchTerm;
            return this;
        }

        TestQuerySearchSpecificationBuilder withProjectCoordinates(List<QueryProjectCoordinates> projectCoordinates)
        {
            this.projectCoordinates = projectCoordinates;
            return this;
        }

        TestQuerySearchSpecificationBuilder withTaggedValues(List<TaggedValue> taggedValues)
        {
            this.taggedValues = taggedValues;
            return this;
        }

        TestQuerySearchSpecificationBuilder withStereotypes(List<StereotypePtr> stereotypes)
        {
            this.stereotypes = stereotypes;
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

        QuerySearchSpecification build()
        {
            QuerySearchSpecification searchSpecification = new QuerySearchSpecification();
            searchSpecification.searchTerm = this.searchTerm;
            searchSpecification.projectCoordinates = this.projectCoordinates;
            searchSpecification.taggedValues = this.taggedValues;
            searchSpecification.stereotypes = this.stereotypes;
            searchSpecification.limit = this.limit;
            searchSpecification.showCurrentUserQueriesOnly = this.showCurrentUserQueriesOnly;
            return searchSpecification;
        }
    }

    static class TestQueryBuilder
    {
        public String id;
        public String name;
        public String owner;
        public String groupId = "test.group";
        public String artifactId = "test-artifact";
        public String versionId = "0.0.0";
        public String description = "description";
        public String mapping = "mapping";
        public String runtime = "runtime";
        public String content = "content";
        public List<TaggedValue> taggedValues = Collections.emptyList();
        public List<StereotypePtr> stereotypes = Collections.emptyList();

        static TestQueryBuilder create(String id, String name, String owner)
        {
            TestQueryBuilder queryBuilder = new TestQueryBuilder();
            queryBuilder.id = id;
            queryBuilder.name = name;
            queryBuilder.owner = owner;
            return queryBuilder;
        }

        TestQueryBuilder withGroupId(String groupId)
        {
            this.groupId = groupId;
            return this;
        }

        TestQueryBuilder withArtifactId(String artifactId)
        {
            this.artifactId = artifactId;
            return this;
        }

        TestQueryBuilder withVersionId(String versionId)
        {
            this.versionId = versionId;
            return this;
        }

        TestQueryBuilder withTaggedValues(List<TaggedValue> taggedValues)
        {
            this.taggedValues = taggedValues;
            return this;
        }

        TestQueryBuilder withStereotypes(List<StereotypePtr> stereotypes)
        {
            this.stereotypes = stereotypes;
            return this;
        }

        Query build()
        {
            Query query = new Query();
            query.id = this.id;
            query.name = this.name;
            query.owner = this.owner;
            query.groupId = this.groupId;
            query.artifactId = this.artifactId;
            query.versionId = this.versionId;
            query.mapping = this.mapping;
            query.runtime = this.runtime;
            query.content = this.content;
            query.description = this.description;
            query.taggedValues = this.taggedValues;
            query.stereotypes = this.stereotypes;
            return query;
        }
    }

    private static QueryProjectCoordinates createTestQueryProjectCoordinate(String groupId, String artifactId, String version)
    {
        QueryProjectCoordinates coordinate = new QueryProjectCoordinates();
        coordinate.groupId = groupId;
        coordinate.artifactId = artifactId;
        coordinate.version = version;
        return coordinate;
    }

    private static TaggedValue createTestTaggedValue(String profile, String tag, String value)
    {
        TaggedValue taggedValue = new TaggedValue();
        taggedValue.tag = new TagPtr();
        taggedValue.tag.profile = profile;
        taggedValue.tag.value = tag;
        taggedValue.value = value;
        return taggedValue;
    }

    private static StereotypePtr createTestStereotype(String profile, String stereotype)
    {
        StereotypePtr taggedValue = new StereotypePtr();
        taggedValue.profile = profile;
        taggedValue.value = stereotype;
        return taggedValue;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    private TestMongoClientProvider testMongoClientProvider = new TestMongoClientProvider();
    private final QueryStoreManager queryStoreManager = new QueryStoreManager(testMongoClientProvider.mongoClient);
    private static final TestVaultImplementation testVaultImplementation = new TestVaultImplementation();

    @BeforeClass
    public static void setupClass()
    {
        testVaultImplementation.setValue("query.mongo.database", "test");
        testVaultImplementation.setValue("query.mongo.collection.query", "query");
        testVaultImplementation.setValue("query.mongo.collection.queryEvent", "query-event");
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
        Function0<Query> _createTestQuery = () -> TestQueryBuilder.create("1", "query1", "testUser").build();
        Query goodQuery = _createTestQuery.get();
        QueryStoreManager.validateQuery(goodQuery);

        // ID
        Query queryWithInvalidId = _createTestQuery.get();
        queryWithInvalidId.id = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidId));
        queryWithInvalidId.id = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidId));

        // Name
        Query queryWithInvalidName = _createTestQuery.get();
        queryWithInvalidName.name = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidName));
        queryWithInvalidId.name = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidName));

        // Group ID
        Query queryWithInvalidGroupId = _createTestQuery.get();
        queryWithInvalidGroupId.groupId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "group-test";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "12314";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));

        // Artifact ID
        Query queryWithInvalidArtifactId = _createTestQuery.get();
        queryWithInvalidArtifactId.artifactId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "Group";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "someArtifact";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));

        // Version
        Query queryWithInvalidVersionId = _createTestQuery.get();
        queryWithInvalidVersionId.versionId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidVersionId));
        queryWithInvalidVersionId.versionId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidVersionId));

        // Mapping
        Query queryWithInvalidMapping = _createTestQuery.get();
        queryWithInvalidMapping.mapping = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidMapping));
        queryWithInvalidMapping.mapping = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidMapping));

        // Runtime
        Query queryWithInvalidRuntime = _createTestQuery.get();
        queryWithInvalidRuntime.runtime = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidRuntime));
        queryWithInvalidRuntime.runtime = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidRuntime));

        // Content
        Query queryWithInvalidContent = _createTestQuery.get();
        queryWithInvalidContent.content = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidContent));
        queryWithInvalidContent.content = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidContent));
    }

    @Test
    public void testGetLightQueries() throws Exception
    {
        String currentUser = "testUser";
        Query newQuery = TestQueryBuilder.create("1", "query1", currentUser).withGroupId("test.group").withArtifactId("test-artifact").build();
        queryStoreManager.createQuery(newQuery, currentUser);
        List<Query> queries = queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser);
        Assert.assertEquals(1, queries.size());
        Assert.assertEquals("{" +
            "\"artifactId\":\"test-artifact\"," +
            "\"content\":null," +
            "\"description\":null," +
            "\"groupId\":\"test.group\"," +
            "\"id\":\"1\"," +
            "\"mapping\":null," +
            "\"name\":\"query1\"," +
            "\"owner\":\"testUser\"," +
            "\"runtime\":null," +
            "\"stereotypes\":null," +
            "\"taggedValues\":null," +
            "\"versionId\":\"0.0.0\"" +
            "}", objectMapper.writeValueAsString(queries.get(0)));
    }

    @Test
    public void testGetQueriesWithLimit() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        queryStoreManager.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(1, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withLimit(1).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withLimit(0).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithProjectCoordinates() throws Exception
    {
        String currentUser = "testUser";
        Query testQuery1 = TestQueryBuilder.create("1", "query1", currentUser).withGroupId("test").withArtifactId("test").build();
        Query testQuery2 = TestQueryBuilder.create("2", "query2", currentUser).withGroupId("test").withArtifactId("test").build();
        Query testQuery3 = TestQueryBuilder.create("3", "query3", currentUser).withGroupId("something").withArtifactId("something").build();
        Query testQuery4 = TestQueryBuilder.create("4", "query4", currentUser).withGroupId("something.another").withArtifactId("something-another").withVersionId("1.0.0").build();
        queryStoreManager.createQuery(testQuery1, currentUser);
        queryStoreManager.createQuery(testQuery2, currentUser);
        queryStoreManager.createQuery(testQuery3, currentUser);
        queryStoreManager.createQuery(testQuery4, currentUser);

        // When no projects specified, return all queries
        Assert.assertEquals(4, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());

        QueryProjectCoordinates coordinate1 = createTestQueryProjectCoordinate("notfound", "notfound", null);
        Assert.assertEquals(0, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate1)).build(), currentUser).size());

        QueryProjectCoordinates coordinate2 = createTestQueryProjectCoordinate("test", "test", null);
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate2)).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate1, coordinate2)).build(), currentUser).size());

        QueryProjectCoordinates coordinate3 = createTestQueryProjectCoordinate("something", "something", null);
        Assert.assertEquals(1, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate3)).build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate1, coordinate2, coordinate3)).build(), currentUser).size());

        QueryProjectCoordinates coordinate4 = createTestQueryProjectCoordinate("something.another", "something-another", "1.0.0");
        Assert.assertEquals(1, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate4)).build(), currentUser).size());
        Assert.assertEquals(4, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withProjectCoordinates(Lists.fixedSize.of(coordinate1, coordinate2, coordinate3, coordinate4)).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithStereotypes() throws Exception
    {
        String currentUser = "testUser";
        StereotypePtr stereotype1 = createTestStereotype("profile1", "stereotype1");
        StereotypePtr stereotype2 = createTestStereotype("profile2", "stereotype2");
        StereotypePtr stereotype3 = createTestStereotype("profile3", "stereotype3");
        Query testQuery1 = TestQueryBuilder.create("1", "query1", currentUser).build();
        Query testQuery2 = TestQueryBuilder.create("2", "query2", currentUser).withStereotypes(Lists.fixedSize.of(stereotype1)).build();
        Query testQuery3 = TestQueryBuilder.create("3", "query3", currentUser).withStereotypes(Lists.fixedSize.of(stereotype2)).build();
        Query testQuery4 = TestQueryBuilder.create("4", "query3", currentUser).withStereotypes(Lists.fixedSize.of(stereotype1, stereotype2)).build();
        queryStoreManager.createQuery(testQuery1, currentUser);
        queryStoreManager.createQuery(testQuery2, currentUser);
        queryStoreManager.createQuery(testQuery3, currentUser);
        queryStoreManager.createQuery(testQuery4, currentUser);

        // When no stereotype provided, return all queries
        Assert.assertEquals(4, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(0, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withStereotypes(Lists.fixedSize.of(stereotype3)).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withStereotypes(Lists.fixedSize.of(stereotype1)).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withStereotypes(Lists.fixedSize.of(stereotype2)).build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withStereotypes(Lists.fixedSize.of(stereotype1, stereotype2)).build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withStereotypes(Lists.fixedSize.of(stereotype1, stereotype2, stereotype3)).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithTaggedValues() throws Exception
    {
        String currentUser = "testUser";
        TaggedValue taggedValue1 = createTestTaggedValue("profile1", "tag1", "value1");
        TaggedValue taggedValue2 = createTestTaggedValue("profile2", "tag2", "value2");
        TaggedValue taggedValue3 = createTestTaggedValue("profile3", "tag3", "value3");
        Query testQuery1 = TestQueryBuilder.create("1", "query1", currentUser).build();
        Query testQuery2 = TestQueryBuilder.create("2", "query2", currentUser).withTaggedValues(Lists.fixedSize.of(taggedValue1)).build();
        Query testQuery3 = TestQueryBuilder.create("3", "query3", currentUser).withTaggedValues(Lists.fixedSize.of(taggedValue2)).build();
        Query testQuery4 = TestQueryBuilder.create("4", "query3", currentUser).withTaggedValues(Lists.fixedSize.of(taggedValue1, taggedValue2)).build();
        queryStoreManager.createQuery(testQuery1, currentUser);
        queryStoreManager.createQuery(testQuery2, currentUser);
        queryStoreManager.createQuery(testQuery3, currentUser);
        queryStoreManager.createQuery(testQuery4, currentUser);

        // When no tagged value provided, return all queries
        Assert.assertEquals(4, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(0, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withTaggedValues(Lists.fixedSize.of(taggedValue3)).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withTaggedValues(Lists.fixedSize.of(taggedValue1)).build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withTaggedValues(Lists.fixedSize.of(taggedValue2)).build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withTaggedValues(Lists.fixedSize.of(taggedValue1, taggedValue2)).build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withTaggedValues(Lists.fixedSize.of(taggedValue1, taggedValue2, taggedValue3)).build(), currentUser).size());
    }

    @Test
    public void testGetQueriesWithSearchText() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        queryStoreManager.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        queryStoreManager.createQuery(TestQueryBuilder.create("3", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query1").build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query2").build(), currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withSearchTerm("query").build(), currentUser).size());
    }

    @Test
    public void testGetQueriesForCurrentUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), "testUser1");
        queryStoreManager.createQuery(TestQueryBuilder.create("2", "query2", currentUser).build(), currentUser);
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withShowCurrentUserQueriesOnly(false).build(), currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().withShowCurrentUserQueriesOnly(true).build(), currentUser).size());
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
        Query newQuery = TestQueryBuilder.create("1", "query1", currentUser).build();
        Query createdQuery = queryStoreManager.createQuery(newQuery, currentUser);
        Assert.assertEquals("{" +
            "\"artifactId\":\"test-artifact\"," +
            "\"content\":\"content\"," +
            "\"description\":\"description\"," +
            "\"groupId\":\"test.group\"," +
            "\"id\":\"1\"," +
            "\"mapping\":\"mapping\"," +
            "\"name\":\"query1\"," +
            "\"owner\":\"" + currentUser + "\"," +
            "\"runtime\":\"runtime\"," +
            "\"stereotypes\":[]," +
            "\"taggedValues\":[]," +
            "\"versionId\":\"0.0.0\"" +
            "}", objectMapper.writeValueAsString(createdQuery));
    }

    @Test
    public void testCreateInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.createQuery(TestQueryBuilder.create("1", null, currentUser).build(), currentUser));
    }

    @Test
    public void testCreateQueryWithSameId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser));
    }

    @Test
    public void testForceCurrentUserToBeOwnerWhenCreatingQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", null).build(), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
        queryStoreManager.createQuery(TestQueryBuilder.create("2", "query2", "testUser2").build(), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("2").owner);
        queryStoreManager.createQuery(TestQueryBuilder.create("3", "query1", "testUser2").build(), null);
        Assert.assertNull(queryStoreManager.getQuery("3").owner);
    }

    @Test
    public void testUpdateQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        queryStoreManager.updateQuery("1", TestQueryBuilder.create("1", "query2", currentUser).build(), currentUser);
        Assert.assertEquals("query2", queryStoreManager.getQuery("1").name);
    }

    @Test
    public void testUpdateWithInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", TestQueryBuilder.create("1", null, currentUser).build(), currentUser));
    }

    @Test
    public void testPreventUpdateQueryId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", TestQueryBuilder.create("2", "query1", "testUser2").build(), currentUser));
    }

    @Test
    public void testUpdateNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser));
    }

    @Test
    public void testAllowUpdateQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        queryStoreManager.updateQuery("1", TestQueryBuilder.create("1", "query2", null).build(), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
    }

    @Test
    public void testForbidUpdateQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", TestQueryBuilder.create("1", "query1", "testUser2").build(), "testUser2"));
    }

    @Test
    public void testDeleteQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
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
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", null).build(), null);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(new TestQuerySearchSpecificationBuilder().build(), currentUser).size());
    }

    @Test
    public void testForbidDeleteQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.deleteQuery("1", "testUser2"));
    }

    @Test
    public void testCreateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(TestQueryBuilder.create("1", "query1", currentUser).build(), currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(1, events.size());
        QueryEvent event = events.get(0);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.CREATED, event.eventType);
    }

    @Test
    public void testUpdateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        Query query = TestQueryBuilder.create("1", "query1", currentUser).build();
        queryStoreManager.createQuery(query, currentUser);
        queryStoreManager.updateQuery(query.id, query, currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.UPDATED, event.eventType);
    }

    @Test
    public void testDeleteQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        Query query = TestQueryBuilder.create("1", "query1", currentUser).build();
        queryStoreManager.createQuery(query, currentUser);
        queryStoreManager.deleteQuery(query.id, currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.DELETED, event.eventType);
    }

    @Test
    public void testGetQueryEvents() throws Exception
    {
        String currentUser = "testUser";
        Query query1 = TestQueryBuilder.create("1", "query1", currentUser).build();

        // NOTE: for this test to work well, we need leave a tiny window of time (10 ms) between each operation
        // so the test for filter using timestamp can be correct
        queryStoreManager.createQuery(query1, currentUser);
        Thread.sleep(10);
        queryStoreManager.updateQuery(query1.id, query1, currentUser);
        Thread.sleep(10);
        queryStoreManager.deleteQuery(query1.id, currentUser);
        Thread.sleep(10);
        Query query2 = TestQueryBuilder.create("2", "query2", currentUser).build();
        queryStoreManager.createQuery(query2, currentUser);
        Thread.sleep(10);
        queryStoreManager.updateQuery(query2.id, query2, currentUser);
        Thread.sleep(10);
        queryStoreManager.deleteQuery(query2.id, currentUser);
        Thread.sleep(10);

        Assert.assertEquals(6, queryStoreManager.getQueryEvents(null, null, null, null, null).size());

        // Query ID
        Assert.assertEquals(3, queryStoreManager.getQueryEvents("1", null, null, null, null).size());
        Assert.assertEquals(3, queryStoreManager.getQueryEvents("2", null, null, null, null).size());

        // Event Type
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.CREATED, null, null, null).size());
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.UPDATED, null, null, null).size());
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.DELETED, null, null, null).size());

        // Limit
        Assert.assertEquals(1, queryStoreManager.getQueryEvents(null, null, null, null, 1).size());
        Assert.assertEquals(5, queryStoreManager.getQueryEvents(null, null, null, null, 5).size());

        Long now = Instant.now().toEpochMilli();
        Assert.assertEquals(0, queryStoreManager.getQueryEvents(null, null, now, null, null).size());
        Assert.assertEquals(6, queryStoreManager.getQueryEvents(null, null, null, now, null).size());

        QueryEvent event1 = queryStoreManager.getQueryEvents("1", QueryEvent.QueryEventType.DELETED, null, null, null).get(0);
        Assert.assertEquals(4, queryStoreManager.getQueryEvents(null, null, event1.timestamp, null, null).size());
        Assert.assertEquals(3, queryStoreManager.getQueryEvents(null, null, null, event1.timestamp, null).size());

        QueryEvent event2 = queryStoreManager.getQueryEvents("2", QueryEvent.QueryEventType.CREATED, null, null, null).get(0);
        Assert.assertEquals(3, queryStoreManager.getQueryEvents(null, null, event2.timestamp, null, null).size());
        Assert.assertEquals(4, queryStoreManager.getQueryEvents(null, null, null, event2.timestamp, null).size());
    }
}
