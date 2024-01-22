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
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

public class LogicalPlanUtilsTest extends IngestModeTest
{
    @Test
    public void testJsonifyDatasetFilters()
    {
        ObjectMapper objectMapper = new ObjectMapper();

        String ts1 = "2023-01-01 00:00:00.0";
        String ts2 = "2023-01-02 00:00:00.0";

        DatasetFilter filter1 = DatasetFilter.of("id", FilterType.GREATER_THAN_EQUAL, 1);
        DatasetFilter filter2 = DatasetFilter.of("id", FilterType.LESS_THAN_EQUAL, 2);
        DatasetFilter filter3 = DatasetFilter.of("start_time", FilterType.GREATER_THAN_EQUAL, ts1);
        DatasetFilter filter4 = DatasetFilter.of("start_time", FilterType.LESS_THAN_EQUAL, ts2);

        try
        {
            Map<String, Object> batchSourceInfoMap = LogicalPlanUtils.jsonifyStagingFilters(Arrays.asList(filter1));
            String stagingFilters1 = objectMapper.writeValueAsString(batchSourceInfoMap);
            Assertions.assertEquals("{\"staging_filters\":{\"id\":{\"GTE\":1}}}", stagingFilters1);

            TypeReference<Map<String,Map<String,Map<String,Object>>>> typeRef = new TypeReference<Map<String,Map<String,Map<String,Object>>>>() {};

            batchSourceInfoMap = LogicalPlanUtils.jsonifyStagingFilters(Arrays.asList(filter1, filter2));
            String stagingFilters2 = objectMapper.writeValueAsString(batchSourceInfoMap);
            Map<String,Map<String,Map<String,Object>>> map = objectMapper.readValue(stagingFilters2, typeRef);
            Assertions.assertEquals(1, map.get("staging_filters").get("id").get("GTE"));
            Assertions.assertEquals(2, map.get("staging_filters").get("id").get("LTE"));

            batchSourceInfoMap = LogicalPlanUtils.jsonifyStagingFilters(Arrays.asList(filter3, filter4));
            String stagingFilters3 = objectMapper.writeValueAsString(batchSourceInfoMap);
            map = objectMapper.readValue(stagingFilters3, typeRef);
            Assertions.assertEquals(ts1, map.get("staging_filters").get("start_time").get("GTE"));
            Assertions.assertEquals(ts2, map.get("staging_filters").get("start_time").get("LTE"));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
