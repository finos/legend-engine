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

package org.finos.legend.engine.application.query.model;

import java.util.List;
import java.util.Map;

/**
 * Result containing all versions of queries for a batch of query IDs.
 * Maps each query ID to its list of versions (including current and historical).
 */
public class QueryVersionsResult
{
    /**
     * Map of query ID to list of all versions for that query.
     * Each list is sorted by version number in descending order (latest first).
     */
    public Map<String, List<Query>> queryVersions;

    public QueryVersionsResult()
    {
    }

    public QueryVersionsResult(Map<String, List<Query>> queryVersions)
    {
        this.queryVersions = queryVersions;
    }
}

