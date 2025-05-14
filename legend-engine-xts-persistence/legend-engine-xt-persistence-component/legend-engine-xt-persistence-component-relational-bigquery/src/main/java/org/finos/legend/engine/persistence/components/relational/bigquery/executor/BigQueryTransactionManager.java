// Copyright 2023 Goldman Sachs
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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.ConnectionProperty;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.finos.legend.engine.persistence.components.common.StatisticName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BigQueryTransactionManager
{
    private final BigQuery bigQuery;
    private String sessionId;
    private static final String CONNECTION_SESSION_PROPERTY = "session_id";

    public BigQueryTransactionManager(BigQuery bigQuery)
    {
        this.bigQuery = bigQuery;
    }

    public void close() throws InterruptedException
    {
        if (this.sessionId != null)
        {
            try
            {
                executeSql("CALL BQ.ABORT_SESSION();");
            }
            finally
            {
                this.sessionId = null;
            }
        }
    }

    public void beginTransaction() throws InterruptedException
    {
        Job job = this.bigQuery.create(JobInfo.of(QueryJobConfiguration
                .newBuilder("BEGIN TRANSACTION")
                .setCreateSession(true)
                .build()));
        job.waitFor();
        this.sessionId = job.getStatistics().getSessionInfo().getSessionId();
    }

    public void commitTransaction() throws InterruptedException
    {
        if (this.sessionId != null)
        {
            executeSql("COMMIT TRANSACTION");
        }
        else
        {
            throw new IllegalStateException("No Transaction started, nothing to commit");
        }
    }

    public void revertTransaction() throws InterruptedException
    {
        if (this.sessionId != null)
        {
            executeSql("ROLLBACK TRANSACTION");
        }
        else
        {
            throw new IllegalStateException("No Transaction started, nothing to revert");
        }
    }

    public boolean executeInCurrentTransaction(String sql) throws InterruptedException
    {
        Job job = this.executeSql(sql);
        return job.getStatus().getError() == null;
    }

    public Map<StatisticName, Object> executeLoadStatement(String sql) throws InterruptedException
    {
        Map<StatisticName, Object> stats = new HashMap<>();

        Job job = this.executeSql(sql);
        JobStatistics.QueryStatistics queryStatistics = job.getStatistics();

        long recordsWritten = queryStatistics.getQueryPlan().get(0).getRecordsWritten();
        long recordsRead = queryStatistics.getQueryPlan().get(0).getRecordsRead();

        stats.put(StatisticName.ROWS_INSERTED, recordsWritten);
        stats.put(StatisticName.ROWS_WITH_ERRORS, recordsRead - recordsWritten);

        return stats;
    }

    public List<Map<String, Object>> convertResultSetToList(String sql)
    {
        try
        {
            List<Map<String, Object>> resultList = new ArrayList<>();
            Job job = this.executeSql(sql);
            for (FieldValueList fieldValues : job.getQueryResults().getValues())
            {
                Map<String, Object> row = new HashMap<>();
                for (Field field: job.getQueryResults().getSchema().getFields())
                {
                    FieldValue value = fieldValues.get(field.getName());
                    Object objectValue;
                    if (value.isNull()) {
                        objectValue = null;
                    }
                    else {
                        switch (field.getType().name())
                        {
                            case "BYTES":
                                objectValue = value.getBytesValue();
                                break;
                            case "STRING":
                                objectValue = value.getStringValue();
                                break;
                            case "INTEGER":
                                objectValue = value.getLongValue();
                                break;
                            case "FLOAT":
                                objectValue = value.getDoubleValue();
                                break;
                            case "NUMERIC":
                            case "BIGNUMERIC":
                                objectValue = value.getNumericValue();
                                break;
                            case "BOOLEAN":
                                objectValue = value.getBooleanValue();
                                break;
                            case "TIMESTAMP":
                                objectValue = value.getTimestampInstant();
                                break;
                            case "RECORD":
                                objectValue = value.getRecordValue();
                                break;
                            default:
                                objectValue = value.getValue();
                        }
                    }
                    String key = field.getName();
                    row.put(key, objectValue);
                }
                resultList.add(row);
            }
            return resultList;
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Job executeSql(String sqlQuery) throws InterruptedException
    {
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job job = this.bigQuery.create(JobInfo.newBuilder(getQueryJobConfiguration(sqlQuery)).setJobId(jobId).build());
        return job.waitFor();
    }

    private QueryJobConfiguration getQueryJobConfiguration(String sqlQuery)
    {
        if (this.sessionId == null)
        {
            return QueryJobConfiguration.newBuilder(sqlQuery).setUseLegacySql(false).build();
        }
        return QueryJobConfiguration
                .newBuilder(sqlQuery)
                .setUseLegacySql(false)
                .setCreateSession(false)
                .setConnectionProperties(Arrays.asList(ConnectionProperty.newBuilder()
                        .setKey(CONNECTION_SESSION_PROPERTY)
                        .setValue(this.sessionId)
                        .build())).build();
    }
}
