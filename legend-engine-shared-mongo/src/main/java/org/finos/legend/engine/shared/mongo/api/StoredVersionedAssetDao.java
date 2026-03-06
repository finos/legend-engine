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

import org.finos.legend.engine.shared.mongo.model.StoredVersionedAssetContent;
import org.finos.legend.engine.shared.mongo.util.StoredVersionedAssetFetchOptions;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * DAO interface for versioned assets with embedded audit information.
 * @param <T> The asset type (must implement StoredVersionedAssetContent with embedded audit info)
 * @param <I> The ID type
 */
public interface StoredVersionedAssetDao<T extends StoredVersionedAssetContent<I>, I>
{
    Optional<T> get(I id);

    Optional<T> get(I id, StoredVersionedAssetFetchOptions options);

    Optional<T> get(I id, Integer version);

    Optional<T> get(I id, Integer version, StoredVersionedAssetFetchOptions options);

    Stream<T> getAll();

    Stream<T> getAll(StoredVersionedAssetFetchOptions options);

    Stream<T> getHistory(I id);

    Stream<T> getHistory(I id, StoredVersionedAssetFetchOptions options);

    Stream<T> find(Map<String, Object> filter, boolean includeHistory, boolean all);

    Stream<T> find(Map<String, Object> filter, boolean includeHistory, boolean all, StoredVersionedAssetFetchOptions options);

    T create(T item, String user);

    T update(I id, T item, String user);

    T update(I id, T item, String user, boolean audit);

    boolean delete(I id, String user);
}
