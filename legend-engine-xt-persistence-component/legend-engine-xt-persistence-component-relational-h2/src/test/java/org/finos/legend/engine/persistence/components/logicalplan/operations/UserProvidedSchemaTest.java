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

package org.finos.legend.engine.persistence.components.logicalplan.operations;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdIn;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOut;
import static org.finos.legend.engine.persistence.components.TestUtils.digest;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDate;
import static org.finos.legend.engine.persistence.components.TestUtils.id;
import static org.finos.legend.engine.persistence.components.TestUtils.income;
import static org.finos.legend.engine.persistence.components.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.name;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTime;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;
import static org.finos.legend.engine.persistence.components.TestUtils.tinyIntId;

public class UserProvidedSchemaTest extends BaseTest
{

    @Test
    void testUserProvidedSchemaLessColumns() throws Exception
    {

        Dataset userProvidedDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .build()
            )
            .build();

        Dataset actualDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        // Create the physical main table
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan createMainTable = LogicalPlan.builder().addOps(Create.of(true, actualDataset)).build();
        SqlPlan physicalPlanCreateMainTable = transformer.generatePhysicalPlan(createMainTable);
        executor.executePhysicalPlan(physicalPlanCreateMainTable);

        try
        {
            executor.validateMainDatasetSchema(userProvidedDataset);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Number of columns in user-provided schema doesn't match with the schema in the database", e.getMessage());
        }
    }

    @Test
    void testUserProvidedSchemaMoreColumns() throws Exception
    {

        Dataset userProvidedDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();

        Dataset actualDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        // Create the physical main table
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan createMainTable = LogicalPlan.builder().addOps(Create.of(true, actualDataset)).build();
        SqlPlan physicalPlanCreateMainTable = transformer.generatePhysicalPlan(createMainTable);
        executor.executePhysicalPlan(physicalPlanCreateMainTable);

        try
        {
            executor.validateMainDatasetSchema(userProvidedDataset);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Number of columns in user-provided schema doesn't match with the schema in the database", e.getMessage());
        }
    }

    @Test
    void testUserProvidedSchemaColumnNameMismatch() throws Exception
    {

        Field userNameField = Field.builder().name("biz_name").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).fieldAlias(nameName).build();
        Dataset userProvidedDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(userNameField)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        Dataset actualDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        // Create the physical main table
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan createMainTable = LogicalPlan.builder().addOps(Create.of(true, actualDataset)).build();
        SqlPlan physicalPlanCreateMainTable = transformer.generatePhysicalPlan(createMainTable);
        executor.executePhysicalPlan(physicalPlanCreateMainTable);

        try
        {
            executor.validateMainDatasetSchema(userProvidedDataset);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Column in user-provided schema doesn't match any column in the schema in the database", e.getMessage());
        }
    }

    @Test
    void testUserProvidedSchemaColumnDataTypeMismatch() throws Exception
    {

        Dataset userProvidedDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(tinyIntId)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        Dataset actualDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        // Create the physical main table
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan createMainTable = LogicalPlan.builder().addOps(Create.of(true, actualDataset)).build();
        SqlPlan physicalPlanCreateMainTable = transformer.generatePhysicalPlan(createMainTable);
        executor.executePhysicalPlan(physicalPlanCreateMainTable);

        try
        {
            executor.validateMainDatasetSchema(userProvidedDataset);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Column in user-provided schema doesn't match the corresponding column in the schema in the database", e.getMessage());
        }
    }

    @Test
    void testUserProvidedSchemaColumnConstraintMismatch() throws Exception
    {
        Field userDigestField = Field.builder().name(digestName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).nullable(false).fieldAlias(digestName).build();
        Dataset userProvidedDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(userDigestField)
                .build()
            )
            .build();

        Dataset actualDataset = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build()
            )
            .build();

        // Create the physical main table
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan createMainTable = LogicalPlan.builder().addOps(Create.of(true, actualDataset)).build();
        SqlPlan physicalPlanCreateMainTable = transformer.generatePhysicalPlan(createMainTable);
        executor.executePhysicalPlan(physicalPlanCreateMainTable);

        try
        {
            executor.validateMainDatasetSchema(userProvidedDataset);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Column in user-provided schema doesn't match the corresponding column in the schema in the database", e.getMessage());
        }
    }
}
