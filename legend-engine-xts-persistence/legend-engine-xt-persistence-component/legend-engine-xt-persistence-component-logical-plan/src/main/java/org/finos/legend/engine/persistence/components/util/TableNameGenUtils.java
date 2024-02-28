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

import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.UNDERSCORE;

public class TableNameGenUtils
{
    public static String LEGEND_PERSISTENCE_MARKER = "lp";
    public static final String TEMP_DATASET_QUALIFIER = "temp";
    public static final String TEMP_DATASET_ALIAS = "legend_persistence_temp";
    public static final String TEMP_STAGING_DATASET_QUALIFIER = "temp_staging";
    public static final String TEMP_STAGING_DATASET_ALIAS = "legend_persistence_temp_staging";

    private static String generateTableSuffix(String ingestRunId)
    {
        int hashCode = Math.abs(ingestRunId.hashCode());
        return LEGEND_PERSISTENCE_MARKER + UNDERSCORE + Integer.toString(hashCode, 36);
    }

    /*
    Table name = <base_table>_<qualifier>_lp_<hash>
     */
    public static String generateTableName(String baseTableName, String qualifier, String ingestRunId)
    {
        return baseTableName + UNDERSCORE + qualifier + UNDERSCORE + generateTableSuffix(ingestRunId);
    }
}
