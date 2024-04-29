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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TableNameGenUtilsTest
{
    @Test
    public void testTableNameGen()
    {
        String ingestRunId = "075605e3-bada-47d7-9ae9-7138f392fe22";
        String expectedTableName = "person_temp_lp_yosulf";
        String tableName = TableNameGenUtils.generateTableName("person", "temp", ingestRunId);
        Assertions.assertEquals(expectedTableName, tableName);
    }
}
