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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.application.query.model.QueryStoreStats;

import java.util.List;

public interface QueryStoreManager
{
    Query createQuery(Query query, String currentUser) throws JsonProcessingException;

    void deleteQuery(String queryId, String currentUser) throws JsonProcessingException;

    Query getQuery(String queryId);

    QueryStoreStats getQueryStoreStats() throws JsonProcessingException;

    List<QueryEvent> getQueryEvents(String queryId, QueryEvent.QueryEventType eventType, Long since, Long until, Integer limit);

    List<Query> getQueries(List<String> queryIds);

    Query updateQuery(String queryId, Query query, String currentUser) throws JsonProcessingException;

    List<Query> searchQueries(QuerySearchSpecification searchSpecification, String currentUser);
}
