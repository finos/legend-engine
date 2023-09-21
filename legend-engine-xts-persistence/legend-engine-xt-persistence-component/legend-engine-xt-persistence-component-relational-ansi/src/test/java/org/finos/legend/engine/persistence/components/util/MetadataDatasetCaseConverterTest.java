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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetCaseConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class MetadataDatasetCaseConverterTest
{

    private static String databaseName = "myDb";
    private static String groupName = "mySchema";
    private static String datasetName = "metadata";
    private static String tableNameField = "tableName";
    private static String batchStartTimeField = "batch_start_ts_utc";
    private static String batchEndTimeField = "batch_end_ts_utc";
    private static String batchStatusField = "batch_status";
    private static String tableBatchIdField = "table_batch_id";

    @Test
    public void testMetadataDatasetCaseConverter()
    {
        DatasetCaseConverter converter = new DatasetCaseConverter();
        MetadataDataset metadataDataset = MetadataDataset.builder()
            .metadataDatasetDatabaseName(databaseName)
            .metadataDatasetGroupName(groupName)
            .metadataDatasetName(datasetName)
            .tableNameField(tableNameField)
            .build();
        // Note that some fields are intentionally omitted in the builder to test on behavior for default fields

        // Convert everything to upper case
        MetadataDataset convertedDataset = converter.applyCaseOnMetadataDataset(metadataDataset, String::toUpperCase);
        assertConvertedDataset(convertedDataset, String::toUpperCase);

        // Convert everything back to lower case
        convertedDataset = converter.applyCaseOnMetadataDataset(convertedDataset, String::toLowerCase);
        assertConvertedDataset(convertedDataset, String::toLowerCase);
    }

    private void assertConvertedDataset(MetadataDataset convertedDataset, Function<String, String> desiredCasing)
    {
        Assertions.assertEquals(desiredCasing.apply(databaseName), convertedDataset.metadataDatasetDatabaseName().get());
        Assertions.assertEquals(desiredCasing.apply(groupName), convertedDataset.metadataDatasetGroupName().get());
        Assertions.assertEquals(desiredCasing.apply(datasetName), convertedDataset.metadataDatasetName());
        Assertions.assertEquals(desiredCasing.apply(tableNameField), convertedDataset.tableNameField());
        Assertions.assertEquals(desiredCasing.apply(batchStartTimeField), convertedDataset.batchStartTimeField());
        Assertions.assertEquals(desiredCasing.apply(batchEndTimeField), convertedDataset.batchEndTimeField());
        Assertions.assertEquals(desiredCasing.apply(batchStatusField), convertedDataset.batchStatusField());
        Assertions.assertEquals(desiredCasing.apply(tableBatchIdField), convertedDataset.tableBatchIdField());
    }
}
