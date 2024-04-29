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
import org.junit.Ignore;

public class TestInOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_String_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { firstName: { _in: [\"John\"] } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_Int_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { age: { _in: [25, 40, 55] } }) {\n" +
                "      fullName," +
                "      age" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"age\":25},{\"fullName()\":\"Anthony Allen\",\"age\":40},{\"fullName()\":\"David Harris\",\"age\":55}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_Float_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { bankBalance: { _in: [5000.0] } }) {\n" +
                "      fullName," +
                "      bankBalance" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Fabrice Roberts\",\"bankBalance\":5000.0}}}";
        runTest(query, expected);
    }

    @Test
    public void test_Boolean_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { isAFullTimeEmployee: { _in: [true] } }) {\n" +
                "      fullName," +
                "      isAFullTimeEmployee" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"John Hill\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"Fabrice Roberts\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"David Harris\",\"isAFullTimeEmployee\":true}]}}";
        runTest(query, expected);
    }

    @Test
    @Ignore(value = "Ignored as collection of enums is not supported as service parameter")
    public void test_Enum_in() throws Exception
    {
        String query = "query Query {\n" +
                "  firms(where: { firmType: { _in: [LLC] } }) {\n" +
                "      legalName," +
                "      firmType" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm A\",\"firmType\":\"LLC\"}}}";
        runTest(query, expected);
    }

    @Test
    public void test_StrictDate_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { dateOfBirth: { _in: [\"1999-01-28\"] } }) {\n" +
                "      fullName," +
                "      dateOfBirth" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Peter Smith\",\"dateOfBirth\":\"1999-01-28\"}}}";
        runTest(query, expected);
    }

    @Test
    public void test_DateTime_in() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { modifiedAt: { _in: [\"2024-01-26 12:00:00\"] } }) {\n" +
                "      fullName," +
                "      modifiedAt" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Anthony Allen\",\"modifiedAt\":\"2024-01-26T12:00:00.000000000\"}}}";
        runTest(query, expected);
    }
}
