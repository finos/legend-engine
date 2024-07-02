// Copyright 2024 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FunctionalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;

public class QueryStatsLogicalPlanUtils
{
    public static final String QUERY_OPERATOR_STATS_FUNCTION = "GET_QUERY_OPERATOR_STATS";
    public static final String QUERY_ID_PARAMETER = "{QUERY_ID}";
    public static final String OPERATOR_TYPE = "OPERATOR_TYPE";
    public static final String OPERATOR_STATISTICS = "OPERATOR_STATISTICS";

    public static final String OPERATOR_TYPE_ALIAS = "operatorType";
    public static final String EXTERNAL_BYTES_SCANNED_ALIAS = "externalBytesScanned";
    public static final String INPUT_ROWS_ALIAS = "inputRows";

    public static final String EXTERNAL_BYTES_SCANNED_PATH = "io.external_bytes_scanned";
    public static final String INPUT_ROWS_PATH = "input_rows";

    public static final String EXTERNAL_SCAN_STAGE = "ExternalScan";
    public static final String INSERT_STAGE = "Insert";

    private QueryStatsLogicalPlanUtils()
    {
    }

    public static LogicalPlan getLogicalPlanForQueryOperatorStats()
    {
        return LogicalPlan.builder()
            .addOps(Selection.builder()
                .addFields(FieldValue.builder().fieldName(OPERATOR_TYPE).alias(OPERATOR_TYPE_ALIAS).build())
                .addFields(FunctionImpl.builder()
                    .functionName(FunctionName.JSON_EXTRACT_PATH_TEXT)
                    .addValue(FieldValue.builder().fieldName(OPERATOR_STATISTICS).build())
                    .addValue(StringValue.of(EXTERNAL_BYTES_SCANNED_PATH))
                    .alias(EXTERNAL_BYTES_SCANNED_ALIAS)
                    .build()
                )
                .addFields(FunctionImpl.builder()
                    .functionName(FunctionName.JSON_EXTRACT_PATH_TEXT)
                    .addValue(FieldValue.builder().fieldName(OPERATOR_STATISTICS).build())
                    .addValue(StringValue.of(INPUT_ROWS_PATH))
                    .alias(INPUT_ROWS_ALIAS)
                    .build()
                )
                .source(FunctionalDataset
                    .builder()
                    .name(QUERY_OPERATOR_STATS_FUNCTION)
                    .addValue(StringValue.of(QUERY_ID_PARAMETER))
                    .build())
                .build())
            .build();
    }
}
