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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionComparator;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;

public class LogicalPlanUtilsTest extends IngestModeTest
{
    @Test
    public void testJsonifyDatasetFilters() throws JsonProcessingException
    {
        String ts1 = "2023-01-01 00:00:00.0";
        String ts2 = "2023-01-02 00:00:00.0";

        DatasetFilter filter1 = DatasetFilter.of("id", FilterType.GREATER_THAN_EQUAL, 1);
        DatasetFilter filter2 = DatasetFilter.of("id", FilterType.LESS_THAN_EQUAL, 2);
        DatasetFilter filter3 = DatasetFilter.of("start_time", FilterType.GREATER_THAN_EQUAL, ts1);
        DatasetFilter filter4 = DatasetFilter.of("start_time", FilterType.LESS_THAN_EQUAL, ts2);

        String stagingFilters1 = LogicalPlanUtils.jsonifyDatasetFilters(Arrays.asList(filter1));
        String stagingFilters2 = LogicalPlanUtils.jsonifyDatasetFilters(Arrays.asList(filter1, filter2));
        String stagingFilters3 = LogicalPlanUtils.jsonifyDatasetFilters(Arrays.asList(filter3, filter4));
        Assertions.assertEquals("{\"id\":{\"GTE\":1}}", stagingFilters1);
        TypeReference<Map<String,Map<String,Object>>> typeRef = new TypeReference<Map<String,Map<String,Object>>>() {};
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String,Object>> map = mapper.readValue(stagingFilters2, typeRef);
        Assertions.assertEquals(1, map.get("id").get("GTE"));
        Assertions.assertEquals(2, map.get("id").get("LTE"));

        map = mapper.readValue(stagingFilters3, typeRef);
        Assertions.assertEquals(ts1, map.get("start_time").get("GTE"));
        Assertions.assertEquals(ts2, map.get("start_time").get("LTE"));
    }



}
