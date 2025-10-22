// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

/*
 * A class to encapsulate compilation-related mappings for tables and schemas.
 */
public final class RelationalCompileState
{
    private static final Map<PureModel, RelationalCompileState> BY_MODEL = new WeakHashMap<>();
    private final ConcurrentHashMap<TablePtr, TablePtr> tableMappings = new ConcurrentHashMap<>();
    public final Map<TablePtrCacheKey, TablePtr> tablePtrResolutionCache = new ConcurrentHashMap<>();

    public static RelationalCompileState of(PureModel pureModel)
    {
        synchronized (BY_MODEL)
        {
            return BY_MODEL.computeIfAbsent(pureModel, k -> new RelationalCompileState());
        }
    }

    private RelationalCompileState()
    {
    }

    public void addTableMapping(TablePtr oldPtr, TablePtr newPtr)
    {
        this.tableMappings.put(oldPtr, newPtr);
    }

    public TablePtr getTableMapping(TablePtr oldPtr)
    {
        return this.tableMappings.get(oldPtr);
    }

    static class TablePtrCacheKey
    {
        private final String databasePath;
        private final TablePtr inputTablePtr;

        public TablePtrCacheKey(String databasePath, TablePtr tableptr)
        {
            this.databasePath = databasePath;
            this.inputTablePtr = tableptr;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            TablePtrCacheKey tablePtrKey = (TablePtrCacheKey) o;
            return Objects.equals(databasePath, tablePtrKey.databasePath) && Objects.equals(inputTablePtr, tablePtrKey.inputTablePtr);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(databasePath, inputTablePtr);
        }
    }
}