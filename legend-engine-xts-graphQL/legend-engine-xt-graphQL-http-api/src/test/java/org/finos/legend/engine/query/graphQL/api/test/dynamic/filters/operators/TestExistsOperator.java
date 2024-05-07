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

public class TestExistsOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void testSimpleExists() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       employees: { _exists : { firstName: { _eq: \"Peter\" } }}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected);
    }

    @Test
    public void testNestedExists() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       employees: { _exists : { employer: { legalName: { _eq: \"Firm X\" } } }}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected);
    }
}
