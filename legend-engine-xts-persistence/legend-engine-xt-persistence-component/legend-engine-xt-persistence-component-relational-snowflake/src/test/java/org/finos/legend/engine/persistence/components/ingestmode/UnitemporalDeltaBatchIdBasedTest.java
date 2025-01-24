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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.SnowflakeTestArtifacts;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;

public class UnitemporalDeltaBatchIdBasedTest extends org.finos.legend.engine.persistence.components.ingestmode.unitemporal.UnitemporalDeltaBatchIdBasedTest
{

    @Override
    public RelationalSink getRelationalSink()
    {
        return SnowflakeSink.get();
    }

    @Override
    protected String getExpectedMetadataTableIngestQuery()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQuery;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithIngestRequestId()
    {
        return "INSERT INTO batch_metadata " +
                "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"ingest_request_id\", \"batch_statistics\")" +
                " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
                "'2000-01-01 00:00:00.000000',SYSDATE(),'DONE','123456789',PARSE_JSON('{BATCH_STATISTICS_PLACEHOLDER}'))";
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithBatchSuccessValue()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithBatchSuccessValue;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadata()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase;
    }

    protected String getExpectedMetadataTableCreateQuery()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFilters(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
                "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\") " +
                "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
                "'2000-01-01 00:00:00.000000',SYSDATE(),'DONE'," +
                String.format("PARSE_JSON('%s'))", stagingFilters);
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFiltersAndAdditionalMetadata(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\", \"additional_metadata\") " +
            "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
            "'2000-01-01 00:00:00.000000',SYSDATE(),'DONE'," +
            String.format("PARSE_JSON('%s'),", stagingFilters) +
            "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";
    }

    protected String getExpectedMaxDataErrorQueryWithDistinctDigest()
    {
        return SnowflakeTestArtifacts.dataErrorCheckSql;
    }

    protected String getExpectedDataErrorQueryWithDistinctDigest()
    {
        return SnowflakeTestArtifacts.dataErrorsSql;
    }

    protected String getExpectedMaxDataErrorQueryWithDistinctDigestUpperCase()
    {
        return SnowflakeTestArtifacts.dataErrorCheckSqlUpperCase;
    }

    protected String getExpectedDataErrorQueryWithDistinctDigestUpperCase()
    {
        return SnowflakeTestArtifacts.dataErrorsSqlUpperCase;
    }

    protected String getExpectedMaxDataErrorQueryWithDistinctDigestWithBizDateVersion()
    {
        return SnowflakeTestArtifacts.dataErrorCheckSqlWithBizDateVersion;
    }

    protected String getExpectedDataErrorQueryWithDistinctDigestWithBizDateVersion()
    {
        return SnowflakeTestArtifacts.dataErrorsSqlWithBizDateVersion;
    }
}
