// Copyright 2026 Goldman Sachs
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.application.query.model.CubeSearchSpecification;
import org.finos.legend.engine.application.query.model.CubeSourceRef;
import org.finos.legend.engine.application.query.model.QuerySearchSortBy;
import org.finos.legend.engine.application.query.model.StoredCube;
import org.finos.legend.engine.application.query.utils.TestMongoClientProvider;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TestCubeStoreManager
{
    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CONTENT = "{\"formatVersion\":1,\"name\":\"Orders\",\"query\":{\"nodes\":[]},\"meta\":{\"a.b\":1,\"$c\":null,\"keys\":{\"z\":1,\"a\":2}}}";
    private static final String CONTENT_2 = "{\"formatVersion\":1,\"name\":\"Orders 2\",\"query\":{\"nodes\":[{\"kind\":\"relational\"}]}}";

    private static final TestVaultImplementation testVaultImplementation = new TestVaultImplementation();

    private TestMongoClientProvider testMongoClientProvider;
    private TestClock clock;
    private CubeStoreManager store;

    /**
     * A clock the tests move
     */
    private static class TestClock extends Clock
    {
        private long millis = 1_700_000_000_000L;

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return Instant.ofEpochMilli(this.millis);
        }

        void advance(long by)
        {
            this.millis += by;
        }
    }

    @BeforeClass
    public static void setupClass()
    {
        testVaultImplementation.setValue("query.mongo.database", "test");
        testVaultImplementation.setValue("query.mongo.collection.cube", "cube");
        testVaultImplementation.setValue("query.mongo.collection.cubeVersion", "cubeVersion");
        testVaultImplementation.setValue("query.mongo.collection.cubeEvent", "cubeEvent");
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
        this.clock = new TestClock();
        this.store = new CubeStoreManager(this.testMongoClientProvider.mongoClient, this.clock);
    }

    @After
    public void cleanUp()
    {
        this.testMongoClientProvider.cleanUp();
    }

    private MongoCollection<Document> collection(String name)
    {
        return this.testMongoClientProvider.mongoClient.getDatabase("test").getCollection(name);
    }

    private static StoredCube newCube(String name, String content)
    {
        StoredCube cube = new StoredCube();
        cube.name = name;
        cube.formatVersion = 1;
        cube.content = content;
        return cube;
    }

    private static StoredCube save(Integer version, String name, String content)
    {
        StoredCube cube = newCube(name, content);
        cube.version = version;
        return cube;
    }

    private static CubeSourceRef sourceRef(String type, String path, String element, String project)
    {
        CubeSourceRef sourceRef = new CubeSourceRef();
        sourceRef.type = type;
        sourceRef.path = path;
        sourceRef.element = element;
        sourceRef.project = project;
        return sourceRef;
    }

    private static Map<String, Object> patch(Object... keysAndValues)
    {
        Map<String, Object> patch = Maps.mutable.empty();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            patch.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return patch;
    }

    private static ApplicationQueryException assertRefused(Response.Status status, String message, Runnable action)
    {
        ApplicationQueryException exception = Assert.assertThrows(ApplicationQueryException.class, action::run);
        Assert.assertEquals(status, exception.getStatus());
        if (message != null)
        {
            Assert.assertEquals(message, exception.getMessage());
        }
        return exception;
    }

    private static CubeSearchSpecification search(String searchTerm)
    {
        CubeSearchSpecification specification = new CubeSearchSpecification();
        specification.searchTerm = searchTerm;
        return specification;
    }

    private List<String> searchNames(CubeSearchSpecification specification)
    {
        return this.store.searchCubes(specification).stream().map(cube -> cube.name).collect(Collectors.toList());
    }

    // --------------------------------------------- create ---------------------------------------------

    @Test
    public void testCreateMakesIdVersionOwnersAndAudit()
    {
        StoredCube request = newCube("  Orders  ", CONTENT);
        request.owners = Arrays.asList(BOB, ALICE, BOB);
        request.tags = Arrays.asList("finance", " finance ", "eu");
        request.sourceRefs = Collections.singletonList(sourceRef("table", "my::Db", "S.ORDERS", "org.finos:northwind"));
        StoredCube created = this.store.createCube(request, ALICE);

        Assert.assertNotNull(created.id);
        Assert.assertEquals(Integer.valueOf(1), created.version);
        Assert.assertEquals("Orders", created.name);
        Assert.assertEquals(Arrays.asList(ALICE, BOB), created.owners);
        Assert.assertEquals(Arrays.asList("finance", "eu"), created.tags);
        Assert.assertEquals(ALICE, created.audit.createdBy);
        Assert.assertEquals(Long.valueOf(this.clock.millis()), created.audit.createdAt);
        Assert.assertEquals(ALICE, created.audit.updatedBy);
        Assert.assertEquals(Long.valueOf(this.clock.millis()), created.audit.updatedAt);
        Assert.assertNull(created.deleted);

        StoredCube read = this.store.getCube(created.id);
        // the spec comes back exactly as sent: keys with . and $, nulls and key order included
        Assert.assertEquals(CONTENT, read.content);
        Assert.assertEquals(Integer.valueOf(1), read.formatVersion);
        Assert.assertEquals("S.ORDERS", read.sourceRefs.get(0).element);
        Assert.assertEquals("org.finos:northwind", read.sourceRefs.get(0).project);
        Assert.assertEquals(CONTENT, this.store.getCubeVersion(created.id, 1).content);
        Assert.assertEquals(1, this.collection("cubeEvent").countDocuments(new Document("queryId", created.id).append("eventType", "CREATED")));
    }

    @Test
    public void testCreateMakesNewIds()
    {
        String first = this.store.createCube(newCube("A", CONTENT), ALICE).id;
        String second = this.store.createCube(newCube("A", CONTENT), ALICE).id;
        Assert.assertNotEquals(first, second);
    }

    @Test
    public void testCreateValidation()
    {
        StoredCube withId = newCube("A", CONTENT);
        withId.id = "mine";
        assertRefused(Response.Status.BAD_REQUEST, "A new cube's id is made by the store: send none", () -> this.store.createCube(withId, ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube name is missing or empty", () -> this.store.createCube(newCube(null, CONTENT), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube name is missing or empty", () -> this.store.createCube(newCube("   ", CONTENT), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube name is longer than 255 characters", () -> this.store.createCube(newCube(String.join("", Collections.nCopies(256, "x")), CONTENT), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content is missing", () -> this.store.createCube(newCube("A", null), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, null, () -> this.store.createCube(newCube("A", "{not json"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content is not a JSON object", () -> this.store.createCube(newCube("A", "[1]"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content's formatVersion must be the cube's, 1", () -> this.store.createCube(newCube("A", "{\"formatVersion\":2}"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content's formatVersion must be the cube's, 1", () -> this.store.createCube(newCube("A", "{\"formatVersion\":\"1\"}"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content's formatVersion must be the cube's, 1", () -> this.store.createCube(newCube("A", "{}"), ALICE));

        StoredCube noFormat = newCube("A", CONTENT);
        noFormat.formatVersion = null;
        assertRefused(Response.Status.BAD_REQUEST, "Cube format version is missing, or not a whole number of at least 1", () -> this.store.createCube(noFormat, ALICE));
        StoredCube zeroFormat = newCube("A", "{\"formatVersion\":0}");
        zeroFormat.formatVersion = 0;
        assertRefused(Response.Status.BAD_REQUEST, "Cube format version is missing, or not a whole number of at least 1", () -> this.store.createCube(zeroFormat, ALICE));

        String padding = String.join("", Collections.nCopies(CubeStoreManager.MAX_CONTENT_BYTES, "x"));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content is larger than 1048576 bytes", () -> this.store.createCube(newCube("A", "{\"formatVersion\":1,\"p\":\"" + padding + "\"}"), ALICE));

        StoredCube blankOwner = newCube("A", CONTENT);
        blankOwner.owners = Collections.singletonList(" ");
        assertRefused(Response.Status.BAD_REQUEST, "Cube owners can't be empty", () -> this.store.createCube(blankOwner, ALICE));
        StoredCube blankTag = newCube("A", CONTENT);
        blankTag.tags = Collections.singletonList("");
        assertRefused(Response.Status.BAD_REQUEST, "Cube tags can't be empty", () -> this.store.createCube(blankTag, ALICE));
        StoredCube noType = newCube("A", CONTENT);
        noType.sourceRefs = Collections.singletonList(sourceRef(null, "my::Db", null, null));
        assertRefused(Response.Status.BAD_REQUEST, "Cube source reference type is missing", () -> this.store.createCube(noType, ALICE));
        StoredCube emptyProject = newCube("A", CONTENT);
        emptyProject.sourceRefs = Collections.singletonList(sourceRef("table", "my::Db", null, ""));
        assertRefused(Response.Status.BAD_REQUEST, "Cube source reference project is empty", () -> this.store.createCube(emptyProject, ALICE));

        assertRefused(Response.Status.UNAUTHORIZED, "A cube can only be changed by a signed-in user", () -> this.store.createCube(newCube("A", CONTENT), null));
        Assert.assertEquals(0, this.collection("cube").countDocuments());
    }

    // --------------------------------------------- read ---------------------------------------------

    @Test
    public void testGetSetsLastOpenAtOnly()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        this.clock.advance(5_000);
        StoredCube read = this.store.getCube(created.id);
        Assert.assertEquals(Long.valueOf(this.clock.millis()), read.lastOpenAt);
        Assert.assertEquals(Integer.valueOf(1), read.version);
        Assert.assertEquals(created.audit.updatedAt, read.audit.updatedAt);
        Assert.assertEquals(CONTENT, read.content);
        // a read records no version
        Assert.assertEquals(1, this.collection("cubeVersion").countDocuments());
    }

    @Test
    public void testGetMissing()
    {
        assertRefused(Response.Status.NOT_FOUND, "Can't find cube 'nope'", () -> this.store.getCube("nope"));
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        assertRefused(Response.Status.NOT_FOUND, "Can't find version 2 of cube '" + created.id + "'", () -> this.store.getCubeVersion(created.id, 2));
        assertRefused(Response.Status.NOT_FOUND, "Can't find cube 'nope'", () -> this.store.getCubeVersion("nope", 1));
    }

    // --------------------------------------------- update ---------------------------------------------

    @Test
    public void testUpdateSavesANewVersion()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        this.clock.advance(1_000);
        StoredCube request = save(1, "B", CONTENT_2);
        request.sourceRefs = Collections.singletonList(sourceRef("dataProductAccessPoint", "my::Product", "ap1", null));
        StoredCube saved = this.store.updateCube(created.id, request, ALICE);

        Assert.assertEquals(created.id, saved.id);
        Assert.assertEquals(Integer.valueOf(2), saved.version);
        Assert.assertEquals("B", saved.name);
        Assert.assertEquals(CONTENT_2, saved.content);
        Assert.assertEquals("ap1", saved.sourceRefs.get(0).element);
        Assert.assertEquals(Collections.singletonList(ALICE), saved.owners);
        Assert.assertEquals(created.audit.createdAt, saved.audit.createdAt);
        Assert.assertEquals(Long.valueOf(this.clock.millis()), saved.audit.updatedAt);

        Assert.assertEquals(CONTENT, this.store.getCubeVersion(created.id, 1).content);
        Assert.assertEquals("A", this.store.getCubeVersion(created.id, 1).name);
        Assert.assertEquals(CONTENT_2, this.store.getCubeVersion(created.id, 2).content);
        Assert.assertEquals(CONTENT_2, this.store.getCube(created.id).content);
        Assert.assertEquals(1, this.collection("cube").countDocuments());
    }

    @Test
    public void testUpdateOverAnOlderVersionConflicts()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        this.store.updateCube(created.id, save(1, "B", CONTENT_2), ALICE);
        assertRefused(Response.Status.CONFLICT, "Cannot change version 1 of cube '" + created.id + "': its current version is 2.", () -> this.store.updateCube(created.id, save(1, "C", CONTENT), ALICE));
        assertRefused(Response.Status.CONFLICT, "Cannot change version 3 of cube '" + created.id + "': its current version is 2.", () -> this.store.updateCube(created.id, save(3, "C", CONTENT), ALICE));
        StoredCube read = this.store.getCube(created.id);
        Assert.assertEquals("B", read.name);
        Assert.assertEquals(Integer.valueOf(2), read.version);
    }

    @Test
    public void testConcurrentSavesOfOneVersionLetOneWin() throws Exception
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        int writers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(writers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response.Status>> results = new ArrayList<>();
        for (int i = 0; i < writers; i++)
        {
            String name = "Writer " + i;
            Callable<Response.Status> save = () ->
            {
                start.await();
                try
                {
                    this.store.updateCube(created.id, save(1, name, CONTENT_2), ALICE);
                    return Response.Status.OK;
                }
                catch (ApplicationQueryException e)
                {
                    return e.getStatus();
                }
            };
            results.add(executor.submit(save));
        }
        start.countDown();
        List<Response.Status> statuses = new ArrayList<>();
        for (Future<Response.Status> result : results)
        {
            statuses.add(result.get());
        }
        executor.shutdown();
        Assert.assertEquals(1, statuses.stream().filter(Response.Status.OK::equals).count());
        Assert.assertEquals(writers - 1, statuses.stream().filter(Response.Status.CONFLICT::equals).count());
        Assert.assertEquals(Integer.valueOf(2), this.store.getCube(created.id).version);
        Assert.assertEquals(2, this.collection("cubeVersion").countDocuments(new Document("id", created.id)));
    }

    @Test
    public void testUpdateByAnotherUserIsForbidden()
    {
        StoredCube request = newCube("A", CONTENT);
        request.owners = Collections.singletonList("carol");
        StoredCube created = this.store.createCube(request, ALICE);
        assertRefused(Response.Status.FORBIDDEN, "Only current owners can change this cube. Current owners are alice, carol.", () -> this.store.updateCube(created.id, save(1, "B", CONTENT_2), BOB));
        // not an owner comes before a stale version
        assertRefused(Response.Status.FORBIDDEN, null, () -> this.store.updateCube(created.id, save(7, "B", CONTENT_2), BOB));
        this.store.updateCube(created.id, save(1, "B", CONTENT_2), "carol");
    }

    @Test
    public void testUpdateValidation()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        assertRefused(Response.Status.BAD_REQUEST, "Cube version is missing: send the version that was read", () -> this.store.updateCube(created.id, newCube("B", CONTENT), ALICE));
        StoredCube withOwners = save(1, "B", CONTENT);
        withOwners.owners = Collections.singletonList(BOB);
        assertRefused(Response.Status.BAD_REQUEST, "A cube's owners and tags are changed with PATCH", () -> this.store.updateCube(created.id, withOwners, ALICE));
        StoredCube otherId = save(1, "B", CONTENT);
        otherId.id = "other";
        assertRefused(Response.Status.BAD_REQUEST, "A cube's id can't be changed", () -> this.store.updateCube(created.id, otherId, ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube content is not a JSON object", () -> this.store.updateCube(created.id, save(1, "B", "1"), ALICE));
        assertRefused(Response.Status.NOT_FOUND, "Can't find cube 'nope'", () -> this.store.updateCube("nope", save(1, "B", CONTENT), ALICE));
        assertRefused(Response.Status.UNAUTHORIZED, null, () -> this.store.updateCube(created.id, save(1, "B", CONTENT), ""));
        Assert.assertEquals(Integer.valueOf(1), this.store.getCube(created.id).version);
    }

    // --------------------------------------------- patch ---------------------------------------------

    @Test
    public void testPatchChangesNameOwnersAndTagsOnly()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        StoredCube patched = this.store.patchCube(created.id, patch("version", 1, "name", "Renamed", "owners", Arrays.asList(ALICE, BOB), "tags", Collections.singletonList("eu")), ALICE);
        Assert.assertEquals(Integer.valueOf(2), patched.version);
        Assert.assertEquals("Renamed", patched.name);
        Assert.assertEquals(Arrays.asList(ALICE, BOB), patched.owners);
        Assert.assertEquals(Collections.singletonList("eu"), patched.tags);
        Assert.assertEquals(CONTENT, patched.content);
        Assert.assertEquals(Arrays.asList(ALICE, BOB), this.store.getCubeVersion(created.id, 2).owners);

        // giving the cube away: its old owner can no longer change it
        this.store.patchCube(created.id, patch("version", 2, "owners", Collections.singletonList(BOB)), ALICE);
        assertRefused(Response.Status.FORBIDDEN, "Only current owners can change this cube. Current owners are bob.", () -> this.store.patchCube(created.id, patch("version", 3, "name", "Mine"), ALICE));
        Assert.assertEquals("Mine", this.store.patchCube(created.id, patch("version", 3, "name", "Mine"), BOB).name);
    }

    @Test
    public void testPatchValidation()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        assertRefused(Response.Status.BAD_REQUEST, "Unknown field 'content': only a cube's name, owners and tags can be patched", () -> this.store.patchCube(created.id, patch("version", 1, "content", CONTENT_2), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube version is missing: send the version that was read", () -> this.store.patchCube(created.id, patch("name", "B"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube version must be a whole number", () -> this.store.patchCube(created.id, patch("version", "1", "name", "B"), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Nothing to change: give a name, owners or tags", () -> this.store.patchCube(created.id, patch("version", 1), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "A cube needs at least one owner", () -> this.store.patchCube(created.id, patch("version", 1, "owners", Collections.emptyList()), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube owners can't be null", () -> this.store.patchCube(created.id, patch("version", 1, "owners", null), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube owners must be a list", () -> this.store.patchCube(created.id, patch("version", 1, "owners", ALICE), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube owners can't be empty", () -> this.store.patchCube(created.id, patch("version", 1, "owners", Collections.singletonList(1)), ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube name must be a string", () -> this.store.patchCube(created.id, patch("version", 1, "name", 1), ALICE));
        assertRefused(Response.Status.CONFLICT, null, () -> this.store.patchCube(created.id, patch("version", 2, "name", "B"), ALICE));
        Assert.assertEquals(Integer.valueOf(1), this.store.getCube(created.id).version);
    }

    // --------------------------------------------- delete ---------------------------------------------

    @Test
    public void testSoftDeleteHidesTheCube()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        this.store.deleteCube(created.id, 1, false, ALICE);
        assertRefused(Response.Status.NOT_FOUND, "Can't find cube '" + created.id + "'", () -> this.store.getCube(created.id));
        assertRefused(Response.Status.NOT_FOUND, null, () -> this.store.getCubeVersion(created.id, 1));
        assertRefused(Response.Status.NOT_FOUND, null, () -> this.store.updateCube(created.id, save(1, "B", CONTENT), ALICE));
        assertRefused(Response.Status.NOT_FOUND, null, () -> this.store.deleteCube(created.id, 1, false, ALICE));
        Assert.assertTrue(this.store.searchCubes(null).isEmpty());

        // kept, marked, with its versions
        Document document = this.collection("cube").find(new Document("id", created.id)).first();
        Assert.assertEquals(ALICE, ((Document) document.get("deleted")).getString("by"));
        Assert.assertEquals(1, this.collection("cubeVersion").countDocuments(new Document("id", created.id)));

        // then for good
        this.store.deleteCube(created.id, 1, true, ALICE);
        Assert.assertEquals(0, this.collection("cube").countDocuments());
        Assert.assertEquals(0, this.collection("cubeVersion").countDocuments());
        Assert.assertEquals(2, this.collection("cubeEvent").countDocuments(new Document("queryId", created.id).append("eventType", "DELETED")));
    }

    @Test
    public void testPermanentDeleteRemovesVersions()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        StoredCube other = this.store.createCube(newCube("B", CONTENT), ALICE);
        this.store.updateCube(created.id, save(1, "A", CONTENT_2), ALICE);
        this.store.deleteCube(created.id, 2, true, ALICE);
        assertRefused(Response.Status.NOT_FOUND, null, () -> this.store.getCube(created.id));
        Assert.assertEquals(0, this.collection("cubeVersion").countDocuments(new Document("id", created.id)));
        Assert.assertEquals(1, this.collection("cubeVersion").countDocuments(new Document("id", other.id)));
    }

    @Test
    public void testDeleteRefusals()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        assertRefused(Response.Status.FORBIDDEN, null, () -> this.store.deleteCube(created.id, 1, false, BOB));
        assertRefused(Response.Status.FORBIDDEN, null, () -> this.store.deleteCube(created.id, 1, true, BOB));
        assertRefused(Response.Status.CONFLICT, "Cannot change version 2 of cube '" + created.id + "': its current version is 1.", () -> this.store.deleteCube(created.id, 2, false, ALICE));
        assertRefused(Response.Status.BAD_REQUEST, "Cube version is missing: send the version that was read", () -> this.store.deleteCube(created.id, null, false, ALICE));
        assertRefused(Response.Status.NOT_FOUND, null, () -> this.store.deleteCube("nope", 1, true, ALICE));
        Assert.assertEquals("A", this.store.getCube(created.id).name);
    }

    // --------------------------------------------- search ---------------------------------------------

    @Test
    public void testSearch()
    {
        StoredCube orders = newCube("French orders", CONTENT);
        orders.tags = Arrays.asList("eu", "finance");
        orders.sourceRefs = Arrays.asList(sourceRef("table", "my::Db", "S.ORDERS", "org.finos:northwind"), sourceRef("table", "my::Db", "S.CUSTOMERS", null));
        StoredCube created = this.store.createCube(orders, ALICE);
        this.clock.advance(1_000);
        StoredCube trades = newCube("Trades a.b", CONTENT);
        trades.tags = Collections.singletonList("eu");
        trades.owners = Collections.singletonList("Carol.Smith");
        trades.sourceRefs = Collections.singletonList(sourceRef("dataProductAccessPoint", "my::Trades", "ap1", null));
        this.store.createCube(trades, BOB);
        this.clock.advance(1_000);
        StoredCube deleted = this.store.createCube(newCube("Deleted orders", CONTENT), ALICE);
        this.store.deleteCube(deleted.id, 1, false, ALICE);

        // the latest saved first, without content, deleted ones left out
        List<StoredCube> all = this.store.searchCubes(new CubeSearchSpecification());
        Assert.assertEquals(Arrays.asList("Trades a.b", "French orders"), all.stream().map(cube -> cube.name).collect(Collectors.toList()));
        Assert.assertTrue(all.stream().allMatch(cube -> cube.content == null && cube.version == 1 && cube.owners != null && cube.audit != null));
        Assert.assertEquals(2, all.get(1).sourceRefs.size());

        // the term: the id exactly, part of the name or of an owner, in any case, quoted
        Assert.assertEquals(Collections.singletonList("French orders"), this.searchNames(search(created.id)));
        Assert.assertEquals(Collections.emptyList(), this.searchNames(search(created.id.substring(0, 8))));
        Assert.assertEquals(Collections.singletonList("French orders"), this.searchNames(search("FRENCH")));
        Assert.assertEquals(Collections.singletonList("Trades a.b"), this.searchNames(search("carol.")));
        Assert.assertEquals(Collections.singletonList("Trades a.b"), this.searchNames(search("a.b")));
        Assert.assertEquals(Collections.emptyList(), this.searchNames(search("a.c")));
        Assert.assertEquals(Arrays.asList("Trades a.b", "French orders"), this.searchNames(search("  ")));

        CubeSearchSpecification byOwner = new CubeSearchSpecification();
        byOwner.owner = BOB;
        Assert.assertEquals(Collections.singletonList("Trades a.b"), this.searchNames(byOwner));
        CubeSearchSpecification byCreator = new CubeSearchSpecification();
        byCreator.creator = ALICE;
        Assert.assertEquals(Collections.singletonList("French orders"), this.searchNames(byCreator));

        CubeSearchSpecification byTags = new CubeSearchSpecification();
        byTags.tags = Arrays.asList("finance", "eu");
        Assert.assertEquals(Collections.singletonList("French orders"), this.searchNames(byTags));
        byTags.tags = Collections.singletonList("eu");
        Assert.assertEquals(2, this.searchNames(byTags).size());

        // the fields given must match one source, not several
        CubeSearchSpecification bySource = new CubeSearchSpecification();
        bySource.sourceRef = sourceRef("table", "my::Db", "S.CUSTOMERS", null);
        Assert.assertEquals(Collections.singletonList("French orders"), this.searchNames(bySource));
        bySource.sourceRef = sourceRef(null, null, "S.CUSTOMERS", "org.finos:northwind");
        Assert.assertEquals(Collections.emptyList(), this.searchNames(bySource));
        bySource.sourceRef = sourceRef("dataProductAccessPoint", null, null, null);
        Assert.assertEquals(Collections.singletonList("Trades a.b"), this.searchNames(bySource));
        bySource.sourceRef = new CubeSourceRef();
        assertRefused(Response.Status.BAD_REQUEST, "A search by source needs at least one of its type, path, element or project", () -> this.store.searchCubes(bySource));

        CubeSearchSpecification byCreation = new CubeSearchSpecification();
        byCreation.sortByOption = QuerySearchSortBy.SORT_BY_CREATE;
        byCreation.limit = 1;
        Assert.assertEquals(Collections.singletonList("Trades a.b"), this.searchNames(byCreation));
        byCreation.limit = 0;
        assertRefused(Response.Status.BAD_REQUEST, "Search limit must be at least 1", () -> this.store.searchCubes(byCreation));
    }

    @Test
    public void testSearchReturnsAtMostAHundred()
    {
        for (int i = 0; i < CubeStoreManager.MAX_SEARCH_RESULTS + 5; i++)
        {
            this.store.createCube(newCube("Cube " + i, CONTENT), ALICE);
        }
        CubeSearchSpecification specification = new CubeSearchSpecification();
        specification.limit = 1000;
        Assert.assertEquals(CubeStoreManager.MAX_SEARCH_RESULTS, this.store.searchCubes(specification).size());
    }

    // --------------------------------------------- the store ---------------------------------------------

    @Test
    public void testIndexesRefuseDuplicates()
    {
        StoredCube created = this.store.createCube(newCube("A", CONTENT), ALICE);
        Assert.assertThrows(MongoWriteException.class, () -> this.collection("cube").insertOne(new Document("id", created.id)));
        Assert.assertThrows(MongoWriteException.class, () -> this.collection("cubeVersion").insertOne(new Document("id", created.id).append("version", 1)));
        this.collection("cubeVersion").insertOne(new Document("id", created.id).append("version", 2));
    }

    @Test
    public void testUnconfiguredStore()
    {
        Vault.INSTANCE.unregisterImplementation(testVaultImplementation);
        try
        {
            RuntimeException exception = Assert.assertThrows(RuntimeException.class, () -> new CubeStoreManager(this.testMongoClientProvider.mongoClient).getCube("a"));
            Assert.assertEquals("Cube store MongoDB database has not been configured properly", exception.getMessage());
        }
        finally
        {
            Vault.INSTANCE.registerImplementation(testVaultImplementation);
        }
    }

    @Test
    public void testErrorMessagesAreEscapedJson() throws Exception
    {
        String message = "Can't find cube 'a\"b\\c'";
        ApplicationQueryException exception = new ApplicationQueryException(message, Response.Status.NOT_FOUND);
        Assert.assertEquals(message, new ObjectMapper().readTree(exception.toJson()).get("message").asText());
    }
}
