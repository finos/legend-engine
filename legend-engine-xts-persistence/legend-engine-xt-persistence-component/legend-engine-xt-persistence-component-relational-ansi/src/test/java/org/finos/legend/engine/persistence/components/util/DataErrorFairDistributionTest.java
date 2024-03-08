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
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
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
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUE, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.TYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUE, ErrorCategory.CHECK_NULL_CONSTRAINT, 5, 5, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.TYPE_CONVERSION, ErrorCategory.TYPE_CONVERSION, 5, 5, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(20, 10, dataErrorsByCategory);
        Assertions.assertEquals(10, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.CHECK_NULL_CONSTRAINT.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.TYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    @Test
    public void testExhaustingOneCategory()
    {
        AnsiSqlSink sink = (AnsiSqlSink) AnsiSqlSink.get();

        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUE, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.TYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUE, ErrorCategory.CHECK_NULL_CONSTRAINT, 5, 5, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.TYPE_CONVERSION, ErrorCategory.TYPE_CONVERSION, 50, 15, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(20, 55, dataErrorsByCategory);
        Assertions.assertEquals(20, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.CHECK_NULL_CONSTRAINT.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.TYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    @Test
    public void testExhaustingBothCategories()
    {
        AnsiSqlSink sink = (AnsiSqlSink) AnsiSqlSink.get();

        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        dataErrorsByCategory.put(ValidationCategory.NULL_VALUE, new LinkedList<>());
        dataErrorsByCategory.put(ValidationCategory.TYPE_CONVERSION, new LinkedList<>());
        List<DataError> expectedNullValuesErrors = new ArrayList<>();
        List<DataError> expectedDatatypeErrors = new ArrayList<>();

        populateDataErrors(ValidationCategory.NULL_VALUE, ErrorCategory.CHECK_NULL_CONSTRAINT, 15, 10, dataErrorsByCategory, expectedNullValuesErrors);
        populateDataErrors(ValidationCategory.TYPE_CONVERSION, ErrorCategory.TYPE_CONVERSION, 20, 9, dataErrorsByCategory, expectedDatatypeErrors);

        List<DataError> results = sink.getDataErrorsWithFairDistributionAcrossCategories(19, 35, dataErrorsByCategory);
        Assertions.assertEquals(19, results.size());
        Assertions.assertEquals(expectedNullValuesErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.CHECK_NULL_CONSTRAINT.name())).collect(Collectors.toList()));
        Assertions.assertEquals(expectedDatatypeErrors, results.stream().filter(error -> error.errorCategory().equals(ErrorCategory.TYPE_CONVERSION.name())).collect(Collectors.toList()));
    }

    private void populateDataErrors(ValidationCategory validationCategory, ErrorCategory errorCategory, int totalCount, int expectedCount, Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory, List<DataError> expectedList)
    {
        int count = 1;
        while (count <= totalCount)
        {
            DataError dataError = getDummyDataError(errorCategory, count);
            dataErrorsByCategory.get(validationCategory).add(dataError);
            if (count <= expectedCount)
            {
                expectedList.add(dataError);
            }
            count++;
        }
    }

    private DataError getDummyDataError(ErrorCategory category, long rowNumber)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(DataError.FILE_NAME, "some_file_name");
        errorDetails.put(DataError.RECORD_NUMBER, rowNumber);
        errorDetails.put(DataError.COLUMN_NAME, "some_column_name");

        return DataError.builder()
            .errorCategory(category)
            .putAllErrorDetails(errorDetails)
            .errorRecord("some_data")
            .errorMessage("some_error_message")
            .build();
    }
}
