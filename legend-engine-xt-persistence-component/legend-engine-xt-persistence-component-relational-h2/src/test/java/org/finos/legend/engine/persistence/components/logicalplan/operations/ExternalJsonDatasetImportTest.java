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
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.importer.Importer;
import org.finos.legend.engine.persistence.components.importer.Importers;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.util.DigestContext;
import org.finos.legend.engine.persistence.components.util.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.stagingTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

public class ExternalJsonDatasetImportTest extends BaseTest
{

    @Test
    void testImportJsonData() throws Exception
    {
        String jsonPath = "src/test/resources/data/import-data/data_pass1.json";
        String csvPath = "src/test/resources/data/import-data/data_expected_no_digest.csv";

        JsonExternalDatasetReference jsonExternalDatasetReference = TestUtils.getJsonDatasetWithoutDigest(jsonPath, testDatabaseName, testSchemaName, stagingTableName);

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        RelationalExecutor executor = new RelationalExecutor(H2Sink.get(), h2Sink);

        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(jsonExternalDatasetReference.getDatasetDefinition(), false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        Importer importer = Importers.forExternalDatasetReference(jsonExternalDatasetReference, transformer, executor);
        importer.importData(jsonExternalDatasetReference, null);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName};
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST_DB\".\"TEST\".\"staging\"");
        TestUtils.assertFileAndTableDataEquals(schema, csvPath, tableData);
    }

    @Test
    void testImportJsonDataWithPopulateDigest() throws Exception
    {
        String jsonPath = "src/test/resources/data/import-data/data_pass1.json";
        String csvPath = "src/test/resources/data/import-data/data_expected_with_digest_pass1.csv";

        JsonExternalDatasetReference jsonExternalDatasetReference = TestUtils.getJsonDatasetWithDigest(jsonPath, TEST_DATABASE, "my_staging_table", TEST_SCHEMA, "staging");

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        RelationalExecutor executor = new RelationalExecutor(H2Sink.get(), h2Sink);

        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(jsonExternalDatasetReference.getDatasetDefinition(), false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        Importer importer = Importers.forExternalDatasetReference(jsonExternalDatasetReference, transformer, executor);
        DigestInfo digestInfo = DigestInfo.builder().populateDigest(true).digestField("digest").addMetaFields("digest").build();
        importer.importData(jsonExternalDatasetReference, digestInfo);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST_DB\".\"TEST\".\"my_staging_table\"");
        TestUtils.assertFileAndTableDataEquals(schema, csvPath, tableData);
    }

    @Test
    public void testDigestLogicForARow()
    {
        SchemaDefinition def = getStagingSchemaWithoutDigest();
        Object[] values = new Object[]{1, "HARRY", 1000, "2020-01-01 00:00:00.0", "2022-12-01"};
        DigestContext context = DigestUtils.getDigestContext(def, Collections.singleton("digest"));
        String digest = DigestUtils.getDigest(values, context, false);
        String expectedDigest = "ec557ebad89621a74ee47c6520bf7b74";
        Assertions.assertEquals(expectedDigest, digest);

        // Change the value and see if digest changes
        digest = DigestUtils.getDigest(values, context, true);
        String expectedDigestUpperCase = "c8fbb5a3a1e1d0a0f633fcdd1e82ca27";
        Assertions.assertEquals(expectedDigestUpperCase, digest);
    }

    @Test
    public void testDigestLogicForARowWithMetaRows()
    {
        Field metaColumn = Field.builder().name("meta_column").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();

        SchemaDefinition def = getStagingSchema();
        List<Field> newFields = new ArrayList<>(def.fields());
        newFields.add(metaColumn);
        def = def.withFields(newFields);

        Object[] values = new Object[]{1, "HARRY", 1000, "2020-01-01 00:00:00.0", "2022-12-01", "digest123", "meta_value"};
        DigestContext context = DigestUtils.getDigestContext(def, new HashSet<>(Arrays.asList("digest", "meta_column")));
        String digest = DigestUtils.getDigest(values, context, false);
        String expectedDigest = "ec557ebad89621a74ee47c6520bf7b74";
        Assertions.assertEquals(expectedDigest, digest);

        // Change the value and see if digest changes
        digest = DigestUtils.getDigest(values, context, true);
        String expectedDigestUpperCase = "c8fbb5a3a1e1d0a0f633fcdd1e82ca27";
        Assertions.assertEquals(expectedDigestUpperCase, digest);
    }

    @Test
    public void testImportJsonDataWithNullValues() throws IOException
    {
        String jsonPath = "src/test/resources/data/import-data/data_pass3.json";
        String csvPath = "src/test/resources/data/import-data/data_expected_with_null_values_pass3.csv";

        JsonExternalDatasetReference jsonExternalDatasetReference = TestUtils.getJsonDatasetWithDigest(jsonPath, TEST_DATABASE, "my_staging_table", TEST_SCHEMA, "staging");

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        RelationalExecutor executor = new RelationalExecutor(H2Sink.get(), h2Sink);

        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(jsonExternalDatasetReference.getDatasetDefinition(), false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        Importer importer = Importers.forExternalDatasetReference(jsonExternalDatasetReference, transformer, executor);
        DigestInfo digestInfo = DigestInfo.builder().populateDigest(true).digestField("digest").addMetaFields("digest").build();
        importer.importData(jsonExternalDatasetReference, digestInfo);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST_DB\".\"TEST\".\"my_staging_table\"");
        TestUtils.assertFileAndTableDataEquals(schema, csvPath, tableData);
    }
}
