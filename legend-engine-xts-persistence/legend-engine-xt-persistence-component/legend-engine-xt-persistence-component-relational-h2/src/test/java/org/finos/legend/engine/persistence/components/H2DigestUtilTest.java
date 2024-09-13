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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class H2DigestUtilTest
{
    private String expectedDigest = "fd40b241c6d2eb55348e3bc51e81925b";
    private String[] columns = new String[]{"COLUMN_1", "test data", "COLUMN_2", "true", "COLUMN_3", "33", "COLUMN_4", "1111", "COLUMN_5", "1.5", "COLUMN_6", null};

    @Test
    void testMD5()
    {
        Assertions.assertEquals(expectedDigest, H2DigestUtil.MD5(columns));
    }
}
