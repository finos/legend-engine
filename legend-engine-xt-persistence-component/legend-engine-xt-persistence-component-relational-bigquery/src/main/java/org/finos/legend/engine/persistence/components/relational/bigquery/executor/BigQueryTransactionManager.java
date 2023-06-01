// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.bigquery.executor;

import com.google.cloud.bigquery.*;
import org.eclipse.collections.api.factory.Lists;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class BigQueryTransactionManager {
    private final BigQuery bigQuery;
    private String sessionId;
    private static final String CONNECTION_SESSION_PROPERTY = "session_id";

    public BigQueryTransactionManager(BigQuery bigQuery) {
        this.bigQuery = bigQuery;
    }

    public void close() throws InterruptedException {
        if (this.sessionId != null) {
            try {
                executeSql("CALL BQ.ABORT_SESSION();");
            } finally {
                this.sessionId = null;
            }
        }
    }

    public void beginTransaction() throws InterruptedException {
            Job job = this.bigQuery.create(JobInfo.of(QueryJobConfiguration
                    .newBuilder("BEGIN TRANSACTION")
                    .setCreateSession(true)
                    .build()));
            job.waitFor();
            this.sessionId = job.getStatistics().getSessionInfo().getSessionId();
    }

    public void commitTransaction() throws InterruptedException {
        if (this.sessionId != null) {
                executeSql("COMMIT TRANSACTION");
        }
    }

    public void revertTransaction() throws InterruptedException {
        if (this.sessionId != null) {
                executeSql("ROLLBACK TRANSACTION");
        }
    }

    public boolean executeInCurrentTransaction(String sql) throws InterruptedException {
        Job job = this.executeSql(sql);
        return job.getStatus().getError() == null;
    }

    public List<Map<String, Object>> convertResultSetToList(String sql) {
        try {
            List<Map<String, Object>> resultList = new ArrayList<>();
            Job job = this.executeSql(sql);
            List<String> fieldNames = job.getQueryResults().getSchema().getFields().stream().map(Field::getName).collect(Collectors.toList());
            int columnCount = fieldNames.size();
            for (FieldValueList fieldValues : job.getQueryResults().getValues()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    row.put(fieldNames.get(i), fieldValues.get(i));
                }
                resultList.add(row);
            }
            return resultList;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Job executeSql(String sqlQuery) throws InterruptedException {
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job job = this.bigQuery.create(JobInfo.newBuilder(getQueryJobConfiguration(sqlQuery)).setJobId(jobId).build());
        return job.waitFor();
    }

    private QueryJobConfiguration getQueryJobConfiguration(String sqlQuery) {
        if (this.sessionId == null) return QueryJobConfiguration.newBuilder(sqlQuery).setUseLegacySql(false).build();
        return QueryJobConfiguration
                .newBuilder(sqlQuery)
                .setUseLegacySql(false)
                .setCreateSession(false)
                .setConnectionProperties(Lists.mutable.of(ConnectionProperty.newBuilder()
                        .setKey(CONNECTION_SESSION_PROPERTY)
                        .setValue(this.sessionId)
                        .build())).build();
    }
}
