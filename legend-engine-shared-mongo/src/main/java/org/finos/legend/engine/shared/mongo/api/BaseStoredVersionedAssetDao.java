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

package org.finos.legend.engine.shared.mongo.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.mongo.model.StoredAuditInformation;
import org.finos.legend.engine.shared.mongo.model.StoredVersionedAssetContent;
import org.finos.legend.engine.shared.mongo.util.StoredVersionedAssetFetchOptions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Base implementation of StoredVersionedAssetDao for MongoDB.
 * Provides CRUD operations with versioning and audit support.
 *
 * @param <T> The asset type (must implement StoredVersionedAssetContent)
 * @param <I> The ID type
 */
public abstract class BaseStoredVersionedAssetDao<T extends StoredVersionedAssetContent<I>, I> implements StoredVersionedAssetDao<T, I>
{
    public static final LocalDateTime INFINITY = LocalDateTime.of(9999, 12, 31, 0, 0, 0);
    protected static final String INFINITY_STRING = "9999-12-31T00:00:00";

    protected static final ObjectMapper OBJECT_MAPPER = PureProtocolObjectMapperFactory.getNewObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    protected final MongoCollection<Document> collection;
    protected final Class<T> type;

    public BaseStoredVersionedAssetDao(MongoClient client, String database, String collectionName, Class<T> type)
    {
        this.collection = client.getDatabase(database).getCollection(collectionName);
        this.type = type;
    }

    @Override
    public Optional<T> get(I id)
    {
        return get(id, StoredVersionedAssetFetchOptions.standard());
    }

    @Override
    public Optional<T> get(I id, StoredVersionedAssetFetchOptions options)
    {
        Document item = createFind(latestFilter(id), options).first();
        return Optional.ofNullable(convertFromDocument(item));
    }

    @Override
    public Optional<T> get(I id, Integer version)
    {
        return get(id, version, StoredVersionedAssetFetchOptions.standard());
    }

    @Override
    public Optional<T> get(I id, Integer version, StoredVersionedAssetFetchOptions options)
    {
        Bson filter = Filters.and(
                Filters.eq("id", id),
                Filters.eq("audit.version", version)
        );
        Document item = createFind(filter, options).first();
        return Optional.ofNullable(convertFromDocument(item));
    }

    @Override
    public Stream<T> getAll()
    {
        return getAll(StoredVersionedAssetFetchOptions.standard());
    }

    @Override
    public Stream<T> getAll(StoredVersionedAssetFetchOptions options)
    {
        return StreamSupport.stream(createFind(latestFilter(), options).spliterator(), false)
                .map(this::convertFromDocument);
    }

    @Override
    public Stream<T> getHistory(I id)
    {
        return getHistory(id, StoredVersionedAssetFetchOptions.standard());
    }

    @Override
    public Stream<T> getHistory(I id, StoredVersionedAssetFetchOptions options)
    {
        Bson filter = Filters.and(
                Filters.eq("id", id),
                Filters.lt("audit.validUntil", INFINITY_STRING)
        );
        return StreamSupport.stream(createFind(filter, options).spliterator(), false)
                .map(this::convertFromDocument);
    }

    @Override
    public Stream<T> find(Map<String, Object> values, boolean includeHistory, boolean all)
    {
        return find(values, includeHistory, all, StoredVersionedAssetFetchOptions.standard());
    }

    @Override
    public Stream<T> find(Map<String, Object> values, boolean includeHistory, boolean all, StoredVersionedAssetFetchOptions options)
    {
        MutableMap<String, Bson> filters = MapIterate.collectValues(values, (k, v) -> v instanceof Iterable
                ? Filters.in(k, FastList.newList((Iterable<?>) v).toArray())
                : Filters.eq(k, v));

        Bson filter = all ? Filters.and(filters) : Filters.or(filters);

        if (!values.isEmpty())
        {
            Bson finalFilter = includeHistory ? filter : Filters.and(latestFilter(), filter);
            return StreamSupport.stream(createFind(finalFilter, options).spliterator(), false)
                    .map(this::convertFromDocument);
        }
        return getAll(options);
    }

    public Stream<T> find(Bson filter, boolean includeHistory, StoredVersionedAssetFetchOptions options)
    {
        if (!filter.toBsonDocument().isEmpty())
        {
            Bson finalFilter = includeHistory ? filter : Filters.and(latestFilter(), filter);
            return StreamSupport.stream(createFind(finalFilter, options).spliterator(), false)
                    .map(this::convertFromDocument);
        }
        return getAll(options);
    }

