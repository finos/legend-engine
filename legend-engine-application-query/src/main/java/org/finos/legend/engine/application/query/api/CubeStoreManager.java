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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.application.query.model.CubeSearchSpecification;
import org.finos.legend.engine.application.query.model.CubeSourceRef;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QuerySearchSortBy;
import org.finos.legend.engine.application.query.model.StoredCube;
import org.finos.legend.engine.application.query.model.StoredCubeAudit;
import org.finos.legend.engine.application.query.model.StoredCubeDeletion;
import org.finos.legend.engine.shared.core.vault.Vault;

import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The Cube store: Legend Cube's saved cubes, apart from Legend Query's and
 * Data Cube's. Three collections: the latest version of each cube (unique by
 * id), every saved version (unique by id and version) and the events.
 * <p>
 * A change sends the version it read, and is made by one atomic update
 * filtered on it, so a cube changed since is refused (409) rather than
 * overwritten. Only a cube's owners may change it (403). Ids are made by the
 * store, so a deleted cube's id is never reused.
 */
public class CubeStoreManager
{
    static final int MAX_SEARCH_RESULTS = 100;
    static final int MAX_CONTENT_BYTES = 1024 * 1024;
    static final int MAX_TEXT_LENGTH = 255;
    static final int MAX_OWNERS = 100;
    static final int MAX_TAGS = 100;
    static final int MAX_SOURCE_REFS = 200;
    static final int MAX_SOURCE_REF_FIELD_LENGTH = 1024;

    private static final String DATABASE_KEY = "query.mongo.database";
    private static final String CUBE_COLLECTION_KEY = "query.mongo.collection.cube";
    private static final String CUBE_VERSION_COLLECTION_KEY = "query.mongo.collection.cubeVersion";
    private static final String CUBE_EVENT_COLLECTION_KEY = "query.mongo.collection.cubeEvent";

