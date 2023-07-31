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

package org.finos.legend.engine.application.query.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.application.query.api.ApplicationQueryException;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QueryEvent.QueryEventType;
import javax.lang.model.SourceVersion;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.regex.Pattern;


public abstract class BaseQueryStoreManager implements QueryStoreManager
{
    protected static final Pattern VALID_ARTIFACT_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*+(-[a-z][a-z0-9_]*+)*+$");

    protected static final int MAX_NUMBER_OF_QUERIES = 100;
    protected static final int MAX_NUMBER_OF_EVENTS = 1000;

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    // NOTE: these are non-compilable profile and tag that we come up with for query
    // so that it records the dataSpace it is created from
    protected static final String QUERY_PROFILE_PATH = "meta::pure::profiles::query";
    protected static final String QUERY_PROFILE_TAG_DATA_SPACE = "dataSpace";
    protected static final int GET_QUERIES_LIMIT = 50;


    protected static QueryEvent createEvent(String queryId, QueryEventType eventType)
    {
        QueryEvent event = new QueryEvent();
        event.queryId = queryId;
        event.timestamp = Instant.now().toEpochMilli();
        event.eventType = eventType;
        return event;
    }

    protected static void validate(boolean predicate, String message)
    {
        if (!predicate)
        {
            throw new ApplicationQueryException(message, Response.Status.BAD_REQUEST);
        }
    }

    protected static void validateNonEmptyQueryField(String fieldValue, String message)
    {
        validate(fieldValue != null && !fieldValue.isEmpty(), message);
    }

    public static void validateQuery(Query query)
    {
        validateNonEmptyQueryField(query.id, "Query ID is missing or empty");
        validateNonEmptyQueryField(query.name, "Query name is missing or empty");
        validateNonEmptyQueryField(query.groupId, "Query project group ID is missing or empty");
        validateNonEmptyQueryField(query.artifactId, "Query project artifact ID is missing or empty");
        validateNonEmptyQueryField(query.versionId, "Query project version is missing or empty");
        validateNonEmptyQueryField(query.mapping, "Query mapping is missing or empty");
        validateNonEmptyQueryField(query.runtime, "Query runtime is missing or empty");
        validateNonEmptyQueryField(query.content, "Query content is missing or empty");

        validate(SourceVersion.isName(query.groupId), "Query project group ID is invalid");
        validate(VALID_ARTIFACT_ID_PATTERN.matcher(query.artifactId).matches(), "Query project artifact ID is invalid");
        // TODO: we can potentially create a pattern check for version
    }

}