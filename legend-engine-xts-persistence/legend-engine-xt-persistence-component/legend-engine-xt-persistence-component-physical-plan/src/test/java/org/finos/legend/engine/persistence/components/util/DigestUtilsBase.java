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

import java.util.Optional;

public class DigestUtilsBase
{

    protected String expectedDigest = "fd40b241c6d2eb55348e3bc51e81925b";
    Field f1 = Field.builder().name("COLUMN_1").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
    Field f2 = Field.builder().name("COLUMN_2").type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();
    Field f3 = Field.builder().name("COLUMN_3").type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).build();
    Field f4 = Field.builder().name("COLUMN_4").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
    Field f5 = Field.builder().name("COLUMN_5").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    Field f6 = Field.builder().name("COLUMN_6").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();

    protected SchemaDefinition getSchemaDef()
    {
        return SchemaDefinition.builder()
            .addFields(f1)
            .addFields(f2)
            .addFields(f3)
            .addFields(f4)
            .addFields(f5)
            .addFields(f6)
            .build();
    }

    protected SchemaDefinition getUnsortedSchemaDef()
    {
        return SchemaDefinition.builder()
            .addFields(f2)
            .addFields(f1)
            .addFields(f4)
            .addFields(f3)
            .addFields(f6)
            .addFields(f5)
            .build();
    }

    protected SchemaDefinition getSchemaDefInLower()
    {

        Field f1 = Field.builder().name("column_1").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
        Field f2 = Field.builder().name("column_2").type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();
        Field f3 = Field.builder().name("column_3").type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).build();
        Field f4 = Field.builder().name("column_4").type(FieldType.of(DataType.DECIMAL, Optional.empty(), Optional.empty())).build();
        Field f5 = Field.builder().name("column_5").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
        Field f6 = Field.builder().name("column_6").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
        return SchemaDefinition.builder()
            .addFields(f1)
            .addFields(f2)
            .addFields(f3)
            .addFields(f4)
            .addFields(f5)
            .addFields(f6)
            .build();
    }
}
