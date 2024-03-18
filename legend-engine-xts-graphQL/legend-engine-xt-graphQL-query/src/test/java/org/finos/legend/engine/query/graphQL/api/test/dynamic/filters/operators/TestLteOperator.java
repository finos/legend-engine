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

package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestLteOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_Int_lte() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { age: { _lte: 35 } }) {\n" +
                "      fullName," +
                "      age" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"age\":25},{\"fullName()\":\"John Johnson\",\"age\":35},{\"fullName()\":\"John Hill\",\"age\":30}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_Float_lte() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { bankBalance: { _lte: 3000.0 } }) {\n" +
                "      fullName," +
                "      bankBalance" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"bankBalance\":2500.0},{\"fullName()\":\"John Hill\",\"bankBalance\":3000.0}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_StrictDate_lte() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { dateOfBirth: { _lte: \"1974-01-28\" } }) {\n" +
                "      fullName," +
                "      dateOfBirth" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Fabrice Roberts\",\"dateOfBirth\":\"1974-01-28\"},{\"fullName()\":\"Oliver Hill\",\"dateOfBirth\":\"1964-01-28\"},{\"fullName()\":\"David Harris\",\"dateOfBirth\":\"1969-01-28\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_DateTime_lte() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { modifiedAt: { _lte: \"2024-01-25 12:00:00\" } }) {\n" +
                "      fullName," +
                "      modifiedAt" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Oliver Hill\",\"modifiedAt\":\"2024-01-25T12:00:00.000000000\"},{\"fullName()\":\"David Harris\",\"modifiedAt\":\"2024-01-25T00:00:00.000000000\"}]}}";
        runTest(query, expected);
    }
}
