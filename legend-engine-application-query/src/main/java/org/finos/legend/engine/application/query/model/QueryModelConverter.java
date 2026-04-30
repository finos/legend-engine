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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.application.query.api.ApplicationQueryException;
import org.finos.legend.engine.shared.mongo.model.StoredAuditInformation;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility class to convert between Query (API model) and ApplicationStoredQuery (storage model).
 */
public class QueryModelConverter
{
    private QueryModelConverter()
    {
    }

    /**
     * Convert from storage model to API model.
     */
    public static Query toQuery(ApplicationStoredQuery stored)
    {
        if (stored == null)
        {
            return null;
        }

        Query query;
        try
        {
            query = new ObjectMapper().convertValue(stored, Query.class);
        }
        catch (Exception e)
        {
            throw new ApplicationQueryException("Unable to deserialize stored asset to class '" + Query.class.getName() + "':" + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }


        // Copy audit information to flattened fields
        if (stored.getAudit() != null)
        {
            query.version = stored.getAudit().getVersion();
            query.createdAt = toEpochMilli(stored.getAudit().getCreatedAt());
            query.lastUpdatedAt = toEpochMilli(stored.getAudit().getUpdatedAt());
            query.owner = stored.getAudit().getCreatedBy();
            query.deletedAt = toEpochMilli(stored.getAudit().getDeletedAt());
            query.validUntil = toEpochMilli(stored.getAudit().getValidUntil());
        }

        return query;
    }

    /**
     * Convert from API model to storage model.
     */
    public static ApplicationStoredQuery toStoredQuery(Query query)
    {
        if (query == null)
        {
            return null;
        }

        ApplicationStoredQuery stored = new ApplicationStoredQuery();
        stored.id = query.id;
        stored.name = query.name;
        stored.description = query.description;
        stored.groupId = query.groupId;
        stored.artifactId = query.artifactId;
        stored.versionId = query.versionId;
        stored.originalVersionId = query.originalVersionId;
        stored.executionContext = query.executionContext;
        stored.content = query.content;
        stored.taggedValues = query.taggedValues;
        stored.stereotypes = query.stereotypes;
        stored.defaultParameterValues = query.defaultParameterValues;
        stored.gridConfig = query.gridConfig;
        stored.lastOpenAt = Instant.now().toEpochMilli();

        // Initialize audit with info from query
        stored.audit = StoredAuditInformation.builder()
                .withVersion(query.version)
                .withCreatedAt(toLocalDateTime(query.createdAt != null ? query.createdAt : Instant.now().toEpochMilli()))
                .withCreatedBy(query.owner)
                .build();

        return stored;
    }

    private static Long toEpochMilli(LocalDateTime dateTime)
    {
        if (dateTime == null)
        {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime toLocalDateTime(Long epochMilli)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }
}