    @Override
    public T create(T item, String user)
    {
        auditOnCreate(item, user);
        Document document = convertToDocument(item);
        this.collection.insertOne(document);
        return item;
    }

    @Override
    public T update(I id, T item, String user)
    {
        return update(id, item, user, true);
    }

    @Override
    public T update(I id, T item, String user, boolean createNewVersion)
    {
        T existing = get(id).orElse(null);
        if (existing != null)
        {
            if (createNewVersion)
            {
                // Mark old version as no longer valid
                existing.getAudit().setValidUntil(LocalDateTime.now());
                this.collection.findOneAndReplace(latestFilter(id), convertToDocument(existing));

                // Create new version
                auditOnUpdate(existing, item, user);
                this.collection.insertOne(convertToDocument(item));
            }
            else
            {
                // Just update in place without versioning
                this.collection.findOneAndReplace(latestFilter(id), convertToDocument(item));
            }
            return item;
        }
        return null;
    }

    @Override
    public boolean delete(I id, String user)
    {
        T existing = get(id).orElse(null);
        if (existing != null)
        {
            auditOnDelete(existing, user);
            this.collection.findOneAndReplace(latestFilter(id), convertToDocument(existing));
            return true;
        }
        return false;
    }

    protected void auditOnCreate(T item, String user)
    {
        LocalDateTime now = LocalDateTime.now();
        StoredAuditInformation audit = item.getAudit();
        if (audit == null)
        {
            audit = new StoredAuditInformation();
            item.setAudit(audit);
        }
        audit.setCreatedAt(now);
        audit.setCreatedBy(user);
        audit.setUpdatedAt(now);
        audit.setUpdatedBy(user);
        audit.setVersion(1);
        audit.setValidUntil(INFINITY);
    }

    protected void auditOnUpdate(T priorVersion, T updatedVersion, String user)
    {
        LocalDateTime now = LocalDateTime.now();
        StoredAuditInformation priorAudit = priorVersion.getAudit();
        StoredAuditInformation updatedAudit = updatedVersion.getAudit();
        if (updatedAudit == null)
        {
            updatedAudit = new StoredAuditInformation();
            updatedVersion.setAudit(updatedAudit);
        }
        updatedAudit.setCreatedAt(priorAudit.getCreatedAt());
        updatedAudit.setCreatedBy(priorAudit.getCreatedBy() != null ? priorAudit.getCreatedBy() : user);
        updatedAudit.setUpdatedAt(now);
        updatedAudit.setUpdatedBy(user);
        updatedAudit.setValidUntil(INFINITY);
        updatedAudit.setVersion(priorAudit.getVersion() + 1);
    }

    protected void auditOnDelete(T item, String user)
    {
        LocalDateTime now = LocalDateTime.now();
        StoredAuditInformation audit = item.getAudit();
        audit.setDeletedAt(now);
        audit.setDeletedBy(user);
        audit.setValidUntil(now);
    }

    protected FindIterable<Document> createFind(Bson filter, StoredVersionedAssetFetchOptions options)
    {
        Bson projection = Projections.exclude(FastList.newList(options.getExcludeFields()));
        Bson sort = options.getSorts().isEmpty()
                ? null
                : Sorts.orderBy(ListIterate.collect(options.getSorts(), s -> s.isDescending() ? Sorts.descending(s.getField()) : Sorts.ascending(s.getField())));

        return this.collection.find(filter).batchSize(options.getBatch()).sort(sort).limit(options.getLimit()).skip(options.getSkip()).projection(projection);
    }

    protected Bson latestFilter(I id)
    {
        return Filters.and(
                Filters.eq("id", id),
                latestFilter()
        );
    }

    protected Bson latestFilter()
    {
        return Filters.eq("audit.validUntil", INFINITY_STRING);
    }

    protected Document convertToDocument(T item)
    {
        try
        {
            return Document.parse(OBJECT_MAPPER.writeValueAsString(item));
        }
        catch (JsonProcessingException e)
        {
            throw new EngineException("Error converting to Document", e);
        }
    }

    protected T convertFromDocument(Document document)
    {
        if (document == null)
        {
            return null;
        }
        return OBJECT_MAPPER.convertValue(document, this.type);
    }
}

