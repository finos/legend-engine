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

import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class DataErrorFairDistributionTest
{
    @Test
    public void testTotalErrorsSmallerThanSampleRowCount()
    {
        AnsiSqlSink sink = (AnsiSqlSink) AnsiSqlSink.get();

        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUES, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.DATATYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUES, 5, 5, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.DATATYPE_CONVERSION, 5, 5, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(20, dataErrorsByCategory);
        Assertions.assertEquals(10, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.NULL_VALUES.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.DATATYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    @Test
    public void testExhaustingOneCategory()
    {
        AnsiSqlSink sink = (AnsiSqlSink) AnsiSqlSink.get();

        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUES, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.DATATYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUES, 5, 5, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.DATATYPE_CONVERSION, 50, 15, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(20, dataErrorsByCategory);
        Assertions.assertEquals(20, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.NULL_VALUES.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.DATATYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    @Test
    public void testExhaustingBothCategories()
    {
        AnsiSqlSink sink = (AnsiSqlSink) AnsiSqlSink.get();

        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUES, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.DATATYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUES, 15, 10, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.DATATYPE_CONVERSION, 20, 9, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(19, dataErrorsByCategory);
        Assertions.assertEquals(19, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.NULL_VALUES.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ValidationCategory.DATATYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    private void populateDataErrors(ValidationCategory category, int totalCount, int expectedCount, Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory, List<DataError> expectedList)
    {
        int count = 1;
        while (count <= totalCount)
        {
            DataError dataError = getDummyDataError(category, count);
            dataErrorsByCategory.get(category).add(dataError);
            if (count <= expectedCount)
            {
                expectedList.add(dataError);
            }
            count++;
        }
    }

    private DataError getDummyDataError(ValidationCategory category, long rowNumber)
    {
        return DataError.builder()
            .file("some_file_name")
            .errorCategory(category.name())
            .rowNumber(rowNumber)
            .columnName("some_column_name")
            .rejectedRecord("some_data")
            .errorMessage(category.getValidationFailedErrorMessage())
            .build();
    }
}
