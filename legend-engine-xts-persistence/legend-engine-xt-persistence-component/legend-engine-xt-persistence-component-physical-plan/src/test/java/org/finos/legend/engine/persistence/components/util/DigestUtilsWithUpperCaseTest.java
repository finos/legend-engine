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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DigestUtilsWithUpperCaseTest extends DigestUtilsBase
{

    @Test
    public void testDigestLogicWithLowerCaseFieldNames()
    {
        SchemaDefinition def = getSchemaDefInLower();
        Object[] values = new Object[]{"test data", true, 33, 1111L, 1.5d, null};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(expectedDigest, digest);
        String digestExp = "891861fb5ba587ecf5287fc5d2af277b";
        Assertions.assertEquals(digestExp, digest);

        // Change the value and see if digest changes
        values[0] = null;
        digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(digestExp, digest);
    }

    @Test
    public void testDigestLogicWithUpperCaseFieldNames()
    {
        SchemaDefinition def = getSchemaDefInLower();
        Object[] values = new Object[]{"test data", true, 33, 1111L, 1.5d, null};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, true);
        Assertions.assertEquals(expectedDigest, digest);

        // Change the value and see if digest changes
        values[0] = null;
        values[0] = null;
        digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(expectedDigest, digest);
    }
}