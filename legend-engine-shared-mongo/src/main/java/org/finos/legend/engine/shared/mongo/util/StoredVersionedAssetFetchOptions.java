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

package org.finos.legend.engine.shared.mongo.util;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ArrayIterate;

import java.util.List;
import java.util.Set;

public class StoredVersionedAssetFetchOptions
{
    private static final int DEFAULT_BATCH = 10000;
    private static final int DEFAULT_LIMIT = 0; //0 being equivalent to no limit

    private final Set<String> excludeFields;
    private final int batch;
    private final int limit;
    private final List<Sort> sorts = FastList.newList();
    private final int skip;

    private StoredVersionedAssetFetchOptions(Set<String> excludeFields, int batch, int limit, List<Sort> sorts, int skip)
    {
        this.excludeFields = excludeFields;
        this.batch = batch;
        this.limit = limit;
        if (sorts != null)
        {
            this.sorts.addAll(sorts);
        }
        this.skip = skip;
    }

    public Set<String> getExcludeFields()
    {
        return excludeFields;
    }

    public int getBatch()
    {
        return batch;
    }

    public int getLimit()
    {
        return limit;
    }

    public int getSkip()
    {
        return skip;
    }

    public List<Sort> getSorts()
    {
        return sorts;
    }

    public static StoredVersionedAssetFetchOptions standard()
    {
        return new StoredVersionedAssetFetchOptions(Sets.mutable.empty(), DEFAULT_BATCH, DEFAULT_LIMIT, FastList.newList(), DEFAULT_LIMIT);
    }

    public static StoredAssetFetchOptionsBuilder builder()
    {
        return new StoredAssetFetchOptionsBuilder();
    }

    public static class StoredAssetFetchOptionsBuilder
    {
        private int batch = DEFAULT_BATCH;
        private int limit = DEFAULT_LIMIT;
        private Set<String> excludeFields = Sets.mutable.empty();
        private List<Sort> sorts = FastList.newList();
        private int skip = DEFAULT_LIMIT;

        public StoredAssetFetchOptionsBuilder withBatch(int batch)
        {
            this.batch = batch;
            return this;
        }

        public StoredAssetFetchOptionsBuilder withLimit(int limit)
        {
            this.limit = limit;
            return this;
        }

        public StoredAssetFetchOptionsBuilder withSkip(int from)
        {
            this.skip = from;
            return this;
        }

        public StoredAssetFetchOptionsBuilder withSorts(List<Sort> sorts)
        {
            this.sorts = sorts;
            return this;
        }

        public StoredAssetFetchOptionsBuilder sortAsc(String... fields)
        {
            this.sorts.addAll(ArrayIterate.collect(fields, f -> new Sort(f, false)));
            return this;
        }

        public StoredAssetFetchOptionsBuilder sortDesc(String... fields)
        {
            this.sorts.addAll(ArrayIterate.collect(fields, f -> new Sort(f, true)));
            return this;
        }

        public StoredAssetFetchOptionsBuilder withExcludeFields(List<String> fields)
        {
            this.excludeFields.addAll(fields);
            return this;
        }

        public StoredVersionedAssetFetchOptions build()
        {
            return new StoredVersionedAssetFetchOptions(excludeFields, batch, limit, sorts, skip);
        }
    }

    public static class Sort
    {
        private final String field;
        private final boolean descending;

        private Sort(String field)
        {
            this(field, false);
        }

        private Sort(String field, boolean descending)
        {
            this.field = field;
            this.descending = descending;
        }

        public String getField()
        {
            return field;
        }

        public boolean isDescending()
        {
            return descending;
        }
    }
}