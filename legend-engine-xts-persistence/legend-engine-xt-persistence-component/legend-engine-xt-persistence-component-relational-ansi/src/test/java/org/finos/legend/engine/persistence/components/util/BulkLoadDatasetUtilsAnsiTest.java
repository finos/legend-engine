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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;

public class BulkLoadDatasetUtilsAnsiTest extends BulkLoadDatasetUtilsTest
{

    public String getExpectedSqlForMetadata()
    {
        return "INSERT INTO bulk_load_batch_metadata " +
                "(\"batch_id\", \"table_name\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\")" +
                " (SELECT (SELECT COALESCE(MAX(bulk_load_batch_metadata.\"batch_id\"),0)+1 FROM bulk_load_batch_metadata as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"table_name\") = 'APPENG_LOG_TABLE_NAME'),'appeng_log_table_name','2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'<BATCH_STATUS_PATTERN>',PARSE_JSON('my_lineage_value'))";
    }

    public String getExpectedSqlForMetadataUpperCase()
    {
        return "INSERT INTO BULK_LOAD_BATCH_METADATA (\"BATCH_ID\", \"TABLE_NAME\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"BATCH_SOURCE_INFO\") " +
                "(SELECT (SELECT COALESCE(MAX(bulk_load_batch_metadata.\"BATCH_ID\"),0)+1 FROM BULK_LOAD_BATCH_METADATA as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"TABLE_NAME\") = 'BULK_LOAD_TABLE_NAME'),'BULK_LOAD_TABLE_NAME','2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'<BATCH_STATUS_PATTERN>',PARSE_JSON('my_lineage_value'))";
    }

    public RelationalSink getRelationalSink()
    {
        return AnsiSqlSink.get();
    }
}
