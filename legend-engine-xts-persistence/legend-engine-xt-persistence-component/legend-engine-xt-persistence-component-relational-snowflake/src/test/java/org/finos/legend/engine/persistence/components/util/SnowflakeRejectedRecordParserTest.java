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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.SnowflakeStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.StandardFileFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SnowflakeRejectedRecordParserTest
{
    private static Field col1 = Field.builder()
        .name("col_int")
        .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
        .build();
    private static Field col2 = Field.builder()
        .name("col_string")
        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
        .build();
    private static Field col3 = Field.builder()
        .name("col_decimal")
        .type(FieldType.of(DataType.DECIMAL, Optional.empty(), Optional.empty()))
        .columnNumber(4)
        .build();
    private static Field col4 = Field.builder()
        .name("col_timestamp")
        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
        .columnNumber(5)
        .build();

    private List filesList = Arrays.asList("/path/xyz/file1.csv", "/path/xyz/file2.csv");

    @Test
    public void testSnowflakeRejectedRecordParserDefaultOptions() throws IOException
    {
        SnowflakeSink sink = (SnowflakeSink) SnowflakeSink.get();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                SnowflakeStagedFilesDatasetProperties.builder()
                    .location("my_location")
                    .fileFormat(StandardFileFormat.builder()
                        .formatType(FileFormatType.CSV)
                        .build())
                    .addAllFilePatterns(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        String commaSeparatedCsvLine = "1,Andy,1.23,2022-01-12 00:00:00.0";
        String expectedJsonString = "{" +
            "\"col_timestamp\":\"2022-01-12 00:00:00.0\"," +
            "\"col_int\":\"1\"," +
            "\"col_decimal\":\"1.23\"," +
            "\"col_string\":\"Andy\"}";
        String actualJsonString = sink.parseSnowflakeRejectedRecord(datasets, commaSeparatedCsvLine);
        Assertions.assertEquals(expectedJsonString, actualJsonString);
    }

    @Test
    public void testSnowflakeRejectedRecordParserWithFileFormatOptions() throws IOException
    {
        SnowflakeSink sink = (SnowflakeSink) SnowflakeSink.get();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                SnowflakeStagedFilesDatasetProperties.builder()
                    .location("my_location")
                    .fileFormat(StandardFileFormat.builder()
                        .formatType(FileFormatType.CSV)
                        .putFormatOptions("FIELD_DELIMITER", ",")
                        .putFormatOptions("FIELD_OPTIONALLY_ENCLOSED_BY", '"')
                        .putFormatOptions("ESCAPE", "\\")
                        .build())
                    .addAllFilePatterns(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        String commaSeparatedCsvLine = "1,\"Andy, Soo\",1.23,\"2022-01-12 00:00:00.0\"";

        String expectedJsonString = "{" +
            "\"col_timestamp\":\"2022-01-12 00:00:00.0\"," +
            "\"col_int\":\"1\"," +
            "\"col_decimal\":\"1.23\"," +
            "\"col_string\":\"Andy, Soo\"}";
        String actualJsonString = sink.parseSnowflakeRejectedRecord(datasets, commaSeparatedCsvLine);
        Assertions.assertEquals(expectedJsonString, actualJsonString);
    }
}
