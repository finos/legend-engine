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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class DigestUtilsTest extends DigestUtilsBase
{

    @Test
    public void testDigestLogic()
    {
        SchemaDefinition def = getSchemaDef();
        Object[] values = new Object[]{"test data", true, 33, 1111L, 1.5d, null};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertEquals(expectedDigest, digest);

        // Change the value and see if digest changes
        values[0] = null;
        digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(expectedDigest, digest);
    }

    @Test
    public void testDigestLogicWithSchemaUnsorted()
    {
        SchemaDefinition def = getUnsortedSchemaDef();
        Object[] values = new Object[]{true, "test data", 1111L, 33, null, 1.5d};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertEquals(expectedDigest, digest);

        // Change the value and see if digest changes
        values[0] = null;
        digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(expectedDigest, digest);
    }

    @Test
    public void testDigestLogicWithNullValues()
    {
        // No impact of adding a field with null values
        Field f7 = Field.builder().name("COLUMN_7").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();

        SchemaDefinition def = getSchemaDef();
        List<Field> newFields = new ArrayList<>(def.fields());
        newFields.add(f7);
        def = def.withFields(newFields);

        Object[] values = new Object[]{"test data", true, 33, 1111L, 1.5d, null, null};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertEquals(expectedDigest, digest);
    }

    @Test
    public void testDigestLogicWithEmptyString()
    {
        // Digest will change on adding empty string
        SchemaDefinition def = getSchemaDef();
        Field f7 = Field.builder().name("COLUMN_7").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
        List<Field> newFields = new ArrayList<>(def.fields());
        newFields.add(f7);
        def = def.withFields(newFields);
        Object[] values = new Object[]{"test data", true, 33, 1111L, 1.5d, null, ""};
        DigestContext context = DigestUtils.getDigestContext(def, null);
        String digest = DigestUtils.getDigest(values, context, false);
        Assertions.assertNotEquals(expectedDigest, digest);
    }

}