    private static final String VERSION_FIELD = "version";
    private static final String NAME_FIELD = "name";
    private static final String OWNERS_FIELD = "owners";
    private static final String TAGS_FIELD = "tags";
    private static final Set<String> PATCH_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(VERSION_FIELD, NAME_FIELD, OWNERS_FIELD, TAGS_FIELD)));

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final FindOneAndUpdateOptions RETURN_UPDATED = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

    private final MongoClient mongoClient;
    private final Clock clock;
    private volatile boolean indexesCreated = false;

    public CubeStoreManager(MongoClient mongoClient)
    {
        this(mongoClient, Clock.systemUTC());
    }

    CubeStoreManager(MongoClient mongoClient, Clock clock)
    {
        this.mongoClient = mongoClient;
        this.clock = clock;
    }

    // --------------------------------------------- collections ---------------------------------------------

    private static String getVaultValue(String key, String what)
    {
        if (Vault.INSTANCE.hasValue(key))
        {
            return Vault.INSTANCE.getValue(key);
        }
        throw new RuntimeException("Cube store MongoDB " + what + " has not been configured properly");
    }

    private MongoDatabase getDatabase()
    {
        return this.mongoClient.getDatabase(getVaultValue(DATABASE_KEY, "database"));
    }

    private MongoCollection<Document> getCollection(String key, String what)
    {
        MongoCollection<Document> collection = this.getDatabase().getCollection(getVaultValue(key, what));
        this.createIndexes();
        return collection;
    }

    private MongoCollection<Document> getCubeCollection()
    {
        return this.getCollection(CUBE_COLLECTION_KEY, "collection");
    }

    private MongoCollection<Document> getVersionCollection()
    {
        return this.getCollection(CUBE_VERSION_COLLECTION_KEY, "version collection");
    }

    private MongoCollection<Document> getEventCollection()
    {
        return this.getCollection(CUBE_EVENT_COLLECTION_KEY, "event collection");
    }

    /**
     * The unique indexes the store's guarantees rest on, made when it is first used
     */
    private void createIndexes()
    {
        if (this.indexesCreated)
        {
            return;
        }
        synchronized (this)
        {
            if (!this.indexesCreated)
            {
                MongoDatabase database = this.getDatabase();
                database.getCollection(getVaultValue(CUBE_COLLECTION_KEY, "collection"))
                        .createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
                database.getCollection(getVaultValue(CUBE_VERSION_COLLECTION_KEY, "version collection"))
                        .createIndex(Indexes.ascending("id", VERSION_FIELD), new IndexOptions().unique(true));
                this.indexesCreated = true;
            }
        }
    }

    // --------------------------------------------- conversions ---------------------------------------------

    private static Document toDocument(Object value)
    {
        try
        {
            return Document.parse(OBJECT_MAPPER.writeValueAsString(value));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException("Unable to write a cube to the store: " + e.getMessage(), e);
        }
    }

    private static List<Document> toDocuments(List<?> values)
    {
        List<Document> documents = new ArrayList<>();
        values.forEach(value -> documents.add(toDocument(value)));
        return documents;
    }

    private static StoredCube toCube(Document document)
    {
        Document copy = new Document(document);
        copy.remove("_id");
        return OBJECT_MAPPER.convertValue(copy, StoredCube.class);
    }

    /**
     * What a version records: the cube as saved, without when it was opened or deleted
     */
    private static Document toVersionDocument(StoredCube cube)
    {
        Document document = toDocument(cube);
        document.remove("lastOpenAt");
        document.remove("deleted");
        return document;
    }

    private void recordVersion(StoredCube cube)
    {
        this.getVersionCollection().insertOne(toVersionDocument(cube));
    }

    private void recordEvent(String cubeId, QueryEvent.QueryEventType eventType, long timestamp)
    {
        QueryEvent event = new QueryEvent();
        event.queryId = cubeId;
        event.eventType = eventType;
        event.timestamp = timestamp;
        this.getEventCollection().insertOne(toDocument(event));
    }

    // --------------------------------------------- validation ---------------------------------------------

    private static ApplicationQueryException badRequest(String message)
    {
        return new ApplicationQueryException(message, Response.Status.BAD_REQUEST);
    }

    private static void validate(boolean predicate, String message)
    {
        if (!predicate)
        {
            throw badRequest(message);
        }
    }

    private static void requireUser(String currentUser)
    {
        if (currentUser == null || currentUser.isEmpty())
        {
            throw new ApplicationQueryException("A cube can only be changed by a signed-in user", Response.Status.UNAUTHORIZED);
        }
    }

    private static void validateVersionRead(Integer version)
    {
        validate(version != null && version >= 1, "Cube version is missing: send the version that was read");
    }

    private static String validateName(Object name)
    {
        validate(name == null || name instanceof String, "Cube name must be a string");
        String trimmed = name == null ? "" : ((String) name).trim();
        validate(!trimmed.isEmpty(), "Cube name is missing or empty");
        validate(trimmed.length() <= MAX_TEXT_LENGTH, "Cube name is longer than " + MAX_TEXT_LENGTH + " characters");
        return trimmed;
    }

    /**
     * The content must be a JSON object of at most 1 MiB, repeating the document's format version
     */
    static void validateContent(Integer formatVersion, String content)
    {
        validate(formatVersion != null && formatVersion >= 1, "Cube format version is missing, or not a whole number of at least 1");
        validate(content != null, "Cube content is missing");
        validate(content.getBytes(StandardCharsets.UTF_8).length <= MAX_CONTENT_BYTES, "Cube content is larger than " + MAX_CONTENT_BYTES + " bytes");
        JsonNode node;
        try
        {
            node = OBJECT_MAPPER.readTree(content);
        }
        catch (JsonProcessingException e)
        {
            throw badRequest("Cube content is not JSON: " + e.getOriginalMessage());
        }
        validate(node != null && node.isObject(), "Cube content is not a JSON object");
        JsonNode contentVersion = node.get("formatVersion");
        validate(contentVersion != null && contentVersion.isIntegralNumber() && contentVersion.asLong() == formatVersion, "Cube content's formatVersion must be the cube's, " + formatVersion);
    }

    /**
     * Trimmed, none empty, none longer than 255 characters, each once, in the order given
     */
    private static List<String> validateTexts(Object values, String what, int max)
    {
        if (values == null)
        {
            return new ArrayList<>();
        }
        validate(values instanceof List, "Cube " + what + "s must be a list");
        Set<String> texts = new LinkedHashSet<>();
        for (Object value : (List<?>) values)
        {
            validate(value instanceof String && !((String) value).trim().isEmpty(), "Cube " + what + "s can't be empty");
            String text = ((String) value).trim();
            validate(text.length() <= MAX_TEXT_LENGTH, "Cube " + what + " '" + text + "' is longer than " + MAX_TEXT_LENGTH + " characters");
            texts.add(text);
        }
        validate(texts.size() <= max, "A cube can't have more than " + max + " " + what + "s");
        return new ArrayList<>(texts);
    }

    private static void validateSourceRefField(String value, String field, boolean required)
    {
        if (value == null)
        {
            validate(!required, "Cube source reference " + field + " is missing");
            return;
        }
        validate(!value.isEmpty(), "Cube source reference " + field + " is empty");
        validate(value.length() <= MAX_SOURCE_REF_FIELD_LENGTH, "Cube source reference " + field + " is longer than " + MAX_SOURCE_REF_FIELD_LENGTH + " characters");
    }

    private static List<CubeSourceRef> validateSourceRefs(List<CubeSourceRef> sourceRefs)
    {
        if (sourceRefs == null)
        {
            return new ArrayList<>();
        }
        validate(sourceRefs.size() <= MAX_SOURCE_REFS, "A cube can't have more than " + MAX_SOURCE_REFS + " source references");
        sourceRefs.forEach(sourceRef ->
        {
            validate(sourceRef != null, "Cube source references can't be empty");
            validateSourceRefField(sourceRef.type, "type", true);
            validateSourceRefField(sourceRef.path, "path", true);
            validateSourceRefField(sourceRef.element, "element", false);
            validateSourceRefField(sourceRef.project, "project", false);
        });
        return sourceRefs;
    }

    // --------------------------------------------- refusals ---------------------------------------------

    private static ApplicationQueryException notFound(String cubeId)
    {
        return new ApplicationQueryException("Can't find cube '" + cubeId + "'", Response.Status.NOT_FOUND);
    }

    /**
     * Filters the one cube a change may be made to: this id, at the version read, with the caller among its owners
     */
    private static Bson changeableCube(String cubeId, Integer version, String currentUser, boolean includeDeleted)
    {
        List<Bson> filters = new ArrayList<>(Arrays.asList(Filters.eq("id", cubeId), Filters.eq(VERSION_FIELD, version), Filters.eq(OWNERS_FIELD, currentUser)));
        if (!includeDeleted)
        {
            filters.add(Filters.exists("deleted", false));
        }
        return Filters.and(filters);
    }

    /**
     * Why a change matched no cube, read after it: the cube is missing, the caller isn't an owner, or it changed since it was read
     */
    private ApplicationQueryException explainRefusal(String cubeId, Integer version, String currentUser, boolean includeDeleted)
    {
        Document document = this.getCubeCollection().find(Filters.eq("id", cubeId)).first();
        if (document == null || (!includeDeleted && document.containsKey("deleted")))
        {
            return notFound(cubeId);
        }
        StoredCube current = toCube(document);
        if (current.owners == null || !current.owners.contains(currentUser))
        {
            return new ApplicationQueryException("Only current owners can change this cube. Current owners are " + String.join(", ", current.owners == null ? Collections.emptyList() : current.owners) + ".", Response.Status.FORBIDDEN);
        }
        if (!version.equals(current.version))
        {
            return new ApplicationQueryException("Cannot change version " + version + " of cube '" + cubeId + "': its current version is " + current.version + ".", Response.Status.CONFLICT);
        }
        return new ApplicationQueryException("Cube '" + cubeId + "' changed while this change was made: read it again", Response.Status.CONFLICT);
    }

    // --------------------------------------------- reads ---------------------------------------------

    /**
     * The latest version, recording when it was opened with $set, never by rewriting the cube
     */
    public StoredCube getCube(String cubeId)
    {
        Document document = this.getCubeCollection().findOneAndUpdate(
                Filters.and(Filters.eq("id", cubeId), Filters.exists("deleted", false)),
                Updates.set("lastOpenAt", this.clock.millis()),
                RETURN_UPDATED);
        if (document == null)
        {
            throw notFound(cubeId);
        }
        return toCube(document);
    }

    public StoredCube getCubeVersion(String cubeId, int version)
    {
        if (this.getCubeCollection().find(Filters.and(Filters.eq("id", cubeId), Filters.exists("deleted", false))).first() == null)
        {
            throw notFound(cubeId);
        }
        Document document = this.getVersionCollection().find(Filters.and(Filters.eq("id", cubeId), Filters.eq(VERSION_FIELD, version))).first();
        if (document == null)
        {
            throw new ApplicationQueryException("Can't find version " + version + " of cube '" + cubeId + "'", Response.Status.NOT_FOUND);
        }
        return toCube(document);
    }

    private static String getSortField(QuerySearchSortBy sortBy)
    {
        if (sortBy == null)
        {
            return "audit.updatedAt";
        }
        switch (sortBy)
        {
            case SORT_BY_CREATE:
                return "audit.createdAt";
            case SORT_BY_VIEW:
                return "lastOpenAt";
            case SORT_BY_UPDATE:
                return "audit.updatedAt";
            default:
                throw badRequest("Unknown sort: " + sortBy);
        }
    }

    /**
     * Cubes not deleted, without their content, the latest saved first
     */
    public List<StoredCube> searchCubes(CubeSearchSpecification searchSpecification)
    {
        CubeSearchSpecification specification = searchSpecification == null ? new CubeSearchSpecification() : searchSpecification;
        validate(specification.limit == null || specification.limit >= 1, "Search limit must be at least 1");
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.exists("deleted", false));
        if (specification.searchTerm != null && !specification.searchTerm.trim().isEmpty())
        {
            String term = specification.searchTerm.trim();
            String pattern = Pattern.quote(term);
            filters.add(Filters.or(Filters.eq("id", term), Filters.regex(NAME_FIELD, pattern, "i"), Filters.regex(OWNERS_FIELD, pattern, "i")));
        }
        if (specification.owner != null)
        {
            filters.add(Filters.eq(OWNERS_FIELD, specification.owner));
        }
        if (specification.creator != null)
        {
            filters.add(Filters.eq("audit.createdBy", specification.creator));
        }
        if (specification.tags != null && !specification.tags.isEmpty())
        {
            filters.add(Filters.all(TAGS_FIELD, specification.tags));
        }
        CubeSourceRef sourceRef = specification.sourceRef;
        if (sourceRef != null)
        {
            List<Bson> sourceFilters = new ArrayList<>();
            if (sourceRef.type != null)
            {
                sourceFilters.add(Filters.eq("type", sourceRef.type));
            }
            if (sourceRef.path != null)
            {
                sourceFilters.add(Filters.eq("path", sourceRef.path));
            }
            if (sourceRef.element != null)
            {
                sourceFilters.add(Filters.eq("element", sourceRef.element));
            }
            if (sourceRef.project != null)
            {
                sourceFilters.add(Filters.eq("project", sourceRef.project));
            }
            validate(!sourceFilters.isEmpty(), "A search by source needs at least one of its type, path, element or project");
            filters.add(Filters.elemMatch("sourceRefs", Filters.and(sourceFilters)));
        }
        int limit = Math.min(MAX_SEARCH_RESULTS, specification.limit == null ? MAX_SEARCH_RESULTS : specification.limit);
        return LazyIterate.collect(this.getCubeCollection()
                .find(Filters.and(filters))
                .projection(Projections.exclude("_id", "content"))
                .sort(Sorts.orderBy(Sorts.descending(getSortField(specification.sortByOption)), Sorts.ascending("id")))
                .limit(limit), CubeStoreManager::toCube).toList();
    }

    // --------------------------------------------- changes ---------------------------------------------

    /**
     * A new cube at version 1, with an id the store makes; the caller is its first owner
     */
    public StoredCube createCube(StoredCube cube, String currentUser)
    {
        requireUser(currentUser);
        validate(cube != null, "Cube is missing");
        validate(cube.id == null, "A new cube's id is made by the store: send none");
        String name = validateName(cube.name);
        validateContent(cube.formatVersion, cube.content);
        List<String> givenOwners = validateTexts(cube.owners, "owner", MAX_OWNERS);
        Set<String> owners = new LinkedHashSet<>();
        owners.add(currentUser);
        owners.addAll(givenOwners);
        validate(owners.size() <= MAX_OWNERS, "A cube can't have more than " + MAX_OWNERS + " owners");
        long now = this.clock.millis();

        StoredCube created = new StoredCube();
        created.id = UUID.randomUUID().toString();
        created.version = 1;
        created.name = name;
        created.owners = new ArrayList<>(owners);
        created.tags = validateTexts(cube.tags, "tag", MAX_TAGS);
        created.formatVersion = cube.formatVersion;
        created.content = cube.content;
        created.sourceRefs = validateSourceRefs(cube.sourceRefs);
        created.audit = new StoredCubeAudit();
        created.audit.createdBy = currentUser;
        created.audit.createdAt = now;
        created.audit.updatedBy = currentUser;
        created.audit.updatedAt = now;
        created.lastOpenAt = now;

        this.getCubeCollection().insertOne(toDocument(created));
        this.recordVersion(created);
        this.recordEvent(created.id, QueryEvent.QueryEventType.CREATED, now);
        return created;
    }

    private StoredCube change(String cubeId, Integer version, String currentUser, List<Bson> updates, long now)
    {
        List<Bson> allUpdates = new ArrayList<>(updates);
        allUpdates.add(Updates.set("audit.updatedBy", currentUser));
        allUpdates.add(Updates.set("audit.updatedAt", now));
        allUpdates.add(Updates.inc(VERSION_FIELD, 1));
        Document document = this.getCubeCollection().findOneAndUpdate(changeableCube(cubeId, version, currentUser, false), Updates.combine(allUpdates), RETURN_UPDATED);
        if (document == null)
        {
            throw this.explainRefusal(cubeId, version, currentUser, false);
        }
        StoredCube changed = toCube(document);
        this.recordVersion(changed);
        this.recordEvent(cubeId, QueryEvent.QueryEventType.UPDATED, now);
        return changed;
    }

    /**
     * Saves new content, and the name, over the version read: a newer version is a conflict (409).
     * Owners and tags are changed with {@link #patchCube}.
     */
    public StoredCube updateCube(String cubeId, StoredCube cube, String currentUser)
    {
        requireUser(currentUser);
        validate(cube != null, "Cube is missing");
        validateVersionRead(cube.version);
        validate(cube.id == null || cube.id.equals(cubeId), "A cube's id can't be changed");
        validate(cube.owners == null && cube.tags == null, "A cube's owners and tags are changed with PATCH");
        String name = validateName(cube.name);
        validateContent(cube.formatVersion, cube.content);
        List<CubeSourceRef> sourceRefs = validateSourceRefs(cube.sourceRefs);
        return this.change(cubeId, cube.version, currentUser, Arrays.asList(
                Updates.set(NAME_FIELD, name),
                Updates.set("formatVersion", cube.formatVersion),
                Updates.set("content", cube.content),
                Updates.set("sourceRefs", toDocuments(sourceRefs))), this.clock.millis());
    }

    /**
     * Changes a cube's name, owners or tags, and nothing else, over the version read
     */
    public StoredCube patchCube(String cubeId, Map<String, Object> patch, String currentUser)
    {
        requireUser(currentUser);
        validate(patch != null, "Cube patch is missing");
        patch.keySet().forEach(field -> validate(PATCH_FIELDS.contains(field), "Unknown field '" + field + "': only a cube's name, owners and tags can be patched"));
        Object version = patch.get(VERSION_FIELD);
        validate(version == null || version instanceof Integer, "Cube version must be a whole number");
        validateVersionRead((Integer) version);
        List<Bson> updates = new ArrayList<>();
        if (patch.containsKey(NAME_FIELD))
        {
            updates.add(Updates.set(NAME_FIELD, validateName(patch.get(NAME_FIELD))));
        }
        if (patch.containsKey(OWNERS_FIELD))
        {
            validate(patch.get(OWNERS_FIELD) != null, "Cube owners can't be null");
            List<String> owners = validateTexts(patch.get(OWNERS_FIELD), "owner", MAX_OWNERS);
            validate(!owners.isEmpty(), "A cube needs at least one owner");
            updates.add(Updates.set(OWNERS_FIELD, owners));
        }
        if (patch.containsKey(TAGS_FIELD))
        {
            validate(patch.get(TAGS_FIELD) != null, "Cube tags can't be null");
            updates.add(Updates.set(TAGS_FIELD, validateTexts(patch.get(TAGS_FIELD), "tag", MAX_TAGS)));
        }
        validate(!updates.isEmpty(), "Nothing to change: give a name, owners or tags");
        return this.change(cubeId, (Integer) version, currentUser, updates, this.clock.millis());
    }

    /**
     * Deletes a cube at the version read: softly, so it is no longer read, searched or changed, or for good with its versions
     */
    public void deleteCube(String cubeId, Integer version, boolean permanent, String currentUser)
    {
        requireUser(currentUser);
        validateVersionRead(version);
        long now = this.clock.millis();
        if (permanent)
        {
            if (this.getCubeCollection().findOneAndDelete(changeableCube(cubeId, version, currentUser, true)) == null)
            {
                throw this.explainRefusal(cubeId, version, currentUser, true);
            }
            this.getVersionCollection().deleteMany(Filters.eq("id", cubeId));
        }
        else
        {
            StoredCubeDeletion deletion = new StoredCubeDeletion();
            deletion.by = currentUser;
            deletion.at = now;
            if (this.getCubeCollection().findOneAndUpdate(changeableCube(cubeId, version, currentUser, false), Updates.set("deleted", toDocument(deletion))) == null)
            {
                throw this.explainRefusal(cubeId, version, currentUser, false);
            }
        }
        this.recordEvent(cubeId, QueryEvent.QueryEventType.DELETED, now);
    }
}
